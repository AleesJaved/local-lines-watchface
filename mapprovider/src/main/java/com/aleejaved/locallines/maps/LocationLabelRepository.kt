package com.aleejaved.locallines.maps

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

class LocationLabelRepository(context: Context) {
    private val context = context.applicationContext
    private val settings = MapSettings(this.context)
    private val resolver = LocationNameResolver(this.context)

    suspend fun refreshAt(location: Location): Boolean {
        if (!settings.hasEnabledLocationParts()) return false
        val names = resolver.resolve(location.latitude, location.longitude) ?: return false
        settings.recordAddress(names.number, names.road, names.town, names.city, names.country)
        GeofenceLocationManager.register(context, location.latitude, location.longitude)
        ComplicationUpdates.requestLocation(context)
        return true
    }

    @SuppressLint("MissingPermission")
    suspend fun refreshCurrent(): Boolean {
        if (!hasLocationPermission()) return false
        val client = LocationServices.getFusedLocationProviderClient(context)
        val cached = runCatching { client.lastLocation.await() }.getOrNull()
        val location = cached?.takeIf { System.currentTimeMillis() - it.time <= MAX_LAST_LOCATION_AGE_MS }
            ?: runCatching {
                val cancellation = CancellationTokenSource()
                client.getCurrentLocation(
                    CurrentLocationRequest.Builder()
                        .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                        .setMaxUpdateAgeMillis(MAX_LAST_LOCATION_AGE_MS)
                        .setDurationMillis(CURRENT_LOCATION_TIMEOUT_MS)
                        .build(),
                    cancellation.token,
                ).await()
            }.getOrNull()
        return location?.let { refreshAt(it) } == true
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    companion object {
        private const val MAX_LAST_LOCATION_AGE_MS = 2 * 60_000L
        private const val CURRENT_LOCATION_TIMEOUT_MS = 8_000L
    }
}
