package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.model.ChangeStage
import com.pdig.uivnext.model.UiVNextNumber
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.ui.components.LabelChip
import com.pdig.uivnext.ui.components.NumberIdentityThumbnail
import com.pdig.uivnext.ui.components.StatusBadge

/**
 * Continuity Scene（brief §10/§11/§12）—— Change Phone 的 adaptive 场景组件。
 * - [ExpandedContinuityScene]：Tablet-native OLD≈0.24 / SERVICES≈0.42 / NEW≈0.24 三列，
 *   主角顺序 OLD → SERVICE NODES → NEW 首屏可见（不再把 fillMaxWidth 三卡塞进 Row 造成死空白）；
 * - [CompactStepper]：COMPACT 6 步 mini progress（全部首屏可见 + 当前步骤），不被横向 clip；
 * - Old/New/Services 卡面带 tag（TabletContinuityThreeColumnsVisible 读取真实 bounds）。
 */

/** Tablet / MEDIUM 三列连续场景（投影切换 current/transition/after 复用）。 */
@Composable
internal fun ExpandedContinuityScene(old: UiVNextNumber?, new: UiVNextNumber?, projection: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OldNumberSurface(old, projection, Modifier.weight(0.24f))
        ContinuityArrow()
        ServicesSurface(projection, Modifier.weight(0.42f))
        ContinuityArrow()
        NewNumberSurface(new, projection, Modifier.weight(0.24f))
    }
}


/**
 * Phone continuity scene：把旧号码与新号码放进同一身份迁移舞台，再展示关键服务。
 * 视觉层级来自 Human-selected light reference；事实状态仍由 projection 决定。
 */
@Composable
internal fun CompactContinuityScene(old: UiVNextNumber?, new: UiVNextNumber?, projection: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PdigV2Colors.PrimarySoft.copy(alpha = 0.46f),
        shape = RoundedCornerShape(VRadius.Xl),
        border = BorderStroke(1.dp, PdigV2Colors.Primary.copy(alpha = 0.22f)),
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                old?.let {
                    CompactChangeIdentity(
                        number = it,
                        label = if (projection == "after") "旧号码 · 计划停用" else "旧号码 · 当前",
                        dimmed = projection == "after",
                        modifier = Modifier
                            .weight(1f)
                            .testTagLocal(VTestIds.CHANGE_OLD),
                    )
                }
                Text("→", color = PdigV2Colors.PrimaryBright, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                new?.let {
                    CompactChangeIdentity(
                        number = it,
                        label = if (projection == "after") "新主号 · 计划" else "新号码 · 目标",
                        dimmed = projection == "current",
                        modifier = Modifier
                            .weight(1f)
                            .testTagLocal(VTestIds.CHANGE_NEW),
                    )
                }
            }
            ServicesSurface(projection)
        }
    }
}

@Composable
private fun CompactChangeIdentity(
    number: UiVNextNumber,
    label: String,
    dimmed: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Lg),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Column(
            Modifier
                .alpha(if (dimmed) 0.55f else 1f)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            NumberIdentityThumbnail(
                number = number,
                privacyMask = false,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(label, color = PdigV2Colors.TextMuted, fontSize = 10.sp)
            Text(number.maskedNumber, color = PdigV2Colors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ContinuityArrow() {
    Text(
        "→",
        Modifier.padding(horizontal = 2.dp),
        color = PdigV2Colors.TextMuted,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
internal fun OldNumberSurface(old: UiVNextNumber?, projection: String, modifier: Modifier = Modifier) {
    if (old == null) return
    val ghost = projection == "after"
    Surface(
        modifier
            .fillMaxWidth()
            .testTagLocal(VTestIds.CHANGE_OLD),
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
internal fun ServicesSurface(projection: String, modifier: Modifier = Modifier) {
    Surface(
        modifier
            .fillMaxWidth()
            .testTagLocal(VTestIds.CHANGE_SERVICES),
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
internal fun NewNumberSurface(new: UiVNextNumber?, projection: String, modifier: Modifier = Modifier) {
    if (new == null) return
    val isAfter = projection == "after"
    Surface(
        modifier
            .fillMaxWidth()
            .testTagLocal(VTestIds.CHANGE_NEW),
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

/** COMPACT 原生 6 步 mini progress：首屏可见全部 6 步 + 当前步骤（brief §12 方案 B）。 */
@Composable
internal fun CompactStepper(stages: List<ChangeStage>) {
    val currentIndex = stages
        .indexOfFirst { it.status == "verifying" }
        .takeIf { it >= 0 }
        ?: stages.indexOfLast { it.status == "completed" }
        .coerceAtLeast(0)
    val current = stages[currentIndex]
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .testTagLocal(VTestIds.CHANGE_STEPPER_MINI),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            stages.forEachIndexed { index, stage ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(22.dp)
                            .background(stepperFill(stage.status), CircleShape)
                            .border(2.dp, stepperBorder(stage.status), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "${stage.stage}",
                            color = if (stage.status == "completed") PdigV2Colors.CanvasDeep else PdigV2Colors.TextPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    if (index == currentIndex) {
                        Spacer(Modifier.height(2.dp))
                        Box(Modifier.size(4.dp).background(PdigV2Colors.PrimaryBright, CircleShape))
                    }
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .testTagLocal(VTestIds.CHANGE_STEPPER_CURRENT)
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "第 ${current.stage}/6 步 · ${stepName(current.key)}",
                color = PdigV2Colors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            StatusBadge(current.status)
        }
        current.blockReason?.let { reason ->
            Surface(color = PdigV2Colors.Critical.copy(alpha = 0.12f), shape = RoundedCornerShape(VRadius.Md), modifier = Modifier.fillMaxWidth()) {
                Text(reason, Modifier.padding(10.dp), color = PdigV2Colors.TextPrimary, fontSize = 12.sp)
            }
        }
    }
}

private fun stepperFill(status: String): Color = when (status) {
    "completed" -> PdigV2Colors.Positive
    "blocked" -> PdigV2Colors.Critical.copy(alpha = 0.3f)
    "verifying", "plan" -> PdigV2Colors.Warning.copy(alpha = 0.35f)
    else -> PdigV2Colors.SurfaceRaised
}

private fun stepperBorder(status: String): Color = when (status) {
    "completed" -> PdigV2Colors.Positive
    "blocked" -> PdigV2Colors.Critical
    "verifying", "plan" -> PdigV2Colors.Warning
    else -> PdigV2Colors.BorderSubtle
}

private fun stepName(key: String): String = when (key) {
    "impact-analysis" -> "影响分析"
    "establish-new-number" -> "建立新号码"
    "verify-new-number" -> "验证新号码"
    "migrate-key-accounts" -> "迁移关键账户"
    "check-recovery-paths" -> "检查恢复路径"
    "retire-old-number" -> "停用旧号码"
    else -> key
}