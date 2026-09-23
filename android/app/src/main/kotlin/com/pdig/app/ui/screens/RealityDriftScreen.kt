package com.pdig.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.pdig.app.data.AppContainer
import com.pdig.app.data.DriftRow
import com.pdig.app.ui.components.EmptyState
import com.pdig.app.ui.components.LoadingState
import com.pdig.app.ui.components.PdigCard
import com.pdig.app.ui.components.PdigTopBar
import com.pdig.app.ui.theme.PdigTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
/** RealityDrift：positive evidence only；absence-only 永不产生 Drift（spec §23）。 */
@Composable
fun RealityDriftScreen(nav: NavController) {
    val context = LocalContext.current
    val container = remember { AppContainer.get(context) }
    val scope = rememberCoroutineScope()
    var drifts by remember { mutableStateOf<List<DriftRow>?>(null) }
    var resolvedIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    fun reload() {
        scope.launch { drifts = withContext(Dispatchers.IO) { container.openDrifts() } }
    }

    LaunchedEffect(Unit) { reload() }

    Scaffold(topBar = { PdigTopBar("可能发生了变化", onBack = { nav.popBackStack() }) }) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(PdigTokens.SpaceLg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceMd),
        ) {
            when {
                drifts == null -> LoadingState()
                drifts!!.isEmpty() -> EmptyState("没有检测到需要确认的变化。")
                else -> drifts!!.forEach { d ->
                    if (d.id in resolvedIds) return@forEach
                    // H-17：发现 → Review → 用户选择 → Reality mutation。
                    // 已更换 / 两者都在用 是 Reality mutation（resolve_* ∈ bumpsOn）；
                    // 没变化 = dismiss（不改 Reality）；稍后确认 = 保持 open（本屏不操作）。
                    PdigCard {
                        Column(verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceXs)) {
                            Text("可能发生了变化", style = PdigTokens.BodyStrong)
                            Text(
                                "依据 ${d.observationCount} 条观测记录（${d.detectedAt.take(10)}）。" +
                                    "请确认：这张卡现在怎么在用？",
                                style = PdigTokens.Caption,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(PdigTokens.SpaceSm)) {
                                Button(onClick = {
                                    scope.launch {
                                        withContext(Dispatchers.IO) { container.resolveDriftAsReplacement(d.id) }
                                        resolvedIds = resolvedIds + d.id
                                    }
                                }) { Text("已更换") }
                                Button(onClick = {
                                    scope.launch {
                                        withContext(Dispatchers.IO) { container.resolveDriftAsAdditionalPath(d.id) }
                                        resolvedIds = resolvedIds + d.id
                                    }
                                }) { Text("两者都在用") }
                                OutlinedButton(onClick = {
                                    scope.launch {
                                        withContext(Dispatchers.IO) { container.dismissDrift(d.id) }
                                        resolvedIds = resolvedIds + d.id
                                    }
                                }) { Text("没变化") }
                                TextButton(onClick = { resolvedIds = resolvedIds + d.id }) { Text("稍后确认") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PdriftCard(d: DriftRow, onClick: () -> Unit) {
    PdigCard(onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceXs)) {
            Text("可能发生了变化", style = PdigTokens.BodyStrong)
            Text(
                "依据 ${d.observationCount} 条观测记录（${d.detectedAt.take(10)}）",
                style = PdigTokens.Caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
