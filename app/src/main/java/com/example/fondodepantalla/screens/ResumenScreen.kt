@file:OptIn(ExperimentalMaterial3Api::class) // 1. Añadido OptIn
package com.example.fondodepantalla.screens // 2. Paquete actualizado

import android.app.Activity
import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.create
import org.json.JSONArray
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
// 3. Importaciones de Fragment, LayoutInflater, etc., eliminadas

// 4. La clase 'ResumenFragment' ha sido eliminada

@Composable
fun ResumenScreen() {
    // 5. El cliente OkHttp ahora se inicializa con remember
    val client = remember { OkHttpClient() }

    val context = LocalContext.current
    var fechaInicio by remember { mutableStateOf("") }
    var fechaFin by remember { mutableStateOf("") }
    var listaResumen by remember { mutableStateOf(listOf<ResumenItem>()) }
    var isLoading by remember { mutableStateOf(false) }

    val calendar = Calendar.getInstance()
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Función para abrir DatePicker (sin cambios)
    fun showDatePicker(onDateSelected: (String) -> Unit) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                onDateSelected(formatter.format(calendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // Función para traer los datos (sin cambios, captura el 'client' y 'context' de Compose)
    fun fetchResumen(inicio: String, fin: String) {
        isLoading = true
        val url = "https://altecomasistencia.onrender.com/api/resumen"
        val json = "{\"inicio\":\"$inicio\",\"fin\":\"$fin\"}"
        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json)
        val request = Request.Builder().url(url).post(body).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                (context as? Activity)?.runOnUiThread {
                    Toast.makeText(context, "Error al conectar", Toast.LENGTH_SHORT).show()
                    isLoading = false
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful && response.body != null) {
                    val res = response.body!!.string()
                    val tmpList = mutableListOf<ResumenItem>()
                    try { // 6. (Recomendado) Añadir un try-catch para el parseo de JSON
                        val jsonArray = JSONArray(res)
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            tmpList.add(
                                ResumenItem(
                                    nombre = obj.getString("nombre"),
                                    asistencias = obj.getInt("asistencias"),
                                    retardos = obj.getInt("retardos"),
                                    faltas = obj.getInt("faltas")
                                )
                            )
                        }
                        (context as? Activity)?.runOnUiThread {
                            listaResumen = tmpList
                            isLoading = false
                        }
                    } catch (e: Exception) {
                        (context as? Activity)?.runOnUiThread {
                            Toast.makeText(context, "Error al procesar datos", Toast.LENGTH_SHORT).show()
                            isLoading = false
                        }
                    }
                } else {
                    (context as? Activity)?.runOnUiThread {
                        Toast.makeText(context, "Error en respuesta", Toast.LENGTH_SHORT).show()
                        isLoading = false
                    }
                }
            }
        })
    }

    // --- UI de Compose (sin cambios) ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Resumen de Asistencias",
            style = MaterialTheme.typography.headlineSmall, // 7. Usando style de M3
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(16.dp))

        // Campos de fecha
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .clickable { showDatePicker { fechaInicio = it } }
                    .padding(12.dp)
            ) {
                Text(
                    text = if (fechaInicio.isEmpty()) "Desde" else fechaInicio,
                    color = if (fechaInicio.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .clickable { showDatePicker { fechaFin = it } }
                    .padding(12.dp)
            ) {
                Text(
                    text = if (fechaFin.isEmpty()) "Hasta" else fechaFin,
                    color = if (fechaFin.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Botón consultar
        Button(
            onClick = {
                if (fechaInicio.isNotEmpty() && fechaFin.isNotEmpty()) {
                    fetchResumen(fechaInicio, fechaFin)
                } else {
                    Toast.makeText(context, "Selecciona las fechas", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Consultar", fontSize = 16.sp)
        }

        Spacer(Modifier.height(16.dp))

        // Leyenda de colores
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            LegendItem(Color(0xFF4CAF50), "Asistencias")
            LegendItem(Color(0xFFFF9800), "Retardos")
            LegendItem(Color(0xFFF44336), "Faltas")
        }

        Spacer(Modifier.height(12.dp))

        // Lista de resultados
        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(listaResumen) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(item.nombre, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                StatBox(Color(0xFF4CAF50), "${item.asistencias}")
                                StatBox(Color(0xFFFF9800), "${item.retardos}")
                                StatBox(Color(0xFFF44336), "${item.faltas}")
                            }
                        }
                    }
                }
            }
        }
    }
}

// 8. Data class movida a nivel de archivo
private data class ResumenItem(
    val nombre: String,
    val asistencias: Int,
    val retardos: Int,
    val faltas: Int
)

// 9. Composables de ayuda movidos a nivel de archivo y hechos privados
@Composable
private fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(color, RoundedCornerShape(3.dp))
        )
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatBox(color: Color, text: String) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, color = color, fontWeight = FontWeight.Bold)
    }
}