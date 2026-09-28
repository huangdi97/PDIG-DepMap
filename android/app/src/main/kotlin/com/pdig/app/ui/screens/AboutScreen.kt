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
            Text("版本 0.3.1", style = PdigTokens.Caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            EmptyState("数据默认留在本机，不上传任何服务器。")
        }
    }
}
