package com.example.fondodepantalla.FragmentosAdministrador

import android.os.Bundle
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.example.fondodepantalla.R
import com.example.fondodepantalla.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ListaAdmin : Fragment() {

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View {
        return ComposeView(requireContext()).apply {
            setContent { ListaAdminComposable() }
        }
    }

    @Composable
    fun ListaAdminComposable() {
        val firestore = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()
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
                    color = Color.Black
                )
            }

            items(tasks) { task ->
                TaskCard(task = task, firestore = firestore) { tarea ->
                    val fragment = RegistrarAdmin()
                    fragment.arguments = Bundle().apply { putString("taskId", tarea.id) }
                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_containerA, fragment)
                        .addToBackStack(null)
                        .commit()
                }
            }
        }
    }

    @Composable
    fun TaskCard(
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
                .clickable { if (task.user == uid) onTaskClick(task) },
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .background(Color.White)
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
                        color = Color.Black
                    )
                    Text(
                        text = task.descripcion ?: "Sin descripción",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        maxLines = 2
                    )
                }

                Switch(
                    checked = isTaken,
                    onCheckedChange = { checked ->
                        if (checked && uid != null) {
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
}
