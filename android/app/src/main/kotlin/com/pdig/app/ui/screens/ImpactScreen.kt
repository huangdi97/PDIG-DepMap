package com.pdig.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.pdig.app.data.AppContainer
import com.pdig.app.data.NodeRow
import com.pdig.app.ui.Route
import com.pdig.app.ui.components.EmptyState
import com.pdig.app.ui.components.LoadingState
import com.pdig.app.ui.components.PdigCard
import com.pdig.app.ui.components.PdigScrollingPage
import com.pdig.app.ui.components.PdigTopBar
import com.pdig.app.ui.components.SectionHeader
import com.pdig.app.ui.theme.PdigTokens
import com.pdig.core.generated.ImpactLevel
import com.pdig.core.generated.ImpactTargetStatus
import com.pdig.core.impact.ImpactResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
// ---------------------------------------------------------------------------
// 影响面与人话标签（spec §65：不显示内部术语；状态不得只靠颜色传达）
// ---------------------------------------------------------------------------

internal fun impactStatusLabel(status: ImpactTargetStatus): String = when (status) {
    ImpactTargetStatus.MUST_CHANGE -> "必须处理"
    ImpactTargetStatus.BACKUP_PATH -> "有备用路径"
    ImpactTargetStatus.DEGRADED -> "能力降级"
    ImpactTargetStatus.NEEDS_REVIEW -> "建议检查"
    ImpactTargetStatus.UNAFFECTED -> "未受影响"
}

internal fun impactLevelLabel(level: ImpactLevel): String = when (level) {
    ImpactLevel.MUST_CHANGE -> "必须处理"
    ImpactLevel.BACKUP_PATH -> "备用路径"
    ImpactLevel.DEGRADED -> "能力降级"
    ImpactLevel.NEEDS_REVIEW -> "建议检查"
    ImpactLevel.UNAFFECTED -> "未受影响"
    ImpactLevel.TARGET_OPERATION -> "最后执行"
}

/** 每个影响级别下的稳定解释句（基于 ImpactLevel 映射，不暴露内部枚举名 —— spec §65）。 */
internal fun impactCaption(level: ImpactLevel): String = when (level) {
    ImpactLevel.MUST_CHANGE ->
        "这个服务只有这一个已确认的支付方式，换卡后可能无法扣款。依据是你确认过的依赖关系。"
    ImpactLevel.BACKUP_PATH ->
        "已确认存在其他可用的支付方式，影响有限，但冗余度下降。"
    ImpactLevel.DEGRADED ->
        "已确认存在其他可用的支付方式，能力会降级，但不会完全中断。"
    ImpactLevel.NEEDS_REVIEW ->
        "还没有确认是否受影响，需要人工核实；未经确认不会当成必须处理。"
    ImpactLevel.UNAFFECTED ->
        "未发现受影响的已确认关系。"
    ImpactLevel.TARGET_OPERATION ->
        "这是最后一步：以上事项处理完成后再执行停用/注销。"
}

/**
 * Impact 结果页：直接展示 core.impact 的输出，UI 不自行推导。
 * machine 未确认的内容在这里最多显示为「建议检查」，不会被提升为必须处理。
 */
@Composable
fun ImpactScreen(nav: NavController, nodeId: String) {
    val context = LocalContext.current
    val container = remember { AppContainer.get(context) }
    var result by remember { mutableStateOf<ImpactResult?>(null) }
    var allNodes by remember { mutableStateOf<List<NodeRow>>(emptyList()) }

    LaunchedEffect(nodeId) {
        result = withContext(Dispatchers.IO) { container.impactFor(nodeId) }
        allNodes = withContext(Dispatchers.IO) { container.nodes() }
    }

    Scaffold(topBar = { PdigTopBar("影响范围", onBack = { nav.popBackStack() }) }) { pad ->
        PdigScrollingPage(
            modifier = Modifier
                .padding(pad)
                .padding(PdigTokens.SpaceLg),
            verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceSm),
        ) {
            val impact = result
            if (impact == null) {
                LoadingState()
            } else {
                val name = allNodes.firstOrNull { it.id == nodeId }?.name ?: nodeId
                val mustChange = impact.targets.filter { it.status == ImpactTargetStatus.MUST_CHANGE }
                val needsReview = impact.targets.filter { it.status == ImpactTargetStatus.NEEDS_REVIEW }
                val backup = impact.targets.filter {
                    it.status == ImpactTargetStatus.BACKUP_PATH || it.status == ImpactTargetStatus.DEGRADED
                }
                val touchedIds = impact.targets.map { it.nodeId }.toSet() + setOf(nodeId)
                val unaffected = allNodes.filter { it.id !in touchedIds }

                SectionHeader("停用「$name」会怎样")
                Text(
                    "下面按处理方式分组列出会使用这张卡的对象。未确认的关系不会当成已确认。",
                    style = PdigTokens.Body,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                ImpactGroup(
                    title = "必须处理（${mustChange.size}）",
                    emptyText = "没有必须处理的事项。",
                    items = mustChange.map { it.nodeName to it.reasonText },
                    caption = impactCaption(ImpactLevel.MUST_CHANGE),
                )
                ImpactGroup(
                    title = "建议检查（${needsReview.size}）",
                    emptyText = "没有需要人工核实的事项。",
                    items = needsReview.map { it.nodeName to it.reasonText },
                    caption = impactCaption(ImpactLevel.NEEDS_REVIEW),
                )
                if (backup.isNotEmpty()) {
                    ImpactGroup(
                        title = "有备用路径 / 能力降级（${backup.size}）",
                        emptyText = "",
                        items = backup.map { it.nodeName to it.reasonText },
                        caption = impactCaption(ImpactLevel.BACKUP_PATH),
                    )
                }
                ImpactGroup(
                    title = "未受影响（${unaffected.size}）",
                    emptyText = "没有其他对象。",
                    items = unaffected.map { it.name to "不使用这张卡支付" },
                )

                if (impact.checklist.isNotEmpty()) {
                    SectionHeader("处理顺序")
                    impact.checklist.forEach { item ->
                        PdigCard {
                            Column(verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceXs)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(item.title, style = PdigTokens.BodyStrong, modifier = Modifier.weight(1f))
                                    Text(
                                        impactLevelLabel(item.level),
                                        style = PdigTokens.Label,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    impactCaption(item.level),
                                    style = PdigTokens.Caption,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    item.detail,
                                    style = PdigTokens.Caption,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = { nav.navigate(Route.SCENARIO_SETUP.replace("{templateId}", "replace_payment_card")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = PdigTokens.MinTouchTarget),
                ) { Text("为这次更换创建变更计划") }
            }
        }
    }
}

@Composable
private fun ImpactGroup(title: String, emptyText: String, items: List<Pair<String, String>>, caption: String? = null) {
    SectionHeader(title)
    if (items.isEmpty()) {
        if (emptyText.isNotBlank()) EmptyState(emptyText)
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceXs)) {
            items.forEach { (name, reason) ->
                PdigCard {
                    Column(verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceXs)) {
                        Text(name, style = PdigTokens.BodyStrong)
                        // 稳定解释句：说明这一类的判定依据（中性色，不渲染成错误）。
                        caption?.let {
                            Text(it, style = PdigTokens.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(reason, style = PdigTokens.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
