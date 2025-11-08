package com.example.fondodepantalla.FragmentosAdministrador

// --- IMPORTS (Asegúrate de tener todos estos) ---
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.airbnb.lottie.compose.*
import com.example.fondodepantalla.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AsistenciaFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var asistenciaManager: AsistenciaManager
    private lateinit var locationHelper: LocationHelper
    private lateinit var uid: String
    private lateinit var nombre: String
    private val fechaHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // 🌟 CORRECCIÓN 1: La firma de onCreateView debe devolver 'View?' (anulable)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? { // <-- AHORA ES View?
        auth = FirebaseAuth.getInstance()
        uid = auth.currentUser?.uid ?: ""
        nombre = auth.currentUser?.displayName ?: "Empleado"

        // Inicialización de tus clases reales
        asistenciaManager = AsistenciaManager(FirebaseFirestore.getInstance(), uid)
        locationHelper = LocationHelper(requireContext())

        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme(colorScheme = lightColorScheme()) {
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
}

// --- 🌟 CORRECCIÓN 2: Todos los Composables de ayuda se mueven ANTES de AsistenciaScreen ---

@Composable
fun InfoCard(
    email: String,
    fechaHoy: String,
    horaEntrada: String?,
    horaSalida: String?,
    estado: String
) {
    val estadoColor = when (estado.lowercase(Locale.ROOT)) {
        "puntual" -> Color(0xFF4CAF50) // Verde
        "retardo" -> Color(0xFFFF9800) // Naranja
        "sitio_cliente" -> Color(0xFF2196F3) // Azul
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = estado.uppercase(Locale.ROOT),
                style = MaterialTheme.typography.headlineSmall,
                color = estadoColor,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp)
            )

            FilaDato(label = "Correo", value = email)
            Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            FilaDato(label = "Fecha", value = fechaHoy)
            Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Schedule, contentDescription = "Hora", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    FilaDatoSimple(label = "Hora Entrada", value = horaEntrada ?: "---")
                    if (horaSalida != null) {
                        Spacer(Modifier.height(4.dp))
                        FilaDatoSimple(label = "Hora Salida", value = horaSalida)
                    }
                }
            }
        }
    }
}

@Composable
fun FilaDato(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = "$label:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun FilaDatoSimple(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun RegistrarBoton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    ElevatedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(0.75f).height(56.dp),
        colors = ButtonDefaults.elevatedButtonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onPrimary)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary)
    }
}

