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

        assertEquals(MapSnapshotRepository.RefreshResult.UPDATED, repository.refresh(force = true))
        assertTrue(repository.currentMapFile().name == "local_lines_map.jpg")
        assertTrue(repository.currentMapFile().length() > 10_000)
    }
}
