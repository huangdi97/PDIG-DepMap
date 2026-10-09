package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.model.ChangeMigration
import com.pdig.uivnext.model.ChangeStage
import com.pdig.uivnext.model.MediaBreakpoint
import com.pdig.uivnext.model.UiVNextNumber
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VTouchTarget
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.ContinuityRail
import com.pdig.uivnext.ui.components.LabelChip
import com.pdig.uivnext.ui.components.SectionHeader
import com.pdig.uivnext.ui.components.StatusBadge

/**
 * Change Phone（flagship）：Current / Transition / After 三投影（任务书 §19-21）。
 * - Current    旧号码仍为主号；迁移未开始（阶段 1 完成）。
 * - Transition 迁移进行中（验证新号码 / 阻塞停用旧号码；make-before-break 闸门）。
 * - After      **Plan Projection**：计划完成后的预期状态，不代表已发生/已验证；
 *              旧号码 ghost，新号码主号，未完成服务仍显示「待处理」。
 * 服务节点重于连线；不做 spaghetti lines。
 */
@Composable
fun ChangePhoneScreen(app: VAppState, breakpoint: MediaBreakpoint) {
    val projection = app.changeProjection
    val old = UiVNextDemoFixture.numberById("num-cn-1")
    val new = UiVNextDemoFixture.numberById("num-cn-4")
    val impact = changeImpactSummary(projection)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(pagePadding(breakpoint)),
        verticalArrangement = Arrangement.spacedBy(pageSectionGap(breakpoint)),
    ) {
        if (breakpoint != MediaBreakpoint.COMPACT) {
            Text("更换手机号", color = PdigV2Colors.TextPrimary, fontSize = pageTitleSize(breakpoint), fontWeight = FontWeight.Bold)
        }
        ProjectionSelector(projection) { app.changeProjection = it }

        // Compact / Medium 保持单 pane：6 步 mini progress 首屏完整可见；
        // Expanded 才使用完整 ContinuityRail。
        if (breakpoint != MediaBreakpoint.EXPANDED) {
            CompactStepper(stages = projectionStages(projection))
        } else {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            ) {
                ContinuityRail(stages = projectionStages(projection))
            }
        }

        ChangeImpactSummaryStrip(impact)

        if (breakpoint == MediaBreakpoint.COMPACT) {
            Text(
                "影响分析 · 关键服务",
                color = PdigV2Colors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            CompactContinuityOrbit(old, projection)
        }
        // The projection truth remains visible, but cannot bury the spatial scene on phone.
        // 投影语义横幅（按投影切换；after 显式 = Plan Projection）
        when (projection) {
            "current" -> InfoBanner(
                "当前：旧号码仍是登录主号，迁移尚未开始；服务关系保持现状。",
                PdigV2Colors.TextMuted,
                PdigV2Colors.Surface.copy(alpha = 0.6f),
            )
            "after" -> InfoBanner(
                "完成后预览：展示计划执行后的预期状态，不代表已经完成或验证；未完成项仍标记「待处理」。",
                PdigV2Colors.TextPrimary,
                PdigV2Colors.Warning.copy(alpha = 0.12f),
            )
            else -> InfoBanner(
                "执行计划：以下是当前迁移步骤；只有实际验证完成的步骤才标记为完成。",
                PdigV2Colors.TextMuted,
                PdigV2Colors.Surface.copy(alpha = 0.6f),
            )
        }

        if (breakpoint != MediaBreakpoint.COMPACT) {
            SectionHeader("旧号码 → 关键服务 → 新号码")
            if (breakpoint == MediaBreakpoint.EXPANDED) {
                ExpandedContinuityScene(old = old, new = new, projection = projection)
            } else {
                CompactContinuityScene(old = old, new = new, projection = projection)
            }
        }

        SectionHeader("阶段明细")
        stageDetail(projectionDetail(projection))

        SectionHeader("风险提示")
        Surface(color = PdigV2Colors.Warning.copy(alpha = 0.12f), shape = RoundedCornerShape(VRadius.Md), modifier = Modifier.fillMaxWidth()) {
            Text(
                if (old.uniqueRecoveryPath == true)
                    "已确认旧号码是唯一恢复路径：新号码完成验证并建立独立替代恢复路径前，不要停用旧号码。"
                else if (old.recoveryOnly)
                    "旧号码承担已记录恢复用途，但唯一性仍未知：停用前必须逐项核对并验证替代恢复路径。"
                else
                    "旧号码的恢复关系仍有未知项：停用前先核对已记录依赖，并验证新的恢复路径。",
                Modifier.padding(14.dp),
                color = PdigV2Colors.TextPrimary,
                fontSize = 13.sp,
            )
        }
    }
}

