package com.pdig.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pdig.app.ui.Route
import com.pdig.app.data.AppContainer
import com.pdig.app.data.DependencyRow
import com.pdig.app.data.NodeRow
import com.pdig.app.ui.components.EmptyState
import com.pdig.app.ui.components.LoadingState
import com.pdig.app.ui.components.PdigCard
import com.pdig.app.ui.components.PdigTopBar
import com.pdig.app.ui.components.SectionHeader
import com.pdig.app.ui.theme.PdigTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 基础设施页一次 IO 取回（节点 + 依赖，供 By Item / By Capability 两种浏览）。 */
private data class InfraSnapshot(val nodes: List<NodeRow>, val deps: List<DependencyRow>)

/**
 * 基础设施总览：搜索 + By Item / By Capability 分段切换（UIUX_FREEZE §7 / design-tokens §12）。
 * 切换文案「按对象 / 按能力」，不暴露内部枚举；能力分组用用户语义
 * 「身份与恢复 / 访问与认证 / 支付 / 设备 / 关键服务」。
 */
@Composable
fun InfrastructureScreen(nav: NavController) {
    val context = LocalContext.current
    val container = remember { AppContainer.get(context) }
    var nodes by remember { mutableStateOf<List<NodeRow>?>(null) }
    var deps by remember { mutableStateOf<List<DependencyRow>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var byCapability by remember { mutableStateOf(false) }

    // 数据库读一律在 IO 线程（主线程做 DB 会在冷启动触发 ANR，2026-09-16 修复）
    LaunchedEffect(Unit) {
        val loaded = withContext(Dispatchers.IO) { InfraSnapshot(container.nodes(), container.dependencies()) }
        nodes = loaded.nodes
        deps = loaded.deps
    }

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
                    CapabilitySegmentedToggle(byCapability, onSelect = { byCapability = it })
                    val filtered = (nodes ?: emptyList())
                        .filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) }
                    if (filtered.isEmpty()) {
                        EmptyState("没有找到匹配「${query.trim()}」的对象。")
                    } else {
                        val groups = if (byCapability) groupedNodesByCapability(filtered, deps) else groupedNodes(filtered)
                        groups.forEach { (label, items) ->
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

/** 分段切换（「按对象 / 按能力」，不暴露 enum；选中段 = indigo accent）。 */
@Composable
private fun CapabilitySegmentedToggle(byCapability: Boolean, onSelect: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PdigTokens.SpaceXs),
    ) {
        SegmentedOption("按对象", selected = !byCapability, onClick = { onSelect(false) })
        SegmentedOption("按能力", selected = byCapability, onClick = { onSelect(true) })
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.SegmentedOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .heightIn(min = PdigTokens.MinTouchTarget),
        shape = RoundedCornerShape(PdigTokens.RadiusSm),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Box(Modifier.padding(vertical = PdigTokens.SpaceSm), contentAlignment = Alignment.Center) {
            Text(
                label,
                style = PdigTokens.Label,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            )
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

/** 能力分组的用户语义顺序（固定；空组不显示）。 */
internal val CAPABILITY_GROUP_ORDER = listOf("身份与恢复", "访问与认证", "支付", "设备", "关键服务", "其他")

/**
 * 节点 → 能力分组的人话标签（不泄漏 Capability wire 值）。
 * 依据该节点已确认依赖边的能力；无边的节点按 kind 兜底归类。
 */
internal fun capabilityGroupLabel(node: NodeRow, deps: List<DependencyRow>): String {
    val caps = deps.filter { it.from == node.id || it.to == node.id }.map { it.capability }.toSet()
    return when {
        node.kind == "device" -> "设备"
        node.kind == "identity_anchor" -> "身份与恢复"
        node.kind == "payment_instrument" -> "支付"
        caps.contains("payment") -> "支付"
        caps.contains("access") || caps.contains("authentication") -> "访问与认证"
        caps.contains("recovery") || caps.contains("identity") -> "身份与恢复"
        caps.contains("communication") ||
            node.kind == "account" ||
            node.kind == "service" ||
            node.kind == "membership" -> "关键服务"
        else -> "其他"
    }
}

/** 按能力分组聚合节点；分组顺序固定，空组不显示。 */
internal fun groupedNodesByCapability(
    nodes: List<NodeRow>,
    deps: List<DependencyRow>,
): List<Pair<String, List<NodeRow>>> {
    val groups = nodes.groupBy { capabilityGroupLabel(it, deps) }
    return CAPABILITY_GROUP_ORDER.mapNotNull { label ->
        groups[label]?.takeIf { it.isNotEmpty() }?.let { label to it }
    }
}
