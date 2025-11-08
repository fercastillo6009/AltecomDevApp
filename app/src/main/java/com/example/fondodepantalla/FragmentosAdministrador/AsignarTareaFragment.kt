@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.fondodepantalla.FragmentosAdministrador

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
// Asegúrate de que AsistenciaManager y LocationHelper no estén aquí para evitar errores de compilación
// ...

// Color Primario Personalizado
val CustomPrimary = Color(0xFF2F78AF)

@Composable
fun CustomTaskTheme(content: @Composable () -> Unit) {
    val customColorScheme = lightColorScheme(
        primary = CustomPrimary,
        primaryContainer = CustomPrimary.copy(alpha = 0.15f), // Contenedor más claro para el encabezado
        onPrimaryContainer = CustomPrimary.copy(alpha = 0.9f),
        // Puedes definir otros colores si es necesario, pero estos son clave para el cambio
    )
    MaterialTheme(colorScheme = customColorScheme, content = content)
}

class AsignarTareaFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()

    data class Usuario(val uid: String, val nombre: String)

    private val listaUsuarios = mutableStateListOf<Usuario>()
    private var uidSeleccionado: String? = null
    private var cargandoUsuarios by mutableStateOf(true)

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View {
        cargarUsuarios()
        return ComposeView(requireContext()).apply {
            setContent {
                CustomTaskTheme { // Usamos el tema personalizado
                    AsignarTareaUI()
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun AsignarTareaUI() {
        var usuarioSeleccionadoNombre by remember { mutableStateOf<String?>(null) }
        var nombreTarea by remember { mutableStateOf("") }
        var descripcion by remember { mutableStateOf("") }
        var expanded by remember { mutableStateOf(false) }

        val contexto = requireContext()
        val habilitarBoton = usuarioSeleccionadoNombre != null && nombreTarea.isNotBlank() && descripcion.isNotBlank()

        // Forma de Input más suave
        val inputShape = RoundedCornerShape(12.dp)

        Scaffold(
            // 1. Título más prominente con LargeTopAppBar
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
                    .background(MaterialTheme.colorScheme.surface) // Fondo de la pantalla
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // 2. Selector de Empleado (Dropdown Moderno)
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
                                shape = inputShape, // 2. Bordes redondeados
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
                                            uidSeleccionado = usuario.uid
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
                    shape = inputShape, // 2. Bordes redondeados
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // 4. Descripción
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción o detalles de la tarea") },
                    shape = inputShape, // 2. Bordes redondeados
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 200.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 5. Botón Asignar
                ElevatedButton(
                    onClick = {
                        if (habilitarBoton) {
                            asignarTarea(nombreTarea, descripcion, usuarioSeleccionadoNombre!!, contexto)
                        } else {
                            Toast.makeText(contexto, "Completa todos los campos y selecciona un empleado", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = habilitarBoton,
                    shape = inputShape, // 2. Bordes redondeados
                    elevation = ButtonDefaults.elevatedButtonElevation(4.dp)
                    // El color ahora viene del tema (CustomPrimary = #2F78AF)
                ) {
                    Text(
                        "Asignar Tarea",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }

    // --- Lógica de Negocio (sin cambios) ---

    private fun cargarUsuarios() {
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
                Toast.makeText(requireContext(), "Error al cargar usuarios", Toast.LENGTH_SHORT).show()
                cargandoUsuarios = false
            }
    }

    private fun asignarTarea(nombreTarea: String, descripcion: String, nombreEmpleado: String, contexto: Context) {
        if (uidSeleccionado == null) {
            Toast.makeText(contexto, "Error interno: UID del empleado no encontrado.", Toast.LENGTH_SHORT).show()
            return
        }

        val tarea = hashMapOf(
            "name" to nombreTarea,
            "descripcion" to descripcion,
            "uidAsignado" to uidSeleccionado,
            "nombreEmpleado" to nombreEmpleado,
            "estado" to "Pendiente",
            "fechaAsignacion" to System.currentTimeMillis()
        )

        db.collection("tareas")
            .add(tarea)
            .addOnSuccessListener {
                Toast.makeText(contexto, "✅ Tarea asignada a $nombreEmpleado", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(contexto, "❌ Error al asignar tarea", Toast.LENGTH_SHORT).show()
            }
    }
}