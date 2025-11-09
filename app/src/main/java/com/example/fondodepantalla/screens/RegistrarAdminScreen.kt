@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.fondodepantalla.screens

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.github.gcacace.signaturepad.views.SignaturePad
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
// 3. Importaciones de Fragment, Bundle, etc., eliminadas

// 4. La clase 'RegistrarAdmin : Fragment' ha sido eliminada

/**
 * Pantalla para registrar el progreso de una tarea, subir evidencia y firmar.
 *
 * @param taskId El ID del documento de la tarea en Firestore.
 * @param onTaskCompleted Lambda que se invoca para navegar hacia atrás cuando la tarea se completa.
 */
@Composable
fun RegistrarAdminScreen(
    taskId: String,
    onTaskCompleted: () -> Unit // 5. Parámetro de navegación
) {
    // --- 6. Estado ---
    val db = remember { FirebaseFirestore.getInstance() }
    val context = LocalContext.current

    var evidenciaSubida by remember { mutableStateOf(false) } // Estado del Fragment ahora local
    var currentStep by remember { mutableStateOf(0) }
    var checkExito by remember { mutableStateOf(false) }
    var comentario by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val signaturePadRef = remember { mutableStateOf<SignaturePad?>(null) }
    val labels = listOf("Inicio", "st1", "st2", "st3", "Completa")

    // --- 7. Lanzador de imágenes (Reemplaza al del Fragment) ---
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { evidenciaUri ->
            // Lógica de Cloudinary movida aquí
            MediaManager.get().upload(evidenciaUri)
                .option("folder", "evidencias/$taskId")
                .option("use_filename", true)
                .option("unique_filename", true)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String?) {}
                    override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
                    override fun onSuccess(requestId: String?, resultData: Map<*, *>?) {
                        val evidenciaUrl = resultData?.get("secure_url") as? String
                        db.collection("tareas").document(taskId)
                            .update("evidencias", evidenciaUrl)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Evidencia subida correctamente", Toast.LENGTH_SHORT).show()
                                evidenciaSubida = true // Actualiza el estado de Compose
                            }
                    }
                    override fun onError(requestId: String?, error: ErrorInfo?) {
                        Toast.makeText(context, "Error al subir evidencia: ${error?.description}", Toast.LENGTH_SHORT).show()
                    }
                    override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
                }).dispatch()
        }
    }

    // --- 8. Cargar estado inicial de la tarea ---
    LaunchedEffect(taskId) {
        db.collection("tareas").document(taskId)
            .get()
            .addOnSuccessListener { doc ->
                val progreso = doc.getLong("progress")?.toInt() ?: 0
                val evidenciaUrl = doc.getString("evidencias")

                currentStep = progreso
                // Si ya hay evidencia, muestra el formulario
                if (!evidenciaUrl.isNullOrEmpty()) {
                    evidenciaSubida = true
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error al cargar la tarea", Toast.LENGTH_SHORT).show()
                onTaskCompleted() // Regresa si la tarea no se puede cargar
            }
    }

    // --- 9. UI (Copiada de tu Composable, con lógica de estado actualizada) ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Steps (Sin cambios)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labels.forEachIndexed { index, label ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val color by animateColorAsState(
                        targetValue = if (index <= currentStep) Color(0xFF9C27B0) else Color.LightGray
                    )
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(color = color, shape = MaterialTheme.shapes.small)
                    ) {
                        Text(
                            text = when (index) {
                                0 -> "🏁"
                                1 -> "📌"
                                2 -> "🚚"
                                3 -> "🔧"
                                else -> "✅"
                            },
                            modifier = Modifier.align(Alignment.Center),
                            fontSize = 20.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = label, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val buttonColor by animateColorAsState(
            targetValue = if (currentStep < labels.lastIndex) Color(0xFF64B5F6) else Color.Gray
        )

        // Botón "Confirmar paso" (Sin cambios, usa 'taskId' de los parámetros)
        Button(
            onClick = {
                if (currentStep < labels.lastIndex) {
                    currentStep++
                    db.collection("tareas").document(taskId)
                        .update("progress", currentStep)
                        .addOnSuccessListener {
                            Toast.makeText(
                                context,
                                "Avanzaste a: ${labels[currentStep]}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                } else {
                    Toast.makeText(context, "Ya estás en el último paso", Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
            modifier = Modifier.fillMaxWidth(0.6f).height(50.dp)
        ) {
            Text("Confirmar paso", color = Color.White, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón subir evidencia (Ahora usa el lanzador de Compose)
        if (currentStep == labels.lastIndex && !evidenciaSubida) {
            Button(
                onClick = { pickImageLauncher.launch("image/*") }, // 10. Llama al nuevo lanzador
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFAAA5)),
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                Text("Subir evidencia", color = Color.Black, fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Formulario de confirmación (Sin cambios, excepto el final)
        AnimatedVisibility(visible = evidenciaSubida) {
            Card(
                modifier = Modifier.fillMaxWidth().animateContentSize(),
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "¿Se realizó el trabajo solicitado con éxito?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = checkExito,
                            onCheckedChange = { checkExito = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sí, se completó")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = comentario,
                        onValueChange = { comentario = it },
                        label = { Text("Comentario (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // SignaturePad (Sin cambios)
                    AndroidView(factory = { ctx ->
                        SignaturePad(ctx, null).apply {
                            setSaveEnabled(false)
                            signaturePadRef.value = this
                            setBackgroundColor(android.graphics.Color.LTGRAY)
                        }
                    }, modifier = Modifier.fillMaxWidth().height(200.dp))

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { signaturePadRef.value?.clear() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Borrar firma", color = Color.White)
                        }

                        Button(
                            onClick = {
                                val firmaBitmap = signaturePadRef.value?.signatureBitmap
                                if (firmaBitmap != null && checkExito) {
                                    isUploading = true
                                    // 11. Pasa 'context' a la función helper
                                    val uri = bitmapToUri(context, firmaBitmap)
                                    if (uri != null) {
                                        MediaManager.get().upload(uri)
                                            .option("folder", "firmas/$taskId")
                                            .option("use_filename", true)
                                            .option("unique_filename", true)
                                            .callback(object : UploadCallback {
                                                override fun onStart(requestId: String?) {}
                                                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
                                                override fun onSuccess(requestId: String?, resultData: Map<*, *>?) {
                                                    val firmaUrl = resultData?.get("secure_url") as? String
                                                    db.collection("tareas").document(taskId)
                                                        .update(
                                                            mapOf(
                                                                "confirmacionExito" to checkExito,
                                                                "comentarioCliente" to comentario,
                                                                "firmaCliente" to firmaUrl,
                                                                "fechaConfirmacion" to FieldValue.serverTimestamp()
                                                            )
                                                        ).addOnSuccessListener {
                                                            Toast.makeText(context, "¡Tarea completada!", Toast.LENGTH_SHORT).show()
                                                            // Reset
                                                            isUploading = false

                                                            // 12. REEMPLAZO DE NAVEGACIÓN
                                                            onTaskCompleted()
                                                        }
                                                }

                                                override fun onError(requestId: String?, error: ErrorInfo?) {
                                                    Toast.makeText(context, "Error al subir firma: ${error?.description}", Toast.LENGTH_SHORT).show()
                                                    isUploading = false
                                                }
                                                override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
                                            }).dispatch()
                                    } else {
                                        Toast.makeText(context, "Error al procesar firma", Toast.LENGTH_SHORT).show()
                                        isUploading = false
                                    }
                                } else {
                                    Toast.makeText(context, "Marca que se completó la tarea y firma antes de enviar", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64B5F6)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Enviar", color = Color.White)
                        }
                    }

                    if (isUploading) {
                        Spacer(modifier = Modifier.height(12.dp))
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                }
            }
        }
    }
}

/**
 * 13. Función Helper movida fuera del Composable, hecha privada y toma Context.
 */
private fun bitmapToUri(context: Context, bitmap: Bitmap): Uri? {
    return try {
        val file = File(context.cacheDir, "firma_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        Uri.fromFile(file)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}