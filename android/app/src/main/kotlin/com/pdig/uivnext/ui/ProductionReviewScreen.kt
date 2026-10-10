package com.pdig.uivnext.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.production.ProductionReviewConsumerInbox
import com.pdig.uivnext.production.ProductionReviewConsumerItem
import com.pdig.uivnext.production.ProductionReviewConsumerKind
import com.pdig.uivnext.production.ProductionReviewDecision
import com.pdig.uivnext.production.ProductionVNextSession
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius

/**
 * Authoritative production Human Review UI.
 *
 * Every decision delegates to ProductionReviewCoordinator and replaces the
 * visible queue only with the coordinator's authoritative re-read result.
 * There is no optimistic local "accepted" state.
 */
@Composable
internal fun ProductionReviewScreen(
    session: ProductionVNextSession,
    modifier: Modifier = Modifier,
) {
    val coordinator = session.authorities?.review
    if (coordinator == null) {
        ProductionReviewUnavailable(modifier)
        return
    }

    var inbox by remember(session) { mutableStateOf(coordinator.load()) }
    var error by remember(session) { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("pdig.production-vnext.review"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "待复核",
                    color = PdigV2Colors.TextPrimary,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "关系建议、候选对象与可能发生的变化只有经过你的明确决定，才会进入已确认数据。",
                    color = PdigV2Colors.TextMuted,
                    fontSize = 12.sp,
                )
            }
        }

        error?.let { message ->
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = PdigV2Colors.Critical.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(VRadius.Md),
                ) {
                    Text(
                        message,
                        Modifier.padding(12.dp),
                        color = PdigV2Colors.Critical,
                        fontSize = 11.sp,
                    )
                }
            }
        }

        item {
            ReviewCountStrip(inbox)
        }

        if (inbox.total == 0) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = PdigV2Colors.SurfaceRaised,
                    shape = RoundedCornerShape(VRadius.Lg),
                ) {
                    Text(
                        "当前没有待复核项。空队列不代表图谱完整，也不代表没有未知关系。",
                        Modifier.padding(14.dp),
                        color = PdigV2Colors.TextSecondary,
                        fontSize = 12.sp,
                    )
                }
            }
        } else {
            if (inbox.proposals.isNotEmpty()) {
                item { ReviewSectionTitle("关系建议") }
                items(inbox.proposals, key = { "proposal:" + it.id }) { item ->
                    ReviewItemCard(item) { decision ->
                        error = null
                        try {
                            inbox = coordinator.decide(item.kind, item.id, decision)
                        } catch (t: Throwable) {
                            error = "复核操作未完成；已确认数据没有被界面自行修改。"
                        }
                    }
                }
            }

            if (inbox.candidates.isNotEmpty()) {
                item { ReviewSectionTitle("对象候选") }
                items(inbox.candidates, key = { "candidate:" + it.id }) { item ->
                    ReviewItemCard(item) { decision ->
                        error = null
                        try {
                            inbox = coordinator.decide(item.kind, item.id, decision)
                        } catch (t: Throwable) {
                            error = "复核操作未完成；候选对象仍保持未确认。"
                        }
                    }
                }
            }

            if (inbox.drifts.isNotEmpty()) {
                item { ReviewSectionTitle("可能发生的变化") }
                items(inbox.drifts, key = { "drift:" + it.id }) { item ->
                    ReviewItemCard(item) { decision ->
                        error = null
                        try {
                            inbox = coordinator.decide(item.kind, item.id, decision)
                        } catch (t: Throwable) {
                            error = "处理未完成；现有已确认数据保持不变。"
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewCountStrip(inbox: ProductionReviewConsumerInbox) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(
            inbox.proposals.size to "关系建议",
            inbox.candidates.size to "对象候选",
            inbox.drifts.size to "可能发生的变化",
        ).forEach { (count, label) ->
            Surface(
                modifier = Modifier.weight(1f),
                color = PdigV2Colors.Surface,
                shape = RoundedCornerShape(VRadius.Md),
                border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
            ) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        count.toString(),
                        color = PdigV2Colors.TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(label, color = PdigV2Colors.TextMuted, fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
private fun ReviewSectionTitle(title: String) {
    Text(
        title,
        color = PdigV2Colors.TextPrimary,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun ReviewItemCard(
    item: ProductionReviewConsumerItem,
    onDecision: (ProductionReviewDecision) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Lg),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Column(
            Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                item.title,
                color = PdigV2Colors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(item.summary, color = PdigV2Colors.TextSecondary, fontSize = 11.sp)
            Text(item.evidenceSummary, color = PdigV2Colors.TextMuted, fontSize = 10.sp)

            item.decisions.chunked(2).forEach { row ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { label ->
                        val decision = reviewDecision(item.kind, label)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onDecision(decision) }
                                .testTag("pdig.production-vnext.review.action"),
                            color = if (isPositiveReviewDecision(decision)) {
                                PdigV2Colors.PrimarySoft
                            } else {
                                PdigV2Colors.SurfaceRaised
                            },
                            shape = RoundedCornerShape(VRadius.Md),
                        ) {
                            Text(
                                label,
                                Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                color = PdigV2Colors.TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    if (row.size == 1) {
                        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private fun reviewDecision(
    kind: ProductionReviewConsumerKind,
    label: String,
): ProductionReviewDecision = when (kind) {
    ProductionReviewConsumerKind.RELATION_PROPOSAL -> when (label) {
        "确认关系" -> ProductionReviewDecision.CONFIRM_RELATION
        "拒绝" -> ProductionReviewDecision.REJECT_RELATION
        else -> error("Unsupported relation review action: $label")
    }
    ProductionReviewConsumerKind.OBJECT_CANDIDATE -> when (label) {
        "确认对象" -> ProductionReviewDecision.CONFIRM_OBJECT
        "忽略" -> ProductionReviewDecision.IGNORE_OBJECT
        else -> error("Unsupported candidate review action: $label")
    }
    ProductionReviewConsumerKind.REALITY_DRIFT -> when (label) {
        "已替换" -> ProductionReviewDecision.REPLACED
        "两个都在用" -> ProductionReviewDecision.ADDITIONAL_PATH
        "没有变化" -> ProductionReviewDecision.NO_CHANGE
        "稍后确认" -> ProductionReviewDecision.LATER
        else -> error("Unsupported drift review action: $label")
    }
}

private fun isPositiveReviewDecision(decision: ProductionReviewDecision): Boolean =
    decision in setOf(
        ProductionReviewDecision.CONFIRM_RELATION,
        ProductionReviewDecision.CONFIRM_OBJECT,
        ProductionReviewDecision.REPLACED,
        ProductionReviewDecision.ADDITIONAL_PATH,
    )

@Composable
private fun ProductionReviewUnavailable(modifier: Modifier) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "生产复核 authority 未绑定",
            color = PdigV2Colors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "不会回退到 Preview 复核数据。",
            color = PdigV2Colors.TextSecondary,
            fontSize = 12.sp,
        )
    }
}
