package com.example.fondodepantalla.screens // 1. Paquete actualizado

import android.widget.Toast
import androidx.compose.foundation.Image // <-- ¡IMPORTACIÓN AÑADIDA!
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.fondodepantalla.R
import com.example.fondodepantalla.Task // Asumo que esta clase de datos existe
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
// 2. Importaciones de Fragment, Bundle, View, etc., eliminadas

/**
 * Muestra la lista de tareas asignadas al administrador.
 * (Función principal 'ListaAdminScreen' sin cambios)
 */
@Composable
fun ListaAdminScreen(
    onTaskNavigate: (taskId: String) -> Unit // 3. Parámetro de navegación
) {
    val firestore = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    val context = LocalContext.current
    var tasks by remember { mutableStateOf(listOf<Task>()) }

    LaunchedEffect(Unit) {
        val uidActual = auth.currentUser?.uid
        if (uidActual != null) {
            firestore.collection("tareas")
                .whereEqualTo("uidAsignado", uidActual)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener

                    val lista = snapshot?.documents?.mapNotNull { doc ->
                        val t = doc.toObject(Task::class.java)?.apply { id = doc.id }
                        // Lógica para filtrar tareas que YA están 100% completadas y firmadas
                        if (t != null &&
                            !(doc.getBoolean("confirmacionExito") == true &&
                                    !doc.getString("firmaCliente").isNullOrEmpty() &&
                                    (doc.getLong("progress") ?: 0L) == 4L)
                        ) t else null
                    } ?: emptyList()

                    tasks = lista
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
                text = "Nuevos encargos",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                color = Color.Black // Considera usar MaterialTheme.colorScheme.onBackground
            )
        }

        items(tasks) { task ->
            // 4. Se pasa el lambda 'onTaskNavigate' al 'TaskCard'
            TaskCard(
                task = task,
                firestore = firestore,
                onTaskClick = { tarea ->
                    // 5. En lugar de hacer una transacción de Fragment,
                    // simplemente notifica al navegador con el ID.
                    if (tarea.id != null) {
                        onTaskNavigate(tarea.id!!)
                    } else {
                        Toast.makeText(context, "Error: Tarea sin ID", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

@Composable
private fun TaskCard( // 6. Hecho privado, ya que solo se usa en esta pantalla
    task: Task,
    firestore: FirebaseFirestore,
    onTaskClick: (Task) -> Unit
) {
    val context = LocalContext.current
    var isTaken by remember { mutableStateOf(task.isTaken) }
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    // --- ¡AQUÍ ESTÁ LA LÓGICA DEL LOGO AÑADIDA! ---
    // 1. Obtiene la primera palabra del nombre de la TAREA
    val firstWord = task.name // Usamos 'task.name' como fuente
        ?.trim()
        ?.split(" ")
        ?.firstOrNull()
        ?.lowercase()
        ?: "default"

    // 2. Decide qué recurso (logo) usar
    val logoResId = when (firstWord) {
        "oxxo" -> R.drawable.logo_oxxo
        "seven" -> R.drawable.logo_seven
        "circlek" -> R.drawable.logo_alsuper
        // --- Agrega más empresas aquí ---

        else -> null // Si no se encuentra, usará el logo por defecto
    }
    // --- FIN DE LA LÓGICA DEL LOGO ---

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 12.dp)
            .clickable { if (task.user == uid) onTaskClick(task) },
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .background(Color.White)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // --- ¡CAMBIO EN EL ICONO! ---
            if (logoResId != null) {
                // Hay un logo personalizado, usamos 'Image' para ver el color
                Image(
                    painter = painterResource(id = logoResId),
                    contentDescription = task.name,
                    modifier = Modifier.size(48.dp)
                )
            } else {
                // No hay logo, usa el 'Icon' de tarea por defecto
                Icon(
                    painter = painterResource(id = R.drawable.ic_task),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color.Unspecified // Mantenemos tu 'tint' original
                )
            }
            // --- FIN DEL CAMBIO ---

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.name ?: "Sin nombre",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Black
                )
                Text(
                    text = task.descripcion ?: "Sin descripción",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    maxLines = 2
                )
            }

            // El Switch (sin cambios)
            Switch(
                checked = isTaken,
                onCheckedChange = { checked ->
                    if (checked && uid != null) {
                        // Tomar la tarea
                        firestore.collection("tareas").document(task.id!!)
                            .update("taken", true, "user", uid)
                            .addOnSuccessListener {
                                isTaken = true
                                onTaskClick(task)
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Error al tomar tarea", Toast.LENGTH_SHORT).show()
                            }
                    } else if (!checked && task.user == uid) {
                        // Soltar la tarea
                        firestore.collection("tareas").document(task.id!!)
                            .update("taken", false, "user", "")
                            .addOnSuccessListener { isTaken = false }
                            .addOnFailureListener {
                                Toast.makeText(context, "Error al liberar tarea", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(context, "No puedes liberar esta tarea", Toast.LENGTH_SHORT).show()
                        isTaken = true
                    }
                }
            )
        }
    }
}