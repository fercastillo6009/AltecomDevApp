package com.example.fondodepantalla

// Imports de Lógica y Tareas de Fondo
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import java.util.*
import java.util.concurrent.Executor

// Imports de Compose UI
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.google.accompanist.systemuicontroller.rememberSystemUiController

class InicioSesion : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    // Variable movida de 'Carga.kt'
    private val PREFS_NAME = "AsistenciaPrefs"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAuth = FirebaseAuth.getInstance()

        // --- INICIO: LÓGICA DE COMPROBACIÓN DE 'Carga.kt' ---
        val currentUser: FirebaseUser? = firebaseAuth.currentUser
        if (currentUser != null) {
            // El usuario YA ESTÁ LOGUEADO. Ejecutamos tareas de fondo y redirigimos.

            // 1. Guardar última apertura
            val prefs: SharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putLong("ultima_apertura", System.currentTimeMillis()).apply()

            // 2. Programar recordatorio
            programarRecordatorio(this)

            // 3. Redirigir a la actividad principal y cerrar esta
            iniciarMainActivity() // Esta función ya incluye la animación y finish()

            // 4. Salir de onCreate para no llamar a setContent
            return
        }
        // --- FIN: LÓGICA DE COMPROBACIÓN ---

        // Si currentUser es null, continuamos y mostramos la pantalla de login

        credentialManager = CredentialManager(applicationContext)

        // Inicialización de Biometría
        executor = ContextCompat.getMainExecutor(this)

        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                // ... (tus callbacks de biometría: onError, onFailed) ...
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext, "Error de Biometría: $errString", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    iniciarSesionFirebaseConBiometria()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Fallo al escanear huella", Toast.LENGTH_SHORT).show()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Inicio de Sesión Rápido")
            .setSubtitle("Usa tu huella para acceder con tu última cuenta")
            .setNegativeButtonText("Usar Correo/Contraseña")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()

        // Dibujar la UI de Login
        setContent {
            MaterialTheme {
                LoginScreen(
                    firebaseAuth = firebaseAuth,
                    credentialManager = credentialManager,
                    onBiometricClick = { autenticarConBiometria() }
                )
            }
        }
    }

    private fun autenticarConBiometria() {
        // ... (tu lógica de autenticarConBiometria, sin cambios) ...
        val credentials: Pair<String?, String?> = credentialManager.getCredentials()
        val emailGuardado = credentials.first
        val passwordGuardado = credentials.second

        if (emailGuardado.isNullOrEmpty() || passwordGuardado.isNullOrEmpty()) {
            Toast.makeText(this, "Inicia sesión una vez manualmente para habilitar la biometría.", Toast.LENGTH_LONG).show()
            return
        }

        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                Log.d("Biometria", "El dispositivo puede autenticar con biometría.")
                biometricPrompt.authenticate(promptInfo)
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Toast.makeText(this, "No hay biometría registrada en el dispositivo.", Toast.LENGTH_LONG).show()
            }
            else -> {
                Toast.makeText(this, "Biometría no disponible o error.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun iniciarSesionFirebaseConBiometria() {
        // ... (lógica de obtener credenciales) ...
        val credentials: Pair<String?, String?> = credentialManager.getCredentials()
        val email = credentials.first
        val password = credentials.second

        if (email.isNullOrEmpty() || password.isNullOrEmpty()) {
            Toast.makeText(this, "Error: No se encontraron credenciales guardadas.", Toast.LENGTH_LONG).show()
            return
        }

        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Acceso con Huella Exitoso!", Toast.LENGTH_SHORT).show()

                    // --- TAREAS DE FONDO ---
                    val prefs: SharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    prefs.edit().putLong("ultima_apertura", System.currentTimeMillis()).apply()
                    programarRecordatorio(this)

                    iniciarMainActivity() // Navegar con animación
                } else {
                    // ... (manejo de error) ...
                    Log.e("Firebase", "Fallo login con credenciales guardadas: ${task.exception?.message}")
                    Toast.makeText(this, "Error de autenticación. Vuelva a iniciar sesión manualmente.", Toast.LENGTH_LONG).show()
                    credentialManager.clearCredentials()
                }
            }
    }

    /**
     * Función centralizada para navegar a MainActivity.
     * APLICA LA NUEVA TRANSICIÓN Y CIERRA LA ACTIVIDAD ACTUAL.
     */
    private fun iniciarMainActivity() {
        val intent = Intent(this, MainActivityAdministrador::class.java)
        startActivity(intent)

        // *** NUEVA LÍNEA: Aplicar animación de fundido ***
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)

        finish() // Cerrar InicioSesion
    }

    // --- Función movida de 'Carga.kt' ---
    private fun programarRecordatorio(context: Context) {
        // ... (tu lógica de programarRecordatorio, sin cambios) ...
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, RecordatorioReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 9)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)

        if (calendar.timeInMillis < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }
}

