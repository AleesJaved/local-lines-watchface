package com.aleejaved.locallines.maps

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService

class LocationComplicationService : SuspendingComplicationDataSourceService() {
    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData =
        buildData(request.complicationType, includeTapAction = true)

    override fun getPreviewData(type: ComplicationType): ComplicationData? =
        buildData(type, includeTapAction = false)

    private fun buildData(type: ComplicationType, includeTapAction: Boolean): ComplicationData {
        val label = MapSettings(this).selectedLocationLabel()
            ?: if (includeTapAction) return NoDataComplicationData() else getString(R.string.location_preview)
        val text = PlainComplicationText.Builder(label).build()
        val contentDescription = PlainComplicationText.Builder(label).build()
        val tapAction = if (includeTapAction) {
            PendingIntent.getActivity(
                this,
                20,
                Intent(this, SettingsActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        } else null

        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(text, contentDescription)
                .apply { tapAction?.let(::setTapAction) }
                .build()
            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(text, contentDescription)
                .apply { tapAction?.let(::setTapAction) }
                .build()
            else -> NoDataComplicationData()
        }
    }
}
