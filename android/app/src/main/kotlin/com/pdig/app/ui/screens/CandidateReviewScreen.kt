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
import com.pdig.app.data.CandidateRow
import com.pdig.app.ui.components.EmptyState
import com.pdig.app.ui.components.LoadingState
import com.pdig.app.ui.components.PdigCard
import com.pdig.app.ui.components.PdigTopBar
import com.pdig.app.ui.theme.PdigTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
/** DiscoveryCandidate：accept 才创建 Node；不进 Impact、不 bump revision（spec §24/§26，H-16）。 */
@Composable
fun CandidateReviewScreen(nav: NavController) {
    val context = LocalContext.current
    val container = remember { AppContainer.get(context) }
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<CandidateRow>?>(null) }
    var acceptedLabels by remember { mutableStateOf<Set<String>>(emptySet()) }

    fun reload() {
        scope.launch { items = withContext(Dispatchers.IO) { container.pendingCandidates() } }
    }

    LaunchedEffect(Unit) { reload() }

    Scaffold(topBar = { PdigTopBar("待确认服务", onBack = { nav.popBackStack() }) }) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(PdigTokens.SpaceLg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceMd),
        ) {
            when {
                items == null -> LoadingState()
                items!!.isEmpty() -> EmptyState("没有新的候选对象。")
                else -> items!!.forEach { c ->
                    if (c.label in acceptedLabels) return@forEach
                    PdigCard {
                        Column(verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceXs)) {
                            Text(c.label, style = PdigTokens.BodyStrong)
                            Text(
                                "观测到 ${c.observationCount} 次；尚未确认，不会参与影响分析。",
                                style = PdigTokens.Caption,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(PdigTokens.SpaceSm)) {
                                Button(onClick = {
                                    scope.launch {
                                        withContext(Dispatchers.IO) { container.acceptCandidate(c.id) }
                                        acceptedLabels = acceptedLabels + c.label
                                    }
                                }) { Text("确认") }
                                OutlinedButton(onClick = {
                                    scope.launch {
                                        withContext(Dispatchers.IO) { container.dismissCandidate(c.id) }
                                        acceptedLabels = acceptedLabels + c.label
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
