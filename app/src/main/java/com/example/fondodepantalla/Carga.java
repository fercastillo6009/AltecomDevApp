package com.example.fondodepantalla;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

public class Carga extends AppCompatActivity {

    TextView app_name, desarrollador;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.carga);

        //aqu se cambia la fuente
        String ubicacion = "fuentes/choco_cooky.ttf";
        Typeface tf = Typeface.createFromAsset(Carga.this.getAssets(),ubicacion);

        app_name = findViewById(R.id.app_name);
        desarrollador = findViewById(R.id.desarrollador);

        final int DURACION = 3000;

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Creamos un Intent para iniciar la nueva actividad
                Intent intent = new Intent(Carga.this, InicioSesion.class);
                startActivity(intent);

                // Finalizamos la actividad actual (pantalla de carga)
                finish();



            }
        }, DURACION);

        app_name.setTypeface(tf);
        desarrollador.setTypeface(tf);
    }
}

