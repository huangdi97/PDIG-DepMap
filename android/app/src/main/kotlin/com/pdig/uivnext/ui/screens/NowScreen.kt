package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.demo.demoAttention
import com.pdig.uivnext.demo.demoCards
import com.pdig.uivnext.demo.demoChanges
import com.pdig.uivnext.demo.demoNumbers
import com.pdig.uivnext.demo.demoRegions
import com.pdig.uivnext.demo.demoUpcoming
import com.pdig.uivnext.globe.VNextGlobe
import com.pdig.uivnext.model.MediaBreakpoint
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VSpacing
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.AttentionRow
import com.pdig.uivnext.ui.components.EmptyKind
import com.pdig.uivnext.ui.components.EmptyState
import com.pdig.uivnext.ui.components.SectionHeader

/**
 * Now：告诉用户现在最值得处理什么。
 * Globe/global context + Need Attention + Active Changes + Upcoming（禁止 8 个统计卡）。
 * 大屏三列并排；手机堆叠（globe hero 在上）。数据经 [VAppState.demo*] 闸门（emptyDemo 空态）。
 */
@Composable
fun NowScreen(app: VAppState, breakpoint: MediaBreakpoint) {
    val regions = app.demoRegions()
    val arcingPairs = arcPairs()
    Column(
        Modifier
            .fillMaxSize()
            .padding(VSpacing.Xxl)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // Globe context（Now 的 global 语境；compact hero 高度较小）
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (breakpoint == MediaBreakpoint.EXPANDED) 360.dp else 280.dp)
                .testTagLocal(VTestIds.NOW_GLOBE),
            color = PdigV2Colors.SurfaceGlass,
            shape = RoundedCornerShape(VRadius.Xl),
            border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
        ) {
            Box {
                VNextGlobe(
                    controller = app.globe,
                    regions = regions,
                    arcingPairs = arcingPairs,
                    reduceMotion = app.reduceMotion,
                )
                Column(Modifier.align(Alignment.TopStart).padding(20.dp)) {
                    Text("现在", color = PdigV2Colors.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "全球 ${app.demoCards().size} 张卡 · ${app.demoNumbers().size} 个号码",
                        color = PdigV2Colors.TextSecondary,
                        fontSize = 13.sp,
                    )
                }
            }
        }

        if (breakpoint == MediaBreakpoint.EXPANDED) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(
                    Modifier
                        .weight(1f)
                        .testTagLocal(VTestIds.NOW_ATTENTION),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AttentionSection(app)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ChangesSection(app)
                }
                Column(
                    Modifier
                        .weight(1f)
                        .testTagLocal(VTestIds.NOW_UPCOMING),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    UpcomingSection(app)
                }
            }
        } else {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .testTagLocal(VTestIds.NOW_ATTENTION),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AttentionSection(app)
                }
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ChangesSection(app)
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .testTagLocal(VTestIds.NOW_UPCOMING),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    UpcomingSection(app)
                }
            }
        }
    }
}

@Composable
private fun AttentionSection(app: VAppState) {
    val items = app.demoAttention()
    SectionHeader("需要你处理（${items.size}）")
    if (items.isEmpty()) {
        EmptyState(
            kind = EmptyKind.ATTENTION,
            title = "没有需要处理的项",
            description = "未记录 ≠ 无风险：当前没有可展示的关注事项，不代表一切安全。",
            primaryCta = "查看基础设施",
            onPrimary = { app.navigate(VScreen.OVERVIEW) },
            secondaryCta = "查看薄弱点",
            onSecondary = { app.navigate(VScreen.WEAKNESSES) },
        )
    } else {
        items.forEach { item ->
            AttentionRow(item = item, onClick = { clicked ->
                when {
                    UiVNextDemoFixture.cardById(clicked.target) != null -> app.openCard(clicked.target)
                    else -> app.openNumber(clicked.target)
                }
            })
        }
    }
}

@Composable
private fun ChangesSection(app: VAppState) {
    val changes = app.demoChanges()
    SectionHeader(
        "进行中的变更",
        trailing = { Text("查看全部", Modifier.clickableLocal { app.navigate(VScreen.RECORDS) }, color = PdigV2Colors.PrimaryBright, fontSize = 12.sp) },
    )
    if (changes.isEmpty()) {
        Text(
            "当前没有进行中的变更。没有记录 ≠ 没有风险：变更开始后才会出现在这里。",
            color = PdigV2Colors.TextMuted,
            fontSize = 12.sp,
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
}

@Composable
private fun UpcomingSection(app: VAppState) {
    val upcoming = app.demoUpcoming()
    SectionHeader("即将到来")
    if (upcoming.isEmpty()) {
        Text(
            "没有已知的即将到期事项。未记录 ≠ 无风险：到期日尚未记录的卡片不会出现在这里。",
            color = PdigV2Colors.TextMuted,
            fontSize = 12.sp,
        )
    } else {
        upcoming.forEach { item ->
            Surface(
                color = PdigV2Colors.SurfaceRaised,
                shape = RoundedCornerShape(VRadius.Md),
                modifier = Modifier.fillMaxWidth(),
            ) {
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
