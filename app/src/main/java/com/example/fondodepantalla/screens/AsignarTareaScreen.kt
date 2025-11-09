@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.fondodepantalla.screens // 1. Paquete actualizado

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Task
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext // 2. Importación añadida
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
// 3. Todas las importaciones de Fragment, LayoutInflater, ViewGroup, Bundle y ComposeView han sido eliminadas.


// Color Primario Personalizado (Sin cambios)
val CustomPrimary = Color(0xFF2F78AF)

@Composable
fun CustomTaskTheme(content: @Composable () -> Unit) {
    val customColorScheme = lightColorScheme(
        primary = CustomPrimary,
        primaryContainer = CustomPrimary.copy(alpha = 0.15f),
        onPrimaryContainer = CustomPrimary.copy(alpha = 0.9f),
    )
    MaterialTheme(colorScheme = customColorScheme, content = content)
}

// 4. La clase 'Fragment' ha sido eliminada. La data class puede vivir a nivel de archivo.
private data class Usuario(val uid: String, val nombre: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsignarTareaScreen() { // 5. Esta es ahora la función principal de tu pantalla

    // --- Estado (Movido desde el Fragment y el antiguo AsignarTareaUI) ---
    val db = remember { FirebaseFirestore.getInstance() }
    val listaUsuarios = remember { mutableStateListOf<Usuario>() }
    var cargandoUsuarios by remember { mutableStateOf(true) }

    var uidSeleccionado by remember { mutableStateOf<String?>(null) }
    var usuarioSeleccionadoNombre by remember { mutableStateOf<String?>(null) }
    var nombreTarea by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val contexto = LocalContext.current // 6. Reemplaza a requireContext()
    val habilitarBoton = uidSeleccionado != null && nombreTarea.isNotBlank() && descripcion.isNotBlank()
    val inputShape = RoundedCornerShape(12.dp)


    // --- Lógica de Negocio (Movida desde el Fragment) ---

    // 7. Cargar usuarios (se ejecuta solo una vez cuando la pantalla aparece)
    LaunchedEffect(Unit) {
        cargandoUsuarios = true
        db.collection("usuarios")
            .get()
            .addOnSuccessListener { docs ->
                listaUsuarios.clear()
                for (doc in docs) {
                    val nombre = doc.getString("nombre")
                    if (nombre != null) {
                        listaUsuarios.add(Usuario(doc.id, nombre))
                    }
                }
                cargandoUsuarios = false
            }
            .addOnFailureListener {
                Toast.makeText(contexto, "Error al cargar usuarios", Toast.LENGTH_SHORT).show()
                cargandoUsuarios = false
            }
    }

    // 8. La función AsignarTarea ahora vive dentro del onClick del botón

    // --- UI (Adaptada desde AsignarTareaUI) ---
    CustomTaskTheme {
        Scaffold(
            topBar = {
                LargeTopAppBar(
                    title = {
                        Text(
                            "Asignar Nueva Tarea",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // 2. Selector de Empleado
                Text(
                    text = "Empleado a asignar",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    if (cargandoUsuarios) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Cargando usuarios...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = usuarioSeleccionadoNombre ?: "Selecciona un empleado",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Empleado") },
                                trailingIcon = {
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = "Seleccionar", Modifier.menuAnchor())
                                },
                                leadingIcon = { Icon(Icons.Filled.Group, contentDescription = "Usuarios") },
                                shape = inputShape,
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )

                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                listaUsuarios.forEach { usuario ->
                                    DropdownMenuItem(
                                        text = { Text(usuario.nombre) },
                                        onClick = {
                                            usuarioSeleccionadoNombre = usuario.nombre
                                            uidSeleccionado = usuario.uid // 9. Se actualiza el estado local
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Nombre de tarea
                OutlinedTextField(
                    value = nombreTarea,
                    onValueChange = { nombreTarea = it },
                    label = { Text("Nombre clave de la tarea") },
                    leadingIcon = { Icon(Icons.Filled.Task, contentDescription = "Tarea") },
                    shape = inputShape,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // 4. Descripción
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción o detalles de la tarea") },
                    shape = inputShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 200.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 5. Botón Asignar
                ElevatedButton(
                    onClick = {
                        if (habilitarBoton) {
                            // 10. Lógica de 'asignarTarea' movida directamente aquí
                            val tarea = hashMapOf(
                                "name" to nombreTarea,
                                "descripcion" to descripcion,
                                "uidAsignado" to uidSeleccionado,
                                "nombreEmpleado" to usuarioSeleccionadoNombre,
                                "estado" to "Pendiente",
                                "fechaAsignacion" to System.currentTimeMillis()
                            )

                            db.collection("tareas")
                                .add(tarea)
                                .addOnSuccessListener {
                                    Toast.makeText(contexto, "✅ Tarea asignada a $usuarioSeleccionadoNombre", Toast.LENGTH_SHORT).show()
                                    // Opcional: Limpiar campos después de asignar
                                    nombreTarea = ""
                                    descripcion = ""
                                    usuarioSeleccionadoNombre = null
                                    uidSeleccionado = null
                                    expanded = false
                                }
                                .addOnFailureListener {
                                    Toast.makeText(contexto, "❌ Error al asignar tarea", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(contexto, "Completa todos los campos y selecciona un empleado", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = habilitarBoton,
                    shape = inputShape,
                    elevation = ButtonDefaults.elevatedButtonElevation(4.dp)
                ) {
                    Text(
                        "Asignar Tarea",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}