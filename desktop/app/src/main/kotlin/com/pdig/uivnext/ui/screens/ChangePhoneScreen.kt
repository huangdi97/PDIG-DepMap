package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.model.ChangeMigration
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VSpacing
import com.pdig.uivnext.theme.VType
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.ContinuityRail
import com.pdig.uivnext.ui.components.LabelChip
import com.pdig.uivnext.ui.components.SectionHeader

/**
 * Change Phone（Review §16/§17）：从流程后台升级为空间迁移图。
 *  - 顶部 6-stage progress（ContinuityRail 保留）；
 *  - 中央：OLD identity node → 迁移关系通道（状态着色 + icon/label）→ NEW identity node；
 *  - 状态：migrated=green / waiting=amber / blocked=red / not-started=muted / manual=neutral；
 *    颜色永远伴随 icon + label（三通道）；
 *  - 下方两组列表降级为 detail inspector（非主视觉）。
 */
@Composable
fun ChangePhoneScreen(app: VAppState) {
    val old = UiVNextDemoFixture.numberById("num-cn-1")
    val new = UiVNextDemoFixture.numberById("num-cn-3")
    val stages = UiVNextDemoFixture.changeStages
    val done = stages.count { it.status == "completed" }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(VSpacing.Xxl),
        verticalArrangement = Arrangement.spacedBy(VSpacing.Xxl),
    ) {
        PageHeader(
            title = "更换手机号",
            subtitle = "让旧号码承担的，由新号码接管 · 验证通过前旧号码不能停用（make-before-break）",
            trailing = {
                Surface(color = PdigV2Colors.PrimarySoft, shape = RoundedCornerShape(VRadius.Md)) {
                    Text(
                        "进度 $done / ${stages.size}",
                        Modifier.padding(horizontal = VSpacing.Lg, vertical = VSpacing.Sm),
                        color = PdigV2Colors.PrimaryBright,
                        style = VType.Label,
                    )
                }
            },
        )

        // 顶部 6-stage 进度（ContinuityRail）
        ContinuityRail(
            stages = stages,
            modifier = Modifier.testTagLocal(VTestIds.CHANGE_PROGRESS),
        )

        // 三态投影：当前 / 迁移中 / 完成后（After = Plan Projection，不冒充 Confirmed Reality）
        ContinuityProjectionSelector()
        // 中央空间迁移图：OLD node → 迁移关系通道 → NEW node
        com.pdig.uivnext.ui.components.LocalGlow(
            color = PdigV2Colors.PrimaryBright,
            alpha = 0.06f,
            radiusFraction = 0.45f,
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VSpacing.Lg),
                verticalAlignment = Alignment.Top,
            ) {
                if (old != null) OldNumberNode(old)
                MigrationLanes(UiVNextDemoFixture.changeMigrations, Modifier.weight(1f))
                if (new != null) NewNumberNode(new)
            }
        }

        // 下方 = detail inspector（非主视觉；保持信息完整）
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(VSpacing.Xxl)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(VSpacing.Sm)) {
                SectionHeader("仍依赖旧号码 · 详情")
                val stillOnOld = UiVNextDemoFixture.changeMigrations.filter { it.status != "migrated" }
                if (stillOnOld.isEmpty()) {
                    Text("暂无 — 旧号码已完成接管", color = PdigV2Colors.TextMuted, style = VType.Secondary)
                } else {
                    stillOnOld.forEach { m -> MigrationRow(m.service, m.status, "仍以旧号码为验证/扣款路径") }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(VSpacing.Sm)) {
                SectionHeader("等待验证 / 已迁移 · 详情")
                val waiting = UiVNextDemoFixture.changeMigrations.filter { it.status == "waiting" }
                val migrated = UiVNextDemoFixture.changeMigrations.filter { it.status == "migrated" }
                if (migrated.isEmpty() && waiting.isEmpty()) {
                    Text("暂无 — 验证新号码后迁移将在此列出", color = PdigV2Colors.TextMuted, style = VType.Secondary)
                } else {
                    waiting.forEach { m -> MigrationRow(m.service, m.status, "等待验证新号码后迁移") }
                    migrated.forEach { m -> MigrationRow(m.service, m.status, "已由新号码接管") }
                }
            }
        }

        SectionHeader("风险提示")
        Surface(color = PdigV2Colors.Warning.copy(alpha = 0.12f), shape = RoundedCornerShape(VRadius.Md), modifier = Modifier.fillMaxWidth()) {
            Text(
                "旧号码是 2 个账户的唯一恢复路径：迁移完成前不要停用；验证阶段未完成时「停用旧号码」必须保持禁用。",
                Modifier.padding(VSpacing.Lg),
                color = PdigV2Colors.TextPrimary,
                style = VType.Secondary,
            )
        }
    }
}

/** 三态投影选择器（当前 / 迁移中 / 完成后；After 标注 Plan Projection）。 */
@Composable
private fun ContinuityProjectionSelector() {
    val projections = listOf("当前", "迁移中", "完成后")
    Row(horizontalArrangement = Arrangement.spacedBy(VSpacing.Sm), verticalAlignment = Alignment.CenterVertically) {
        Text("投影", color = PdigV2Colors.TextMuted, style = VType.Label)
        projections.forEach { label ->
            Surface(
                color = if (label == "迁移中") PdigV2Colors.PrimaryBright.copy(alpha = 0.14f) else PdigV2Colors.SurfaceGlass,
                shape = RoundedCornerShape(VRadius.Sm),
                border = BorderStroke(1.dp, if (label == "迁移中") PdigV2Colors.PrimaryBright.copy(alpha = 0.3f) else PdigV2Colors.BorderSubtle),
            ) {
                Text(
                    label + if (label == "完成后") " · Plan Projection" else "",
                    Modifier.padding(horizontal = VSpacing.Md, vertical = 4.dp),
                    color = if (label == "迁移中") PdigV2Colors.PrimaryBright else PdigV2Colors.TextSecondary,
                    style = VType.Label,
                )
            }
        }
    }
}

