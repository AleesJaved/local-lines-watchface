package com.aleejaved.locallines.maps

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RefreshPolicyTest {
    @Test fun `first live map always renders`() {
        assertTrue(RefreshPolicy.shouldRender(false, false, 0f, RefreshMode.BALANCED))
    }

    @Test fun `manual refresh always renders`() {
        assertTrue(RefreshPolicy.shouldRender(true, true, 0f, RefreshMode.BATTERY_SAVER))
    }

    @Test fun `balanced mode reuses map below one kilometre`() {
        assertFalse(RefreshPolicy.shouldRender(false, true, 999f, RefreshMode.BALANCED))
    }

    @Test fun `each movement threshold is inclusive`() {
        RefreshMode.entries.forEach { mode ->
            assertTrue(RefreshPolicy.shouldRender(false, true, mode.movementMetres, mode))
        }
    }
}
