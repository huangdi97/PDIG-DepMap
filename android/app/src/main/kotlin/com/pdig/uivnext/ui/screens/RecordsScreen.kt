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
import androidx.compose.foundation.shape.CircleShape
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
import com.pdig.uivnext.model.ChangeStage
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.AttentionRow
import com.pdig.uivnext.ui.components.EmptyKind
import com.pdig.uivnext.ui.components.EmptyState
import com.pdig.uivnext.ui.components.SectionHeader
import com.pdig.uivnext.ui.components.StatusBadge

/** 记录：把变更过程、关注事项和时间节点放进一条可追溯的连续记录。 */
@Composable
fun RecordsScreen(app: VAppState) {
    val changes = app.demoChanges()
    val attention = app.demoAttention()
    val upcoming = app.demoUpcoming()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("记录", color = PdigV2Colors.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(
            "追踪正在发生的变更、需要处理的风险，以及接下来已知的时间节点。",
            color = PdigV2Colors.TextSecondary,
            fontSize = 13.sp,
        )
        RecordsSummarySurface(
            activeChanges = changes.size,
            attentionCount = attention.size,
            upcomingCount = upcoming.size,
        )

        SectionHeader("正在进行")
        if (changes.isEmpty()) {
            EmptyState(
                kind = EmptyKind.CHANGE,
                title = "没有进行中的变更",
                description = "当前没有执行中的变更计划。没有记录 ≠ 没有风险：开始新的变更后会出现在这里。",
                primaryCta = "规划更换手机号",
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
                    color = PdigV2Colors.PrimarySoft.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(VRadius.Lg),
                    border = BorderStroke(1.dp, PdigV2Colors.Primary.copy(alpha = 0.38f)),
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                change.title,
                                color = PdigV2Colors.TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                            )
                            Text("当前阶段 · 验证新号码", color = PdigV2Colors.Warning, fontSize = 12.sp)
                        }
                        Text("继续查看 →", color = PdigV2Colors.PrimaryBright, fontSize = 12.sp)
                    }
                }
            }

            SectionHeader("迁移进度")
            UiVNextDemoFixture.changeStages.forEachIndexed { index, stage ->
                TimelineStage(
                    stage = stage,
                    showConnector = index < UiVNextDemoFixture.changeStages.lastIndex,
                )
            }
        }

        SectionHeader("需要关注（${attention.size}）")
        if (attention.isEmpty()) {
            EmptyState(
                kind = EmptyKind.ATTENTION,
                title = "没有需要处理的记录",
                description = "当前没有可展示的关注事项；未记录的关系仍然保持未知。",
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

        SectionHeader("即将到来（${upcoming.size}）")
        if (upcoming.isEmpty()) {
            Text(
                "当前没有已知的时间节点；没有记录的到期日仍保持未知。",
                color = PdigV2Colors.TextMuted,
                fontSize = 12.sp,
            )
        } else {
            upcoming.forEach { item ->
                Surface(
                    color = PdigV2Colors.SurfaceRaised,
                    shape = RoundedCornerShape(VRadius.Md),
                    border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(item.title, color = PdigV2Colors.TextSecondary, fontSize = 13.sp)
                        Surface(
                            color = PdigV2Colors.Warning.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(VRadius.Sm),
                        ) {
                            Text(
                                "${item.days} 天后",
                                Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                color = PdigV2Colors.Warning,
                                fontSize = 11.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordsSummarySurface(activeChanges: Int, attentionCount: Int, upcomingCount: Int) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTagLocal("pdig.records.summary"),
        color = PdigV2Colors.PrimarySoft.copy(alpha = 0.82f),
        shape = RoundedCornerShape(VRadius.Xl),
        border = BorderStroke(1.dp, PdigV2Colors.Primary.copy(alpha = 0.20f)),
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RecordsMetric(activeChanges.toString(), "进行中的变更", Modifier.weight(1f))
            RecordsMetric(attentionCount.toString(), "需要处理", Modifier.weight(1f))
            RecordsMetric(upcomingCount.toString(), "即将到来", Modifier.weight(1f))
        }
    }
}

@Composable
private fun RecordsMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, color = PdigV2Colors.PrimaryBright, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(label, color = PdigV2Colors.TextSecondary, fontSize = 11.sp)
    }
}

@Composable
private fun TimelineStage(stage: ChangeStage, showConnector: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(24.dp),
                color = if (stage.status == "completed") PdigV2Colors.Primary else PdigV2Colors.SurfaceRaised,
                shape = CircleShape,
                border = BorderStroke(
                    1.dp,
                    if (stage.status == "completed") PdigV2Colors.PrimaryBright else PdigV2Colors.BorderStrong,
                ),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        stage.stage.toString(),
                        color = if (stage.status == "completed") PdigV2Colors.CanvasDeep else PdigV2Colors.TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            if (showConnector) {
                Box(
                    Modifier
                        .width(2.dp)
                        .height(48.dp)
                        .background(PdigV2Colors.BorderStrong),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Surface(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 10.dp),
            color = PdigV2Colors.Surface.copy(alpha = 0.88f),
            shape = RoundedCornerShape(VRadius.Md),
            border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stageLabel(stage.key),
                        color = PdigV2Colors.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    stage.blockReason?.let {
                        Text(it, color = PdigV2Colors.TextMuted, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.width(10.dp))
                StatusBadge(stage.status)
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
