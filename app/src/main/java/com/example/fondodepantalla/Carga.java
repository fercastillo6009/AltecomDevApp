package com.example.fondodepantalla;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Calendar;

public class Carga extends AppCompatActivity {

    TextView app_name, desarrollador;
    ImageView logoCarga;

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
            // Iniciar actividad siguiente
            Intent intent = new Intent(Carga.this, InicioSesion.class);
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
}

