package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pdig.uivnext.model.MediaBreakpoint
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.r9.R10CardImageStudio

/**
 * Android card personalization remains a deliberately small feature at every
 * window size. Wider screens provide breathing room, not an engineering Studio.
 */
@Composable
internal fun R19CardImageStudio(
    app: VAppState,
    breakpoint: MediaBreakpoint,
) {
    val maxWidth = when (breakpoint) {
        MediaBreakpoint.COMPACT -> 600.dp
        MediaBreakpoint.MEDIUM -> 680.dp
        MediaBreakpoint.EXPANDED -> 760.dp
    }
    Box(
        Modifier.fillMaxSize().testTag("pdig.r19.card-image.workspace"),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            Modifier.fillMaxHeight().widthIn(max = maxWidth),
        ) {
            R10CardImageStudio(app)
        }
    }
}
