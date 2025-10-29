package com.example.fondodepantalla.utils;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.fondodepantalla.FragmentosAdministrador.AsistenciaFragment;
import com.example.fondodepantalla.MainActivity;
import com.example.fondodepantalla.MainActivityAdministrador;
import com.example.fondodepantalla.R;

import java.util.Calendar;

public class NotificationHelper extends BroadcastReceiver {

    private static final String CHANNEL_ID = "asistencia_channel";
    private static final int NOTIFICATION_ID = 1001;

    // ✅ Se llama cuando se activa la alarma programada
    @Override
    public void onReceive(Context context, Intent intent) {
        if ("ENVIAR_RECORDATORIO_ASISTENCIA".equals(intent.getAction())) {
            mostrarNotificacion(context,
                    "Recordatorio de Asistencia",
                    "Tu clase está por comenzar. Evita llegar tarde 🚶‍♂️");
        }
    }

    // ✅ Muestra la notificación
    public static void mostrarNotificacion(Context context, String titulo, String mensaje) {
        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Crear canal (solo necesario una vez)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Recordatorios de Asistencia",
                    NotificationManager.IMPORTANCE_HIGH
            );
            manager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, MainActivityAdministrador.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(titulo)
                .setContentText(mensaje)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();

        manager.notify(NOTIFICATION_ID, notification);
    }

    // ✅ Programa la notificación (por ejemplo 10 minutos antes de las 8:00 AM)
    public static void programarRecordatorio(Context context) {
        Calendar horaInicio = Calendar.getInstance();
        horaInicio.set(Calendar.HOUR_OF_DAY, 8);  // Hora de inicio de clase
        horaInicio.set(Calendar.MINUTE, 0);
        horaInicio.set(Calendar.SECOND, 0);

        // Restar 10 minutos
        horaInicio.add(Calendar.MINUTE, -10);

        // Evita programar si ya pasó
        if (horaInicio.getTimeInMillis() <= System.currentTimeMillis()) return;

        Intent intent = new Intent(context, NotificationHelper.class);
        intent.setAction("ENVIAR_RECORDATORIO_ASISTENCIA");

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                1001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager)
                context.getSystemService(Context.ALARM_SERVICE);

        if (alarmManager != null) {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    horaInicio.getTimeInMillis(),
                    pendingIntent
            );
        }
    }
}