@Composable
fun ConfirmacionFueraDeRangoDialog(
    tipo: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Confirmación de $tipo") },
        text = { Text(text = "Detectamos que no estás en la oficina. ¿Deseas registrar tu $tipo desde esta ubicación (sitio de cliente)?") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Sí, registrar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// --- FIN DE COMPOSABLES DE AYUDA ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsistenciaScreen(
    nombre: String,
    uid: String,
    fechaHoy: String,
    asistenciaManager: AsistenciaManager,
    locationHelper: LocationHelper
) {
    var entradaRegistrada by remember { mutableStateOf(false) }
    var horaEntrada by remember { mutableStateOf<String?>(null) }
    var horaSalida by remember { mutableStateOf<String?>(null) }
    var estado by remember { mutableStateOf("Verificando...") }
    var mostrarSalida by remember { mutableStateOf(false) }
    var cargando by remember { mutableStateOf(true) }

    var cargandoAccion by remember { mutableStateOf(false) }
    var mostrarDialogoEntradaFuera by remember { mutableStateOf(false) }
    var mostrarDialogoSalidaFuera by remember { mutableStateOf(false) }
    var ubicacionTemporal by remember { mutableStateOf<android.location.Location?>(null) }

    val context = LocalContext.current

    LaunchedEffect(uid, fechaHoy) {
        asistenciaManager.verificarAsistenciaHoy(fechaHoy) { document ->
            cargando = false
            if (document != null && document.exists()) {
                entradaRegistrada = true
                horaEntrada = document.getString("horaEntrada")
                horaSalida = document.getString("horaSalida")
                mostrarSalida = horaSalida == null
                estado = document.getString("estado") ?: "Registrada"
            } else {
                estado = "No has registrado asistencia"
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            MediumTopAppBar(
                title = { Text("Registro de Asistencia", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                // 🌟 CORRECCIÓN 3: Se usa 'paddingValues'
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp)
                .padding(top = 0.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            val composition by rememberLottieComposition(LottieCompositionSpec.Asset("working.json"))
            LottieAnimation(
                composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier.size(180.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // InfoCard (ahora definida arriba)
            InfoCard(
                email = FirebaseAuth.getInstance().currentUser?.email ?: "---",
                fechaHoy = fechaHoy,
                horaEntrada = horaEntrada,
                horaSalida = horaSalida,
                estado = estado
            )

            Spacer(modifier = Modifier.height(30.dp))

            AnimatedVisibility(visible = cargando || cargandoAccion) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            AnimatedContent(
                targetState = when {
                    cargando -> "Cargando"
                    !entradaRegistrada -> "Entrada"
                    mostrarSalida -> "Salida"
                    else -> "Completo"
                },
                label = "Botones de Asistencia",
                transitionSpec = { fadeIn() + slideInVertically { it } togetherWith fadeOut() + slideOutVertically { -it } }
            ) { targetState -> // 'targetState' se infiere correctamente ahora
                when (targetState) {
                    "Entrada" -> RegistrarBoton(
                        label = "Registrar Entrada",
                        icon = Icons.Filled.Login,
                        enabled = !cargandoAccion,
                        onClick = {
                            cargandoAccion = true
                            iniciarRegistroEntrada(
                                context, locationHelper,
                                onInsideArea = { location ->
                                    registrarEntradaEnFirestore(
                                        context, asistenciaManager, location, nombre, fechaHoy, "oficina"
                                    ) {
                                        entradaRegistrada = true
                                        mostrarSalida = true
                                        horaEntrada = it.first
                                        estado = it.second
                                        cargandoAccion = false
                                    }
                                },
                                onOutsideArea = { location ->
                                    ubicacionTemporal = location
                                    mostrarDialogoEntradaFuera = true
                                    cargandoAccion = false
                                },
                                onLocationError = {
                                    cargandoAccion = false
                                }
                            )
                        }
                    )
                    "Salida" -> RegistrarBoton(
                        label = "Registrar Salida",
                        icon = Icons.Filled.Logout,
                        enabled = !cargandoAccion,
                        onClick = {
                            cargandoAccion = true
                            iniciarRegistroSalida( // Reutiliza la misma lógica de inicio
                                context, locationHelper,
                                onInsideArea = { location ->
                                    registrarSalidaEnFirestore(
                                        context, asistenciaManager, location, fechaHoy, "oficina"
                                    ) { hora ->
                                        mostrarSalida = false
                                        horaSalida = hora
                                        cargandoAccion = false
                                    }
                                },
                                onOutsideArea = { location ->
                                    ubicacionTemporal = location
                                    mostrarDialogoSalidaFuera = true
                                    cargandoAccion = false
                                },
                                onLocationError = {
                                    cargandoAccion = false
                                }
                            )
                        }
                    )
                    "Completo" -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.EventAvailable, contentDescription = "Día Completo", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("¡Jornada completada!", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    else -> Spacer(Modifier.height(56.dp))
                }
            }
        }
    }

    if (mostrarDialogoEntradaFuera) {
        ConfirmacionFueraDeRangoDialog(
            tipo = "Entrada",
            onDismiss = { mostrarDialogoEntradaFuera = false },
            onConfirm = {
                mostrarDialogoEntradaFuera = false
                cargandoAccion = true
                ubicacionTemporal?.let { location ->
                    registrarEntradaEnFirestore(
                        context, asistenciaManager, location, nombre, fechaHoy, "sitio_cliente"
                    ) {
                        entradaRegistrada = true
                        mostrarSalida = true
                        horaEntrada = it.first
                        estado = it.second
                        cargandoAccion = false
                    }
                } ?: run {
                    Toast.makeText(context, "Error de ubicación. Intente de nuevo.", Toast.LENGTH_SHORT).show()
                    cargandoAccion = false
                }
            }
        )
    }

    if (mostrarDialogoSalidaFuera) {
        ConfirmacionFueraDeRangoDialog(
            tipo = "Salida",
            onDismiss = { mostrarDialogoSalidaFuera = false },
            onConfirm = {
                mostrarDialogoSalidaFuera = false
                cargandoAccion = true
                ubicacionTemporal?.let { location ->
                    registrarSalidaEnFirestore(
                        context, asistenciaManager, location, fechaHoy, "sitio_cliente"
                    ) { hora ->
                        mostrarSalida = false
                        horaSalida = hora
                        cargandoAccion = false
                    }
                } ?: run {
                    Toast.makeText(context, "Error de ubicación. Intente de nuevo.", Toast.LENGTH_SHORT).show()
                    cargandoAccion = false
                }
            }
        )
    }
}


// --- LÓGICA DE NEGOCIO (REFACTORIZADA) ---

/**
 * 🌟 CORRECCIÓN 4: Lógica de GPS adaptada a la interfaz Java
 */
private fun iniciarRegistroEntrada(
    context: Context,
    locationHelper: LocationHelper,
    onInsideArea: (android.location.Location) -> Unit,
    onOutsideArea: (android.location.Location) -> Unit,
    onLocationError: () -> Unit
) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        Toast.makeText(context, "Permiso de ubicación requerido", Toast.LENGTH_SHORT).show()
        onLocationError()
        return
    }

    // Así se llama a una interfaz de Java desde Kotlin
    locationHelper.getCurrentLocation(object : LocationHelper.OnLocationResultListener {
        override fun onLocationResult(location: android.location.Location?) {
            // Este código se ejecuta cuando el listener (Java) devuelve un resultado
            // Ejecutar en el hilo principal (aunque FusedLocationProvider suele hacerlo)
            (context as? androidx.activity.ComponentActivity)?.runOnUiThread {
                if (location == null) {
                    Toast.makeText(context, "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show()
                    onLocationError()
                } else if (locationHelper.isInsideCompanyArea(location.latitude, location.longitude)) {
                    onInsideArea(location) // Está en la oficina
                } else {
                    onOutsideArea(location) // Está fuera, preguntar
                }
            }
        }
    })
}

private fun registrarEntradaEnFirestore(
    context: Context,
    asistenciaManager: AsistenciaManager,
    location: android.location.Location,
    nombre: String,
    fechaHoy: String,
    tipoRegistro: String,
    onSuccessUI: (Pair<String, String>) -> Unit
) {
    val horaActual = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

    val estadoEntrada = if (tipoRegistro == "oficina") {
        if (horaActual > "09:15") "retardo" else "puntual"
    } else {
        "sitio_cliente"
    }

    val data = mapOf(
        "nombre" to nombre,
        "fecha" to fechaHoy,
        "horaEntrada" to horaActual,
        "estado" to estadoEntrada,
        "tipoEntrada" to tipoRegistro,
        "latitudEntrada" to location.latitude,
        "longitudEntrada" to location.longitude
    )

    asistenciaManager.registrarEntrada(data,
        {
            onSuccessUI(Pair(horaActual, estadoEntrada))
            Toast.makeText(context, "✅ Entrada registrada ($estadoEntrada)", Toast.LENGTH_SHORT).show()
        },
        // 🌟 CORRECCIÓN 5: Especificar el tipo de 'e'
        { e: Exception ->
            Toast.makeText(context, "Error al registrar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    )
}

// Reutiliza iniciarRegistroEntrada ya que la lógica de GPS es idéntica
private fun iniciarRegistroSalida(
    context: Context,
    locationHelper: LocationHelper,
    onInsideArea: (android.location.Location) -> Unit,
    onOutsideArea: (android.location.Location) -> Unit,
    onLocationError: () -> Unit
) {
    iniciarRegistroEntrada(context, locationHelper, onInsideArea, onOutsideArea, onLocationError)
}


private fun registrarSalidaEnFirestore(
    context: Context,
    asistenciaManager: AsistenciaManager,
    location: android.location.Location,
    fechaHoy: String,
    tipoRegistro: String,
    onSuccessUI: (String) -> Unit
) {
    val horaActual = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

    val dataUpdate = mapOf(
        "horaSalida" to horaActual,
        "tipoSalida" to tipoRegistro,
        "latitudSalida" to location.latitude,
        "longitudSalida" to location.longitude
    )

    // 🌟 CORRECCIÓN 6: Llamada a 'actualizarAsistencia'
    // Este método 'actualizarAsistencia' DEBES crearlo en tu clase AsistenciaManager.
    asistenciaManager.actualizarAsistencia(fechaHoy, dataUpdate,
        {
            onSuccessUI(horaActual)
            Toast.makeText(context, "🕕 Salida registrada ($tipoRegistro)", Toast.LENGTH_SHORT).show()
            asistenciaManager.actualizarResumenSemanal(fechaHoy)
        },
        // 🌟 CORRECCIÓN 7: Especificar el tipo de 'e'
        { e: Exception ->
            Toast.makeText(context, "Error al registrar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    )
}

/*
NOTA IMPORTANTE:
Para que 'registrarSalidaEnFirestore' funcione, necesitas añadir el
método 'actualizarAsistencia' a tu clase AsistenciaManager.
El error 'Unresolved reference: actualizarAsistencia' desaparecerá cuando lo agregues.

Este sería el código para AsistenciaManager (si es Kotlin):

fun actualizarAsistencia(fechaDocId: String, data: Map<String, Any>, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
    // Asumo que 'db' es tu instancia de Firestore y 'uid' es el ID del usuario
    db.collection("asistencia").document(uid).collection("registros").document(fechaDocId)
        .update(data)
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { onFailure(it) }
}

Y este es el método 'verificarAsistenciaHoy' que también necesitas (si es Kotlin):

fun verificarAsistenciaHoy(fechaDocId: String, onResult: (DocumentSnapshot?) -> Unit) {
    db.collection("asistencia").document(uid).collection("registros").document(fechaDocId)
        .get()
        .addOnSuccessListener { document -> onResult(document) }
        .addOnFailureListener { onResult(null) }
}
*/