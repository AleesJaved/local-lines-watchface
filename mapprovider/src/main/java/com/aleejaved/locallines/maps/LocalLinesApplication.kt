package com.aleejaved.locallines.maps

import android.app.Application
import org.maplibre.android.MapLibre

class LocalLinesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
        MapSnapshotRepository.get(this).ensureFallback()
        RefreshScheduler.schedule(this)
    }
}
