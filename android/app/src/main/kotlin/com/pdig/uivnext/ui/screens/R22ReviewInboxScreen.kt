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
import com.pdig.uivnext.demo.UI_REVIEW_REFERENCE_ITEMS
import com.pdig.uivnext.demo.UiReviewReferenceItem
import com.pdig.uivnext.demo.UiReviewReferenceKind
import com.pdig.uivnext.demo.reviewReferenceSummary
import com.pdig.uivnext.model.MediaBreakpoint
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VTouchTarget
import com.pdig.uivnext.ui.VAppState

/**
 * R22 Human Review Inbox.
 *
 * Preview renders isolated NOT-CONFIRMED reference items. It intentionally does
 * not mutate Reality. Production acceptance/rejection will bind the already
 * existing VNextReviewActionGateway.
 */
@Composable
internal fun R22ReviewInboxScreen(
    app: VAppState,
    breakpoint: MediaBreakpoint,
) {
    val items = if (app.emptyDemo) emptyList() else UI_REVIEW_REFERENCE_ITEMS
    val summary = reviewReferenceSummary(items)
    val maxWidth = when (breakpoint) {
        MediaBreakpoint.COMPACT -> 640.dp
        MediaBreakpoint.MEDIUM -> 820.dp
        MediaBreakpoint.EXPANDED -> 980.dp
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
                .testTag("pdig.r22.review-inbox"),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "待复核",
                    color = PdigV2Colors.TextPrimary,
                    fontSize = if (breakpoint == MediaBreakpoint.COMPACT) 22.sp else 27.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "系统可以发现和建议；只有你的明确确认才能改变已记录现实。",
                    color = PdigV2Colors.TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 17.sp,
                )
            }

            ReviewTruthBoundary()

            Surface(
                modifier = Modifier.fillMaxWidth().testTag("pdig.r22.review.summary"),
                color = PdigV2Colors.Surface,
                shape = RoundedCornerShape(VRadius.Xl),
                border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ReviewMetric(summary.proposals, "关系建议", PdigV2Colors.PrimaryBright, Modifier.weight(1f))
                    ReviewMetric(summary.candidates, "对象候选", PdigV2Colors.Warning, Modifier.weight(1f))
                    ReviewMetric(summary.drifts, "现实漂移", PdigV2Colors.Critical, Modifier.weight(1f))
                }
            }

            if (items.isEmpty()) {
                ReviewBoundaryCard("当前没有待复核的参考项。没有待复核项不代表所有关系都完整或最新。")
            } else {
                ReviewKindSection(
                    title = "关系建议",
                    subtitle = "Proposal → 只有确认后才进入 Reality",
                    kind = UiReviewReferenceKind.DEPENDENCY_PROPOSAL,
                    items = items,
                )
                ReviewKindSection(
                    title = "对象候选",
                    subtitle = "Candidate → 只有确认后才创建对象",
                    kind = UiReviewReferenceKind.DISCOVERY_CANDIDATE,
                    items = items,
                )
                ReviewKindSection(
                    title = "现实漂移",
                    subtitle = "Drift → 先判断现实如何变化，再决定是否修改图",
                    kind = UiReviewReferenceKind.REALITY_DRIFT,
                    items = items,
                )
            }

            ReviewSectionTitle("正式工作区的决策边界", "Preview 不执行")
            ReviewBoundaryCard(
                "正式版会把“确认 / 拒绝 / 忽略 / 已替换 / 两个都在用 / 没有变化”交给现有生产 authority。" +
                    "本预览页不会因为点击、滚动或高置信度自动修改任何节点、依赖或 graphRevision。",
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = VTouchTarget.Min)
                    .clickable { app.navigate(VScreen.SOURCES) }
                    .testTag("pdig.r22.review.sources"),
                color = PdigV2Colors.SurfaceRaised,
                shape = RoundedCornerShape(VRadius.Lg),
                border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("查看数据与来源", color = PdigV2Colors.TextPrimary,
                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("理解来源覆盖与事实边界", color = PdigV2Colors.TextMuted, fontSize = 9.sp)
                    }
                    Text("查看 →", color = PdigV2Colors.PrimaryText, fontSize = 10.sp)
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ReviewTruthBoundary() {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("pdig.r22.review.truth-boundary"),
        color = PdigV2Colors.PrimarySoft.copy(alpha = 0.65f),
        shape = RoundedCornerShape(VRadius.Xl),
        border = BorderStroke(1.dp, PdigV2Colors.Primary.copy(alpha = 0.18f)),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("发现 ≠ 事实", color = PdigV2Colors.PrimaryText, fontSize = 14.sp,
                fontWeight = FontWeight.Bold)
            Text(
                "Observation / Proposal / Candidate / Drift 都不进入已确认 Reality。" +
                    "“机器很确定”也不能跳过人工确认。",
                color = PdigV2Colors.TextSecondary,
                fontSize = 10.sp,
                lineHeight = 16.sp,
            )
        }
    }
}

@Composable
private fun ReviewKindSection(
    title: String,
    subtitle: String,
    kind: UiReviewReferenceKind,
    items: List<UiReviewReferenceItem>,
) {
    val scoped = items.filter { it.kind == kind }
    if (scoped.isEmpty()) return

    ReviewSectionTitle(title, subtitle)
    scoped.forEach { item -> ReviewItemCard(item) }
}

@Composable
private fun ReviewItemCard(item: UiReviewReferenceItem) {
    val tint = when (item.kind) {
        UiReviewReferenceKind.DEPENDENCY_PROPOSAL -> PdigV2Colors.PrimaryBright
        UiReviewReferenceKind.DISCOVERY_CANDIDATE -> PdigV2Colors.Warning
        UiReviewReferenceKind.REALITY_DRIFT -> PdigV2Colors.Critical
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pdig.r22.review.item.${item.id}"),
        color = PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Lg),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    item.title,
                    modifier = Modifier.weight(1f),
                    color = PdigV2Colors.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
                item.confidenceLabel?.let {
                    Surface(color = tint.copy(alpha = 0.12f), shape = RoundedCornerShape(VRadius.Sm)) {
                        Text(
                            it,
                            Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            color = tint,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            Text(item.summary, color = PdigV2Colors.TextSecondary, fontSize = 10.sp, lineHeight = 16.sp)
            ReviewFact("依据", item.evidence)
            ReviewFact("确认后会发生什么", item.consequence)

            Text(
                when (item.kind) {
                    UiReviewReferenceKind.DEPENDENCY_PROPOSAL ->
                        "正式决策：确认关系 / 拒绝"
                    UiReviewReferenceKind.DISCOVERY_CANDIDATE ->
                        "正式决策：确认对象 / 忽略"
                    UiReviewReferenceKind.REALITY_DRIFT ->
                        "正式决策：已替换 / 两个都在用 / 没有变化 / 稍后确认"
                },
                color = PdigV2Colors.TextMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun ReviewFact(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = PdigV2Colors.TextMuted, fontSize = 9.sp)
        Text(value, color = PdigV2Colors.TextPrimary, fontSize = 10.sp, lineHeight = 15.sp)
    }
}

@Composable
private fun ReviewMetric(value: Int, label: String, tint: Color, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value.toString(), color = tint, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = PdigV2Colors.TextMuted, fontSize = 9.sp)
    }
}

@Composable
private fun ReviewSectionTitle(title: String, subtitle: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(title, color = PdigV2Colors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = PdigV2Colors.TextMuted, fontSize = 9.sp)
    }
}

@Composable
private fun ReviewBoundaryCard(text: String) {
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