// ===================================================================
//   DISEÑO DE LA PANTALLA DE LOGIN (Glassmorphism)
// ===================================================================

@Composable
fun LoginScreen(firebaseAuth: FirebaseAuth, credentialManager: CredentialManager, onBiometricClick: () -> Unit) {
    var correo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val prefs = context.getSharedPreferences("AsistenciaPrefs", Context.MODE_PRIVATE)

    // Lottie
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset("iniciosesion.json"))
    val progress by animateLottieCompositionAsState(composition, iterations = LottieConstants.IterateForever)

    // System UI
    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setSystemBarsColor(
            color = Color.Transparent,
            darkIcons = false
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. FONDO
        Image(
            painter = painterResource(id = R.drawable.background_log),
            contentDescription = "Fondo",
            modifier = Modifier
                .fillMaxSize()
                .blur(8.dp), // Efecto desenfoque
            contentScale = ContentScale.Crop
        )

        // 2. TARJETA DE VIDRIO
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0x40FFFFFF)) // Fondo de vidrio
                    .border(
                        1.dp,
                        Color(0x80FFFFFF), // Borde sutil
                        RoundedCornerShape(32.dp)
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // ... (Lottie, Textos "Bienvenido", etc. ...
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier
                        .size(180.dp)
                )

                Text(
                    text = "Bienvenido",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "Inicia sesión para continuar",
                    fontSize = 16.sp,
                    color = Color(0xCCFFFFFF)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Campo de Correo
                TextField(
                    value = correo,
                    onValueChange = { correo = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Correo Electrónico") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Correo"
                        )
                    },
                    // ... (resto de las propiedades del TextField: singleLine, shape, colors, keyboardOptions) ...
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0x80FFFFFF),
                        unfocusedContainerColor = Color(0x80FFFFFF),
                        disabledContainerColor = Color(0x80FFFFFF),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color(0xFF333333),
                        focusedLabelColor = Color(0xFF0363F4),
                        unfocusedLabelColor = Color.Gray,
                        focusedLeadingIconColor = Color(0xFF0363F4),
                        unfocusedLeadingIconColor = Color.Gray
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Campo de Contraseña
                TextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Contraseña") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Contraseña"
                        )
                    },
                    // ... (resto de las propiedades del TextField: singleLine, visualTransformation, shape, colors) ...
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0x80FFFFFF),
                        unfocusedContainerColor = Color(0x80FFFFFF),
                        disabledContainerColor = Color(0x80FFFFFF),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color(0xFF333333),
                        focusedLabelColor = Color(0xFF0363F4),
                        unfocusedLabelColor = Color.Gray,
                        focusedLeadingIconColor = Color(0xFF0363F4),
                        unfocusedLeadingIconColor = Color.Gray
                    )
                )

                // Texto de Error
                if (error.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = error,
                        color = Color(0xFFFACDCD),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Botón de Acceder
                Button(
                    onClick = {
                        when {
                            !Patterns.EMAIL_ADDRESS.matcher(correo).matches() -> error = "Correo inválido"
                            password.length < 6 -> error = "Contraseña mínima 6 caracteres"
                            else -> {
                                loading = true
                                firebaseAuth.signInWithEmailAndPassword(correo, password)
                                    .addOnCompleteListener { task ->
                                        loading = false
                                        if (task.isSuccessful) {
                                            // Guardar credenciales y prefs
                                            credentialManager.saveCredentials(correo, password)
                                            prefs.edit().putLong("ultima_apertura", System.currentTimeMillis()).apply()

                                            // Navegar
                                            val intent = Intent(context, MainActivityAdministrador::class.java)
                                            context.startActivity(intent)

                                            // *** NUEVA LÍNEA: Aplicar animación de fundido ***
                                            if (context is AppCompatActivity) {
                                                context.overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                                            }

                                            // Finalizar
                                            (context as AppCompatActivity).finish()
                                        } else {
                                            error = "Correo o contraseña incorrectos"
                                        }
                                    }
                            }
                        }
                    },
                    // ... (resto del botón: modifier, shape, colors, contentPadding) ...
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        // ... (Box del gradiente) ...
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.horizontalGradient(
                                    listOf(Color(0xFF0363F4), Color(0xFF003C9E))
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Acceder", color = Color.White, fontSize = 18.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botón de Huella
                OutlinedButton(
                    onClick = { onBiometricClick() },
                    // ... (resto del OutlinedButton: modifier, shape, border) ...
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF0363F4))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Fingerprint,
                        contentDescription = "Huella",
                        tint = Color(0xFF0363F4),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Acceder con huella",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }
        }

        // 3. OVERLAY DE CARGA
        if (loading) {
            Box(
                // ... (Overlay de carga) ...
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF0363F4),
                    strokeWidth = 5.dp,
                    modifier = Modifier.size(60.dp)
                )
            }
        }
    }
}