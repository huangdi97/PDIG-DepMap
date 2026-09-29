package com.pdig.uivnext

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.VNextShell

/**
 * vNext 应用入口（Presentation Layer 演示；不触碰真实 domain repos）。
 * 默认进入 Now；--vnext 启动参数由 Main.kt 路由到此处。
 */
@Composable
fun VNextApp(app: VAppState) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = PdigV2Colors.Primary,
            background = PdigV2Colors.Canvas,
            surface = PdigV2Colors.Surface,
        ),
    ) {
        VNextShell(app)
    }
}

/** 构造应用状态（含初始屏/相机）。 */
fun createVNextAppState(screen: VScreen = VScreen.NOW, cameraPreset: String? = null): VAppState {
    val app = VAppState(initialScreen = screen)
    if (cameraPreset != null) app.applyCameraPreset(cameraPreset)
    return app
}