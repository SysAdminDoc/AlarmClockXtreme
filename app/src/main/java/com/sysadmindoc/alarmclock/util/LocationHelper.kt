package com.sysadmindoc.alarmclock.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat

/**
 * Gets device location using Android's built-in LocationManager.
 * Uses NETWORK_PROVIDER with ACCESS_COARSE_LOCATION - no Google Play Services needed.
 * F-Droid compatible.
 */
object LocationHelper {

    data class LatLng(val latitude: Double, val longitude: Double)

    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun getLastKnownLocation(context: Context): LatLng? {
        if (!hasLocationPermission(context)) return null

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Try network provider first (faster, coarse is fine for weather)
        val providers = listOf(
            LocationManager.NETWORK_PROVIDER,
            LocationManager.GPS_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )

        var bestLocation: Location? = null

        try {
            for (provider in providers) {
                if (!locationManager.isProviderEnabled(provider)) continue
                @Suppress("MissingPermission")
                val location = locationManager.getLastKnownLocation(provider)
                if (location != null) {
                    if (bestLocation == null || location.accuracy < bestLocation.accuracy) {
                        bestLocation = location
                    }
                }
            }
        } catch (e: SecurityException) {
            return null
        }

        return bestLocation?.let { LatLng(it.latitude, it.longitude) }
    }
}
