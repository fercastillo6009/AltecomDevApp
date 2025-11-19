@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.fondodepantalla

import android.Manifest
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
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
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

// --- NUEVO ---
import android.util.Log
import androidx.appcompat.app.AlertDialog
// --- FIN NUEVO ---


class MainActivityAdministrador : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
    BottomNavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var firebaseAuth: FirebaseAuth
    private var user: FirebaseUser? = null
    private lateinit var bottomNavigationView: BottomNavigationView
    private var userRole = "admin" // Valor por defecto
    private lateinit var toolbar: Toolbar
    private lateinit var navController: NavHostController

    private val PREFS_NAME = "SesionPrefs"
    private val KEY_TOAST_MOSTRADO = "toastMostrado"
    private var tapCount = 0
    private var lastTapTime: Long = 0
    private var toast: Toast? = null
    private var gifPool: MutableList<Int> = ArrayList()

    // --- Definición del lanzador de permisos de ubicación ---
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

            if (fineLocationGranted) {
                // ¡CORREGIDO!
                Toast.makeText(this, "Permiso de ubicación precisa otorgado", Toast.LENGTH_SHORT).show()
            } else if (coarseLocationGranted) {
                // ¡CORREGIDO!
                Toast.makeText(this, "Permiso de ubicación aproximada otorgado", Toast.LENGTH_SHORT).show()
            } else {
                // ¡CORREGIDO!
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
            }
        }

    // --- NUEVO! Lanzador para permiso de SEGUNDO PLANO ---
    private val requestBackgroundPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                Toast.makeText(this, "Permiso de fondo otorgado. Iniciando rastreo.", Toast.LENGTH_SHORT).show()
                startLocationService() // Inicia el servicio ahora que tenemos permiso
            } else {
                Toast.makeText(this, "El permiso de fondo es necesario para el rastreo de tareas.", Toast.LENGTH_LONG).show()
                // Opcional: Mostrar un diálogo explicando por qué es necesario
                showBackgroundPermissionRationale()
            }
        }
    // --- FIN NUEVO ---

    // Lista de permisos requeridos
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    // --- Fin de la definición de permisos ---


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_administrador)

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

        // Solicitar permiso de Notificaciones (Android 13+)
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

        // --- Solicitud de permisos de Ubicación ---
        if (!allPermissionsGranted()) {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
        // --- Fin de la solicitud ---

        toolbar = findViewById(R.id.toolbarA)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout_A)
        val navigationView = findViewById<NavigationView>(R.id.nav_viewA)
        navigationView.setNavigationItemSelectedListener(this)
        navigationView.itemIconTintList = null

        // Verificar rol del usuario
        val db = FirebaseFirestore.getInstance()
        db.collection("usuarios").document(user!!.uid).get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val rol = documentSnapshot.getString("rol")
                    userRole = rol ?: "empleado"
                    val menu = navigationView.menu

                    // --- ¡NUEVA LÓGICA DE INICIO DE SERVICIO! ---
                    if (userRole == "empleado" || userRole == "soporte") {
                        // Es un rol que SÍ debe ser rastreado.
                        // Iniciar el proceso de verificación de permisos y arranque del servicio.
                        checkAndStartBackgroundTracking()
                    }
                    // --- FIN DE LA NUEVA LÓGICA ---

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
                        menu.findItem(R.id.ListarAdmin).isVisible = false
                        // --- ¡NUEVO! Habilitar el item del monitor para el admin ---
                        // (Asegúrate de que el ID R.id.MonitorEmpleados exista en tu archivo XML de menú)
                        menu.findItem(R.id.MonitorEmpleados).isVisible = true

                        setupComposeNavHost("inicio_admin") // Ruta inicial para Admin

                        if (savedInstanceState == null) {
                            navigationView.setCheckedItem(R.id.InicioAdmin)
                        }

                    } else { // Rol: "empleado", "soporte" o cualquier otro
                        bottomNavigationView.visibility = View.VISIBLE
                        navigationView.visibility = View.GONE
                        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

                        supportActionBar?.setDisplayHomeAsUpEnabled(false)

                        menu.findItem(R.id.AsignarTarea).isVisible = false
                        menu.findItem(R.id.ResumenAsistencias).isVisible = false
                        menu.findItem(R.id.CrearUsuario).isVisible = false
                        menu.findItem(R.id.ListarAdmin).isVisible = false
                        // --- ¡NUEVO! Ocultar el item del monitor para otros roles ---
                        menu.findItem(R.id.MonitorEmpleados).isVisible = false

                        setupComposeNavHost("inicio_empleado") // Ruta inicial para Empleado/Soporte

                        if (savedInstanceState == null) {
                            bottomNavigationView.selectedItemId = R.id.InicioEmpleado
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al verificar el rol", Toast.LENGTH_SHORT).show()
            }

        // Logo dinámico y Easter Egg
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
                // ¡CORREGIDO!
                toast = Toast.makeText(this, "Presiona $remaining veces más...", Toast.LENGTH_SHORT)
                toast!!.show()
            } else if (tapCount == TAP_THRESHOLD) {
                tapCount = 0
                if (toast != null) toast!!.cancel()
                // ¡CORREGIDO!
                toast = Toast.makeText(this, "¡Modo desarrollador activado!", Toast.LENGTH_SHORT)
                toast!!.show()
                startDeveloperEasterEgg()
            }
        }

        ComprobandoInicioSesion()
        AppUpdater.checkForUpdate(this)
        iniciarPingPeriodico()

        // Controlar el botón de "atrás"
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START) && "admin" == userRole) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    // No hacer nada (bloquea el botón de "atrás")
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    private fun setupComposeNavHost(startRoute: String) {
        findViewById<ComposeView>(R.id.compose_nav_host_container).setContent {
            MaterialTheme {
                navController = rememberNavController()
                AppNavHost(
                    navController = navController,
                    startRoute = startRoute
                )
            }
        }
    }

    // --- Función helper para verificar permisos ---
    private fun allPermissionsGranted(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
        }
    }
    // --- Fin función helper ---

    // Ping Automático
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

    // Logo por mes
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

    // Manejador de navegación (para Drawer y BottomNav)
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (!::navController.isInitialized) return false

        var route: String? = null

        if ("admin" == userRole) {
            // Lógica para el Drawer (Admin)
            when (item.itemId) {
                R.id.InicioAdmin -> route = "inicio_admin"
                R.id.PerfilAdmin -> route = "perfil"
                R.id.CrearUsuario -> route = "crear_usuario"
                R.id.RegistrarAdmin -> route = "asistencia"
                R.id.AsignarTarea -> route = "asignar_tarea"
                R.id.ResumenAsistencias -> route = "resumen_asistencias"
                // --- ¡NUEVO! Añadir la ruta de navegación para el monitor ---
                // (Asegúrate de que la ruta "monitor_empleados" exista en tu AppNavHost)
                R.id.MonitorEmpleados -> route = "monitor_empleados"
                R.id.SalirAdmin -> CerrarSesion()
            }
            drawerLayout.closeDrawer(GravityCompat.START)

        } else {
            // Lógica para Bottom Nav (Empleado, Soporte, etc.)
            when (item.itemId) {
                R.id.InicioEmpleado -> route = "inicio_empleado"
                R.id.AsistenciaEmpleado -> route = "asistencia_empleado"
                R.id.PerfilEmpleado -> route = "perfil"
                R.id.SalirEmpleado -> CerrarSesion()

                // Aquí se diferencia entre roles de empleado
                R.id.ListaEmpleado -> {
                    route = if ("soporte" == userRole) {
                        "lista_soporte"
                    } else {
                        "lista_empleado"
                    }
                }
            }
        }

        // Ejecutar la navegación de Compose
        route?.let {
            navController.navigate(it) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }

        return true
    }

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

    private fun CerrarSesion() {
        // --- ¡NUEVO! Detener el servicio al cerrar sesión ---
        if (userRole == "empleado" || userRole == "soporte") {
            val serviceIntent = Intent(this, LocationTrackingService::class.java).apply {
                action = LocationTrackingService.ACTION_STOP
            }
            stopService(serviceIntent)
        }
        // --- FIN DE LÓGICA DE SERVICIO ---

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

    // --- Lógica del Easter Egg ---
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


    // --- ¡NUEVAS FUNCIONES HELPER! (Añadir al final de tu clase) ---

    /**
     * Verifica los permisos de segundo plano e inicia el servicio si se cumplen.
     */
    private fun checkAndStartBackgroundTracking() {
        // Primero, asegurarnos de que tenemos el permiso de ubicación normal
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w("TrackService", "No se puede iniciar el rastreo de fondo sin permiso de ubicación fina.")
            // El lanzador principal (requestPermissionLauncher) ya debería haberse encargado de esto.
            return
        }

        // Si estamos en Android 10 (Q) o superior, necesitamos el permiso de fondo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                    // Ya tenemos permiso, iniciar el servicio
                    startLocationService()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION) -> {
                    // Mostrar una explicación antes de pedir
                    showBackgroundPermissionRationale()
                }
                else -> {
                    // Pedir el permiso de fondo
                    requestBackgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
            }
        } else {
            // En versiones anteriores a Android 10, el permiso FINE_LOCATION es suficiente
            startLocationService()
        }
    }

    /**
     * Inicia el LocationTrackingService
     */
    private fun startLocationService() {
        // Asegurarnos de que el usuario sigue logueado
        val currentUserId = firebaseAuth.currentUser?.uid
        if (currentUserId == null) {
            Toast.makeText(this, "Error de sesión, no se puede iniciar rastreo.", Toast.LENGTH_SHORT).show()
            return
        }

        val serviceIntent = Intent(this, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_START
            putExtra(LocationTrackingService.EXTRA_USER_ID, currentUserId)
        }
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    /**
     * Muestra un diálogo explicando por qué se necesita el permiso de fondo.
     */
    private fun showBackgroundPermissionRationale() {
        AlertDialog.Builder(this)
            .setTitle("Permiso de Ubicación en Segundo Plano")
            .setMessage("Para registrar tu ubicación durante una tarea activa (incluso cuando la app está cerrada), Altecomdev necesita el permiso de ubicación 'Permitir todo el tiempo'.")
            .setPositiveButton("Entendido") { dialog, _ ->
                requestBackgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(this, "No se podrá rastrear la ubicación de la tarea.", Toast.LENGTH_LONG).show()
            }
            .create()
            .show()
    }

    // --- FIN DE NUEVAS FUNCIONES ---


    companion object {
        private const val TAP_THRESHOLD = 10
        private const val RESET_DELAY: Long = 2000
    }
}