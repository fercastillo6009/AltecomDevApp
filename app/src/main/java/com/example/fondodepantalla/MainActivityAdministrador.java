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

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.fondodepantalla.FragmentosAdministrador.InicioAdmin;
import com.example.fondodepantalla.FragmentosAdministrador.ListaAdmin;
import com.example.fondodepantalla.FragmentosAdministrador.PerfilAdmin;
import com.example.fondodepantalla.FragmentosAdministrador.AsistenciaFragment;
import com.example.fondodepantalla.FragmentosAdministrador.ResumenFragment;
import com.example.fondodepantalla.Utils.AppUpdater;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;

public class MainActivityAdministrador extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    DrawerLayout drawerLayout;
    FirebaseAuth firebaseAuth;
    FirebaseUser user;

    private static final String PREFS_NAME = "SesionPrefs";
    private static final String KEY_TOAST_MOSTRADO = "toastMostrado";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_administrador);

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

        Toolbar toolbar = findViewById(R.id.toolbarA);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout_A);
        NavigationView navigationView = findViewById(R.id.nav_viewA);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setItemIconTintList(null);

        // 🔹 Verificar rol del usuario y ajustar el menú
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("usuarios").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String rol = documentSnapshot.getString("rol");
                        Menu menu = navigationView.getMenu();

                        if ("admin".equals(rol)) {
                            menu.findItem(R.id.AsignarTarea).setVisible(true);
                            menu.findItem(R.id.ResumenAsistencias).setVisible(true); // <-- nuevo
                            menu.findItem(R.id.ListarAdmin).setVisible(false);
                        } else {
                            menu.findItem(R.id.AsignarTarea).setVisible(false);
                            menu.findItem(R.id.ResumenAsistencias).setVisible(false); // <-- oculto para no-admin
                            menu.findItem(R.id.ListarAdmin).setVisible(true);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al verificar el rol", Toast.LENGTH_SHORT).show()
                );

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // 🔹 Logo dinámico según mes
        View headerView = navigationView.getHeaderView(0);
        AppCompatImageView logo = headerView.findViewById(R.id.logo_encabezado);
        logo.setImageResource(getLogoPorMes());

        // Cargar fragmento inicial
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_containerA, new InicioAdmin())
                    .commit();
            navigationView.setCheckedItem(R.id.InicioAdmin);
        }

        ComprobandoInicioSesion();
        AppUpdater.checkForUpdate(this);
    }

    // 🔹 Logo por mes
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
        Toast.makeText(this, "Botón de retroceso deshabilitado", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.InicioAdmin) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_containerA, new InicioAdmin()).commit();
        } else if (id == R.id.PerfilAdmin) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_containerA, new PerfilAdmin()).commit();
        } else if (id == R.id.RegistrarAdmin) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_containerA, new AsistenciaFragment()).commit();
        } else if (id == R.id.ListarAdmin) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_containerA, new ListaAdmin()).commit();
        } else if (id == R.id.AsignarTarea) {
            Intent intent = new Intent(this, AsignarTareaActivity.class);
            startActivity(intent);
        } else if (id == R.id.ResumenAsistencias) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_containerA, new ResumenFragment())
                    .commit();
        } else if (id == R.id.SalirAdmin) {
            CerrarSesion();
        }

        item.setChecked(true);
        drawerLayout.closeDrawer(GravityCompat.START);
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
}
