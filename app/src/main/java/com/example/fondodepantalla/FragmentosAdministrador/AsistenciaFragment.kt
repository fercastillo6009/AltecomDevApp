package com.example.fondodepantalla.FragmentosAdministrador

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.airbnb.lottie.compose.*
import com.example.fondodepantalla.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AsistenciaFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var asistenciaManager: AsistenciaManager
    private lateinit var locationHelper: LocationHelper
    private lateinit var uid: String
    private lateinit var nombre: String
    private val fechaHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View {
        auth = FirebaseAuth.getInstance()
        uid = auth.currentUser?.uid ?: ""
        nombre = auth.currentUser?.displayName ?: "Empleado"
        asistenciaManager = AsistenciaManager(FirebaseFirestore.getInstance(), uid)
        locationHelper = LocationHelper(requireContext())

        return ComposeView(requireContext()).apply {
            setContent {
                AsistenciaScreen(
                    nombre = nombre,
                    uid = uid,
                    fechaHoy = fechaHoy,
                    asistenciaManager = asistenciaManager,
                    locationHelper = locationHelper
                )
            }
        }
    }
}

@Composable
fun AsistenciaScreen(
    nombre: String,
    uid: String,
    fechaHoy: String,
    asistenciaManager: AsistenciaManager,
    locationHelper: LocationHelper
) {
    var entradaRegistrada by remember { mutableStateOf(false) }
    var hora by remember { mutableStateOf("---") }
    var estado by remember { mutableStateOf("No has registrado asistencia") }
    var mostrarSalida by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    // Verificar si ya registró la asistencia
    LaunchedEffect(uid, fechaHoy) {
        asistenciaManager.verificarAsistenciaHoy(fechaHoy) { document ->
            if (document.exists()) {
                entradaRegistrada = true
                mostrarSalida = !document.contains("horaSalida")
                hora = if (document.contains("horaSalida")) {
                    "${document.getString("horaEntrada")} - ${document.getString("horaSalida")}"
                } else {
                    document.getString("horaEntrada") ?: "---"
                }
                estado = document.getString("estado") ?: estado
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Lottie animación
        val composition by rememberLottieComposition(LottieCompositionSpec.Asset("working.json"))
        LottieAnimation(
            composition,
            iterations = LottieConstants.IterateForever,
            modifier = Modifier.size(220.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Card con info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Correo: ${FirebaseAuth.getInstance().currentUser?.email ?: "---"}", style = MaterialTheme.typography.bodyLarge)
                Text(text = "Fecha: $fechaHoy")
                Text(text = "Hora: $hora")
                Text(text = "Estado: $estado")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Botón registrar entrada
        if (!entradaRegistrada) {
            Button(onClick = {
                // Permisos
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(context, "Permiso de ubicación requerido", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                // Obtener ubicación
                locationHelper.getCurrentLocation { location ->
                    if (location == null) {
                        Toast.makeText(context, "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show()
                    } else if (locationHelper.isInsideCompanyArea(location.latitude, location.longitude)) {
                        val horaActual = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                        val estadoEntrada = if (horaActual > "09:15") "retardo" else "puntual"
                        val data = mapOf(
                            "nombre" to nombre,
                            "fecha" to fechaHoy,
                            "horaEntrada" to horaActual,
                            "estado" to estadoEntrada,
                            "latitudEntrada" to location.latitude,
                            "longitudEntrada" to location.longitude
                        )
                        asistenciaManager.registrarEntrada(data,
                            {
                                entradaRegistrada = true
                                mostrarSalida = true
                                hora = horaActual
                                estado = estadoEntrada
                                Toast.makeText(context, "✅ Entrada registrada ($estadoEntrada)", Toast.LENGTH_SHORT).show()
                            },
                            { e -> Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
                        )
                    } else {
                        Toast.makeText(context, "❌ Estás fuera del rango permitido", Toast.LENGTH_SHORT).show()
                    }
                }
            }) {
                Text("Registrar Asistencia")
            }
        }

        // Botón registrar salida
        if (mostrarSalida) {
            Spacer(modifier = Modifier.height(10.dp))
            Button(onClick = {
                locationHelper.getCurrentLocation { location ->
                    if (location == null) {
                        Toast.makeText(context, "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show()
                    } else if (locationHelper.isInsideCompanyArea(location.latitude, location.longitude)) {
                        asistenciaManager.registrarSalida(
                            fechaHoy,
                            location.latitude,
                            location.longitude,
                            {
                                mostrarSalida = false
                                Toast.makeText(context, "🕕 Salida registrada", Toast.LENGTH_SHORT).show()
                                asistenciaManager.actualizarResumenSemanal(fechaHoy)
                            },
                            { e -> Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
                        )
                    } else {
                        Toast.makeText(context, "❌ Estás fuera del rango permitido", Toast.LENGTH_SHORT).show()
                    }
                }
            }) {
                Text("Registrar Salida")
            }
        }
    }
}
