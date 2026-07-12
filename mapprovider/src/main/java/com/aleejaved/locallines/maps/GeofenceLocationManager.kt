package com.aleejaved.locallines.maps

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

object GeofenceLocationManager {
    private const val REQUEST_CODE = 41
    private const val REQUEST_ID = "local-lines-last-area"
    private const val RADIUS_METRES = 100f
    private const val RESPONSIVENESS_MS = 60_000

    @SuppressLint("MissingPermission")
    fun register(context: Context, latitude: Double, longitude: Double) {
        if (!hasBackgroundLocation(context) || !MapSettings(context).hasEnabledLocationParts()) return
        val geofence = Geofence.Builder()
            .setRequestId(REQUEST_ID)
            .setCircularRegion(latitude, longitude, RADIUS_METRES)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
            .setNotificationResponsiveness(RESPONSIVENESS_MS)
            .build()
        val request = GeofencingRequest.Builder().addGeofence(geofence).build()
        LocationServices.getGeofencingClient(context)
            .addGeofences(request, pendingIntent(context))
    }

    fun registerLastKnown(context: Context) {
        val settings = MapSettings(context)
        val latitude = settings.lastLatitude ?: return
        val longitude = settings.lastLongitude ?: return
        register(context, latitude, longitude)
    }

    fun unregister(context: Context) {
        LocationServices.getGeofencingClient(context).removeGeofences(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, GeofenceLocationReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
    )

    private fun hasBackgroundLocation(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
}

class GeofenceLocationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError() || event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_EXIT) return
        val location: Location? = event.triggeringLocation
        RefreshScheduler.enqueueGeofenceRefresh(context, location)
    }
}
