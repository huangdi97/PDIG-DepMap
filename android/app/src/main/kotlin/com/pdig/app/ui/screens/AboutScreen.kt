package com.pdig.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.pdig.app.ui.components.EmptyState
import com.pdig.app.ui.components.PdigTopBar
import com.pdig.app.ui.theme.PdigTokens
@Composable
fun AboutScreen(nav: NavController) {
    Scaffold(topBar = { PdigTopBar("关于", onBack = { nav.popBackStack() }) }) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(PdigTokens.SpaceLg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceMd),
        ) {
            Text("PDIG", style = PdigTokens.Display)
            Text("个人数字基础设施图谱", style = PdigTokens.Body)
            Text("版本 0.1.0-milestone（Native Migration）", style = PdigTokens.Caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            EmptyState("本版本为原生迁移内部里程碑版本，不是对外正式发布版本。")
        }
    }
}
