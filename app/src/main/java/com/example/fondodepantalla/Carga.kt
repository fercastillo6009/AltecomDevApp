package com.example.fondodepantalla

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.delay
import java.util.*

class Carga : ComponentActivity() {

    private val PREFS_NAME = "AsistenciaPrefs"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CargaScreen()
        }
    }

    @Composable
    fun CargaScreen() {
        val context = LocalContext.current
        val chocoCooky = FontFamily.Cursive

        // Lottie animación
        val composition by rememberLottieComposition(LottieCompositionSpec.Asset("carga_animacion.json"))
        val progress by animateLottieCompositionAsState(composition, iterations = LottieConstants.IterateForever)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF2F78AF)), // Fondo azul
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {

                Image(
                    painter = painterResource(id = getLogoPorMes()),
                    contentDescription = "Logo",
                    modifier = Modifier.size(180.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = context.getString(R.string.app_name),
                    fontFamily = chocoCooky,
                    fontSize = 24.sp,
                    color = Color(0xFFFDF9EA)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = context.getString(R.string.desarrollador),
                    fontFamily = chocoCooky,
                    fontSize = 18.sp,
                    color = Color(0xFFFDF9EB)
                )

                Spacer(modifier = Modifier.height(20.dp))

                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier
                        .height(150.dp)
                        .fillMaxWidth()
                )
            }
        }

        // Lógica de espera y navegación
        LaunchedEffect(Unit) {
            delay(3000L)
            val mAuth = FirebaseAuth.getInstance()
            val user: FirebaseUser? = mAuth.currentUser

            if (user != null) {
                val prefs: SharedPreferences =
                    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putLong("ultima_apertura", System.currentTimeMillis()).apply()

                programarRecordatorio(context)
            }

            val intent = if (user != null) {
                Intent(context, MainActivityAdministrador::class.java)
            } else {
                Intent(context, InicioSesion::class.java)
            }
            context.startActivity(intent)
            (context as ComponentActivity).finish()
        }
    }

    private fun getLogoPorMes(): Int {
        val mes = Calendar.getInstance().get(Calendar.MONTH) + 1
        return when (mes) {
            2 -> R.drawable.logofeb
            9 -> R.drawable.logosep
            10 -> R.drawable.logooct
            11 -> R.drawable.logonov
            12 -> R.drawable.logodec
            else -> R.drawable.logodev
        }
    }

    private fun programarRecordatorio(context: Context) {
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
