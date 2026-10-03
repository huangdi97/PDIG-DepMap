package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.pdig.uivnext.demo.demoAttention
import com.pdig.uivnext.demo.demoChanges
import com.pdig.uivnext.demo.demoUpcoming
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.AttentionRow
import com.pdig.uivnext.ui.components.EmptyKind
import com.pdig.uivnext.ui.components.EmptyState
import com.pdig.uivnext.ui.components.SectionHeader
import com.pdig.uivnext.ui.components.StatusBadge

/**
 * Records（记录，一级导航）：变更历史时间线 + 关注项 + 即将到来。
 * 历史条目带状态徽标；空态坚持「未记录 ≠ 无风险」语义。
 */
@Composable
fun RecordsScreen(app: VAppState) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("记录", color = PdigV2Colors.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("查看正在进行的变更、需要关注的事项，以及已经知道的时间节点。", color = PdigV2Colors.TextSecondary, fontSize = 13.sp)

        val changes = app.demoChanges()
        SectionHeader("进行中的变更（${changes.size}）")
        if (changes.isEmpty()) {
            EmptyState(
                kind = EmptyKind.CHANGE,
                title = "没有进行中的变更",
                description = "当前没有处于执行中的变更计划。没有记录 ≠ 没有风险：新的变更开始后才会出现在这里。",
                primaryCta = "发起变更",
                onPrimary = { app.navigate(VScreen.CHANGE_PHONE) },
                secondaryCta = "查看基础设施",
                onSecondary = { app.navigate(VScreen.OVERVIEW) },
            )
        } else {
            changes.forEach { change ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickableLocal { app.navigate(VScreen.CHANGE_PHONE) },
                    color = PdigV2Colors.SurfaceRaised,
                    shape = RoundedCornerShape(VRadius.Md),
                    border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(change.title, color = PdigV2Colors.TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("阶段：验证新号码", color = PdigV2Colors.Warning, fontSize = 12.sp)
                    }
                }
            }
        }

        SectionHeader("执行步骤（迁移中）")
        UiVNextDemoFixture.changeStages.forEach { stage ->
            Surface(
                color = PdigV2Colors.Surface.copy(alpha = 0.92f),
                shape = RoundedCornerShape(VRadius.Md),
                border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stageLabel(stage.key), color = PdigV2Colors.TextPrimary, fontSize = 13.sp)
                    StatusBadge(stage.status)
                }
            }
        }

        val attention = app.demoAttention()
        SectionHeader("关注记录（${attention.size}）")
        if (attention.isEmpty()) {
            EmptyState(
                kind = EmptyKind.ATTENTION,
                title = "没有需要处理的记录",
                description = "未记录 ≠ 无风险：当前没有可展示的关注事项，不代表一切安全。",
                primaryCta = "返回现在",
                onPrimary = { app.navigate(VScreen.NOW) },
            )
        } else {
            attention.forEach { item ->
                AttentionRow(item = item, onClick = { clicked ->
                    when {
                        UiVNextDemoFixture.cardById(clicked.target) != null -> app.openCard(clicked.target)
                        else -> app.openNumber(clicked.target)
                    }
                })
            }
        }

        val upcoming = app.demoUpcoming()
        SectionHeader("即将到来（${upcoming.size}）")
        upcoming.forEach { item ->
            Surface(color = PdigV2Colors.SurfaceRaised, shape = RoundedCornerShape(VRadius.Md), modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(item.title, color = PdigV2Colors.TextSecondary, fontSize = 13.sp)
                    Surface(color = PdigV2Colors.Warning.copy(alpha = 0.16f), shape = RoundedCornerShape(VRadius.Sm)) {
                        Text(
                            "${item.days} 天后",
                            Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            color = PdigV2Colors.Warning,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        }
    }
}

private fun stageLabel(key: String): String = when (key) {
    "impact-analysis" -> "影响分析"
    "establish-new-number" -> "建立新号码"
    "verify-new-number" -> "验证新号码"
    "migrate-key-accounts" -> "迁移关键账户"
    "check-recovery-paths" -> "检查恢复路径"
    "retire-old-number" -> "停用旧号码"
    else -> key
}
