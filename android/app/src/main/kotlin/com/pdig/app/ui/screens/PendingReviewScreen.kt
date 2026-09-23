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
import com.pdig.app.data.ProposalRow
import com.pdig.app.ui.components.EmptyState
import com.pdig.app.ui.components.LoadingState
import com.pdig.app.ui.components.PdigCard
import com.pdig.app.ui.components.PdigTopBar
import com.pdig.app.ui.theme.PdigTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
/** 待确认：Proposal（候选关系）必须由用户确认，不得自动成为 Reality（spec §13）。 */
@Composable
fun PendingReviewScreen(nav: NavController) {
    val context = LocalContext.current
    val container = remember { AppContainer.get(context) }
    var proposals by remember { mutableStateOf<List<ProposalRow>?>(null) }
    var nodeNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            proposals = container.pendingProposals()
            nodeNames = container.nodes(includeArchived = true).associate { it.id to it.name }
        }
    }

    /** 显示人名而非内部 id（依赖详情页同理：不暴露内部术语，spec §65）。 */
    fun name(id: String): String = nodeNames[id] ?: id

    Scaffold(topBar = { PdigTopBar("待确认服务", onBack = { nav.popBackStack() }) }) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(PdigTokens.SpaceLg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceMd),
        ) {
            when {
                proposals == null -> LoadingState()
                proposals?.isEmpty() == true -> EmptyState("没有待确认的项目。")
                else -> proposals?.forEach { p ->
                    var done by remember(p.id) { mutableStateOf(false) }
                    if (done) return@forEach
                    PdigCard {
                        Column(verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceSm)) {
                            Text("${name(p.from)} → ${name(p.to)}", style = PdigTokens.BodyStrong)
                            Text(
                                "观测到 ${p.observationCount} 次，置信度 ${(p.confidence * 100).toInt()}%",
                                style = PdigTokens.Caption,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "确认前不会当成事实，也不参与影响分析。",
                                style = PdigTokens.Caption,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(PdigTokens.SpaceSm)) {
                                Button(onClick = {
                                    scope.launch {
                                        withContext(Dispatchers.IO) { container.acceptProposal(p.id) }
                                        done = true
                                    }
                                }) { Text("确认") }
                                OutlinedButton(onClick = {
                                    scope.launch {
                                        withContext(Dispatchers.IO) { container.rejectProposal(p.id) }
                                        done = true
                                    }
                                }) { Text("忽略") }
                            }
                        }
                    }
                }
            }
        }
    }
}
