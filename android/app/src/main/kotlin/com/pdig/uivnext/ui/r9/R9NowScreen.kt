package com.pdig.uivnext.ui.r9

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.demo.demoAttention
import com.pdig.uivnext.demo.demoCards
import com.pdig.uivnext.demo.demoChanges
import com.pdig.uivnext.demo.demoUpcoming
import com.pdig.uivnext.demo.demoNumbers
import com.pdig.uivnext.demo.demoRegions
import com.pdig.uivnext.demo.reviewReferenceSummary
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.ui.VAppState
import java.util.Calendar

/** Consumer Earth must be completely framed; 0.72 is the renderer's diameter fraction.
 * A prior 1.57x transform produced a visually clipped sphere despite passing CI.
 * Reserve a minimum 12% of the Globe canvas diameter for the visible limb.
 */
internal const val R9_WORLD_VISUAL_SCALE: Float = 1.27f
internal fun r9PlanetDiameterFraction(scale: Float): Float = 0.72f * scale

/** Greeting reflects the actual device clock; it must not say morning at 14:10. */
internal fun r9Greeting(hour: Int): String = when (hour) {
    in 5..10 -> "早上好"
    in 11..12 -> "中午好"
    in 13..18 -> "下午好"
    in 19..23 -> "晚上好"
    else -> "你好"
}

/**
 * R9 phone NOW is a fresh composition; the R8 NowScreen is deliberately NOT used
 * in this preview route. All region/attention/metric facts come from the active
 * demo gate, including honest zero/empty states.
 */
@Composable
internal fun R9NowScreen(app: VAppState) {
    val regions = app.demoRegions()
    val attention = app.demoAttention()
    val review = reviewReferenceSummary(
        if (app.emptyDemo) emptyList() else com.pdig.uivnext.demo.UI_REVIEW_REFERENCE_ITEMS,
    )
    val taskCount = attention.size + review.total
    val changes = app.demoChanges()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 13.dp, vertical = 12.dp)
            .testTag("pdig.r9.screen.now"),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(r9Greeting(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)), color = R9.Ink, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text("你的数字基础设施 · 连接全球，触手可及", color = R9.Muted, fontSize = 11.sp)
            }
            if (taskCount > 0) {
                R9Badge("需要处理 $taskCount", R9.Rose)
            }
        }

        // R15 changes the entire hero composition, not only the Earth shader.
        // All five known region and four asset surfaces belong to ONE spatial stage.
        R17FramelessWorldHero(app, regions, app.demoCards().size, app.demoNumbers().size,
            if (app.emptyDemo) 0 else UiVNextDemoFixture.accounts.size,
            if (app.emptyDemo) 0 else UiVNextDemoFixture.services.size)

        if (review.total > 0) {
            R9SectionTitle("待复核（${review.total}）")
            Surface(
                modifier = Modifier.fillMaxWidth()
                    .clickable { app.navigate(VScreen.REVIEW) }
                    .testTag("pdig.r22.now.review"),
                color = Color(0xFFF3F8FF),
                shape = RoundedCornerShape(15.dp),
                border = BorderStroke(1.dp, Color(0xFFC9DDF5)),
            ) {
                Row(
                    Modifier.padding(horizontal = 13.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    R9Badge("${review.total}", R9.Blue)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("系统发现了需要你确认的信息", color = R9.Ink,
                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${review.proposals} 关系建议 · ${review.candidates} 对象候选 · ${review.drifts} 现实漂移",
                            color = R9.Muted,
                            fontSize = 9.sp,
                        )
                    }
                    Text("复核 →", color = R9.Blue, fontSize = 10.sp)
                }
            }
        }

        R9SectionTitle("其他需要处理（${attention.size}）")
        if (attention.isEmpty()) {
            Surface(shape = RoundedCornerShape(15.dp), color = Color.White) {
                Text("暂无已记录事项；未知不等于安全。", Modifier.padding(16.dp),
                    color = R9.Muted, fontSize = 12.sp)
            }
        } else {
            attention.forEachIndexed { index, item ->
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable {
                        if (UiVNextDemoFixture.cardById(item.target) != null) app.openCard(item.target)
                        else if (app.demoNumbers().any { it.id == item.target }) app.openNumber(item.target)
                        else app.navigate(VScreen.WEAKNESSES)
                    }.then(if (index == 0) Modifier.testTag(VTestIds.NOW_ATTENTION) else Modifier),
                    shape = RoundedCornerShape(15.dp),
                    color = Color(0xFFFFF4F2),
                    border = BorderStroke(1.dp, Color(0xFFF4C8CE)),
                ) {
                    Row(
                        Modifier.padding(horizontal = 13.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            Modifier.size(30.dp).background(R9.Rose, RoundedCornerShape(9.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Text(
                            item.title,
                            Modifier.weight(1f),
                            color = R9.Ink,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            maxLines = 3,
                        )
                        Text("›", color = R9.Muted, fontSize = 22.sp)
                    }
                }
            }
        }

        // R12: known dependencies, not a phantom safety score.
        R12ContinuityInsight(app)

        R9SectionTitle("进行中的变更", "变更中心 →") { app.navigate(VScreen.CHANGE) }
        if (changes.isEmpty()) {
            Text("没有记录正在执行的变更。", color = R9.Muted, fontSize = 12.sp)
        } else {
            changes.take(2).forEach { change ->
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { app.navigate(VScreen.CHANGE_PHONE) },
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, R9.Line),
                ) {
                    Row(Modifier.padding(13.dp), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(change.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = R9.Ink)
                            Text("计划仍在进行 · 未验证的步骤不视为完成",
                                fontSize = 10.sp, color = R9.Muted)
                        }
                        Text("继续 →", fontSize = 11.sp, color = R9.Blue)
                    }
                }
            }
        }
        // Display preferences must have a real consumer-facing effect. Upcoming
        // items belong to the frozen SYNTHETIC fixture, not the user's live calendar.
        if (app.showUpcoming) {
            val upcoming = app.demoUpcoming()
            R9SectionTitle("演示 · 时间节点")
            if (upcoming.isEmpty()) {
                Text("没有已记录的时间节点。未知不等于没有风险。",
                    color = R9.Muted, fontSize = 11.sp)
            } else {
                upcoming.take(2).forEach { item ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White, shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, R9.Line),
                    ) {
                        Row(Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            R9Badge("时间", R9.Amber)
                            Text(item.title, modifier = Modifier.weight(1f),
                                color = R9.Ink, fontSize = 11.sp, maxLines = 2)
                            Text("›", fontSize = 17.sp, color = R9.Blue)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}

