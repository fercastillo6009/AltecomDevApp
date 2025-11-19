package com.example.fondodepantalla

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.SetOptions

class LocationTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var currentUserId: String? = null
    private val db = FirebaseFirestore.getInstance()

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_USER_ID = "EXTRA_USER_ID"
        private const val NOTIFICATION_CHANNEL_ID = "location_tracking_channel"
        private const val NOTIFICATION_ID = 12345
        private const val TAG = "LocationTrackService"
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        when (action) {
            ACTION_START -> {
                currentUserId = intent.getStringExtra(EXTRA_USER_ID)
                if (currentUserId == null) {
                    Log.e(TAG, "No se proporcionó USER_ID. Deteniendo servicio.")
                    stopSelf()
                } else {
                    startForegroundWithNotification()
                    startLocationUpdates()
                    Log.i(TAG, "Servicio de rastreo iniciado para $currentUserId")
                }
            }
            ACTION_STOP -> {
                stopLocationUpdates()
                stopForeground(true)
                stopSelf()
                Log.i(TAG, "Servicio de rastreo detenido.")
            }
        }
        return START_STICKY // Reinicia el servicio si el sistema lo mata
    }

    private fun startForegroundWithNotification() {
        val notification = createNotification("Rastreando ubicación de tarea...")
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Altecomdev App")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.logodev) // Asegúrate de tener este drawable
            .setOngoing(true) // Notificación persistente
            .build()
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 30000 // Intervalo de 30 segundos
        ).apply {
            setMinUpdateIntervalMillis(15000) // Mínimo 15 segundos
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.lastLocation?.let { location ->
                    Log.d(TAG, "Nueva ubicación: ${location.latitude}, ${location.longitude}")
                    updateLocationInFirestore(location)
                }
            }
        }

        // Verificar permisos (aunque MainActivity ya debería haberlos pedido)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } else {
            Log.w(TAG, "No hay permiso de ubicación para iniciar updates.")
            stopSelf() // Detener si no hay permiso
        }
    }

    private fun updateLocationInFirestore(location: android.location.Location) {
        currentUserId?.let { userId ->
            val geoPoint = GeoPoint(location.latitude, location.longitude)
            val updateData = mapOf(
                "currentLocation" to geoPoint,
                "lastUpdate" to FieldValue.serverTimestamp(),
                "isTracking" to true
            )

            db.collection("locations").document(userId)
                .set(updateData, SetOptions.merge()) // set con merge crea o actualiza
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error al escribir en Firestore", e)
                }
        }
    }

    private fun stopLocationUpdates() {
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        // Marcar al usuario como que ya no está rastreando
        currentUserId?.let { userId ->
            db.collection("locations").document(userId)
                .update("isTracking", false)
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error al actualizar 'isTracking' a false", e)
                }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // No es un servicio enlazado
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Seguimiento de Ubicación",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Canal para notificar el rastreo de ubicación en segundo plano"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}