package com.pdig.uivnext.ui.r9

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Privacy is a display transformation; source data is immutable.
 * Sensitive tail digits must not leak through a secondary R9 label.
 */
class R9PrivacyPresentationTest {
    @Test
    fun cardTailIsHiddenWhenMaskEnabled() {
        val original = "4821"
        assertEquals("••••", r9VisibleLast4(original, true))
        assertEquals("4821", r9VisibleLast4(original, false))
        assertEquals("4821", original)
    }

    @Test
    fun phoneNumberIsHiddenAcrossPreviewScreens() {
        val original = "+86 138****8823"
        assertEquals("号码已遮蔽", r9VisibleNumber(original, true))
        assertEquals(original, r9VisibleNumber(original, false))
        assertEquals("+86 138****8823", original)
    }
}
