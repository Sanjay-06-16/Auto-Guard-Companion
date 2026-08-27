package com.autoguard.companion.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

class LocationHelper(private val context: Context) {

    private val TAG = "AutoGuardLocation"

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    /**
     * Check whether at least one location permission exists.
     */
    fun hasLocationPermission(): Boolean {
        val fineGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        Log.d(
            TAG,
            "Fine=$fineGranted | Coarse=$coarseGranted"
        )

        return fineGranted || coarseGranted
    }

    /**
     * Check whether Android Location Services are enabled.
     */
    fun isLocationServicesEnabled(): Boolean {
        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        return try {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get the best available phone location.
     *
     * Priority:
     * 1. Current high-accuracy location
     * 2. Last known location
     * 3. null
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {

        Log.d(TAG, "========== LOCATION REQUEST ==========")

        if (!hasLocationPermission()) {
            Log.e(TAG, "LOCATION FAILED: Permission not granted")
            return null
        }

        if (!isLocationServicesEnabled()) {
            Log.e(TAG, "LOCATION FAILED: Location services are OFF")
            return null
        }

        try {

            Log.d(TAG, "Requesting HIGH ACCURACY phone location...")

            val cancellationTokenSource = CancellationTokenSource()

            val currentLocation =
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).await()

            if (currentLocation != null) {

                Log.d(
                    TAG,
                    "CURRENT PHONE LOCATION FOUND"
                )

                Log.d(
                    TAG,
                    "Latitude = ${currentLocation.latitude}"
                )

                Log.d(
                    TAG,
                    "Longitude = ${currentLocation.longitude}"
                )

                Log.d(
                    TAG,
                    "Accuracy = ${currentLocation.accuracy} meters"
                )

                return currentLocation
            }

            Log.w(
                TAG,
                "Current location returned NULL"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Current location error: ${e.message}",
                e
            )
        }

        /*
         * If a fresh location wasn't available,
         * try the last known location.
         */
        try {

            Log.d(
                TAG,
                "Trying LAST KNOWN phone location..."
            )

            val lastLocation =
                fusedLocationClient.lastLocation.await()

            if (lastLocation != null) {

                Log.d(
                    TAG,
                    "LAST KNOWN LOCATION FOUND"
                )

                Log.d(
                    TAG,
                    "Latitude = ${lastLocation.latitude}"
                )

                Log.d(
                    TAG,
                    "Longitude = ${lastLocation.longitude}"
                )

                Log.d(
                    TAG,
                    "Accuracy = ${lastLocation.accuracy} meters"
                )

                return lastLocation
            }

            Log.e(
                TAG,
                "LAST KNOWN LOCATION also NULL"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Last location error: ${e.message}",
                e
            )
        }

        Log.e(
            TAG,
            "========== LOCATION UNAVAILABLE =========="
        )

        return null
    }
}