package com.aleejaved.locallines.maps

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager

object RefreshScheduler {
    private const val UNIQUE_WORK = "local-lines-map-refresh"

    fun schedule(context: Context, mode: RefreshMode = MapSettings(context).refreshMode) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
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
    }
}
