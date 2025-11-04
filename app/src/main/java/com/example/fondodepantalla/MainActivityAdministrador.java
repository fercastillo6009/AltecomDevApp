package com.example.fondodepantalla;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.fondodepantalla.FragmentosAdministrador.InicioAdmin;
import com.example.fondodepantalla.FragmentosAdministrador.ListaAdmin;
import com.example.fondodepantalla.FragmentosAdministrador.PerfilAdmin;
import com.example.fondodepantalla.FragmentosAdministrador.AsistenciaFragment;
import com.example.fondodepantalla.FragmentosAdministrador.ResumenFragment;
import com.example.fondodepantalla.FragmentosAdministrador.AsignarTareaFragment;
import com.example.fondodepantalla.Utils.AppUpdater;
import com.google.android.material.bottomnavigation.BottomNavigationView; // Importar BottomNavigationView
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.Calendar;

// Implementamos también el listener para BottomNavigationView
public class MainActivityAdministrador extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, BottomNavigationView.OnNavigationItemSelectedListener {

    DrawerLayout drawerLayout;
    FirebaseAuth firebaseAuth;
    FirebaseUser user;
    // Nueva variable para BottomNavigationView
    BottomNavigationView bottomNavigationView;
    // Variable para guardar el rol
    private String userRole = "admin"; // Valor predeterminado
    Toolbar toolbar; // Declarada aquí para accesibilidad global

