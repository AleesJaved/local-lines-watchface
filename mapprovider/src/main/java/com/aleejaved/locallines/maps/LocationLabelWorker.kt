package com.aleejaved.locallines.maps

import android.content.Context
import android.location.Location
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class LocationLabelWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val latitude = inputData.getDouble(KEY_LATITUDE, Double.NaN)
        val longitude = inputData.getDouble(KEY_LONGITUDE, Double.NaN)
        val time = inputData.getLong(KEY_LOCATION_TIME, 0L)
        val repository = LocationLabelRepository(applicationContext)
        if (latitude.isFinite() && longitude.isFinite()) {
            repository.refreshAt(Location("fused-update").apply {
                this.latitude = latitude
                this.longitude = longitude
                this.time = time
            })
        } else {
            repository.refreshCurrent()
        }
        return Result.success()
    }

    companion object {
        const val KEY_LATITUDE = "label_latitude"
        const val KEY_LONGITUDE = "label_longitude"
        const val KEY_LOCATION_TIME = "label_location_time"
    }
}
