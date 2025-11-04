package com.example.fondodepantalla.FragmentosAdministrador

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

class InicioAdmin : Fragment() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    InicioAdminScreen(db)
                }
            }
        }
    }
}

@Composable
fun InicioAdminScreen(db: FirebaseFirestore) {
    var tareas by remember { mutableStateOf(listOf<Int>()) }
    var inventario by remember { mutableStateOf(listOf<Pair<String, Int>>()) }

    LaunchedEffect(Unit) {
        tareas = cargarTareas(db)
        inventario = cargarInventario(db)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()) //permite hacer scroll
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Panel general", style = MaterialTheme.typography.titleLarge)

        if (tareas.isEmpty() && inventario.isEmpty()) {
            CircularProgressIndicator()
        } else {
            if (tareas.isNotEmpty()) {
                TarjetaGrafico(
                    titulo = "Progreso de tareas",
                    etiquetas = listOf("Completadas", "En proceso", "Faltantes"),
                    valores = tareas
                )
            }

            if (inventario.isNotEmpty()) {
                TarjetaGraficoBarras(
                    titulo = "Inventario general",
                    datos = inventario
                )
            }
        }
    }
}

// -------------------- FIRESTORE --------------------

suspend fun cargarTareas(db: FirebaseFirestore): List<Int> {
    val snapshot = db.collection("tareas").get().await()
    var completadas = 0
    var enProceso = 0
    var faltantes = 0

    for (doc in snapshot) {
        val taken = doc.getBoolean("taken") ?: false
        val progress = doc.getLong("progress") ?: 0
        val evidencia = doc.contains("evidencias")

        when {
            taken && progress == 4L && evidencia -> completadas++
            taken -> enProceso++
            else -> faltantes++
        }
    }
    return listOf(completadas, enProceso, faltantes)
}

suspend fun cargarInventario(db: FirebaseFirestore): List<Pair<String, Int>> {
    val snapshot = db.collection("inventario").get().await()
    return snapshot.mapNotNull {
        val nombre = it.getString("nombre")
        val cantidad = it.getLong("cantidad")?.toInt()
        if (nombre != null && cantidad != null) nombre to cantidad else null
    }
}

// -------------------- UI --------------------

@Composable
fun TarjetaGrafico(titulo: String, etiquetas: List<String>, valores: List<Int>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            AnimatedBarChart(etiquetas, valores)
        }
    }
}

@Composable
fun TarjetaGraficoBarras(titulo: String, datos: List<Pair<String, Int>>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            AnimatedBarChart(datos.map { it.first }, datos.map { it.second })
        }
    }
}

@Composable
fun AnimatedBarChart(labels: List<String>, values: List<Int>) {
    val maxValue = values.maxOrNull()?.toFloat() ?: 1f
    val pastelColors = listOf(
        Color(0xFFBAFFC9), Color(0xFFFFFFBA), Color(0xFFFFBABA),
        Color(0xFFBADCFF), Color(0xFFD5BAFF)
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        values.forEachIndexed { index, value ->
            val progress = (value / maxValue)
            val animatedProgress = animateFloatAsState(targetValue = progress).value

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = labels[index],
                    modifier = Modifier.width(110.dp),
                    style = MaterialTheme.typography.bodyMedium
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(22.dp)
                        .background(
                            color = pastelColors[index % pastelColors.size].copy(alpha = 0.3f),
                            shape = RoundedCornerShape(50)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress)
                            .background(
                                color = pastelColors[index % pastelColors.size],
                                shape = RoundedCornerShape(50)
                            )
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text("${value}")
            }
        }
    }
}
