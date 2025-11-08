package com.example.fondodepantalla.FragmentosAdministrador

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class CrearUsuarioFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return androidx.compose.ui.platform.ComposeView(requireContext()).apply {
            setContent {
                // Uso de el tema de Material 3 para un diseño pulido
                MaterialTheme(
                    colorScheme = lightColorScheme(
                        primary = Color(0xFF2F78AF),
                        onPrimary = Color.White,
                        error = Color(0xFFD32F2F)
                    )
                ) {
                    AdminUserRegistrationScreen()
                }
            }
        }
    }
}

@Composable
fun AdminUserRegistrationScreen() {
    // 1. Inicialización de Firebase (se asume que la app ya fue inicializada)
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }

    // 2. Estados para los campos de texto
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("empleado") } // Valor por defecto
    var isDropdownExpanded by remember { mutableStateOf(false) }

    // 3. Estado para la UI y mensajes
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    // Roles disponibles definidos por el usuario
    val roles = listOf("admin", "empleado")

    /**
     * Función que maneja el registro en Auth y la base de datos en Firestore.
     */
    fun registerNewUser() {
        if (email.isBlank() || name.isBlank() || password.isBlank() || selectedRole.isBlank()) {
            message = "Todos los campos son obligatorios."
            return
        }
        if (password.length < 6) {
            message = "La contraseña debe tener al menos 6 caracteres."
            return
        }

        isLoading = true
        message = "Registrando usuario..."

        coroutineScope.launch {
            try {
                // PASO 1: Creación del usuario en Firebase Authentication
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val uid = authResult.user?.uid

                if (uid != null) {
                    // PASO 2: Guardar los datos del usuario en Firestore
                    val userData = hashMapOf(
                        "correo" to email,
                        "nombre" to name,
                        "rol" to selectedRole
                    )

                    // Guarda el documento en "usuarios/{UID_DEL_NUEVO_USUARIO}"
                    db.collection("usuarios").document(uid).set(userData).await()

                    message = "Usuario '${name}' registrado como ${selectedRole.uppercase()} con éxito."

                    // Limpiar campos tras el registro exitoso
                    email = ""
                    name = ""
                    password = ""
                    selectedRole = "empleado"
                } else {
                    message = "Error: No se pudo obtener el UID del nuevo usuario."
                }
            } catch (e: Exception) {
                // Manejo de excepciones de Firebase (ej. 'email ya en uso', 'weak password')
                // Se usa e.message para un mensaje de error más específico de Firebase
                message = "Error al registrar: ${e.message ?: "Error desconocido"}"
            } finally {
                isLoading = false
            }
        }
    }

    // --- Estructura de la UI con desplazamiento para dispositivos pequeños ---
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Registro de Empleados",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 32.dp, bottom = 16.dp)
            )

            Text(
                text = "Crea una nueva cuenta de usuario y asigna su rol (Admin/Empleado).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // 1. Campo Nombre
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre Completo") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            // 2. Campo Correo
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Correo Electrónico") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions( // <-- USO SIMPLIFICADO
                    keyboardType = KeyboardType.Email
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            // 3. Campo Contraseña
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña (mín. 6 caracteres)") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                keyboardOptions = KeyboardOptions( // <-- USO SIMPLIFICADO
                    keyboardType = KeyboardType.Password
                ),
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))

            // 4. Selector de Rol (Diseño Intuitivo con Dropdown)
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clickable { isDropdownExpanded = true },
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Security, contentDescription = "Rol", tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = "Rol: ${selectedRole.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Expandir")
                }

                // Menú desplegable para la selección
                DropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f) // El menú no debe ser más ancho que su contenedor
                ) {
                    roles.forEach { role ->
                        DropdownMenuItem(
                            text = { Text(role.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }) },
                            onClick = {
                                selectedRole = role
                                isDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // 5. Botón de Registro
            Button(
                onClick = { registerNewUser() },
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text("CREAR CUENTA", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(24.dp))

            // 6. Mensaje de estado/error (Animado para mejor UX)
            AnimatedVisibility(
                visible = message.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val isSuccess = message.contains("éxito")
                Text(
                    text = message,
                    color = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}