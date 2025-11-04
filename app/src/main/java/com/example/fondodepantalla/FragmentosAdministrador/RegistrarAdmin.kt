package com.example.fondodepantalla.FragmentosAdministrador

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.github.gcacace.signaturepad.views.SignaturePad
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream

class RegistrarAdmin : Fragment() {

    private var taskId: String? = null
    private val db = FirebaseFirestore.getInstance()
    private var evidenciaSubida by mutableStateOf(false)

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                // Subir evidencia a Cloudinary
                MediaManager.get().upload(uri)
                    .option("folder", "evidencias/${taskId ?: "tarea"}")
                    .option("use_filename", true)
                    .option("unique_filename", true)
                    .callback(object : UploadCallback {
                        override fun onStart(requestId: String?) {}
                        override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
                        override fun onSuccess(requestId: String?, resultData: Map<*, *>?) {
                            Toast.makeText(requireContext(), "Evidencia subida", Toast.LENGTH_SHORT).show()
                            evidenciaSubida = true
                        }

                        override fun onError(requestId: String?, error: ErrorInfo?) {
                            Toast.makeText(
                                requireContext(),
                                "Error al subir evidencia: ${error?.description}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
                    }).dispatch()
            }
        }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View {
        taskId = arguments?.getString("taskId")

        return ComposeView(requireContext()).apply {
            setContent {
                RegistrarAdminScreen()
            }
        }
    }

    @Composable
    private fun RegistrarAdminScreen() {
        val labels = listOf("Inicio", "st1", "st2", "st3", "Completa")
        var currentStep by remember { mutableStateOf(0) }
        var checkExito by remember { mutableStateOf(false) }
        var comentario by remember { mutableStateOf("") }
        var isUploading by remember { mutableStateOf(false) }
        val scrollState = rememberScrollState()
        val context = LocalContext.current
        val signaturePadRef = remember { mutableStateOf<SignaturePad?>(null) }

        // Cargar progreso inicial
        LaunchedEffect(taskId) {
            taskId?.let {
                db.collection("tareas").document(it)
                    .get()
                    .addOnSuccessListener { doc ->
                        val progreso = doc.getLong("progress")?.toInt() ?: 0
                        currentStep = progreso
                    }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Steps
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

            Button(
                onClick = {
                    if (currentStep < labels.lastIndex) {
                        currentStep++
                        taskId?.let { id ->
                            db.collection("tareas").document(id)
                                .update("progress", currentStep)
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        context,
                                        "Avanzaste a: ${labels[currentStep]}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
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

            // Botón subir evidencia
            if (currentStep == labels.lastIndex && !evidenciaSubida) {
                Button(
                    onClick = { pickImageLauncher.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFAAA5)),
                    modifier = Modifier.fillMaxWidth(0.6f)
                ) {
                    Text("Subir evidencia", color = Color.Black, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Formulario de confirmación
            AnimatedVisibility(visible = evidenciaSubida) {
                Card(
                    modifier = Modifier.fillMaxWidth().animateContentSize(),
                    shape = MaterialTheme.shapes.medium,
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "¿Se realizó el trabajo solicitado con éxito?",
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
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
                                        taskId?.let { id ->
                                            val uri = bitmapToUri(firmaBitmap)
                                            if (uri != null) {
                                                MediaManager.get().upload(uri)
                                                    .option("folder", "firmas/$id")
                                                    .option("use_filename", true)
                                                    .option("unique_filename", true)
                                                    .callback(object : UploadCallback {
                                                        override fun onStart(requestId: String?) {}
                                                        override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
                                                        override fun onSuccess(requestId: String?, resultData: Map<*, *>?) {
                                                            val firmaUrl = resultData?.get("secure_url") as? String
                                                            db.collection("tareas").document(id)
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
                                                                    signaturePadRef.value?.clear()
                                                                    checkExito = false
                                                                    comentario = ""
                                                                    evidenciaSubida = false
                                                                    isUploading = false
                                                                    parentFragmentManager.beginTransaction().apply {
                                                                        // Remueve el fragmento actual
                                                                        remove(this@RegistrarAdmin)

                                                                        // Agrega ListaAdmin al mismo contenedor del fragmento actual
                                                                        val parent = view?.parent as? ViewGroup
                                                                        parent?.let { add(it.id, ListaAdmin()) }
                                                                        commit()
                                                                    }
                                                                }
                                                        }

                                                        override fun onError(requestId: String?, error: ErrorInfo?) {
                                                            Toast.makeText(context, "Error al subir firma: ${error?.description}", Toast.LENGTH_SHORT).show()
                                                            isUploading = false
                                                        }

                                                        override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
                                                    }).dispatch()
                                            }
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

    private fun bitmapToUri(bitmap: Bitmap): Uri? {
        return try {
            val file = File(requireContext().cacheDir, "firma_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
