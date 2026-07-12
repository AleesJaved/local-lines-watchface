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
    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        val settings = MapSettings(this)
        if (System.currentTimeMillis() - settings.lastLocationLabelUpdatedMillis > LABEL_MAX_AGE_MS) {
            RefreshScheduler.enqueueGlanceRefresh(this)
        }
        return buildData(request.complicationType, includeTapAction = true)
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? =
        buildData(type, includeTapAction = false)

    private fun buildData(type: ComplicationType, includeTapAction: Boolean): ComplicationData {
        val settings = MapSettings(this)
        val label = settings.selectedLocationLabel() ?: when {
            !includeTapAction -> getString(R.string.location_preview)
            settings.hasEnabledLocationParts() -> getString(R.string.location_resolving)
            else -> return NoDataComplicationData()
        }
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

    companion object {
        private const val LABEL_MAX_AGE_MS = 5 * 60_000L
    }
}
