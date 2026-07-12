package com.aleejaved.locallines.maps

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

object PassiveLocationManager {
    private const val REQUEST_CODE = 40

    @SuppressLint("MissingPermission")
    fun register(context: Context) {
        if (!hasBackgroundLocation(context)) return
        val request = LocationRequest.Builder(Priority.PRIORITY_PASSIVE, 15 * 60_000L)
            .setMinUpdateIntervalMillis(5 * 60_000L)
            .setMinUpdateDistanceMeters(100f)
            .build()
        LocationServices.getFusedLocationProviderClient(context)
            .requestLocationUpdates(request, pendingIntent(context))
    }

    fun unregister(context: Context) {
        LocationServices.getFusedLocationProviderClient(context)
            .removeLocationUpdates(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, PassiveLocationReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun hasBackgroundLocation(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
}

class PassiveLocationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val location = LocationResult.extractResult(intent)?.lastLocation ?: return
        RefreshScheduler.enqueuePassiveRefresh(context, location)
    }
}
