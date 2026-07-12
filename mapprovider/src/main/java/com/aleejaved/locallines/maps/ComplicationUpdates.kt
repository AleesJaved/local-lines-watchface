package com.aleejaved.locallines.maps

import android.content.ComponentName
import android.content.Context
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester

object ComplicationUpdates {
    fun requestAll(context: Context) {
        listOf(
            MapComplicationService::class.java,
            LocationComplicationService::class.java,
        ).forEach { service ->
            ComplicationDataSourceUpdateRequester.create(
                context,
                ComponentName(context, service),
            ).requestUpdateAll()
        }
    }
}
