package com.pdig.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.navigation.NavController
import com.pdig.app.ui.Route
import com.pdig.app.data.AppContainer
import com.pdig.app.data.DependencyRow
import com.pdig.app.data.NodeRow
import com.pdig.app.data.SourceRow
import com.pdig.app.ui.components.EmptyState
import com.pdig.app.ui.components.LoadingState
import com.pdig.app.ui.components.PdigCard
import com.pdig.app.ui.components.PdigTopBar
import com.pdig.app.ui.components.SectionHeader
import com.pdig.app.ui.theme.PdigTokens

/** 基础设施总览：一级入口（Graph 仍是二级/高级视图，不得重新变成首页 —— spec §189）。 */
@Composable
fun InfrastructureScreen(nav: NavController) {
    val context = LocalContext.current
    val container = remember { AppContainer.get(context) }
    var nodes by remember { mutableStateOf<List<NodeRow>?>(null) }
    // 数据库读一律在 IO 线程（主线程做 DB 会在冷启动触发 ANR，2026-09-16 修复）
    LaunchedEffect(Unit) { nodes = withContext(Dispatchers.IO) { container.nodes() } }

    Scaffold(topBar = { PdigTopBar("我的基础设施", onBack = { nav.popBackStack() }) }) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(PdigTokens.SpaceLg)
                .verticalScroll(rememberScrollState()),
        ) {
            when {
                nodes == null -> LoadingState()
                nodes?.isEmpty() == true -> EmptyState("还没有记录任何对象。可以先导入一份账单。")
                else -> {
                    nodes?.forEach { n ->
                        PdigCard(onClick = { nav.navigate(Route.NODE.replace("{nodeId}", n.id)) }) {
                            Text(n.name, style = PdigTokens.BodyStrong)
                        }
                    }
                    SectionHeader("高级视图")
                    PdigCard(onClick = { nav.navigate(Route.GRAPH) }) { Text("依赖图") }
                }
            }
        }
    }
}

/** 依赖图（二级视图）：只展示用户已确认的现实关系，不显示内部术语（spec §65）。 */
@Composable
fun GraphScreen(nav: NavController) {
    val context = LocalContext.current
    val container = remember { AppContainer.get(context) }
    var deps by remember { mutableStateOf<List<DependencyRow>?>(null) }
    LaunchedEffect(Unit) { deps = withContext(Dispatchers.IO) { container.dependencies() } }

    Scaffold(topBar = { PdigTopBar("依赖图", onBack = { nav.popBackStack() }) }) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(PdigTokens.SpaceLg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceSm),
        ) {
            when {
                deps == null -> LoadingState()
                deps?.isEmpty() == true -> EmptyState("还没有已确认的依赖关系。")
                else -> deps?.forEach { d ->
                    PdigCard(onClick = { nav.navigate(Route.NODE.replace("{nodeId}", d.to)) }) {
                        Column {
                            Text("${d.fromName} → ${d.toName}", style = PdigTokens.BodyStrong)
                            Text(
                                when (d.relation) {
                                    "funding_source" -> "资金来源"
                                    "merchant_agreement" -> "服务付费关系"
                                    else -> "关系"
                                } + " · " + if (d.criticality == "required") "必需" else "未确认",
                                style = PdigTokens.Caption,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NodeDetailScreen(nav: NavController, nodeId: String) {
    val context = LocalContext.current
    val container = remember { AppContainer.get(context) }
    val scope = rememberCoroutineScope()
    var node by remember { mutableStateOf<NodeRow?>(null) }
    var deps by remember { mutableStateOf<List<DependencyRow>>(emptyList()) }
    var refresh by remember { mutableStateOf(0) }

    LaunchedEffect(nodeId, refresh) {
        val loaded = withContext(Dispatchers.IO) {
            val n = container.nodes(true).firstOrNull { it.id == nodeId }
            val d = container.dependencies().filter { it.from == nodeId || it.to == nodeId }
            n to d
        }
        node = loaded.first
        deps = loaded.second
    }

    Scaffold(topBar = { PdigTopBar("对象详情", onBack = { nav.popBackStack() }) }) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(PdigTokens.SpaceLg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceMd),
        ) {
            when {
                node == null -> EmptyState("没有找到这个对象。")
                else -> {
                    Text(node?.name ?: "", style = PdigTokens.Title)

                    // 支付工具可以直接进行影响面模拟（走 core.impact，UI 不推导）
                    if (node?.kind == "payment_instrument") {
                        PdigCard(
                            onClick = { nav.navigate(Route.IMPACT.replace("{nodeId}", nodeId)) },
                        ) {
                            Text("如果这张卡停用，会影响到什么", style = PdigTokens.BodyStrong)
                        }
                    }

                    SectionHeader("相关依赖")
                    if (deps.isEmpty()) {
                        EmptyState("暂无相关依赖。")
                    } else {
                        deps.forEach { d ->
                            PdigCard {
                                Column(verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceXs)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${d.fromName} → ${d.toName}", style = PdigTokens.Body, modifier = Modifier.weight(1f))
                                        Text(
                                            if (d.criticality == "required") "必需" else "未确认",
                                            style = PdigTokens.Label,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    // criticality=required 只能由用户显式确认（机器永不产生 required）。
                                    // 这一步属于依赖确认更新，会 bump graphRevision（见 GraphRevisionMachine）。
                                    Button(onClick = {
                                        scope.launch {
                                            withContext(Dispatchers.IO) {
                                                container.setDependencyCriticality(
                                                    d.id, d.criticality != "required",
                                                )
                                            }
                                            refresh += 1
                                        }
                                    }) {
                                        Text(if (d.criticality == "required") "取消必需" else "标记为必需")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SourceManagementScreen(nav: NavController) {
    val context = LocalContext.current
    val container = remember { AppContainer.get(context) }
    var rows by remember { mutableStateOf<List<SourceRow>?>(null) }
    LaunchedEffect(Unit) { rows = withContext(Dispatchers.IO) { container.sourceInstances() } }

    Scaffold(topBar = { PdigTopBar("数据来源", onBack = { nav.popBackStack() }) }) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(PdigTokens.SpaceLg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceMd),
        ) {
            when {
                rows == null -> LoadingState()
                rows?.isEmpty() == true -> EmptyState("还没有数据来源。")
                else -> rows?.forEach { s ->
                    PdigCard {
                        Column {
                            Text(s.label, style = PdigTokens.BodyStrong)
                            Text(
                                if (s.lastIngestedAt == null) "从未导入过数据" else "最近更新：${s.lastIngestedAt?.take(10) ?: ""}",
                                style = PdigTokens.Caption,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            PdigCard(onClick = { nav.navigate(Route.IMPORT) }) { Text("导入账单文件") }
        }
    }
}
