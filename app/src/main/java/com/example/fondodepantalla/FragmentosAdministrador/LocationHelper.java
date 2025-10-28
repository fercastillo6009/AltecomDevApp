package com.example.fondodepantalla.FragmentosAdministrador;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;

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
                .addOnSuccessListener(location -> listener.onLocationResult(location))
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
        void onLocationResult(Location location);
    }
}
