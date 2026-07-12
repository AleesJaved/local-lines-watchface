package com.aleejaved.locallines.maps

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class MapRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val latitude = inputData.getDouble(KEY_LATITUDE, Double.NaN)
        val longitude = inputData.getDouble(KEY_LONGITUDE, Double.NaN)
        val locationTime = inputData.getLong(KEY_LOCATION_TIME, 0L)
        val repository = MapSnapshotRepository.get(applicationContext)
        val refreshResult = if (latitude.isFinite() && longitude.isFinite() && locationTime > 0L) {
            repository.refreshFromPassive(latitude, longitude, locationTime)
        } else {
            repository.refresh(force = false)
        }
        return when (refreshResult) {
            MapSnapshotRepository.RefreshResult.UPDATED -> Result.success()
            MapSnapshotRepository.RefreshResult.FAILED -> Result.retry()
            else -> Result.success()
        }
    }

    companion object {
        const val KEY_LATITUDE = "latitude"
        const val KEY_LONGITUDE = "longitude"
        const val KEY_LOCATION_TIME = "location_time"
    }
}
