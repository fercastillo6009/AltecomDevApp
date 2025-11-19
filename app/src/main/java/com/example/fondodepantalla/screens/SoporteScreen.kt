package com.example.fondodepantalla.screens

import android.widget.Toast
import androidx.compose.foundation.Image // <-- ¡IMPORTACIÓN AÑADIDA!
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fondodepantalla.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Define la estructura de datos para un servicio de soporte.
 */
data class ServicioSoporte(
    @get:Exclude @set:Exclude
    var docId: String? = null,
    val Numero_servicio: String? = null,
    val Nombre_empresa: String? = null,
    val Estado: String? = null,
    val Foto: String? = null,
    val Observaciones: String? = null,
    val tipo_servicio: List<String>? = null,
    val firmaCliente: String? = null,
    val uidAsignado: String? = null,
    val progress: Long? = null
) {
    // Constructor vacío requerido por Firestore
    constructor() : this(null, null, null, null, null, null, null, null, null, null)
}


/**
 * Muestra la lista de servicios de soporte asignados al usuario.
 */
@Composable
fun SoporteScreen(
    onServiceNavigate: (serviceId: String) -> Unit
) {
    val firestore = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    val context = LocalContext.current

    var servicios by remember { mutableStateOf(listOf<ServicioSoporte>()) }

    LaunchedEffect(Unit) {
        val uidActual = auth.currentUser?.uid
        if (uidActual != null) {

            firestore.collection("tareas")
                .whereEqualTo("uidAsignado", uidActual)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Toast.makeText(context, "Error al cargar servicios", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }

                    val lista = snapshot?.documents?.mapNotNull { doc ->
                        val servicio = doc.toObject(ServicioSoporte::class.java)?.apply {
                            docId = doc.id
                        }

                        // Filtro para ocultar tareas finalizadas y firmadas
                        if (servicio == null) {
                            null
                        } else {
                            val isFinalizado = servicio.progress == 4L
                            val hasFirma = !servicio.firmaCliente.isNullOrEmpty()

                            if (isFinalizado && hasFirma) {
                                null
                            } else {
                                servicio
                            }
                        }
                    } ?: emptyList()

                    servicios = lista
                }
        } else {
            Toast.makeText(context, "Error: no se encontró UID del usuario", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        item {
            Text(
                text = "Servicios de Soporte",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 8.dp),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        items(servicios) { servicio ->
            SoporteCard(
                servicio = servicio,
                onServiceClick = { servicioClickeado ->
                    if (!servicioClickeado.Numero_servicio.isNullOrEmpty()) {
                        onServiceNavigate(servicioClickeado.Numero_servicio)
                    } else {
                        Toast.makeText(context, "Error: Este servicio no tiene ID", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

@Composable
private fun SoporteCard(
    servicio: ServicioSoporte,
    onServiceClick: (ServicioSoporte) -> Unit
) {
    // ... (Lógica de 'estadoMostrado' y 'firstWord' - Sin cambios) ...
    val estadoMostrado = when (servicio.progress) {
        1L -> "En camino"
        2L -> "Llegando"
        3L -> "Dando servicio"
        4L -> "Finalizado"
        else -> servicio.Estado ?: "Pendiente"
    }

    val firstWord = servicio.Nombre_empresa
        ?.trim()
        ?.split(" ")
        ?.firstOrNull()
        ?.lowercase()
        ?: "default"

    val logoResId = when (firstWord) {
        "oxxo" -> R.drawable.logo_oxxo
        "seven" -> R.drawable.logo_seven
        "circlek" -> R.drawable.logo_alsuper
        else -> null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 12.dp)
            .clickable { onServiceClick(servicio) },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // --- ¡AQUÍ ESTÁ EL CAMBIO! ---
            if (logoResId != null) {
                // Hay un logo personalizado, USAMOS 'Image'
                Image( // <-- CAMBIADO DE 'Icon' A 'Image'
                    painter = painterResource(id = logoResId),
                    contentDescription = servicio.Nombre_empresa,
                    modifier = Modifier.size(48.dp),
                )
            } else {
                // No hay logo, usa el 'Icon' de por defecto
                Icon(
                    imageVector = Icons.Default.SupportAgent,
                    contentDescription = "Servicio de Soporte",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary // <-- 'tint' DEVUELTO AQUÍ
                )
            }
            // --- FIN DEL CAMBIO ---

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = servicio.Nombre_empresa ?: "Sin nombre de empresa",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))

                val tiposDeServicio = if (servicio.tipo_servicio.isNullOrEmpty()) {
                    "Sin tipo de servicio"
                } else {
                    servicio.tipo_servicio.joinToString(", ")
                }

                Text(
                    text = tiposDeServicio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Estado: $estadoMostrado",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}