package com.aleejaved.locallines.maps

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.complications.data.PhotoImageComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService

class MapComplicationService : SuspendingComplicationDataSourceService() {
    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        if (request.complicationType != ComplicationType.PHOTO_IMAGE) return NoDataComplicationData()
        return buildData(includeTapAction = true)
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? =
        if (type == ComplicationType.PHOTO_IMAGE) buildData(includeTapAction = false) else null

    private fun buildData(includeTapAction: Boolean): ComplicationData {
        val bitmap = MapSnapshotRepository.get(this).loadCurrentBitmap()
        val builder = PhotoImageComplicationData.Builder(
            photoImage = Icon.createWithBitmap(bitmap),
            contentDescription = PlainComplicationText.Builder(
                text = getText(R.string.map_content_description),
            ).build(),
        )
        if (includeTapAction) {
            val intent = Intent(this, SettingsActivity::class.java)
            builder.setTapAction(
                PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
        }
        return builder.build()
    }
}
