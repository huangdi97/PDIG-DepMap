package com.pdig.uivnext.ui.screens

import com.pdig.uivnext.model.MediaBreakpoint
import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenKitResponsiveTest {

    @Test
    fun secondaryInfrastructureUsesOneColumnOnPhoneAndTwoOnLargerSurfaces() {
        assertEquals(1, infraObjectColumnCount(MediaBreakpoint.COMPACT))
        assertEquals(2, infraObjectColumnCount(MediaBreakpoint.MEDIUM))
        assertEquals(2, infraObjectColumnCount(MediaBreakpoint.EXPANDED))
    }
}
