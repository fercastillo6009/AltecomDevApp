package com.example.fondodepantalla

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*
import com.google.firebase.auth.FirebaseAuth

class InicioSesion : ComponentActivity() {

    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAuth = FirebaseAuth.getInstance()

        setContent {
            MaterialTheme {
                LoginScreen(firebaseAuth)
            }
        }
    }
}

@Composable
fun LoginScreen(firebaseAuth: FirebaseAuth) {
    var correo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Lottie animación
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset("iniciosesion.json"))
    val progress by animateLottieCompositionAsState(composition, iterations = LottieConstants.IterateForever)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Fondo
        Image(
            painter = painterResource(id = R.drawable.background_log),
            contentDescription = "Fondo",
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )

        // Contenido principal
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Iniciar Sesión",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier
                    .height(150.dp)
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = correo,
                onValueChange = { correo = it },
                label = { Text("Correo") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(25.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedBorderColor = Color(0xFF6200EE),
                    unfocusedBorderColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(25.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedBorderColor = Color(0xFF6200EE),
                    unfocusedBorderColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ---------- Botón personalizado ----------
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
                                        val intent = Intent(context, MainActivityAdministrador::class.java)
                                        context.startActivity(intent)
                                    } else {
                                        error = "Correo o contraseña incorrectos"
                                    }
                                }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(Color(0xFF0363F4), Color(0xFF003C9E))
                            ),
                            shape = RoundedCornerShape(25.dp)
                        )
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Acceder", color = Color.White)
                }
            }

            if (error.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = error, color = Color.Red)
            }
        }

        // Overlay de carga
        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x80000000)),
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
