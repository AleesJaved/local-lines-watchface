package com.aleejaved.locallines.maps

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MapSnapshotDeviceTest {
    @Test
    fun rendersLiveMapUsingWatchLocation() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<LocalLinesApplication>()
        val repository = MapSnapshotRepository.get(context)

        val liveResult = repository.refresh(force = true)
        val result = if (liveResult == MapSnapshotRepository.RefreshResult.NO_LOCATION) {
            val settings = MapSettings(context)
            repository.refreshAt(
                requireNotNull(settings.lastLatitude) { "No saved latitude available for palette test" },
                requireNotNull(settings.lastLongitude) { "No saved longitude available for palette test" },
            )
        } else {
            liveResult
        }

        assertEquals(MapSnapshotRepository.RefreshResult.UPDATED, result)
        assertTrue(repository.currentMapFile().name == "local_lines_map.jpg")
        assertTrue(repository.currentMapFile().length() > 10_000)
        assertTrue(repository.currentMapFile(MapPalette.LIGHT).name == "local_lines_map_light.jpg")
        assertTrue(repository.currentMapFile(MapPalette.LIGHT).length() > 10_000)
    }
}
