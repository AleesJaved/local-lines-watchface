package com.aleejaved.locallines.maps

import android.content.Context
import android.location.Location
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.location.LocationServices
import java.util.concurrent.TimeUnit

object RefreshScheduler {
    private const val UNIQUE_WORK = "local-lines-map-refresh"
    private const val PASSIVE_WORK = "local-lines-passive-refresh"
    private const val GLANCE_WORK = "local-lines-glance-refresh"

    fun schedule(context: Context, mode: RefreshMode = MapSettings(context).refreshMode) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        val request = PeriodicWorkRequestBuilder<MapRefreshWorker>(mode.interval)
            .setConstraints(constraints)
            .addTag(UNIQUE_WORK)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
        if (mode == RefreshMode.LIVE || MapSettings(context).hasEnabledLocationParts()) {
            PassiveLocationManager.register(context)
            GeofenceLocationManager.registerLastKnown(context)
        } else {
            PassiveLocationManager.unregister(context)
            GeofenceLocationManager.unregister(context)
        }
    }

    fun enqueuePassiveRefresh(context: Context, location: Location) {
        if (MapSettings(context).refreshMode != RefreshMode.LIVE) return
        enqueueMapRefresh(context, location)
    }

    fun enqueueGeofenceRefresh(context: Context, location: Location?) {
        enqueueLocationRefresh(context, location)
        location ?: return
        val settings = MapSettings(context)
        val previousLatitude = settings.lastLatitude ?: return enqueueMapRefresh(context, location)
        val previousLongitude = settings.lastLongitude ?: return enqueueMapRefresh(context, location)
        val distance = FloatArray(1)
        Location.distanceBetween(
            previousLatitude,
            previousLongitude,
            location.latitude,
            location.longitude,
            distance,
        )
        if (distance[0] >= settings.refreshMode.movementMetres) enqueueMapRefresh(context, location)
    }

    private fun enqueueMapRefresh(context: Context, location: Location) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        val input = Data.Builder()
            .putDouble(MapRefreshWorker.KEY_LATITUDE, location.latitude)
            .putDouble(MapRefreshWorker.KEY_LONGITUDE, location.longitude)
            .putLong(MapRefreshWorker.KEY_LOCATION_TIME, location.time)
            .build()
        val request = OneTimeWorkRequestBuilder<MapRefreshWorker>()
            .setInputData(input)
            .setConstraints(constraints)
            .setInitialDelay(30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            PASSIVE_WORK,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    fun enqueueGlanceRefresh(context: Context) {
        if (!MapSettings(context).hasEnabledLocationParts()) return
        LocationServices.getFusedLocationProviderClient(context).flushLocations()
        enqueueLocationRefresh(context)
    }

    fun enqueueLocationRefresh(context: Context, location: Location? = null) {
        if (!MapSettings(context).hasEnabledLocationParts()) return
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val input = Data.Builder().apply {
            location?.let {
                putDouble(LocationLabelWorker.KEY_LATITUDE, it.latitude)
                putDouble(LocationLabelWorker.KEY_LONGITUDE, it.longitude)
                putLong(LocationLabelWorker.KEY_LOCATION_TIME, it.time)
            }
        }.build()
        val request = OneTimeWorkRequestBuilder<LocationLabelWorker>()
            .setInputData(input)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            GLANCE_WORK,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }
}