/** OLD 号码身份节点。 */
@Composable
private fun OldNumberNode(old: com.pdig.uivnext.model.UiVNextNumber) {
    Surface(
        Modifier.width(300.dp),
        color = PdigV2Colors.Surface.copy(alpha = 0.9f),
        shape = RoundedCornerShape(VRadius.Xl),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Column(Modifier.padding(VSpacing.Xl), verticalArrangement = Arrangement.spacedBy(VSpacing.Sm)) {
            LabelChip("旧号码")
            Text(old.maskedNumber, style = VType.SectionTitle, color = PdigV2Colors.TextPrimary)
            Text("${old.carrier} · 主号", color = PdigV2Colors.TextMuted, style = VType.Meta)
            Text("承担：银行验证 / 注册 / 2FA（${old.usages.size} 类用途）", color = PdigV2Colors.TextSecondary, style = VType.Secondary)
        }
    }
}

/** NEW 号码身份节点（待验证接管）。 */
@Composable
private fun NewNumberNode(new: com.pdig.uivnext.model.UiVNextNumber) {
    Surface(
        Modifier.width(300.dp),
        color = PdigV2Colors.Primary.copy(alpha = 0.14f),
        shape = RoundedCornerShape(VRadius.Xl),
        border = BorderStroke(1.dp, PdigV2Colors.PrimaryBright),
    ) {
        Column(Modifier.padding(VSpacing.Xl), verticalArrangement = Arrangement.spacedBy(VSpacing.Sm)) {
            LabelChip("新号码", highlight = true)
            Text(new.maskedNumber, style = VType.SectionTitle, color = PdigV2Colors.TextPrimary)
            Text("${new.carrier} · 副号", color = PdigV2Colors.TextMuted, style = VType.Meta)
            Text("待验证通过后接管关键账户绑定", color = PdigV2Colors.Warning, style = VType.Secondary)
        }
    }
}

/** 迁移关系通道：每个服务一条状态线（OLD ──状态──▶ NEW）。 */
@Composable
private fun MigrationLanes(items: List<ChangeMigration>, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("迁移关系", style = VType.SectionTitle, color = PdigV2Colors.TextPrimary)
        items.forEach { m ->
            val color = migrationStatusColor(m.status)
            val icon = migrationStatusIcon(m.status)
            Row(
                Modifier.fillMaxWidth().height(46.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 服务名（左）
                Text(
                    m.service,
                    Modifier.width(120.dp),
                    color = PdigV2Colors.TextPrimary,
                    style = VType.Label,
                    maxLines = 1,
                )
                // 状态关系线：旧侧圆点 ── 状态色线 ── 新侧箭头
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp),
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(3.dp)
                        .background(color.copy(alpha = 0.55f), RoundedCornerShape(2.dp)),
                )
                Icon(
                    Icons.Filled.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(10.dp),
                )
                Spacer(Modifier.width(6.dp))
                Surface(color = color.copy(alpha = 0.14f), shape = RoundedCornerShape(VRadius.Sm)) {
                    Text(
                        migrationStatusLabel(m.status),
                        Modifier.padding(horizontal = VSpacing.Sm, vertical = 3.dp),
                        style = VType.StatusLabel,
                        color = color,
                    )
                }
            }
        }
        Text(
            "migrated 已接管 · waiting 待验证 · manual 需手动 · blocked 不可停 · not-started 未开始",
            style = VType.Meta,
            color = PdigV2Colors.TextMuted,
        )
    }
}

private fun migrationStatusColor(status: String): Color = when (status) {
    "migrated" -> PdigV2Colors.Positive
    "waiting" -> PdigV2Colors.Warning
    "blocked" -> PdigV2Colors.Critical
    "manual" -> PdigV2Colors.Unknown
    else -> PdigV2Colors.TextMuted
}

private fun migrationStatusIcon(status: String): ImageVector = when (status) {
    "migrated" -> Icons.Filled.CheckCircle
    "waiting" -> Icons.Filled.HourglassEmpty
    "blocked" -> Icons.Filled.Error
    "manual" -> Icons.Filled.Handyman
    else -> Icons.Filled.RadioButtonUnchecked
}

private fun migrationStatusLabel(status: String): String = when (status) {
    "migrated" -> "已迁移"
    "waiting" -> "等待中"
    "blocked" -> "阻止"
    "manual" -> "手动"
    "not_started" -> "未开始"
    else -> status
}

@Composable
private fun MigrationRow(name: String, status: String, hint: String) {
    Surface(
        color = PdigV2Colors.Surface.copy(alpha = 0.8f),
        shape = RoundedCornerShape(VRadius.Md),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = VSpacing.Lg, vertical = VSpacing.Md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(name, color = PdigV2Colors.TextPrimary, style = VType.Label)
                Text(hint, color = PdigV2Colors.TextMuted, style = VType.Meta)
            }
            Spacer(Modifier.width(VSpacing.Md))
            Surface(color = migrationStatusColor(status).copy(alpha = 0.14f), shape = RoundedCornerShape(VRadius.Sm)) {
                Text(
                    migrationStatusLabel(status),
                    Modifier.padding(horizontal = VSpacing.Sm, vertical = 3.dp),
                    style = VType.StatusLabel,
                    color = migrationStatusColor(status),
                )
            }
        }
    }
}
