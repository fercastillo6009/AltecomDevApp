package com.example.fondodepantalla;

import android.app.Application;
import com.cloudinary.android.MediaManager;
import java.util.HashMap;
import java.util.Map;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Configuración de Cloudinary
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dowsbrhel");
        config.put("api_key", "138955893821595");
        config.put("api_secret", "-IPqKgvL9ptklWZvKOgzW5pBnEY");
        MediaManager.init(this, config);
    }
}