package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.globe.VNextGlobe
import com.pdig.uivnext.model.MediaBreakpoint
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VSpacing
import com.pdig.uivnext.theme.VType
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.AttentionRow
import com.pdig.uivnext.ui.components.SectionHeader

/**
 * Now（G11）：Globe Context + Need Attention（3–5 项）+ Active Change + Upcoming。
 * 禁止一堆小 KPI 卡：globe 是语境主角，attention 是 L5 最重要区块。
 */
@Composable
fun NowScreen(app: VAppState, breakpoint: MediaBreakpoint) {
    val regions = UiVNextDemoFixture.regionSummaries()
    val arcingPairs = arcPairs()
    Column(
        Modifier
            .fillMaxSize()
            .padding(VSpacing.Xxl)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(VSpacing.Xxxl),
    ) {
        // Globe Context（L1 主角；edge-to-edge，不塞卡片）
        Box(
            Modifier
                .fillMaxWidth()
                .height(if (breakpoint == MediaBreakpoint.WIDE) 400.dp else 300.dp)
                .testTagLocal(VTestIds.NOW_GLOBE),
        ) {
            VNextGlobe(
                controller = app.globe,
                regions = regions,
                arcingPairs = arcingPairs,
                reduceMotion = app.reduceMotion,
                showRegionLabels = true,
            )
            Column(Modifier.align(Alignment.TopStart).padding(VSpacing.Xxl)) {
                Text("现在", style = VType.PageTitle, color = PdigV2Colors.TextPrimary)
                Text(
                    "全球 ${UiVNextDemoFixture.cards.size} 张卡 · ${UiVNextDemoFixture.numbers.size} 个号码",
                    style = VType.Secondary,
                    color = PdigV2Colors.TextSecondary,
                )
            }
            Surface(
                Modifier.align(Alignment.BottomStart).padding(VSpacing.Lg),
                color = PdigV2Colors.SurfaceGlass,
                shape = RoundedCornerShape(VRadius.Md),
            ) {
                Text(
                    "点击地区聚焦基础设施 · 再次点击打开地区抽屉",
                    Modifier.padding(horizontal = VSpacing.Lg, vertical = VSpacing.Sm),
                    style = VType.Secondary,
                    color = PdigV2Colors.TextSecondary,
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(VSpacing.Xxl)) {
            // Need Attention（L5 最重要；最多 3–5 项）
            Column(
                Modifier
                    .weight(1.2f)
                    .testTagLocal(VTestIds.NOW_ATTENTION),
                verticalArrangement = Arrangement.spacedBy(VSpacing.Lg),
            ) {
                SectionHeader("需要你处理（${UiVNextDemoFixture.attentionItems.size}）")
                UiVNextDemoFixture.attentionItems.forEach { item ->
                    AttentionRow(item = item, onClick = { clicked ->
                        when {
                            UiVNextDemoFixture.cardById(clicked.target) != null -> app.openCard(clicked.target)
                            else -> app.openNumber(clicked.target)
                        }
                    })
                }
            }
            // Active Change
            Column(
                Modifier
                    .weight(0.9f)
                    .testTagLocal(VTestIds.NOW_CHANGES),
                verticalArrangement = Arrangement.spacedBy(VSpacing.Lg),
            ) {
                SectionHeader(
                    "进行中的变更",
                    trailing = { Text("查看全部", color = PdigV2Colors.PrimaryBright, style = VType.Label) },
                )
                ActiveChangeCard(app)
            }
            // Upcoming
            Column(
                Modifier
                    .weight(0.9f)
                    .testTagLocal(VTestIds.NOW_UPCOMING),
                verticalArrangement = Arrangement.spacedBy(VSpacing.Lg),
            ) {
                SectionHeader("即将到来")
                UiVNextDemoFixture.upcoming.forEach { item ->
                    Surface(
                        color = PdigV2Colors.Surface,
                        shape = RoundedCornerShape(VRadius.Md),
                        modifier = Modifier.fillMaxWidth(),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
                    ) {
                        Row(
                            Modifier.padding(VSpacing.Lg),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(item.title, style = VType.Secondary, color = PdigV2Colors.TextSecondary, modifier = Modifier.weight(1f))
                            Surface(color = PdigV2Colors.Warning.copy(alpha = 0.16f), shape = RoundedCornerShape(VRadius.Sm)) {
                                Text(
                                    "${item.days} 天后",
                                    Modifier.padding(horizontal = VSpacing.Sm, vertical = 2.dp),
                                    style = VType.Label,
                                    color = PdigV2Colors.Warning,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Active Change 卡：「更换手机号 2 / 6 · 下一步：验证新号码」（fixture 派生）。 */
@Composable
private fun ActiveChangeCard(app: VAppState) {
    val stages = UiVNextDemoFixture.changeStages
    val done = stages.count { it.status == "completed" }
    val current = stages.firstOrNull { it.status == "verifying" }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { app.navigate(VScreen.CHANGE_PHONE) },
        color = PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Lg),
        border = androidx.compose.foundation.BorderStroke(1.dp, PdigV2Colors.BorderStrong),
    ) {
        Column(Modifier.padding(VSpacing.Xxl), verticalArrangement = Arrangement.spacedBy(VSpacing.Sm)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("更换手机号", style = VType.SectionTitle, color = PdigV2Colors.TextPrimary)
                Text("$done / ${stages.size}", style = VType.MajorNumber, color = PdigV2Colors.PrimaryBright)
            }
            Text(
                "下一步：${stageName(current?.key ?: "")}",
                style = VType.Secondary,
                color = PdigV2Colors.Warning,
            )
            // 迷你进度条（完成/进行/未开始/阻止）
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                stages.forEach { stage ->
                    val barColor = when (stage.status) {
                        "completed" -> PdigV2Colors.Positive
                        "verifying" -> PdigV2Colors.Warning
                        "blocked" -> PdigV2Colors.Critical
                        else -> PdigV2Colors.BorderSubtle
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .height(3.dp)
                            .background(barColor, RoundedCornerShape(1.dp)),
                    )
                }
            }
        }
    }
}

private fun stageName(key: String): String = when (key) {
    "impact-analysis" -> "影响分析"
    "establish-new-number" -> "建立新号码"
    "verify-new-number" -> "验证新号码"
    "migrate-key-accounts" -> "迁移关键账户"
    "check-recovery-paths" -> "检查恢复路径"
    "retire-old-number" -> "停用旧号码"
    else -> key
}
