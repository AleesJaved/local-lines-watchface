package com.aleejaved.locallines.maps

import android.content.ComponentName
import android.content.Context
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class MapRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return when (MapSnapshotRepository.get(applicationContext).refresh(force = false)) {
            MapSnapshotRepository.RefreshResult.UPDATED -> {
                ComplicationDataSourceUpdateRequester.create(
                    applicationContext,
                    ComponentName(applicationContext, MapComplicationService::class.java),
                ).requestUpdateAll()
                Result.success()
            }
            MapSnapshotRepository.RefreshResult.FAILED -> Result.retry()
            else -> Result.success()
        }
    }
}
