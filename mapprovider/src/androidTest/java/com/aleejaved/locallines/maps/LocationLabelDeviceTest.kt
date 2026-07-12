package com.aleejaved.locallines.maps

import android.location.Location
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocationLabelDeviceTest {
    @Test
    fun updatesLabelWithoutRenderingMap() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<LocalLinesApplication>()
        val settings = MapSettings(context).apply { locationRoadEnabled = true }
        val location = Location("saved-test-location").apply {
            latitude = requireNotNull(settings.lastLatitude)
            longitude = requireNotNull(settings.lastLongitude)
            time = System.currentTimeMillis()
        }
        val started = SystemClock.elapsedRealtime()

        val updated = LocationLabelRepository(context).refreshAt(location)

        assertTrue("Location label lookup failed", updated)
        assertTrue("Location label remained empty", settings.selectedLocationLabel().isNullOrBlank().not())
        assertTrue("Location label update took too long", SystemClock.elapsedRealtime() - started < 15_000L)
    }
}