/** 投影切换（current / transition / after；触控目标 ≥48dp）。 */
@Composable
private fun ProjectionSelector(projection: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            "current" to "当前",
            "transition" to "迁移中",
            "after" to "完成后（计划）",
        ).forEach { (key, label) ->
            val selected = projection == key
            Box(
                modifier = Modifier
                    .defaultMinSize(minHeight = VTouchTarget.Min)
                    .clickableLocal { onSelect(key) }
                    .testTagLocal("pdig.change.projection.$key"),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    color = if (selected) PdigV2Colors.PrimarySoft else PdigV2Colors.SurfaceRaised,
                    shape = RoundedCornerShape(VRadius.Sm),
                    border = BorderStroke(
                        1.dp,
                        if (selected) PdigV2Colors.PrimaryBright.copy(alpha = 0.72f) else PdigV2Colors.BorderSubtle,
                    ),
                ) {
                    Text(
                        label,
                        Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        color = if (selected) PdigV2Colors.PrimaryText else PdigV2Colors.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoBanner(text: String, fg: Color, bg: Color) {
    Surface(color = bg, shape = RoundedCornerShape(VRadius.Md), modifier = Modifier.fillMaxWidth()) {
        Text(text, Modifier.padding(14.dp), color = fg, fontSize = 12.sp)
    }
}

/** 三投影的阶段状态（current / transition=fixture / after=plan）。 */
internal fun projectionStages(projection: String): List<ChangeStage> {
    val base = UiVNextDemoFixture.changeStages
    return when (projection) {
        "current" -> base.map { s ->
            when (s.stage) {
                1 -> s.copy(status = "completed")
                6 -> s.copy(status = "not_started", blockReason = null)
                else -> s.copy(status = "not_started", blockReason = null)
            }
        }
        "after" -> base.map { s ->
            when (s.stage) {
                1, 2, 3, 4 -> s.copy(status = "completed")
                5 -> s.copy(status = "plan", blockReason = null)
                6 -> s.copy(status = "plan", blockReason = "新号码验证通过并确认恢复路径后按计划停用")
                else -> s.copy(status = "not_started")
            }
        }
        else -> base
    }
}

/** 三投影的服务迁移状态（after 中未安排的服务保持「待处理」，绝不伪装完成）。 */
internal fun projectionMigrations(projection: String): List<ChangeMigration> {
    val base = UiVNextDemoFixture.changeMigrations
    return when (projection) {
        "current" -> base.map { it.copy(status = "not_started") }
        "after" -> base.mapIndexed { index, m ->
            if (index == 2) m.copy(status = "unresolved") else m.copy(status = "plan")
        }
        else -> base
    }
}


@Composable
private fun stageDetail(items: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (title, body) ->
            Surface(
                color = PdigV2Colors.SurfaceRaised,
                shape = RoundedCornerShape(VRadius.Md),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(title, color = PdigV2Colors.TextPrimary, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    Text(body, color = PdigV2Colors.TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

/** 阶段明细文案（随投影切换；after 全部使用计划措辞）。 */
private fun projectionDetail(projection: String): List<Pair<String, String>> = when (projection) {
    "current" -> listOf(
        "1 影响分析" to "已确认需要迁移的服务：微信支付、支付宝、招商银行网银、腾讯视频；2 个账户以旧号码为登录验证。",
        "2 建立新号码" to "尚未开始（当前投影）。",
        "3 验证新号码" to "尚未开始（当前投影）。",
        "4 迁移关键账户" to "尚未开始（当前投影）。",
        "5 检查恢复路径" to "尚未开始（当前投影）。",
        "6 停用旧号码" to "未开始：新号码验证完成并确认恢复路径前，不允许停用旧号码。",
    )
    "after" -> listOf(
        "1 影响分析" to "已确认需要迁移的服务：微信支付、支付宝、招商银行网银、腾讯视频；2 个账户以旧号码为登录验证。",
        "2 建立新号码" to "计划：新号码 +86 139****6421 已加入（迁移目标）。",
        "3 验证新号码" to "计划：验证通过（投影）。",
        "4 迁移关键账户" to "计划：逐个迁移绑定（投影）。",
        "5 检查恢复路径" to "计划：确保每个账户存在非旧号码的恢复方式（投影）。",
        "6 停用旧号码" to "计划：验证通过后停用旧号码（投影；不代表已执行）。",
    )
    else -> listOf(
        "1 影响分析" to "已确认需要迁移的服务：微信支付、支付宝、招商银行网银、腾讯视频；2 个账户以旧号码为登录验证。",
        "2 建立新号码" to "新号码 +86 139****6421 已加入（迁移目标）。",
        "3 验证新号码" to "等待接收验证码并确认（正在验证）。",
        "4 迁移关键账户" to "待验证通过后逐个迁移绑定。",
        "5 检查恢复路径" to "确保每个账户存在非旧号码的恢复方式。",
        "6 停用旧号码" to "暂不可执行：新号码完成验证并确认恢复路径后，才能停用旧号码。",
    )
}

internal data class ChangeImpactSummary(
    val linkedServices: Int,
    val needsReview: Int,
    val blockerCount: Int,
)

internal fun changeImpactSummary(projection: String): ChangeImpactSummary {
    val migrations = projectionMigrations(projection)
    val stages = projectionStages(projection)
    return ChangeImpactSummary(
        linkedServices = migrations.size,
        needsReview = migrations.count { it.status != "completed" },
        blockerCount = stages.count { it.status == "blocked" } +
            migrations.count { it.status == "unresolved" },
    )
}

@Composable
private fun ChangeImpactSummaryStrip(summary: ChangeImpactSummary) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTagLocal("pdig.r18.change.impact-summary"),
        color = PdigV2Colors.SurfaceRaised,
        shape = RoundedCornerShape(VRadius.Md),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf(
                Triple(summary.linkedServices, "已记录关联", PdigV2Colors.PrimaryBright),
                Triple(summary.needsReview, "需要核对", PdigV2Colors.Warning),
                Triple(summary.blockerCount, "阻断 / 待解决", PdigV2Colors.Critical),
            ).forEach { (value, label, tint) ->
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(value.toString(), color = tint, fontSize = 17.sp,
                        fontWeight = FontWeight.Bold)
                    Text(label, color = PdigV2Colors.TextMuted, fontSize = 9.sp,
                        textAlign = TextAlign.Center, maxLines = 2)
                }
            }
        }
    }
}
