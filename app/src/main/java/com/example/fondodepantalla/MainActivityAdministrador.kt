@file:OptIn(ExperimentalMaterial3Api::class) // 📍 CAMBIO: Añadido para M3
package com.example.fondodepantalla

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.Toolbar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.bumptech.glide.Glide
import com.example.fondodepantalla.Utils.AppUpdater
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.Random
import androidx.activity.OnBackPressedCallback

// 📍 CAMBIO: Ya no implementa 'OnNavigationItemSelectedListener' dos veces
class MainActivityAdministrador : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
    BottomNavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var firebaseAuth: FirebaseAuth
    private var user: FirebaseUser? = null
    private lateinit var bottomNavigationView: BottomNavigationView
    private var userRole = "admin" // Valor por defecto
    private lateinit var toolbar: Toolbar

    // 📍 CAMBIO: El NavController de Compose
    private lateinit var navController: NavHostController

    private val PREFS_NAME = "SesionPrefs"
    private val KEY_TOAST_MOSTRADO = "toastMostrado"
    private var tapCount = 0
    private var lastTapTime: Long = 0
    private var toast: Toast? = null

    // 📍 CAMBIO: Pool de Gifs ahora es de Kotlin
    private var gifPool: MutableList<Int> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_administrador)

        // ... (Inicialización de BottomNav, Auth, AppCheck, Permisos...) ...
        bottomNavigationView = findViewById(R.id.bottom_nav_view)
        bottomNavigationView.setOnNavigationItemSelectedListener(this)
        bottomNavigationView.itemIconTintList = null

        firebaseAuth = FirebaseAuth.getInstance()
        user = firebaseAuth.currentUser

        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )

        if (user == null) {
            startActivity(Intent(this, InicioSesion::class.java))
            finish()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }

        toolbar = findViewById(R.id.toolbarA)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout_A)
        val navigationView = findViewById<NavigationView>(R.id.nav_viewA)
        navigationView.setNavigationItemSelectedListener(this)
        navigationView.itemIconTintList = null

        // 🔹 Verificar rol del usuario
        val db = FirebaseFirestore.getInstance()
        db.collection("usuarios").document(user!!.uid).get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val rol = documentSnapshot.getString("rol")
                    userRole = rol ?: "empleado" // Asignar el rol
                    val menu = navigationView.menu

                    if ("admin" == rol) {
                        bottomNavigationView.visibility = View.GONE
                        navigationView.visibility = View.VISIBLE
                        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)

                        val toggle = ActionBarDrawerToggle(
                            this, drawerLayout, toolbar,
                            R.string.navigation_drawer_open,
                            R.string.navigation_drawer_close
                        )
                        drawerLayout.addDrawerListener(toggle)
                        toggle.syncState()

                        menu.findItem(R.id.AsignarTarea).isVisible = true
                        menu.findItem(R.id.ResumenAsistencias).isVisible = true
                        menu.findItem(R.id.CrearUsuario).isVisible = true
                        // 📍 CAMBIO: Esta ruta ahora es compartida
                        menu.findItem(R.id.ListarAdmin).isVisible = false

                        // 📍 CAMBIO: Configurar ComposeView
                        setupComposeNavHost("inicio_admin") // Ruta inicial para Admin

                        if (savedInstanceState == null) {
                            navigationView.setCheckedItem(R.id.InicioAdmin)
                        }

                    } else { // Rol: "empleado"
                        bottomNavigationView.visibility = View.VISIBLE
                        navigationView.visibility = View.GONE
                        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

                        supportActionBar?.setDisplayHomeAsUpEnabled(false)

                        menu.findItem(R.id.AsignarTarea).isVisible = false
                        menu.findItem(R.id.ResumenAsistencias).isVisible = false
                        menu.findItem(R.id.CrearUsuario).isVisible = false
                        menu.findItem(R.id.ListarAdmin).isVisible = false // Oculto para empleado

                        // 📍 CAMBIO: Configurar ComposeView
                        setupComposeNavHost("inicio_empleado") // Ruta inicial para Empleado

                        if (savedInstanceState == null) {
                            bottomNavigationView.selectedItemId = R.id.InicioEmpleado
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al verificar el rol", Toast.LENGTH_SHORT).show()
            }

        // 🔹 Logo dinámico y Easter Egg (Sin cambios)
        val headerView = navigationView.getHeaderView(0)
        val logo = headerView.findViewById<AppCompatImageView>(R.id.logo_encabezado)
        logo.setImageResource(getLogoPorMes())
        logo.setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastTapTime > RESET_DELAY) {
                tapCount = 0
            }
            lastTapTime = currentTime
            tapCount++
            if (toast != null) toast!!.cancel()
            if (tapCount < TAP_THRESHOLD) {
                val remaining = TAP_THRESHOLD - tapCount
                toast = Toast.makeText(this, "Presiona $remaining veces más...", Toast.LENGTH_SHORT)
                toast!!.show()
            } else if (tapCount == TAP_THRESHOLD) {
                tapCount = 0
                if (toast != null) toast!!.cancel()
                toast = Toast.makeText(this, "¡Modo desarrollador activado!", Toast.LENGTH_SHORT)
                toast!!.show()
                startDeveloperEasterEgg()
            }
        }

        ComprobandoInicioSesion()
        AppUpdater.checkForUpdate(this)
        iniciarPingPeriodico()

        val callback = object : OnBackPressedCallback(true) { // 'true' = está habilitado
            override fun handleOnBackPressed() {
                // Aquí va tu lógica:
                if (drawerLayout.isDrawerOpen(GravityCompat.START) && "admin" == userRole) {
                    // Si el drawer está abierto Y es un admin, ciérralo.
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    // Si el drawer está cerrado O no es admin, no hagas NADA.
                    // Al dejar esto vacío, el botón de retroceso
                    // se "consume" pero no realiza ninguna acción (ni muestra el Toast).
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    // 📍 CAMBIO: Nueva función para inicializar Compose
    private fun setupComposeNavHost(startRoute: String) {
        findViewById<ComposeView>(R.id.compose_nav_host_container).setContent {
            // Es importante envolver tu NavHost en un tema
            MaterialTheme {
                // Inicializa el NavController aquí
                navController = rememberNavController()
                // Llama a tu AppNavHost
                AppNavHost(
                    navController = navController,
                    startRoute = startRoute
                )
            }
        }
    }


    // ======= Ping Automático (Convertido a Kotlin) =======
    private val handlerPing = Handler(Looper.getMainLooper())
    private val pingRunnable: Runnable = object : Runnable {
        override fun run() {
            Thread {
                try {
                    val url = URL("https://altecomasistencia.onrender.com/")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    conn.connect()
                    val responseCode = conn.responseCode
                    conn.disconnect()
                    runOnUiThread {
                        if (responseCode == 200) {
                            println("✅ Ping exitoso")
                        } else {
                            println("⚠️ Ping falló con código: $responseCode")
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()
            handlerPing.postDelayed(this, 50000)
        }
    }

    private fun iniciarPingPeriodico() {
        handlerPing.post(pingRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handlerPing.removeCallbacks(pingRunnable)
    }

    //Logo por mes (Convertido a Kotlin)
    private fun getLogoPorMes(): Int {
        val mes = Calendar.getInstance()[Calendar.MONTH] + 1
        return when (mes) {
            2 -> R.drawable.logofeb
            9 -> R.drawable.logosep
            10 -> R.drawable.logooct
            11 -> R.drawable.logonov
            12 -> R.drawable.logodec
            else -> R.drawable.logodev
        }
    }

    // 📍 CAMBIO: onNavigationItemSelected AHORA USA NAVCONTROLLER
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Asegurarse de que el navController esté inicializado
        if (!::navController.isInitialized) return false

        var route: String? = null

        // 1. Lógica para el Drawer (Admin)
        if ("admin" == userRole) {
            when (item.itemId) {
                R.id.InicioAdmin -> route = "inicio_admin"
                R.id.PerfilAdmin -> route = "perfil"
                R.id.CrearUsuario -> route = "crear_usuario"
                R.id.RegistrarAdmin -> route = "asistencia"
                R.id.AsignarTarea -> route = "asignar_tarea"
                R.id.ResumenAsistencias -> route = "resumen_asistencias"
                R.id.SalirAdmin -> CerrarSesion()
            }
            drawerLayout.closeDrawer(GravityCompat.START)

            // 2. Lógica para el Bottom Nav (Empleado)
        } else if ("empleado" == userRole) {
            when (item.itemId) {
                R.id.InicioEmpleado -> route = "inicio_empleado"
                R.id.AsistenciaEmpleado -> route = "asistencia_empleado"
                R.id.ListaEmpleado -> route = "lista_empleado"
                R.id.PerfilEmpleado -> route = "perfil" // Reutiliza la ruta
                R.id.SalirEmpleado -> CerrarSesion()
            }
        }

        // 3. Ejecutar la navegación de Compose
        route?.let {
            navController.navigate(it) {
                // Pop up to the start destination para evitar acumular stack
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                // Evita múltiples copias del mismo destino
                launchSingleTop = true
                // Restaura el estado al re-seleccionar
                restoreState = true
            }
        }

        return true
    }

    // ComprobandoInicioSesion (Convertido a Kotlin)
    private fun ComprobandoInicioSesion() {
        if (user != null) {
            val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val toastMostrado = prefs.getBoolean(KEY_TOAST_MOSTRADO, false)
            if (!toastMostrado) {
                Toast.makeText(this, "Se ha iniciado sesión", Toast.LENGTH_SHORT).show()
                prefs.edit().putBoolean(KEY_TOAST_MOSTRADO, true).apply()
            }
        } else {
            startActivity(Intent(this, InicioSesion::class.java))
            finish()
        }
    }

    // CerrarSesion (Sin cambios, tu lógica ya era correcta)
    private fun CerrarSesion() {
        firebaseAuth.signOut()
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_TOAST_MOSTRADO, false)
            .apply()
        Toast.makeText(this, "Cerraste sesión exitosamente", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, InicioSesion::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // onStart (Convertido a Kotlin)
    override fun onStart() {
        super.onStart()
        user = firebaseAuth.currentUser
        if (user == null) {
            startActivity(Intent(this, InicioSesion::class.java))
            finish()
        } else {
            val prefs = getSharedPreferences("AsistenciaPrefs", MODE_PRIVATE)
            prefs.edit().putLong("ultima_apertura", System.currentTimeMillis()).apply()
        }
    }

    // --- Easter Egg (Convertido a Kotlin) ---
    private fun startDeveloperEasterEgg() {
        val rootView = findViewById<ViewGroup>(android.R.id.content)
        val overlay = FrameLayout(this)
        overlay.setBackgroundColor(Color.parseColor("#80000000"))
        overlay.alpha = 0f
        rootView.addView(overlay, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))
        overlay.animate().alpha(1f).setDuration(400).start()

        val gifs = intArrayOf(
            R.drawable.khalid, R.drawable.a, R.drawable.b, R.drawable.c,
            R.drawable.d, R.drawable.e, R.drawable.o, R.drawable.g,
            R.drawable.h, R.drawable.i, R.drawable.p
        )
        if (gifPool.isEmpty()) {
            for (gif in gifs) gifPool.add(gif)
        }

        val randomIndex = Random().nextInt(gifPool.size)
        val selectedGif = gifPool.removeAt(randomIndex)

        val gifView = ImageView(this)
        gifView.layoutParams = FrameLayout.LayoutParams(250, 250)
        overlay.addView(gifView)
        Glide.with(this).asGif().load(selectedGif).into(gifView)

        val dm = resources.displayMetrics
        val screenWidth = dm.widthPixels
        val screenHeight = dm.heightPixels
        val random = Random()
        val anim: ObjectAnimator

        when (selectedGif) {
            R.drawable.o -> {
                val randomX = random.nextInt(screenWidth - 300).toFloat()
                gifView.x = randomX
                gifView.y = -300f
                anim = ObjectAnimator.ofFloat(gifView, "y", -300f, (screenHeight + 300).toFloat())
            }
            R.drawable.e, R.drawable.g, R.drawable.h, R.drawable.i -> {
                val randomY = (random.nextInt(screenHeight / 2) + 100).toFloat()
                gifView.y = randomY
                gifView.x = (screenWidth + 300).toFloat()
                anim = ObjectAnimator.ofFloat(gifView, "x", (screenWidth + 300).toFloat(), -300f)
            }
            R.drawable.p -> {
                val randomX = random.nextInt(screenWidth - 300).toFloat()
                gifView.x = randomX
                gifView.y = (screenHeight + 300).toFloat()
                anim = ObjectAnimator.ofFloat(gifView, "y", (screenHeight + 300).toFloat(), -300f)
            }
            else -> {
                val randomY = (random.nextInt(screenHeight / 2) + 100).toFloat()
                gifView.y = randomY
                gifView.x = -300f
                anim = ObjectAnimator.ofFloat(gifView, "x", -300f, (screenWidth + 300).toFloat())
            }
        }
        anim.duration = (6000 + random.nextInt(4000)).toLong()
        anim.interpolator = LinearInterpolator()
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                gifView.animate().alpha(0f).setDuration(800).start()
                overlay.animate().alpha(0f).setDuration(800)
                    .withEndAction { (rootView as ViewGroup).removeView(overlay) }
                    .start()
            }
        })
        anim.start()
    }

    // 📍 CAMBIO: Constantes de Easter Egg
    companion object {
        private const val TAP_THRESHOLD = 10
        private const val RESET_DELAY: Long = 2000
    }
}