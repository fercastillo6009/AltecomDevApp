package com.example.fondodepantalla;

import android.app.Application;
import android.content.Context; // <-- NUEVO IMPORT
import androidx.preference.PreferenceManager; // <-- NUEVO IMPORT

import com.cloudinary.android.MediaManager;

import org.osmdroid.config.Configuration;

import java.util.HashMap;
import java.util.Map;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Configuración de Cloudinary (TU CÓDIGO EXISTENTE)
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dowsbrhel");
        config.put("api_key", "138955893821595");
        config.put("api_secret", "-IPqKgvL9ptklWZvKOgzW5pBnEY");
        MediaManager.init(this, config);

        // En tu MyApp.java dentro de onCreate:
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID);
    }
}