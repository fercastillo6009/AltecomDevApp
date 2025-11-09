package com.example.fondodepantalla.Utils // 1. Paquete 'Utils' (con mayúscula)

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices

// 2. El 'context' se pasa al constructor primario
class LocationHelper(context: Context) {

    // 3. La propiedad se inicializa directamente
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    /**
     * 4. La interfaz 'OnLocationResultListener' se reemplaza por un tipo de función: (Location?) -> Unit
     * Esto te permite llamar a la función usando un bloque lambda, que es mucho más limpio.
     */
    @SuppressLint("MissingPermission")
    fun getCurrentLocation(listener: (location: Location?) -> Unit) {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location -> listener(location) } // 'it' también funcionaría, pero 'location' es más explícito
            .addOnFailureListener { listener(null) }
    }

    /**
     * 5. Asegúrate de que tu archivo 'Constants' (o donde sea que
     * 'COMPANY_LATITUDE', 'COMPANY_LONGITUDE' y 'ALLOWED_RADIUS_METERS' vivan)
     * sea accesible desde este paquete (por ejemplo, importándolo).
     */
    fun isInsideCompanyArea(currentLat: Double, currentLon: Double): Boolean {
        // La creación de 'FloatArray' es un poco diferente en Kotlin
        val result = FloatArray(1)
        Location.distanceBetween(
            currentLat, currentLon,
            Constants.COMPANY_LATITUDE, // Asumo que 'Constants' está importado
            Constants.COMPANY_LONGITUDE,
            result
        )
        return result[0] <= Constants.ALLOWED_RADIUS_METERS
    }
}