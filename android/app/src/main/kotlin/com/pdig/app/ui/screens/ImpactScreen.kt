package com.pdig.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import com.pdig.app.ui.theme.PdigStatus
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
    ImpactTargetStatus.NEEDS_REVIEW -> "需要确认"
    ImpactTargetStatus.UNAFFECTED -> "暂无影响"
}

internal fun impactLevelLabel(level: ImpactLevel): String = when (level) {
    ImpactLevel.MUST_CHANGE -> "必须处理"
    ImpactLevel.BACKUP_PATH -> "有备用路径"
    ImpactLevel.DEGRADED -> "能力降级"
    ImpactLevel.NEEDS_REVIEW -> "需要确认"
    ImpactLevel.UNAFFECTED -> "暂无影响"
    ImpactLevel.TARGET_OPERATION -> "最后执行"
}

/** 每个影响级别下的稳定解释句（基于 ImpactLevel 映射，不暴露内部枚举名 —— spec §65）。 */
internal fun impactCaption(level: ImpactLevel): String = when (level) {
    ImpactLevel.MUST_CHANGE ->
        "这个对象只有这一条已确认的支付方式，换卡后可能无法扣款。依据是你确认过的依赖关系。"
    ImpactLevel.BACKUP_PATH ->
        "已确认存在其他可用的支付方式，影响有限，但冗余度下降。"
    ImpactLevel.DEGRADED ->
        "已确认存在其他可用的支付方式，能力会降级，但不会完全中断。"
    ImpactLevel.NEEDS_REVIEW ->
        "是否受影响还没有确认，需要人工核实；未确认的内容不会当成必须处理。"
    ImpactLevel.UNAFFECTED ->
        "未发现受影响的已确认关系。"
    ImpactLevel.TARGET_OPERATION ->
        "这是最后一步：以上事项处理完成后再执行停用/注销。"
}

/** 影响分组的视觉权重：must_change 最突出（danger），其余依次减弱。 */
private enum class ImpactTone { DANGER, WARNING, SUCCESS, NEUTRAL }

/**
 * Impact 结果页：四类分组（must_change 视觉最突出 / needs_review / 影响较小 / 暂无影响），
 * 不把不同严重度的对象平铺成同权重 checklist（UIUX_FREEZE §9 / spec §33）。
 * machine 未确认的内容最多显示在「需要确认」，不会被提升为必须处理。
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
                    tone = ImpactTone.DANGER,
                    strip = true,
                    emptyText = "没有必须处理的事项。",
                    caption = impactCaption(ImpactLevel.MUST_CHANGE),
                    items = mustChange.map { it.nodeName to it.reasonText },
                )
                ImpactGroup(
                    title = "需要确认（${needsReview.size}）",
                    tone = ImpactTone.WARNING,
                    emptyText = "没有需要人工核实的事项。",
                    caption = impactCaption(ImpactLevel.NEEDS_REVIEW),
                    items = needsReview.map { it.nodeName to it.reasonText },
                )
                ImpactGroup(
                    title = "影响较小 / 有备用路径（${backup.size}）",
                    tone = ImpactTone.SUCCESS,
                    emptyText = "没有影响较小的对象。",
                    caption = impactCaption(ImpactLevel.BACKUP_PATH),
                    items = backup.map { it.nodeName to it.reasonText },
                )
                ImpactGroup(
                    title = "暂无影响（${unaffected.size}）",
                    tone = ImpactTone.NEUTRAL,
                    emptyText = "没有其他对象。",
                    caption = null,
                    items = unaffected.map { it.name to "不使用这张卡支付" },
                )

                Button(
                    onClick = { nav.navigate(Route.SCENARIO_SETUP.replace("{templateId}", "replace_payment_card")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = PdigTokens.MinTouchTarget),
                ) { Text("生成变更计划") }
                if (mustChange.isEmpty()) {
                    Text(
                        "当前没有必须处理的事项，仍可生成计划查看步骤。",
                        style = PdigTokens.Caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/** 每个分组：icon/shape + 标题 + 颜色三通道；must_change 用 danger 软底条强化。 */
@Composable
private fun ImpactGroup(
    title: String,
    tone: ImpactTone,
    items: List<Pair<String, String>>,
    caption: String?,
    emptyText: String,
    strip: Boolean = false,
) {
    SectionHeader(title)
    if (items.isEmpty()) {
        if (emptyText.isNotBlank()) EmptyState(emptyText)
        return
    }
    val tint: Color
    val soft: Color
    when (tone) {
        ImpactTone.DANGER -> {
            tint = PdigStatus.blocked
            soft = PdigStatus.blocked.copy(alpha = 0.10f)
        }
        ImpactTone.WARNING -> {
            tint = PdigStatus.reviewRequired
            soft = PdigStatus.reviewRequired.copy(alpha = 0.10f)
        }
        ImpactTone.SUCCESS -> {
            tint = MaterialTheme.colorScheme.secondary
            soft = MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f)
        }
        ImpactTone.NEUTRAL -> {
            tint = MaterialTheme.colorScheme.onSurfaceVariant
            soft = Color.Transparent
        }
    }
    val rows: @Composable ColumnScope.() -> Unit = {
        items.forEach { (name, reason) ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PdigTokens.SpaceMd),
                verticalAlignment = Alignment.Top,
            ) {
                ImpactGroupIcon(tone = tone, tint = tint)
                Column(verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceXs)) {
                    Text(name, style = PdigTokens.BodyStrong)
                    caption?.let {
                        Text(it, style = PdigTokens.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(reason, style = PdigTokens.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    if (strip) {
        Surface(
            shape = RoundedCornerShape(PdigTokens.RadiusSm),
            color = soft,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                Modifier.padding(PdigTokens.SpaceMd),
                verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceMd),
            ) { rows() }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceMd)) { rows() }
    }
}

/** 分组图标/形状：danger = 实心「!」圆；warning = 三角；success = 勾；neutral = info。 */
@Composable
private fun ImpactGroupIcon(tone: ImpactTone, tint: Color) {
    when (tone) {
        ImpactTone.DANGER -> Box(
            Modifier.size(22.dp).background(PdigStatus.blocked, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("!", color = MaterialTheme.colorScheme.onError, style = PdigTokens.Label)
        }
        ImpactTone.WARNING -> Icon(
            Icons.Filled.Warning,
            contentDescription = "需要确认",
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
        ImpactTone.SUCCESS -> Icon(
            Icons.Filled.Check,
            contentDescription = "有备用路径",
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
        ImpactTone.NEUTRAL -> Icon(
            Icons.Filled.Info,
            contentDescription = "暂无影响",
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}
