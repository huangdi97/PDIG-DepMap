package com.pdig.uivnext.evidence

import androidx.compose.ui.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pdig.uivnext.theme.PdigV2Colors
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Accessibility guard for the Android light-first visual system.
 *
 * The reference uses normal-size metadata and semantic labels throughout. Text colors therefore need
 * normal-text contrast on every light surface where the Android presentation system places them.
 */
@RunWith(AndroidJUnit4::class)
class AndroidLightAccessibilityPaletteContractTest {

    @Test
    fun normalTextPaletteMaintainsReadableContrast() {
        val surfaces = listOf(
            "surface" to PdigV2Colors.Surface,
            "canvas" to PdigV2Colors.Canvas,
            "raised" to PdigV2Colors.SurfaceRaised,
            "soft" to PdigV2Colors.PrimarySoft,
        )
        val textColors = listOf(
            "textPrimary" to PdigV2Colors.TextPrimary,
            "textSecondary" to PdigV2Colors.TextSecondary,
            "textMuted" to PdigV2Colors.TextMuted,
            "positive" to PdigV2Colors.Positive,
            "warning" to PdigV2Colors.Warning,
            "critical" to PdigV2Colors.Critical,
            "unknown" to PdigV2Colors.Unknown,
        )

        textColors.forEach { (textName, textColor) ->
            surfaces.forEach { (surfaceName, surfaceColor) ->
                val ratio = contrast(textColor, surfaceColor)
                assertTrue(
                    "$textName on $surfaceName must be >= 4.5:1, actual=$ratio",
                    ratio >= 4.5,
                )
            }
        }

        val filledActionRatio = contrast(Color.White, PdigV2Colors.PrimaryBright)
        assertTrue(
            "white filled-action text on PrimaryBright must be >= 4.5:1, actual=$filledActionRatio",
            filledActionRatio >= 4.5,
        )
    }

    private fun contrast(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private fun luminance(color: Color): Double {
        fun linear(channel: Float): Double {
            val c = channel.toDouble()
            return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * linear(color.red) +
            0.7152 * linear(color.green) +
            0.0722 * linear(color.blue)
    }
}
