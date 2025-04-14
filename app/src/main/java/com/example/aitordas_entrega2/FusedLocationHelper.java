package com.example.aitordas_entrega2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

public class FusedLocationHelper {

    public interface LocationCallback {
        void onLocationResult(Location location);
    }

    @SuppressLint("MissingPermission")
    public static void getLastLocation(Context context, LocationCallback callback) {
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        callback.onLocationResult(location);
                    } else {
                        Log.e("LOCALIZACION", "No se pudo obtener ubicación");
                        callback.onLocationResult(null);
                    }
                });
    }
}
