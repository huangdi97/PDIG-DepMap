package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.model.ChangeMigration
import com.pdig.uivnext.model.ChangeStage
import com.pdig.uivnext.model.MediaBreakpoint
import com.pdig.uivnext.model.UiVNextNumber
import androidx.compose.foundation.layout.defaultMinSize
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
    val new = UiVNextDemoFixture.numberById("num-cn-3")
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("更换手机号", color = PdigV2Colors.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        ProjectionSelector(projection) { app.changeProjection = it }

        // 投影语义横幅（按投影切换；after 显式 = Plan Projection）
        when (projection) {
            "current" -> InfoBanner(
                "当前状态：旧号码仍是登录主号，迁移尚未开始；所有服务保持现状。",
                PdigV2Colors.TextMuted,
                PdigV2Colors.Surface.copy(alpha = 0.6f),
            )
            "after" -> InfoBanner(
                "计划投影：以下为计划完成后的预期状态，不代表已经完成或验证；未完成服务仍显示「待处理」。",
                PdigV2Colors.TextPrimary,
                PdigV2Colors.Warning.copy(alpha = 0.12f),
            )
            else -> InfoBanner(
                "计划投影：以下步骤为当前执行计划；完成状态只在实际验证后标记。",
                PdigV2Colors.TextMuted,
                PdigV2Colors.Surface.copy(alpha = 0.6f),
            )
        }

        // 手机宽度不足 6×64dp 节点宽，允许横向滚动；大屏自然铺开。
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        ) {
            ContinuityRail(stages = projectionStages(projection))
        }

        SectionHeader("旧号码 → 关键服务 → 新号码")
        if (breakpoint == MediaBreakpoint.EXPANDED || breakpoint == MediaBreakpoint.MEDIUM) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OldNumberSurface(old, projection)
                Text("→", color = PdigV2Colors.TextMuted, fontSize = 16.sp)
                ServicesSurface(projection)
                Text("→", color = PdigV2Colors.TextMuted, fontSize = 16.sp)
                NewNumberSurface(new, projection)
            }
        } else {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OldNumberSurface(old, projection)
                Text("→", color = PdigV2Colors.TextMuted, fontSize = 16.sp)
                ServicesSurface(projection)
                Text("→", color = PdigV2Colors.TextMuted, fontSize = 16.sp)
                NewNumberSurface(new, projection)
            }
        }

        SectionHeader("阶段明细")
        stageDetail(projectionDetail(projection))

        SectionHeader("风险提示")
        Surface(color = PdigV2Colors.Warning.copy(alpha = 0.12f), shape = RoundedCornerShape(VRadius.Md), modifier = Modifier.fillMaxWidth()) {
            Text(
                "旧号码是 2 个账户的唯一恢复路径：迁移完成前不要停用；验证阶段未完成时「停用旧号码」必须保持禁用（Make-Before-Break）。",
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
            "after" to "计划完成（投影）",
        ).forEach { (key, label) ->
            val selected = projection == key
            Surface(
                modifier = Modifier.defaultMinSize(minHeight = VTouchTarget.Min).clickableLocal { onSelect(key) }.testTagLocal("pdig.change.projection.$key"),
                color = if (selected) PdigV2Colors.Primary.copy(alpha = 0.28f) else PdigV2Colors.SurfaceRaised,
                shape = RoundedCornerShape(VRadius.Sm),
                border = BorderStroke(1.dp, if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.BorderSubtle),
            ) {
                Text(
                    label,
                    Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                )
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
                6 -> s.copy(status = "plan", blockReason = "新号码验证通过后按计划停用（make-before-break）")
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
private fun OldNumberSurface(old: UiVNextNumber?, projection: String) {
    if (old == null) return
    val ghost = projection == "after"
    Surface(
        Modifier.fillMaxWidth(),
        color = if (ghost) PdigV2Colors.Surface.copy(alpha = 0.5f) else PdigV2Colors.SurfaceRaised,
        shape = RoundedCornerShape(VRadius.Lg),
        border = if (ghost) BorderStroke(1.dp, PdigV2Colors.BorderSubtle) else null,
    ) {
        Column(Modifier.padding(14.dp).alpha(if (ghost) 0.55f else 1f)) {
            LabelChip(if (ghost) "旧号码（已停用·计划）" else "旧号码")
            Spacer(Modifier.height(8.dp))
            Text(old.maskedNumber, color = PdigV2Colors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("${old.carrier} · 主号", color = PdigV2Colors.TextMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ServicesSurface(projection: String) {
    Surface(
        Modifier.fillMaxWidth(),
        color = PdigV2Colors.Surface.copy(alpha = 0.7f),
        shape = RoundedCornerShape(VRadius.Lg),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Column(Modifier.padding(14.dp)) {
            LabelChip("关键服务与账户")
            Spacer(Modifier.height(8.dp))
            projectionMigrations(projection).forEach { m ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(m.service, color = PdigV2Colors.TextSecondary, fontSize = 13.sp)
                    StatusBadge(m.status)
                }
            }
            if (projection == "after") {
                Text(
                    "未完成的服务保持「待处理」：计划不代表已验证迁移。",
                    color = PdigV2Colors.TextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun NewNumberSurface(new: UiVNextNumber?, projection: String) {
    if (new == null) return
    val isAfter = projection == "after"
    Surface(
        Modifier.fillMaxWidth(),
        color = PdigV2Colors.Primary.copy(alpha = if (isAfter) 0.22f else 0.14f),
        shape = RoundedCornerShape(VRadius.Lg),
        border = BorderStroke(1.dp, if (isAfter) PdigV2Colors.PrimaryBright else PdigV2Colors.PrimaryBright.copy(alpha = 0.6f)),
    ) {
        Column(Modifier.padding(14.dp)) {
            LabelChip(if (isAfter) "新主号（计划）" else "新号码", highlight = true)
            Spacer(Modifier.height(8.dp))
            Text(new.maskedNumber, color = PdigV2Colors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("${new.carrier} · 副号", color = PdigV2Colors.TextMuted, fontSize = 12.sp)
        }
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
        "6 停用旧号码" to "未开始：验证通过前不允许停用（make-before-break）。",
    )
    "after" -> listOf(
        "1 影响分析" to "已确认需要迁移的服务：微信支付、支付宝、招商银行网银、腾讯视频；2 个账户以旧号码为登录验证。",
        "2 建立新号码" to "计划：新号码 +86 139****2204 已加入（保号副号）。",
        "3 验证新号码" to "计划：验证通过（投影）。",
        "4 迁移关键账户" to "计划：逐个迁移绑定（投影）。",
        "5 检查恢复路径" to "计划：确保每个账户存在非旧号码的恢复方式（投影）。",
        "6 停用旧号码" to "计划：验证通过后停用旧号码（投影；不代表已执行）。",
    )
    else -> listOf(
        "1 影响分析" to "已确认需要迁移的服务：微信支付、支付宝、招商银行网银、腾讯视频；2 个账户以旧号码为登录验证。",
        "2 建立新号码" to "新号码 +86 139****2204 已加入（保号副号）。",
        "3 验证新号码" to "等待接收验证码并确认（正在验证）。",
        "4 迁移关键账户" to "待验证通过后逐个迁移绑定。",
        "5 检查恢复路径" to "确保每个账户存在非旧号码的恢复方式。",
        "6 停用旧号码" to "阻止执行：新手机号验证通过后才能停用旧手机号（make-before-break）。",
    )
}
