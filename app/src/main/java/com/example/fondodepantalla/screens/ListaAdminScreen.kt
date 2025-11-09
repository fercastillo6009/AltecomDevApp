package com.example.fondodepantalla.screens // 1. Paquete actualizado

import android.widget.Toast
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
 *
 * @param onTaskNavigate Una función lambda que se invoca cuando el usuario
 * selecciona una tarea, pasando el ID de la tarea
 * para que el sistema de navegación principal se encargue.
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 12.dp)
            // 7. El click de la tarjeta también usa el lambda
            .clickable { if (task.user == uid) onTaskClick(task) },
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .background(Color.White) // Considera MaterialTheme.colorScheme.surface
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_task),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.Unspecified
            )
            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.name ?: "Sin nombre",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Black // Considera MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = task.descripcion ?: "Sin descripción",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray, // Considera MaterialTheme.colorScheme.onSurfaceVariant
                    maxLines = 2
                )
            }

            // El Switch permite "tomar" o "soltar" la tarea
            Switch(
                checked = isTaken,
                onCheckedChange = { checked ->
                    if (checked && uid != null) {
                        // Tomar la tarea
                        firestore.collection("tareas").document(task.id!!)
                            .update("taken", true, "user", uid)
                            .addOnSuccessListener {
                                isTaken = true
                                // 8. Navega a la tarea en cuanto la toma
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
                        // Evita que un admin libere la tarea de otro
                        Toast.makeText(context, "No puedes liberar esta tarea", Toast.LENGTH_SHORT).show()
                        isTaken = true
                    }
                }
            )
        }
    }
}