package com.example.fondodepantalla;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.fondodepantalla.FragmentosAdministrador.InicioAdmin;
import com.example.fondodepantalla.FragmentosAdministrador.ListaAdmin;
import com.example.fondodepantalla.FragmentosAdministrador.PerfilAdmin;
import com.example.fondodepantalla.FragmentosAdministrador.RegistrarAdmin;
import com.example.fondodepantalla.FragmentosAdministrador.AsistenciaFragment;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivityAdministrador extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    DrawerLayout drawerLayout;
    FirebaseAuth firebaseAuth;
    FirebaseUser user;

    // SharedPreferences para controlar el Toast
    private static final String PREFS_NAME = "SesionPrefs";
    private static final String KEY_TOAST_MOSTRADO = "toastMostrado";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_administrador);

        Toolbar toolbar = findViewById(R.id.toolbarA);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout_A);

        NavigationView navigationView = findViewById(R.id.nav_viewA);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setItemIconTintList(null);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);

        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_containerA,
                    new InicioAdmin()).commit();
            navigationView.setCheckedItem(R.id.InicioAdmin);
        }

        // Comprobación al iniciar por primera vez
        ComprobandoInicioSesion();
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        Toast.makeText(this, "Botón de retroceso deshabilitado", Toast.LENGTH_SHORT).show();
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.InicioAdmin){
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_containerA,
                    new InicioAdmin()).commit();
        }
        if(item.getItemId() == R.id.PerfilAdmin){
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_containerA,
                    new PerfilAdmin()).commit();
        }
        if (item.getItemId() == R.id.RegistrarAdmin) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_containerA, new AsistenciaFragment())
                    .commit();
        }
        if(item.getItemId() == R.id.ListarAdmin){
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_containerA,
                    new ListaAdmin()).commit();
        }
        if(item.getItemId() == R.id.SalirAdmin){
            CerrarSesion();
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return super.onOptionsItemSelected(item);
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
            startActivity(new Intent(MainActivityAdministrador.this, MainActivity.class));
            finish();
        }
    }

    private void CerrarSesion() {
        firebaseAuth.signOut();

        // Resetear Toast para la próxima sesión
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
        // Solo redirigir si no hay usuario
        if (user == null) {
            startActivity(new Intent(MainActivityAdministrador.this, MainActivity.class));
            finish();
        }
    }
}
