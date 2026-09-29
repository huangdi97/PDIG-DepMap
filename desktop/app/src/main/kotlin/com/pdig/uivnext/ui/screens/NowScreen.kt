package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
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
import com.pdig.uivnext.globe.VNextGlobe
import com.pdig.uivnext.model.MediaBreakpoint
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.AttentionRow
import com.pdig.uivnext.ui.components.SectionHeader

/**
 * Now：告诉用户现在最值得处理什么。
 * Globe/global context + Need Attention + Active Changes + Upcoming（禁止 8 个统计卡）。
 */
@Composable
fun NowScreen(app: VAppState, breakpoint: MediaBreakpoint) {
    val regions = UiVNextDemoFixture.regionSummaries()
    val arcingPairs = arcPairs()
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // Globe context（Now 的 global 语境；compact）
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (breakpoint == MediaBreakpoint.WIDE) 360.dp else 280.dp)
                .testTagLocal(VTestIds.NOW_GLOBE),
            color = PdigV2Colors.SurfaceGlass,
            shape = RoundedCornerShape(VRadius.Xl),
            border = androidx.compose.foundation.BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
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
                        "全球 ${UiVNextDemoFixture.cards.size} 张卡 · ${UiVNextDemoFixture.numbers.size} 个号码",
                        color = PdigV2Colors.TextSecondary,
                        fontSize = 13.sp,
                    )
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            // Need Attention（L5 最重要）
            Column(
                Modifier
                    .weight(1f)
                    .testTagLocal(VTestIds.NOW_ATTENTION),
                verticalArrangement = Arrangement.spacedBy(12.dp),
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
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader(
                    "进行中的变更",
                    trailing = { Text("查看全部", color = PdigV2Colors.PrimaryBright, fontSize = 12.sp) },
                )
                UiVNextDemoFixture.activeChanges.forEach { change ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickableLocal { app.navigate(VScreen.CHANGE_PHONE) },
                        color = PdigV2Colors.SurfaceRaised,
                        shape = RoundedCornerShape(VRadius.Md),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(change.title, color = PdigV2Colors.TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text("阶段：验证新号码", color = PdigV2Colors.Warning, fontSize = 12.sp)
                        }
                    }
                }
            }
            // Upcoming
            Column(
                Modifier
                    .weight(1f)
                    .testTagLocal(VTestIds.NOW_UPCOMING),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SectionHeader("即将到来")
                UiVNextDemoFixture.upcoming.forEach { item ->
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
    }
}