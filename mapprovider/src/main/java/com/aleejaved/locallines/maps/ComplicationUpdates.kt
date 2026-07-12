package com.aleejaved.locallines.maps

import android.content.ComponentName
import android.content.Context
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester

object ComplicationUpdates {
    fun requestAll(context: Context) {
        requestMap(context)
        requestLocation(context)
    }

    fun requestMap(context: Context) = request(context, MapComplicationService::class.java)

    fun requestLocation(context: Context) = request(context, LocationComplicationService::class.java)

    private fun request(context: Context, service: Class<*>) {
            ComplicationDataSourceUpdateRequester.create(
                context,
                ComponentName(context, service),
            ).requestUpdateAll()
    }
}
