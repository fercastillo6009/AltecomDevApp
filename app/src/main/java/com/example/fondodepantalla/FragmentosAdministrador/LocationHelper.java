package com.example.fondodepantalla.FragmentosAdministrador;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import androidx.annotation.Nullable;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

public class LocationHelper {

    private final FusedLocationProviderClient fusedLocationClient;

    public LocationHelper(Context context) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    @SuppressLint("MissingPermission")
    public void getCurrentLocation(OnLocationResultListener listener) {
        fusedLocationClient.getLastLocation()
                // 'location' aquí PUEDE ser nulo si no hay última ubicación conocida
                .addOnSuccessListener(location -> listener.onLocationResult(location))
                // Aquí envías 'null' explícitamente en caso de fallo
                .addOnFailureListener(e -> listener.onLocationResult(null));
    }

    public boolean isInsideCompanyArea(double currentLat, double currentLon) {
        float[] result = new float[1];
        Location.distanceBetween(
                currentLat, currentLon,
                Constants.COMPANY_LATITUDE, Constants.COMPANY_LONGITUDE,
                result
        );
        return result[0] <= Constants.ALLOWED_RADIUS_METERS;
    }

    public interface OnLocationResultListener {
        // 🌟 CORRECCIÓN: Añadir '@Nullable' para permitir valores nulos
        // Esto soluciona la incompatibilidad entre el listener y la implementación
        void onLocationResult(@Nullable Location location);
    }
}