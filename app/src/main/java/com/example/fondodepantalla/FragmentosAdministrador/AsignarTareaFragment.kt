@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.fondodepantalla.FragmentosAdministrador

import android.os.Bundle
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore

class AsignarTareaFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()

    private val listaUsuariosNombres = mutableStateListOf<String>()
    private val listaUsuariosUID = mutableStateListOf<String>()
    private var uidSeleccionado: String? = null

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View {
        val composeView = ComposeView(requireContext())
        composeView.setContent { AsignarTareaUI() }
        cargarUsuarios()
        return composeView
    }

    @Composable
    private fun AsignarTareaUI() {
        var usuarioSeleccionado by remember { mutableStateOf<String?>(null) }
        var nombreTarea by remember { mutableStateOf("") }
        var descripcion by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {

            Text(text = "Seleccionar empleado:", style = MaterialTheme.typography.titleMedium)

            Spacer(modifier = Modifier.height(8.dp))

            // Lista de empleados
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
                itemsIndexed(listaUsuariosNombres) { index, nombre ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                usuarioSeleccionado = nombre
                                uidSeleccionado = listaUsuariosUID.getOrNull(index)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (usuarioSeleccionado == nombre) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Text(
                            text = nombre,
                            modifier = Modifier.padding(16.dp),
                            color = if (usuarioSeleccionado == nombre) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nombre de tarea
            OutlinedTextField(
                value = nombreTarea,
                onValueChange = { nombreTarea = it },
                label = { Text("Nombre de la tarea") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Descripción
            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Descripción de la tarea") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Botón Asignar
            Button(
                onClick = {
                    if (usuarioSeleccionado != null) {
                        asignarTarea(nombreTarea, descripcion, usuarioSeleccionado!!)
                    } else {
                        Toast.makeText(requireContext(), "Selecciona un empleado", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Asignar tarea")
            }
        }
    }

    private fun cargarUsuarios() {
        db.collection("usuarios")
            .get()
            .addOnSuccessListener { docs ->
                listaUsuariosNombres.clear()
                listaUsuariosUID.clear()
                for (doc in docs) {
                    val nombre = doc.getString("nombre") ?: continue
                    listaUsuariosNombres.add(nombre)
                    listaUsuariosUID.add(doc.id)
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al cargar usuarios", Toast.LENGTH_SHORT).show()
            }
    }

    private fun asignarTarea(nombreTarea: String, descripcion: String, nombreEmpleado: String) {
        if (nombreTarea.isBlank() || descripcion.isBlank() || uidSeleccionado == null) {
            Toast.makeText(requireContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val tarea = hashMapOf(
            "name" to nombreTarea,
            "descripcion" to descripcion,
            "uidAsignado" to uidSeleccionado,
            "nombreEmpleado" to nombreEmpleado,
            "fechaAsignacion" to System.currentTimeMillis()
        )

        db.collection("tareas")
            .add(tarea)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Tarea asignada correctamente", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al asignar tarea", Toast.LENGTH_SHORT).show()
            }
    }
}
