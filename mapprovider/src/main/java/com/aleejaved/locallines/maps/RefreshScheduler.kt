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
import java.util.concurrent.TimeUnit

object RefreshScheduler {
    private const val UNIQUE_WORK = "local-lines-map-refresh"
    private const val PASSIVE_WORK = "local-lines-passive-refresh"

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
        if (mode == RefreshMode.LIVE) PassiveLocationManager.register(context)
        else PassiveLocationManager.unregister(context)
    }

    fun enqueuePassiveRefresh(context: Context, location: Location) {
        if (MapSettings(context).refreshMode != RefreshMode.LIVE) return
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
            .setInitialDelay(5, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            PASSIVE_WORK,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }
}
