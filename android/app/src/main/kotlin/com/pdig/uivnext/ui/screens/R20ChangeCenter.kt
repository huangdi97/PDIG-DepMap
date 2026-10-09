package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.demo.demoChanges
import com.pdig.uivnext.model.MediaBreakpoint
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VTouchTarget
import com.pdig.uivnext.ui.VAppState

/**
 * Primary Change root.
 *
 * Change is a product mode ("what am I changing?"), not an alias for one
 * hard-coded phone flow. Executable detail routes remain scenario-specific.
 */
@Composable
internal fun R20ChangeCenter(app: VAppState, breakpoint: MediaBreakpoint) {
    val changes = app.demoChanges()
    val maxWidth = when (breakpoint) {
        MediaBreakpoint.COMPACT -> 620.dp
        MediaBreakpoint.MEDIUM -> 780.dp
        MediaBreakpoint.EXPANDED -> 980.dp
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .fillMaxWidth()
                .widthIn(max = maxWidth)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = if (breakpoint == MediaBreakpoint.COMPACT) 13.dp else 24.dp,
                    vertical = if (breakpoint == MediaBreakpoint.COMPACT) 12.dp else 22.dp,
                )
                .testTag("pdig.r20.change-center"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    "变更",
                    color = PdigV2Colors.TextPrimary,
                    fontSize = if (breakpoint == MediaBreakpoint.COMPACT) 22.sp else 27.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "先看影响，再开始迁移；完成必须经过实际验证。",
                    color = PdigV2Colors.TextSecondary,
                    fontSize = 12.sp,
                )
            }

            ChangeSectionHeading("正在进行的变更", "进行中")
            if (changes.isEmpty()) {
                ChangeBoundaryCard("当前没有已记录的进行中变更。没有记录不代表没有需要处理的事情。")
            } else {
                changes.forEach { change ->
                    val actionable = change.target == "change-phone"
                    ChangeWorkCard(
                        title = change.title,
                        subtitle = changePhaseLabel(change.phase),
                        accent = PdigV2Colors.Warning,
                        action = if (actionable) "继续处理 →" else "查看记录 →",
                    ) {
                        if (actionable) app.navigate(VScreen.CHANGE_PHONE)
                        else app.navigate(VScreen.RECORDS)
                    }
                }
            }

            ChangeSectionHeading("准备改变", "准备阶段")
            ChangeWorkCard(
                title = "更换手机号",
                subtitle = "检查认证、恢复、关键服务与旧号停用条件。",
                accent = PdigV2Colors.PrimaryBright,
                action = "分析并进入计划 →",
            ) {
                app.navigate(VScreen.CHANGE_PHONE)
            }
            ChangeWorkCard(
                title = "更换银行卡",
                subtitle = "先选择具体卡片，查看已确认支付依赖与影响；不在这里生成虚假计划。",
                accent = PdigV2Colors.PrimaryBright,
                action = "选择卡片查看影响 →",
            ) {
                app.navigate(VScreen.CARDS)
            }

            ChangeSectionHeading("维护与核对", "日常维护")
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ChangeMiniAction(
                    title = "薄弱点",
                    subtitle = "先处理已确认风险",
                    modifier = Modifier.weight(1f),
                ) { app.navigate(VScreen.WEAKNESSES) }
                ChangeMiniAction(
                    title = "记录",
                    subtitle = "查看变更与验证",
                    modifier = Modifier.weight(1f),
                ) { app.navigate(VScreen.RECORDS) }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = PdigV2Colors.PrimarySoft.copy(alpha = 0.58f),
                shape = RoundedCornerShape(VRadius.Lg),
            ) {
                Text(
                    "Current / Transition / After 是不同状态：完成后视图只是计划投影，" +
                        "不会因为点击或时间经过就自动成为已执行、已验证的现实。",
                    Modifier.padding(13.dp),
                    color = PdigV2Colors.TextSecondary,
                    fontSize = 10.sp,
                    lineHeight = 16.sp,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ChangeSectionHeading(title: String, english: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(title, color = PdigV2Colors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(english, color = PdigV2Colors.TextMuted, fontSize = 9.sp)
    }
}

@Composable
private fun ChangeWorkCard(
    title: String,
    subtitle: String,
    accent: Color,
    action: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 92.dp)
            .clickable(onClick = onClick),
        color = PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Xl),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.18f)),
    ) {
        Row(
            Modifier.padding(horizontal = 15.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                color = accent.copy(alpha = 0.11f),
                shape = RoundedCornerShape(VRadius.Md),
            ) {
                Box(
                    Modifier.defaultMinSize(minWidth = 42.dp, minHeight = 42.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("↻", color = accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(title, color = PdigV2Colors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = PdigV2Colors.TextMuted, fontSize = 10.sp, lineHeight = 15.sp)
                Text(action, color = accent, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ChangeMiniAction(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = VTouchTarget.Min)
            .clickable(onClick = onClick),
        color = PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Lg),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Column(
            Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(title, color = PdigV2Colors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = PdigV2Colors.TextMuted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun ChangeBoundaryCard(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PdigV2Colors.SurfaceRaised,
        shape = RoundedCornerShape(VRadius.Lg),
    ) {
        Text(
            text,
            Modifier.padding(14.dp),
            color = PdigV2Colors.TextSecondary,
            fontSize = 11.sp,
            lineHeight = 17.sp,
        )
    }
}

private fun changePhaseLabel(phase: String): String = when (phase) {
    "verify_new_phone" -> "正在验证新号码 · 旧号码继续保留"
    "impact_analysis" -> "正在分析影响"
    "migrate_accounts" -> "正在迁移已确认关系"
    "verify" -> "等待验证"
    else -> "已记录的变更流程"
}
