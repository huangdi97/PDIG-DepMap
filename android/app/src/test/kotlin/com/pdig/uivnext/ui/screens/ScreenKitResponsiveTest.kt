package com.pdig.uivnext.ui.screens

import com.pdig.uivnext.model.MediaBreakpoint
import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenKitResponsiveTest {

    @Test
    fun previewIdentityLabelsHonorGlobalPrivacyWithoutMutatingSourceValues() {
        val originalName = "微信账户"
        val originalProvider = "示例服务方"

        assertEquals("账户（已遮蔽）", previewVisibleObjectName(originalName, true, "账户"))
        assertEquals(originalName, previewVisibleObjectName(originalName, false, "账户"))
        assertEquals("服务方已遮蔽", previewVisibleProvider(originalProvider, true))
        assertEquals(originalProvider, previewVisibleProvider(originalProvider, false))
        assertEquals(originalName, "微信账户")
        assertEquals(originalProvider, "示例服务方")
    }

    @Test
    fun secondaryInfrastructureUsesOneColumnOnPhoneAndTwoOnLargerSurfaces() {
        assertEquals(1, infraObjectColumnCount(MediaBreakpoint.COMPACT))
        assertEquals(2, infraObjectColumnCount(MediaBreakpoint.MEDIUM))
        assertEquals(2, infraObjectColumnCount(MediaBreakpoint.EXPANDED))
    }
}
