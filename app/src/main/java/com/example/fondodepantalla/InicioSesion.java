package com.example.fondodepantalla;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class InicioSesion extends AppCompatActivity {

    EditText Correo, Password;
    Button Acceder;
    FirebaseAuth firebaseAuth;
    ConstraintLayout rootLayout; // variable de clase

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inicio_sesion);

        ocultarStatusBarYNavBar();

        Correo = findViewById(R.id.Correo);
        Password = findViewById(R.id.Password);
        Acceder = findViewById(R.id.Acceder);

        rootLayout = findViewById(R.id.rootLayout); // ✅ asignación correcta

        firebaseAuth = FirebaseAuth.getInstance();

        Acceder.setOnClickListener(v -> {
            String correo = Correo.getText().toString();
            String pass = Password.getText().toString();

            if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                Correo.setError("Correo inválido");
                Correo.requestFocus();
            } else if (pass.length() < 6) {
                Password.setError("Contraseña mínima de 6 caracteres");
                Password.requestFocus();
            } else {
                iniciarSesion(correo, pass);
            }
        });
    }

    private void iniciarSesion(String correo, String pass) {
        firebaseAuth.signInWithEmailAndPassword(correo, pass)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();

                        // Coordenadas del centro del botón
                        int cx = Acceder.getWidth() / 2 + Acceder.getLeft();
                        int cy = Acceder.getHeight() / 2 + Acceder.getTop();
                        float finalRadius = (float) Math.hypot(rootLayout.getWidth(), rootLayout.getHeight());

                        Animator anim = ViewAnimationUtils.createCircularReveal(rootLayout, cx, cy, finalRadius, 0f);
                        anim.setDuration(800);
                        anim.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                Intent intent = new Intent(InicioSesion.this, MainActivityAdministrador.class);
                                intent.putExtra("cx", cx);
                                intent.putExtra("cy", cy);
                                startActivity(intent);
                                finish();
                                overridePendingTransition(0, 0); // la animación circular ya hace la transición
                            }
                        });
                        anim.start();

                    } else {
                        mostrarError("Verifique si el correo o la contraseña son correctos.");
                    }
                })
                .addOnFailureListener(e -> mostrarError("Error: " + e.getMessage()));
    }

    private void mostrarError(String mensaje) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Error de inicio de sesión")
                .setMessage(mensaje)
                .setPositiveButton("Entendido", (dialog, which) -> dialog.dismiss())
                .setIcon(R.drawable.ic_error_outline)
                .show();
    }

    private void ocultarStatusBarYNavBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final WindowInsetsController insetsController = getWindow().getInsetsController();
            if (insetsController != null) {
                insetsController.hide(
                        WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars()
                );
                insetsController.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }
    }
}
