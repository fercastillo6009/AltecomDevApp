package com.example.fondodepantalla.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.example.fondodepantalla.BuildConfig;
import com.google.firebase.firestore.FirebaseFirestore;

public class AppUpdater {

    public static void checkForUpdate(Activity activity) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("config").document("actualizacion").get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String ultima = doc.getString("ultimaVersion");
                        String minVersion = doc.getString("minVersion");
                        Boolean force = doc.getBoolean("forceUpdate");
                        String link = doc.getString("linkDescarga");
                        String mensaje = doc.getString("mensaje");

                        String current = BuildConfig.VERSION_NAME;
                        int cmpUltima = compareVersion(current, ultima);
                        int cmpMin = (minVersion != null) ? compareVersion(current, minVersion) : 1;

                        if (cmpUltima < 0) {
                            boolean obligatorio = (force != null && force) || (cmpMin < 0);
                            showUpdateDialog(activity, mensaje, link, obligatorio);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(activity, "Error al verificar actualización", Toast.LENGTH_SHORT).show();
                });
    }

    private static void showUpdateDialog(Activity activity, String mensaje, String url, boolean obligatorio) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle("Actualización disponible")
                .setMessage(mensaje != null ? mensaje : "Hay una nueva versión disponible.")
                .setCancelable(!obligatorio)
                .setPositiveButton("Actualizar", (d, w) -> {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        activity.startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(activity, "No se pudo abrir el enlace", Toast.LENGTH_SHORT).show();
                    }
                    if (obligatorio) {
                        activity.finish(); // cierra la app para que no siga usando la versión antigua
                    }
                });

        if (!obligatorio) {
            builder.setNegativeButton("Más tarde", (d, w) -> d.dismiss());
        }

        AlertDialog dialog = builder.create();
        if (obligatorio) {
            dialog.setCanceledOnTouchOutside(false); // no permite cerrar tocando afuera
        }
        dialog.show();
    }

    private static int compareVersion(String v1, String v2) {
        if (v1 == null || v2 == null) return 0;
        String[] a1 = v1.split("\\.");
        String[] a2 = v2.split("\\.");
        int len = Math.max(a1.length, a2.length);
        for (int i = 0; i < len; i++) {
            int n1 = i < a1.length ? Integer.parseInt(a1[i]) : 0;
            int n2 = i < a2.length ? Integer.parseInt(a2[i]) : 0;
            if (n1 != n2) return Integer.compare(n1, n2);
        }
        return 0;
    }
}