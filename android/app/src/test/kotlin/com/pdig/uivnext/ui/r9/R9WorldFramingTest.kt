package com.pdig.uivnext.ui.r9

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class R9WorldFramingTest {
    @Test
    fun fullEarthFitsStageAfterVisualScale() {
        val occupied = r9PlanetDiameterFraction(R9_WORLD_VISUAL_SCALE)
        assertTrue("Planet must not be cropped to an oversized hemisphere", occupied <= .88f)
        assertTrue("Planet still needs to dominate the world identity scene", occupied >= .77f)
    }

    @Test
    fun greetingTracksRealHourInsteadOfFixedMorningCopy() {
        assertEquals("早上好", r9Greeting(8))
        assertEquals("中午好", r9Greeting(12))
        assertEquals("下午好", r9Greeting(14))
        assertEquals("晚上好", r9Greeting(21))
        assertEquals("你好", r9Greeting(2))
    }
}
