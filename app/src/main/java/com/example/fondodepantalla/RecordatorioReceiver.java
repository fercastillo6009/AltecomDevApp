package com.example.fondodepantalla;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.core.app.NotificationCompat;
import java.util.Calendar;

public class RecordatorioReceiver extends BroadcastReceiver {

    private static final String PREFS_NAME = "AsistenciaPrefs";

    @Override
    public void onReceive(Context context, android.content.Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long ultimaApertura = prefs.getLong("ultima_apertura", 0);

        Calendar hoy = Calendar.getInstance();
        Calendar ultima = Calendar.getInstance();
        ultima.setTimeInMillis(ultimaApertura);

        boolean mismoDia = hoy.get(Calendar.YEAR) == ultima.get(Calendar.YEAR)
                && hoy.get(Calendar.DAY_OF_YEAR) == ultima.get(Calendar.DAY_OF_YEAR);

        // Si no abrió la app hoy, mandar notificación
        if (!mismoDia) {
            mostrarNotificacion(context);
        }
    }

    private void mostrarNotificacion(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "asistencia_channel";

        // Crear canal de notificación para Android 8+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Recordatorios de asistencia",
                    NotificationManager.IMPORTANCE_HIGH
            );
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_launcher_background) // Cambia por tu icono
                .setContentTitle("Recordatorio de asistencia")
                .setContentText("No registraste tu asistencia hoy. No olvides hacerlo.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        manager.notify(1, builder.build());
    }
}
