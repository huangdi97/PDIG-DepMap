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
import androidx.compose.material3.OutlinedTextField
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
    var query by remember { mutableStateOf("") }
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
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("搜索对象") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val filtered = (nodes ?: emptyList())
                        .filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) }
                    if (filtered.isEmpty()) {
                        EmptyState("没有找到匹配「${query.trim()}」的对象。")
                    } else {
                        groupedNodes(filtered).forEach { (label, items) ->
                            SectionHeader("$label（${items.size}）")
                            items.forEach { n ->
                                PdigCard(onClick = { nav.navigate(Route.NODE.replace("{nodeId}", n.id)) }) {
                                    Text(n.name, style = PdigTokens.BodyStrong)
                                }
                            }
                        }
                    }
                    SectionHeader("高级视图")
                    PdigCard(onClick = { nav.navigate(Route.GRAPH) }) { Text("依赖图") }
                }
            }
        }
    }
}

/**
 * 节点 kind 的人话分组名（spec §65：内部 wire 值绝不上屏）。
 * NodeKind 枚举没有 counterparty —— 收款对象以 service 节点表示（导入时 merchantRaw → SERVICE）。
 */
internal fun nodeKindGroupLabel(kind: String): String = when (kind) {
    "payment_instrument" -> "支付方式"
    "service" -> "收款对象"
    else -> "其他"
}

/** 按人话分组名聚合节点；分组顺序固定，空组不显示。 */
internal fun groupedNodes(nodes: List<NodeRow>): List<Pair<String, List<NodeRow>>> {
    val groups = nodes.groupBy { nodeKindGroupLabel(it.kind) }
    return listOf("支付方式", "收款对象", "其他").mapNotNull { label ->
        groups[label]?.takeIf { it.isNotEmpty() }?.let { label to it }
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
    var pendingIssueCount by remember { mutableStateOf<Int?>(null) }
    var hasPendingProposals by remember { mutableStateOf(false) }

    LaunchedEffect(nodeId, refresh) {
        val loaded = withContext(Dispatchers.IO) {
            val n = container.nodes(true).firstOrNull { it.id == nodeId }
            val d = container.dependencies().filter { it.from == nodeId || it.to == nodeId }
            val proposals = container.pendingProposals().count { it.from == nodeId || it.to == nodeId }
            val drifts = container.openDrifts().count { it.targetNodeId == nodeId }
            NodeDetailSnapshot(n, d, proposals, drifts)
        }
        node = loaded.node
        deps = loaded.deps
        pendingIssueCount = loaded.pendingProposals + loaded.openDrifts
        hasPendingProposals = loaded.pendingProposals > 0
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

                    // NodeDetail 六问：① 这是什么 ② 确认了什么（③ 相关依赖见下）
                    NodeIdentityCard(node?.kind ?: "")
                    ConfirmedCard(node?.name ?: "", node?.kind ?: "", deps.size)

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

                    // NodeDetail 六问 · ⑥ 有没有待处理问题（④ 依据来源 / ⑤ 最近确认无字段，诚实省略）
                    SectionHeader("待处理问题")
                    PendingIssuesCard(
                        count = pendingIssueCount,
                        hasProposals = hasPendingProposals,
                        onClickProposals = { nav.navigate(Route.REVIEW) },
                        onClickDrift = { nav.navigate(Route.DRIFT) },
                    )
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
