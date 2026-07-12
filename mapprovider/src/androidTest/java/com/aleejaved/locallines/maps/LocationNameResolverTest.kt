package com.aleejaved.locallines.maps

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocationNameResolverTest {
    @Test
    fun parsesStreetTownAndCityFromNominatimAddress() {
        val result = LocationNameResolver.parseNominatim(
            """{"address":{"road":"High Street","suburb":"Old Town","city":"London"}}""",
        )

        assertEquals("High Street", result?.street)
        assertEquals("Old Town", result?.town)
        assertEquals("London", result?.city)
    }

    @Test
    fun usesUsefulFallbackAddressLevels() {
        val result = LocationNameResolver.parseNominatim(
            """{"address":{"pedestrian":"Market Walk","village":"Oakley","state":"England"}}""",
        )

        assertEquals("Market Walk", result?.street)
        assertEquals("Oakley", result?.town)
        assertEquals("England", result?.city)
    }

    @Test
    fun returnsNullWithoutAddressNames() {
        assertNull(LocationNameResolver.parseNominatim("""{"address":{}}"""))
    }
}
