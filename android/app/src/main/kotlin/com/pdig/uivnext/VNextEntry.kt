package com.pdig.uivnext

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.pdig.uivnext.model.PresentationProfile
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.WorkspacePreferences
import com.pdig.uivnext.ui.VNextShell

/**
 * vNext 应用入口（Presentation Layer 演示；不触碰真实 domain repos）。
 * 默认进入 Now；MainActivity 的 `--ez vnext_demo true`（debug flag）路由到此处。
 * [forcedViewportWidthDp] 供证据测试冻结宽度（rail/bottom-nav 分支判定），不参与截图像素。
 */
@Composable
fun VNextApp(app: VAppState, forcedViewportWidthDp: Int? = null) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = PdigV2Colors.Primary,
            onPrimary = androidx.compose.ui.graphics.Color.White,
            primaryContainer = PdigV2Colors.PrimarySoft,
            onPrimaryContainer = PdigV2Colors.TextPrimary,
            background = PdigV2Colors.Canvas,
            onBackground = PdigV2Colors.TextPrimary,
            surface = PdigV2Colors.Surface,
            onSurface = PdigV2Colors.TextPrimary,
            surfaceVariant = PdigV2Colors.SurfaceRaised,
            onSurfaceVariant = PdigV2Colors.TextSecondary,
            outline = PdigV2Colors.BorderStrong,
            error = PdigV2Colors.Critical,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFFF9FBFF),
                            PdigV2Colors.Canvas,
                            Color(0xFFEEF5FF),
                        ),
                    ),
                ),
        ) {
            VNextShell(app, forcedViewportWidthDp)
        }
    }
}

/** 构造应用状态（含初始屏/相机/证据参数）。 */
fun createVNextAppState(
    screen: VScreen = VScreen.NOW,
    cameraPreset: String? = null,
    customTheme: String? = null,
    changeProjection: String? = null,
    emptyDemo: Boolean = false,
    initialPresentationProfiles: Map<String, PresentationProfile> = emptyMap(),
    initialWorkspacePreferences: WorkspacePreferences = WorkspacePreferences(),
    onPresentationProfileSaved: (PresentationProfile) -> Unit = {},
    onWorkspacePreferencesSaved: (WorkspacePreferences) -> Unit = {},
): VAppState {
    val app = VAppState(
        initialScreen = screen,
        initialPresentationProfiles = initialPresentationProfiles,
        initialWorkspacePreferences = initialWorkspacePreferences,
        onPresentationProfileSaved = onPresentationProfileSaved,
        onWorkspacePreferencesSaved = onWorkspacePreferencesSaved,
    )
    if (cameraPreset != null) app.applyCameraPreset(cameraPreset)
    if (customTheme != null) app.evidenceThemeId = customTheme
    if (changeProjection != null) app.changeProjection = changeProjection
    app.emptyDemo = emptyDemo
    return app
}
