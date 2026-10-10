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
import androidx.compose.foundation.shape.CircleShape
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
import com.pdig.uivnext.demo.UiRecordTraceItem
import com.pdig.uivnext.demo.demoChanges
import com.pdig.uivnext.demo.UiRecordTraceState
import com.pdig.uivnext.demo.referenceRecordTrace
import com.pdig.uivnext.demo.referenceRecordTraceSummary
import com.pdig.uivnext.model.MediaBreakpoint
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VTouchTarget
import com.pdig.uivnext.ui.VAppState

/**
 * R21 Records = evidence / verification trace.
 *
 * Attention and upcoming maintenance belong to Now. Records answers:
 * “发生过什么、验证过什么、依据是什么？”
 */
@Composable
internal fun R21RecordsScreen(app: VAppState, breakpoint: MediaBreakpoint) {
    val trace = if (app.emptyDemo) emptyList() else referenceRecordTrace()
    val summary = referenceRecordTraceSummary(trace)
    val maxWidth = when (breakpoint) {
        MediaBreakpoint.COMPACT -> 620.dp
        MediaBreakpoint.MEDIUM -> 820.dp
        MediaBreakpoint.EXPANDED -> 1040.dp
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .fillMaxWidth()
                .widthIn(max = maxWidth)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = if (breakpoint == MediaBreakpoint.COMPACT) 13.dp else 22.dp,
                    vertical = if (breakpoint == MediaBreakpoint.COMPACT) 12.dp else 20.dp,
                )
                .testTag("pdig.r21.records"),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "记录",
                    color = PdigV2Colors.TextPrimary,
                    fontSize = if (breakpoint == MediaBreakpoint.COMPACT) 22.sp else 27.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "发生过什么、验证过什么、依据是什么。关注事项和未来维护留在“现在”。",
                    color = PdigV2Colors.TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 17.sp,
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth().testTag("pdig.r21.records.summary"),
                color = PdigV2Colors.Surface,
                shape = RoundedCornerShape(VRadius.Xl),
                border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    RecordMetric(summary.recordedComplete, "已记录完成", PdigV2Colors.PrimaryBright, Modifier.weight(1f))
                    RecordMetric(summary.verified, "已验证", PdigV2Colors.Positive, Modifier.weight(1f))
                    RecordMetric(summary.pendingVerification, "待验证", PdigV2Colors.Warning, Modifier.weight(1f))
                }
            }

            SectionTitle("变更轨迹", "只显示已经发生或正在验证的阶段")
            if (trace.isEmpty()) {
                BoundarySurface("暂无已记录事件。没有记录不等于没有风险或没有历史。")
            } else {
                trace.forEachIndexed { index, item ->
                    RecordTraceRow(item, first = index == 0, last = index == trace.lastIndex)
                }
            }

            SectionTitle("验证状态", "done ≠ verified")
            Surface(
                modifier = Modifier.fillMaxWidth().testTag("pdig.r21.records.verification"),
                color = PdigV2Colors.PrimarySoft.copy(alpha = 0.62f),
                shape = RoundedCornerShape(VRadius.Lg),
            ) {
                Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("完成记录不会自动升级为已验证", color = PdigV2Colors.TextPrimary,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "只有显式验证结果与证据才能进入“已验证”。页面打开、时间经过、计划进入下一阶段都不能替代验证。",
                        color = PdigV2Colors.TextSecondary, fontSize = 10.sp, lineHeight = 16.sp,
                    )
                }
            }

            if (!app.emptyDemo && app.demoChanges().isNotEmpty()) {
                SectionTitle("关联计划", "继续处理")
                app.demoChanges().forEach { change ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = VTouchTarget.Min)
                            .clickable {
                                if (change.target == "change-phone") app.navigate(VScreen.CHANGE_PHONE)
                                else app.navigate(VScreen.CHANGE)
                            },
                        color = PdigV2Colors.Surface,
                        shape = RoundedCornerShape(VRadius.Lg),
                        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
                    ) {
                        Row(
                            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(change.title, color = PdigV2Colors.TextPrimary, fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold)
                                Text("计划上下文 · 不是新的记录事件", color = PdigV2Colors.TextMuted, fontSize = 9.sp)
                            }
                            Text("继续 →", color = PdigV2Colors.PrimaryText, fontSize = 10.sp)
                        }
                    }
                }
            }

            SectionTitle("记录边界", "依据")
            BoundarySurface(
                "生产版记录必须来自 Timeline / ChangePlan / Review / Verification / Evidence。" +
                    "未处理的 Attention、未来到期提醒和推测结果不会因为出现在界面里就成为历史事实。",
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun RecordMetric(value: Int, label: String, tint: Color, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value.toString(), color = tint, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = PdigV2Colors.TextMuted, fontSize = 9.sp)
    }
}

@Composable
private fun RecordTraceRow(item: UiRecordTraceItem, first: Boolean, last: Boolean) {
    val tint = when (item.state) {
        UiRecordTraceState.RECORDED_COMPLETE -> PdigV2Colors.PrimaryBright
        UiRecordTraceState.VERIFIED -> PdigV2Colors.Positive
        UiRecordTraceState.PENDING_VERIFICATION -> PdigV2Colors.Warning
    }
    val stateLabel = when (item.state) {
        UiRecordTraceState.RECORDED_COMPLETE -> "已记录完成"
        UiRecordTraceState.VERIFIED -> "已验证"
        UiRecordTraceState.PENDING_VERIFICATION -> "待验证"
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (!first) Surface(Modifier.height(11.dp).widthIn(min = 2.dp, max = 2.dp), color = PdigV2Colors.BorderStrong) {}
            Surface(
                modifier = Modifier.defaultMinSize(minWidth = 22.dp, minHeight = 22.dp),
                color = tint.copy(alpha = 0.14f),
                shape = CircleShape,
                border = BorderStroke(1.dp, tint),
            ) {}
            if (!last) Surface(Modifier.height(48.dp).widthIn(min = 2.dp, max = 2.dp), color = PdigV2Colors.BorderStrong) {}
        }
        Surface(
            modifier = Modifier.weight(1f),
            color = PdigV2Colors.Surface,
            shape = RoundedCornerShape(VRadius.Lg),
            border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
        ) {
            Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(item.title, color = PdigV2Colors.TextPrimary, fontSize = 12.sp,
                        fontWeight = FontWeight.Bold)
                    Text(stateLabel, color = tint, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(item.context, color = PdigV2Colors.TextMuted, fontSize = 9.sp)
                Text(item.detail, color = PdigV2Colors.TextSecondary, fontSize = 10.sp, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom) {
        Text(title, color = PdigV2Colors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = PdigV2Colors.TextMuted, fontSize = 9.sp)
    }
}

@Composable
private fun BoundarySurface(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PdigV2Colors.SurfaceRaised,
        shape = RoundedCornerShape(VRadius.Lg),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Text(text, Modifier.padding(13.dp), color = PdigV2Colors.TextSecondary,
            fontSize = 10.sp, lineHeight = 16.sp)
    }
}
