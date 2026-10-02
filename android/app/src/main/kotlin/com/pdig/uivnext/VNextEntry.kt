package com.pdig.uivnext

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.VNextShell

/**
 * vNext 应用入口（Presentation Layer 演示；不触碰真实 domain repos）。
 * 默认进入 Now；MainActivity 的 `--ez vnext_demo true`（debug flag）路由到此处。
 * [forcedViewportWidthDp] 供证据测试冻结宽度（rail/bottom-nav 分支判定），不参与截图像素。
 */
@Composable
fun VNextApp(app: VAppState, forcedViewportWidthDp: Int? = null) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = PdigV2Colors.Primary,
            background = PdigV2Colors.Canvas,
            surface = PdigV2Colors.Surface,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = PdigV2Colors.Canvas,
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
): VAppState {
    val app = VAppState(initialScreen = screen)
    if (cameraPreset != null) app.applyCameraPreset(cameraPreset)
    if (customTheme != null) app.evidenceThemeId = customTheme
    if (changeProjection != null) app.changeProjection = changeProjection
    app.emptyDemo = emptyDemo
    return app
}
