package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
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
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.demo.cardImpactLens
import com.pdig.uivnext.model.ChangeStage
import com.pdig.uivnext.model.MediaBreakpoint
import com.pdig.uivnext.model.UiVNextCard
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VTouchTarget
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.ContinuityRail
import com.pdig.uivnext.ui.components.ObjectImpactLens

/**
 * R21 payment-card replacement reference.
 *
 * The production scenario already exists as replace_payment_card. This screen
 * therefore exposes the continuity shape, but Preview never mutates Reality.
 * Current / Transition / After remain distinct and After is plan projection only.
 */
@Composable
internal fun R21ChangeCardScreen(
    app: VAppState,
    breakpoint: MediaBreakpoint,
) {
    val oldCard = app.selectedCardId?.let(UiVNextDemoFixture::cardById)
    if (oldCard == null || app.emptyDemo) {
        R21ChangeCardMissing("请先选择一张已记录卡片，再分析更换影响。")
        return
    }

    val replacement = app.selectedReplacementCardId?.let(UiVNextDemoFixture::cardById)
    val services = UiVNextDemoFixture.servicesForCard(oldCard.id)
    val candidates = UiVNextDemoFixture.cards.filter {
        it.id != oldCard.id && it.status == "active"
    }
    val stages = cardChangeStages(
        projection = app.cardChangeProjection,
        hasReplacement = replacement != null,
    )
    val maxWidth = when (breakpoint) {
        MediaBreakpoint.COMPACT -> 620.dp
        MediaBreakpoint.MEDIUM -> 820.dp
        MediaBreakpoint.EXPANDED -> 1100.dp
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
                .testTag("pdig.r21.change-card"),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "更换银行卡",
                    color = PdigV2Colors.TextPrimary,
                    fontSize = if (breakpoint == MediaBreakpoint.COMPACT) 21.sp else 26.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "先核对支付依赖，再迁移，最后用真实支付/账单证据验证。点击不会自动完成计划。",
                    color = PdigV2Colors.TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 17.sp,
                )
            }

            CardChangeProjectionSelector(
                projection = app.cardChangeProjection,
                onProjection = { app.cardChangeProjection = it },
            )

            if (breakpoint == MediaBreakpoint.COMPACT) {
                CompactCardChangeFlow(app, oldCard, replacement, services)
            } else {
                WideCardChangeFlow(app, oldCard, replacement, services)
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = PdigV2Colors.Surface,
                shape = RoundedCornerShape(VRadius.Xl),
                border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
            ) {
                Column(
                    Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("连续性步骤", color = PdigV2Colors.TextPrimary, fontSize = 14.sp,
                        fontWeight = FontWeight.Bold)
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    ) {
                        ContinuityRail(stages = stages)
                    }
                    Text(
                        when (app.cardChangeProjection) {
                            "after" -> "完成后是计划投影：不表示支付关系已经迁移，也不表示新卡已经验证可用。"
                            "transition" -> if (replacement == null)
                                "尚未选择替代卡片，迁移阶段保持阻断。"
                            else
                                "迁移中的关系仍需逐项完成，并在最后阶段用实际证据验证。"
                            else -> "当前视图只展示旧卡与已记录关系；尚未创建或执行真实生产计划。"
                        },
                        color = PdigV2Colors.TextMuted,
                        fontSize = 10.sp,
                        lineHeight = 16.sp,
                    )
                }
            }

            ObjectImpactLens(impact = cardImpactLens(oldCard.id))

            Text(
                "选择替代卡片",
                color = PdigV2Colors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "这里只列出其他已记录且处于 active 的卡片；它们是可选对象，不代表系统推荐或独立备用路径。",
                color = PdigV2Colors.TextMuted,
                fontSize = 10.sp,
            )
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .testTag("pdig.r21.change-card.replacements"),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                candidates.forEach { candidate ->
                    val candidateProfile = app.savedPresentationProfile("card", candidate.id)
                    ReplacementChoice(
                        card = candidate,
                        profile = candidateProfile,
                        selected = candidate.id == replacement?.id,
                        privacyMask = app.privacyMask ||
                            (candidateProfile?.maskSensitive == true),
                    ) {
                        app.chooseReplacementCard(
                            if (candidate.id == replacement?.id) null else candidate.id,
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = PdigV2Colors.PrimarySoft.copy(alpha = 0.58f),
                shape = RoundedCornerShape(VRadius.Lg),
            ) {
                Text(
                    "生产版必须复用 replace_payment_card → ChangePlan → completeAction → verifyAction。" +
                        "参考 UI 不在本地切换 done/verified，也不会把候选替代卡写入 Canonical。",
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
private fun CompactCardChangeFlow(
    app: VAppState,
    oldCard: UiVNextCard,
    replacement: UiVNextCard?,
    services: List<com.pdig.uivnext.model.UiVNextService>,
) {
    ChangeCardNode("当前卡片", oldCard, app, Modifier.fillMaxWidth())
    Text("↓", color = PdigV2Colors.PrimaryBright, fontSize = 18.sp,
        modifier = Modifier.fillMaxWidth())
    ChangeCardServices(services, app.cardChangeProjection)
    Text("↓", color = PdigV2Colors.PrimaryBright, fontSize = 18.sp,
        modifier = Modifier.fillMaxWidth())
    if (replacement != null) {
        ChangeCardNode("替代卡片 · 计划目标", replacement, app, Modifier.fillMaxWidth())
    } else {
        ChangeCardEmptyTarget(Modifier.fillMaxWidth())
    }
}

@Composable
private fun WideCardChangeFlow(
    app: VAppState,
    oldCard: UiVNextCard,
    replacement: UiVNextCard?,
    services: List<com.pdig.uivnext.model.UiVNextService>,
) {
    Row(
        Modifier.fillMaxWidth().testTag("pdig.r21.change-card.flow"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChangeCardNode("当前卡片", oldCard, app, Modifier.weight(0.29f))
        Text("→", color = PdigV2Colors.PrimaryBright, fontSize = 18.sp)
        ChangeCardServices(services, app.cardChangeProjection, Modifier.weight(0.34f))
        Text("→", color = PdigV2Colors.PrimaryBright, fontSize = 18.sp)
        if (replacement != null) {
            ChangeCardNode("替代卡片 · 计划目标", replacement, app, Modifier.weight(0.29f))
        } else {
            ChangeCardEmptyTarget(Modifier.weight(0.29f))
        }
    }
}

@Composable
private fun ChangeCardNode(
    label: String,
    card: UiVNextCard,
    app: VAppState,
    modifier: Modifier = Modifier,
) {
    val profile = app.savedPresentationProfile("card", card.id)
    Surface(
        modifier = modifier,
        color = PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Xl),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Column(
            Modifier.padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(label, color = PdigV2Colors.TextMuted, fontSize = 9.sp)
            R19PresentedCardThumbnail(
                card = card,
                profile = profile,
                privacyMask = app.privacyMask || (profile?.maskSensitive == true),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(card.nickname, color = PdigV2Colors.TextPrimary, fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun ChangeCardServices(
    services: List<com.pdig.uivnext.model.UiVNextService>,
    projection: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Xl),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("已记录支付关系", color = PdigV2Colors.TextPrimary, fontSize = 12.sp,
                fontWeight = FontWeight.Bold)
            if (services.isEmpty()) {
                Text("没有已记录关系；这不代表不存在依赖。",
                    color = PdigV2Colors.TextMuted, fontSize = 10.sp)
            } else {
                services.forEach { service ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(service.name, color = PdigV2Colors.TextSecondary,
                            fontSize = 10.sp, maxLines = 1)
                        Text(
                            cardServiceProjectionLabel(projection),
                            color = when (projection) {
                                "after" -> PdigV2Colors.PrimaryBright
                                "transition" -> PdigV2Colors.Warning
                                else -> PdigV2Colors.TextMuted
                            },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChangeCardEmptyTarget(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.defaultMinSize(minHeight = 110.dp),
        color = PdigV2Colors.SurfaceRaised,
        shape = RoundedCornerShape(VRadius.Xl),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Box(Modifier.fillMaxSize().padding(14.dp), contentAlignment = Alignment.Center) {
            Text(
                "尚未选择替代卡片\n替代对象是可选计划输入",
                color = PdigV2Colors.TextMuted,
                fontSize = 10.sp,
                lineHeight = 16.sp,
            )
        }
    }
}

@Composable
private fun ReplacementChoice(
    card: UiVNextCard,
    profile: com.pdig.uivnext.model.PresentationProfile?,
    selected: Boolean,
    privacyMask: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .width(174.dp)
            .defaultMinSize(minHeight = VTouchTarget.Min)
            .clickable(onClick = onClick),
        color = if (selected) PdigV2Colors.PrimarySoft else PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Lg),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.BorderSubtle,
        ),
    ) {
        Column(Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            R19PresentedCardThumbnail(
                card = card,
                profile = profile,
                privacyMask = privacyMask,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(card.nickname, color = PdigV2Colors.TextPrimary, fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(if (selected) "已选计划目标" else "选择为计划目标",
                color = if (selected) PdigV2Colors.PrimaryText else PdigV2Colors.TextMuted,
                fontSize = 9.sp)
        }
    }
}

@Composable
private fun CardChangeProjectionSelector(
    projection: String,
    onProjection: (String) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().testTag("pdig.r21.change-card.projections"),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        listOf(
            "current" to "当前",
            "transition" to "迁移中",
            "after" to "完成后（计划）",
        ).forEach { (key, label) ->
            val selected = key == projection
            Surface(
                modifier = Modifier.weight(1f).defaultMinSize(minHeight = VTouchTarget.Min)
                    .clickable { onProjection(key) },
                color = if (selected) PdigV2Colors.PrimarySoft else PdigV2Colors.Surface,
                shape = RoundedCornerShape(VRadius.Md),
                border = BorderStroke(
                    1.dp,
                    if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.BorderSubtle,
                ),
            ) {
                Box(Modifier.padding(horizontal = 8.dp, vertical = 11.dp),
                    contentAlignment = Alignment.Center) {
                    Text(label, color = if (selected) PdigV2Colors.PrimaryText else PdigV2Colors.TextMuted,
                        fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}

internal fun cardChangeStages(
    projection: String,
    hasReplacement: Boolean,
): List<ChangeStage> = when (projection) {
    "transition" -> listOf(
        ChangeStage(1, "review-payment-dependencies", "completed"),
        if (hasReplacement) {
            ChangeStage(2, "migrate-payment-relations", "verifying")
        } else {
            ChangeStage(2, "migrate-payment-relations", "blocked",
                blockReason = "先选择替代卡片，才能进入迁移阶段")
        },
        ChangeStage(3, "verify-payment-path", "blocked",
            blockReason = "支付关系迁移完成并取得实际验证证据后，才能确认新路径"),
    )
    "after" -> listOf(
        ChangeStage(1, "review-payment-dependencies", "plan"),
        ChangeStage(2, "migrate-payment-relations", "plan"),
        ChangeStage(3, "verify-payment-path", "plan"),
    )
    else -> listOf(
        ChangeStage(1, "review-payment-dependencies", "not_started"),
        ChangeStage(2, "migrate-payment-relations", "not_started"),
        ChangeStage(3, "verify-payment-path", "not_started"),
    )
}

internal fun cardServiceProjectionLabel(projection: String): String = when (projection) {
    "transition" -> "待迁移"
    "after" -> "计划迁移"
    else -> "当前依赖"
}

@Composable
private fun R21ChangeCardMissing(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = PdigV2Colors.TextMuted, fontSize = 12.sp)
    }
}