    private static final String PREFS_NAME = "SesionPrefs";
    private static final String KEY_TOAST_MOSTRADO = "toastMostrado";
    private int tapCount = 0;
    private long lastTapTime = 0;
    private static final int TAP_THRESHOLD = 10;
    private static final long RESET_DELAY = 2000;
    private Toast toast;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_administrador);

        final View rootView = findViewById(R.id.rootLayout); // rootLayout es tu contenedor principal
        rootView.setVisibility(View.INVISIBLE);

        rootView.post(() -> {
            int cx = getIntent().getIntExtra("cx", rootView.getWidth()/2);
            int cy = getIntent().getIntExtra("cy", rootView.getHeight()/2);
            float finalRadius = (float) Math.hypot(rootView.getWidth(), rootView.getHeight());
            Animator anim = ViewAnimationUtils.createCircularReveal(rootView, cx, cy, 0f, finalRadius);
            rootView.setVisibility(View.VISIBLE);
            anim.setDuration(1400);
            anim.start();
        });

        // Inicializar BottomNavigationView
        bottomNavigationView = findViewById(R.id.bottom_nav_view);
        bottomNavigationView.setOnNavigationItemSelectedListener(this);
        bottomNavigationView.setItemIconTintList(null);

        // Inicializar FirebaseAuth y usuario antes de cualquier uso
        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();

        //app check para pruebas
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
        );

        // Si el usuario es nulo, redirigir al login y salir
        if (user == null) {
            startActivity(new Intent(MainActivityAdministrador.this, InicioSesion.class));
            finish();
            return;
        }

        // Permisos de notificación (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        101
                );
            }
        }

        toolbar = findViewById(R.id.toolbarA);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout_A);
        NavigationView navigationView = findViewById(R.id.nav_viewA);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setItemIconTintList(null);

        // Inicializar BottomNavigationView
        bottomNavigationView = findViewById(R.id.bottom_nav_view);
        bottomNavigationView.setOnNavigationItemSelectedListener(this);


        // 🔹 Verificar rol del usuario y ajustar la interfaz (Lógica Asíncrona)
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("usuarios").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String rol = documentSnapshot.getString("rol");
                        userRole = rol; // Asignar el rol
                        Menu menu = navigationView.getMenu();

                        if ("admin".equals(rol)) {
                            // **ROL ADMIN: Habilitar Drawer y su Toggle**
                            bottomNavigationView.setVisibility(View.GONE);
                            navigationView.setVisibility(View.VISIBLE);
                            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED); // Habilitar Drawer

                            // 🚨 CRUCIAL: Inicializar el Toggle aquí (después de saber que es Admin)
                            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                                    MainActivityAdministrador.this, drawerLayout, toolbar,
                                    R.string.navigation_drawer_open,
                                    R.string.navigation_drawer_close
                            );
                            drawerLayout.addDrawerListener(toggle);
                            toggle.syncState(); // Sincroniza el estado del toggle (muestra las 3 rayitas)

                            // Ajustar menú del Drawer para Admin
                            menu.findItem(R.id.AsignarTarea).setVisible(true);
                            menu.findItem(R.id.ResumenAsistencias).setVisible(true);
                            menu.findItem(R.id.ListarAdmin).setVisible(false); // Admin no ve Lista

                            // Cargar fragmento inicial para Admin
                            if (savedInstanceState == null) {
                                getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.fragment_containerA, new InicioAdmin())
                                        .commit();
                                navigationView.setCheckedItem(R.id.InicioAdmin);
                            }
                        } else { // Rol: "empleado"
                            // **ROL EMPLEADO: Habilitar Bottom Nav y deshabilitar Drawer**
                            bottomNavigationView.setVisibility(View.VISIBLE);
                            navigationView.setVisibility(View.GONE);
                            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED); // Deshabilitar Drawer

                            // Quitar el botón de navegación/hamburguesa de la Toolbar para empleado
                            if (getSupportActionBar() != null) {
                                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                            }

                            // Ocultar todas las opciones de admin en el Drawer (redundante pero seguro)
                            menu.findItem(R.id.AsignarTarea).setVisible(false);
                            menu.findItem(R.id.ResumenAsistencias).setVisible(false);
                            menu.findItem(R.id.ListarAdmin).setVisible(false); // ListaAdmin en Drawer también se oculta

                            // Cargar fragmento inicial para Empleado
                            if (savedInstanceState == null) {
                                getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.fragment_containerA, new InicioAdmin())
                                        .commit();
                                bottomNavigationView.setSelectedItemId(R.id.InicioEmpleado); // Usar ID de BottomNav
                            }
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al verificar el rol", Toast.LENGTH_SHORT).show()
                );

        // 🚨 IMPORTANTE: Se eliminó el ActionBarDrawerToggle de aquí para evitar conflictos de sincronización

        // 🔹 Logo dinámico según mes
        View headerView = navigationView.getHeaderView(0);
        AppCompatImageView logo = headerView.findViewById(R.id.logo_encabezado);
        logo.setImageResource(getLogoPorMes());

        logo.setOnClickListener(v -> {
            long currentTime = System.currentTimeMillis();

            // Reiniciar si pasa demasiado tiempo entre toques
            if (currentTime - lastTapTime > RESET_DELAY) {
                tapCount = 0;
            }

            lastTapTime = currentTime;
            tapCount++;

            // --- Toast persistente ---
            if (toast != null) toast.cancel();
            if (tapCount < TAP_THRESHOLD) {
                int remaining = TAP_THRESHOLD - tapCount;
                toast = Toast.makeText(this, "Presiona " + remaining + " veces más para activar el modo desarrollador", Toast.LENGTH_SHORT);
                toast.show();
            } else if (tapCount == TAP_THRESHOLD) {
                tapCount = 0;
                if (toast != null) toast.cancel();
                toast = Toast.makeText(this, "¡Modo desarrollador activado!", Toast.LENGTH_SHORT);
                toast.show();
                startDeveloperEasterEgg();
            }
        });

        ComprobandoInicioSesion();
        AppUpdater.checkForUpdate(this);

        //Iniciar pings automáticos cada 50 segundos
        iniciarPingPeriodico();
    }

    // ======= Ping Automático =======
    private final android.os.Handler handlerPing = new android.os.Handler();
    private final Runnable pingRunnable = new Runnable() {
        @Override
        public void run() {
            new Thread(() -> {
                try {
                    java.net.URL url = new java.net.URL("https://altecomasistencia.onrender.com/");
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    conn.connect();

                    int responseCode = conn.getResponseCode();
                    conn.disconnect();

                    runOnUiThread(() -> {
                        if (responseCode == 200) {
                            // Puedes quitar este log si no quieres mostrar nada
                            System.out.println("✅ Ping exitoso");
                        } else {
                            System.out.println("⚠️ Ping falló con código: " + responseCode);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

            // Repetir cada 50 segundos (50000 ms)
            handlerPing.postDelayed(this, 50000);
        }
    };

    private void iniciarPingPeriodico() {
        handlerPing.post(pingRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Detiene los pings cuando se cierra la actividad
        handlerPing.removeCallbacks(pingRunnable);
    }
    //Logo por mes
    private int getLogoPorMes() {
        int mes = Calendar.getInstance().get(Calendar.MONTH) + 1;
        switch (mes) {
            case 2:  return R.drawable.logofeb;
            case 9:  return R.drawable.logosep;
            case 10: return R.drawable.logooct;
            case 11: return R.drawable.logonov;
            case 12: return R.drawable.logodec;
            default: return R.drawable.logodev;
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        // Permitir que el Drawer se cierre si está abierto (solo para Admin)
        if (drawerLayout.isDrawerOpen(GravityCompat.START) && "admin".equals(userRole)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            Toast.makeText(this, "Botón de retroceso deshabilitado", Toast.LENGTH_SHORT).show();
        }
    }

    // 🔹 Método para manejar la selección de ítems (tanto para Drawer como para BottomNav)
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        // 1. Lógica para el Drawer (Admin)
        if ("admin".equals(userRole)) {
            // Se asume que el ítem pertenece al NavigationView
            if (id == R.id.InicioAdmin) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_containerA, new InicioAdmin()).commit();
            } else if (id == R.id.PerfilAdmin) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_containerA, new PerfilAdmin()).commit();
            } else if (id == R.id.RegistrarAdmin) { // AsistenciaFragment
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_containerA, new AsistenciaFragment()).commit();
            } else if (id == R.id.ListarAdmin) { // Oculto, pero mantenido por si cambia la lógica futura
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_containerA, new ListaAdmin()).commit();
            } else if (id == R.id.AsignarTarea) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_containerA, new AsignarTareaFragment())
                        .addToBackStack(null)
                        .commit();
            } else if (id == R.id.ResumenAsistencias) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_containerA, new ResumenFragment())
                        .commit();
            } else if (id == R.id.SalirAdmin) {
                CerrarSesion();
            }

            // Cerrar Drawer si la selección viene del menú lateral
            drawerLayout.closeDrawer(GravityCompat.START);

            // 2. Lógica para el Bottom Nav (Empleado)
        } else if ("empleado".equals(userRole)) {
            // Se asume que el ítem pertenece al BottomNavigationView
            if (id == R.id.InicioEmpleado) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_containerA, new InicioAdmin()).commit();
            } else if (id == R.id.AsistenciaEmpleado) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_containerA, new AsistenciaFragment()).commit();
            } else if (id == R.id.ListaEmpleado) { // Para ver la lista de registros
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_containerA, new ListaAdmin()).commit();
            } else if (id == R.id.PerfilEmpleado) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_containerA, new PerfilAdmin()).commit();
            } else if (id == R.id.SalirEmpleado) {
                CerrarSesion();
            }
        }

        return true;
    }

    private void ComprobandoInicioSesion() {
        if (user != null) {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            boolean toastMostrado = prefs.getBoolean(KEY_TOAST_MOSTRADO, false);
            if (!toastMostrado) {
                Toast.makeText(this, "Se ha iniciado sesión", Toast.LENGTH_SHORT).show();
                prefs.edit().putBoolean(KEY_TOAST_MOSTRADO, true).apply();
            }
        } else {
            startActivity(new Intent(MainActivityAdministrador.this, InicioSesion.class));
            finish();
        }
    }

    private void CerrarSesion() {
        firebaseAuth.signOut();
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_TOAST_MOSTRADO, false)
                .apply();
        startActivity(new Intent(MainActivityAdministrador.this, InicioSesion.class));
        Toast.makeText(this, "Cerraste sesión exitosamente", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        user = firebaseAuth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(MainActivityAdministrador.this, InicioSesion.class));
            finish();
        } else {
            SharedPreferences prefs = getSharedPreferences("AsistenciaPrefs", MODE_PRIVATE);
            prefs.edit().putLong("ultima_apertura", System.currentTimeMillis()).apply();
        }
    }
    // Variable global (fuera del método, en tu Activity)
    private List<Integer> gifPool = new ArrayList<>();

    private void startDeveloperEasterEgg() {
        ViewGroup rootView = findViewById(android.R.id.content);
        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(Color.parseColor("#80000000")); // fondo semitransparente
        overlay.setAlpha(0f);
        rootView.addView(overlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        overlay.animate().alpha(1f).setDuration(400).start();

        // Lista de todos tus GIFs
        int[] gifs = {
                R.drawable.khalid, // normal (izq→der)
                R.drawable.a,
                R.drawable.b,
                R.drawable.c,
                R.drawable.d,
                R.drawable.e,       // derecha → izquierda
                R.drawable.o,       // arriba → abajo
                R.drawable.g,       // derecha → izquierda
                R.drawable.h,       // derecha → izquierda
                R.drawable.i,       // derecha → izquierda
                R.drawable.p        // abajo → arriba
        };

        // 🔹 Reiniciar el pool cuando se vacíe
        if (gifPool.isEmpty()) {
            for (int gif : gifs) gifPool.add(gif);
        }

        // 🔹 Elegir aleatoriamente uno que aún no haya salido
        int randomIndex = new Random().nextInt(gifPool.size());
        int selectedGif = gifPool.remove(randomIndex);

        ImageView gifView = new ImageView(this);
        gifView.setLayoutParams(new FrameLayout.LayoutParams(250, 250));
        overlay.addView(gifView);

        Glide.with(this).asGif().load(selectedGif).into(gifView);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int screenWidth = dm.widthPixels;
        int screenHeight = dm.heightPixels;
        Random random = new Random();

        ObjectAnimator anim;

        if (selectedGif == R.drawable.o) {
            // 🔹 De arriba hacia abajo
            float randomX = random.nextInt(screenWidth - 300);
            gifView.setX(randomX);
            gifView.setY(-300f);
            anim = ObjectAnimator.ofFloat(gifView, "y", -300f, screenHeight + 300f);

        } else if (selectedGif == R.drawable.e ||
                selectedGif == R.drawable.g ||
                selectedGif == R.drawable.h ||
                selectedGif == R.drawable.i) {
            // 🔹 De derecha a izquierda
            float randomY = random.nextInt(screenHeight / 2) + 100;
            gifView.setY(randomY);
            gifView.setX(screenWidth + 300f);
            anim = ObjectAnimator.ofFloat(gifView, "x", screenWidth + 300f, -300f);

        } else if (selectedGif == R.drawable.p) {
            // 🔹 De abajo hacia arriba
            float randomX = random.nextInt(screenWidth - 300);
            gifView.setX(randomX);
            gifView.setY(screenHeight + 300f);
            anim = ObjectAnimator.ofFloat(gifView, "y", screenHeight + 300f, -300f);

        } else {
            // 🔹 Los demás: de izquierda a derecha
            float randomY = random.nextInt(screenHeight / 2) + 100;
            gifView.setY(randomY);
            gifView.setX(-300f);
            anim = ObjectAnimator.ofFloat(gifView, "x", -300f, screenWidth + 300f);
        }

        // 🔹 Animación
        anim.setDuration(6000 + random.nextInt(4000));
        anim.setInterpolator(new LinearInterpolator());

        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                gifView.animate().alpha(0f).setDuration(800).start();
                overlay.animate().alpha(0f).setDuration(800)
                        .withEndAction(() -> ((ViewGroup) rootView).removeView(overlay))
                        .start();
            }
        });

        anim.start();
    }
}