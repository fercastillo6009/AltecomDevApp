@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.fondodepantalla.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
// 2. Importaciones de Fragment, View, Bundle, etc., eliminadas

@Composable
fun PerfilAdminScreen() {

    // --- 3. Estado y Lógica ---
    val context = LocalContext.current
    // 'db' y 'isAgregar' se manejan como estado de Compose
    val db = remember { FirebaseFirestore.getInstance() }
    var isAgregar by remember { mutableStateOf(true) }

    // 4. Reemplazo de 'registerForActivityResult'
    // Se define el "contrato" (qué app lanzar) y el "callback" (qué hacer con el resultado)
    val barcodeLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            // 'isAgregar' se lee desde el estado actual
            actualizarInventario(db, context, result.contents, isAgregar)
        } else {
            Toast.makeText(context, "Escaneo cancelado", Toast.LENGTH_SHORT).show()
        }
    }

    // Función para lanzar el escáner
    fun launchScanner() {
        val options = ScanOptions().apply {
            setPrompt(if (isAgregar) "Escanea para AGREGAR" else "Escanea para QUITAR")
            setBeepEnabled(true)
            setOrientationLocked(true)
        }
        barcodeLauncher.launch(options)
    }

    // --- 5. UI (Reemplazo del XML) ---
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestión de Inventario") },
                navigationIcon = {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Selecciona una operación y escanea el producto",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 40.dp)
            )

            // Botón para Agregar
            Button(
                onClick = {
                    isAgregar = true
                    launchScanner()
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar")
                Spacer(Modifier.width(8.dp))
                Text("Agregar Stock", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botón para Quitar
            Button(
                onClick = {
                    isAgregar = false
                    launchScanner()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Quitar")
                Spacer(Modifier.width(8.dp))
                Text("Quitar Stock", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// 6. Lógica de Firestore (traducida a función privada de Kotlin)
private fun actualizarInventario(
    db: FirebaseFirestore,
    context: Context,
    codigo: String,
    sumar: Boolean
) {
    val docRef = db.collection("inventario").document(codigo)

    db.runTransaction { transaction ->
        val snapshot = transaction.get(docRef)
        if (snapshot.exists()) {
            val cantidad = snapshot.getLong("cantidad") ?: 0L
            if (sumar) {
                transaction.update(docRef, "cantidad", FieldValue.increment(1))
            } else {
                if (cantidad > 0) {
                    transaction.update(docRef, "cantidad", FieldValue.increment(-1))
                } else {
                    // Lanzar excepción detiene la transacción y activa onFaliure
                    throw RuntimeException("No hay stock disponible")
                }
            }
        } else {
            if (sumar) {
                // 7. Reemplazo de 'new Producto()' por un 'mapOf' de Kotlin
                val nuevoProducto = mapOf(
                    "nombre" to "Producto sin nombre",
                    "cantidad" to 1
                )
                transaction.set(docRef, nuevoProducto)
            } else {
                throw RuntimeException("El producto no existe en inventario")
            }
        }
        null // Requerido por la transacción de Kotlin
    }
        .addOnSuccessListener {
            Toast.makeText(context, "Inventario actualizado", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener { e ->
            // Muestra el mensaje de error personalizado (ej. "No hay stock")
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}