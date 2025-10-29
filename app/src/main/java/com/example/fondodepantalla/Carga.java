package com.example.fondodepantalla;

import androidx.appcompat.app.AppCompatActivity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Calendar;

public class Carga extends AppCompatActivity {

    TextView app_name, desarrollador;
    ImageView logoCarga;
    private static final String PREFS_NAME = "AsistenciaPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.carga);

        // Cambiar la fuente
        String ubicacion = "fuentes/choco_cooky.ttf";
        Typeface tf = Typeface.createFromAsset(getAssets(), ubicacion);

        app_name = findViewById(R.id.app_name);
        desarrollador = findViewById(R.id.desarrollador);
        logoCarga = findViewById(R.id.logo_carga);

        // Cambiar el logo según el mes
        logoCarga.setImageResource(getLogoPorMes());

        final int DURACION = 3000;

        new Handler().postDelayed(() -> {
            // Verificar si hay usuario autenticado
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            FirebaseUser user = mAuth.getCurrentUser();

            // Guardar última apertura si hay usuario
            if (user != null) {
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putLong("ultima_apertura", System.currentTimeMillis());
                editor.apply();

                // Programar recordatorio diario
                programarRecordatorio();
            }

            Intent intent;
            if (user != null) {
                intent = new Intent(Carga.this, MainActivityAdministrador.class);
            } else {
                intent = new Intent(Carga.this, InicioSesion.class);
            }

            startActivity(intent);
            finish();

        }, DURACION);

        app_name.setTypeface(tf);
        desarrollador.setTypeface(tf);
    }

    // Método para obtener logo según mes
    private int getLogoPorMes() {
        int mes = Calendar.getInstance().get(Calendar.MONTH) + 1; // Enero=1
        switch (mes) {
            case 2:  return R.drawable.logofeb;
            case 9:  return R.drawable.logosep;
            case 10: return R.drawable.logooct;
            case 11: return R.drawable.logonov;
            case 12: return R.drawable.logodec;
            default: return R.drawable.logodev;
        }
    }

    // ----------------------- RECORDATORIO DIARIO -----------------------
    private void programarRecordatorio() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(this, RecordatorioReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        );

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 9);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent
        );
    }
}
