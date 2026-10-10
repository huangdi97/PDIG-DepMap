package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.pdig.uivnext.model.MediaBreakpoint
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius

/**
 * R25 Manual Relationship reference.
 *
 * Canonical has DependencyOrigin.MANUAL, but production VNext does not yet expose
 * an authoritative create-dependency API. The screen therefore freezes consumer
 * semantics without a fake Save action.
 */
@Composable
internal fun R25ManualRelationshipScreen(
    breakpoint: MediaBreakpoint,
) {
    val maxWidth = when (breakpoint) {
        MediaBreakpoint.COMPACT -> 640.dp
        MediaBreakpoint.MEDIUM -> 820.dp
        MediaBreakpoint.EXPANDED -> 940.dp
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
                .testTag("pdig.r25.manual-relationship"),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "手工记录关系",
                    color = PdigV2Colors.TextPrimary,
                    fontSize = if (breakpoint == MediaBreakpoint.COMPACT) 22.sp else 27.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "只在你明确知道两个已记录对象之间存在关系时使用。",
                    color = PdigV2Colors.TextSecondary,
                    fontSize = 11.sp,
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth().testTag("pdig.r25.relationship.truth-boundary"),
                color = PdigV2Colors.PrimarySoft.copy(alpha = 0.64f),
                shape = RoundedCornerShape(VRadius.Xl),
                border = BorderStroke(1.dp, PdigV2Colors.Primary.copy(alpha = 0.18f)),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "关系是 Reality，不是标签",
                        color = PdigV2Colors.PrimaryText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "确认一条关系后，它会进入依赖图并影响 Impact / Change。" +
                            "因此方向、关系类型、能力和关键性都必须明确，不允许 UI 自动猜。",
                        color = PdigV2Colors.TextSecondary,
                        fontSize = 10.sp,
                        lineHeight = 16.sp,
                    )
                }
            }

            RelationStep("1", "从哪个对象开始？", "From 必须是已确认 Reality 对象；不能选 Proposal/Candidate。")
            RelationStep("2", "关系是什么？", "只显示当前 runtime relation registry 支持的关系。")
            RelationStep("3", "指向哪个对象？", "To 必须是已确认 Reality 对象；方向必须与 relation 语义一致。")
            RelationStep("4", "承担什么能力？", "payment / access / authentication / recovery / communication。")
            RelationStep("5", "这是必须路径吗？", "默认 unknown；required 只能由用户显式确认，机器不能设置。")

            ManualRelationExample()

            ManualRelationSection("当前 runtime relation")
            RelationVocabularyRow("付款来源", "funding_source", "payment")
            RelationVocabularyRow("支付绑定", "merchant_agreement", "payment")
            RelationVocabularyRow("恢复", "recovers", "recovery")
            RelationVocabularyRow("登录验证", "authenticates", "authentication")
            RelationVocabularyRow("控制", "controls", "access")

            ManualRelationSection("存储允许但当前 runtime 不开放")
            RelationVocabularyRow("验证（legacy/future）", "verifies", "HOLD")
            RelationVocabularyRow("绑定（legacy/future）", "bound_to", "HOLD")

            Surface(
                modifier = Modifier.fillMaxWidth().testTag("pdig.r25.relationship.preview-disabled"),
                color = PdigV2Colors.SurfaceRaised,
                shape = RoundedCornerShape(VRadius.Lg),
                border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        "当前 Preview 不提供“确认关系”按钮",
                        color = PdigV2Colors.TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "正式 VNext 仍缺少 AppContainer-facing 的 manual Dependency authority。" +
                            "在 authority 和 revision/conformance 测试完成之前，本页只冻结交互与文案。",
                        color = PdigV2Colors.TextMuted,
                        fontSize = 10.sp,
                        lineHeight = 16.sp,
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = PdigV2Colors.Warning.copy(alpha = 0.09f),
                shape = RoundedCornerShape(VRadius.Lg),
                border = BorderStroke(1.dp, PdigV2Colors.Warning.copy(alpha = 0.22f)),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("不会在这里判断“独立备用路径”", color = PdigV2Colors.TextPrimary,
                        fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "两条边不等于两条独立路径。FailureDomain / RecoveryCycle / ProviderPolicy " +
                            "仍由 Continuity engine 判断，手工关系页不做 degree-count 捷径。",
                        color = PdigV2Colors.TextSecondary,
                        fontSize = 10.sp,
                        lineHeight = 16.sp,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ManualRelationExample() {
    ManualRelationSection("示例 · 仅用于理解方向")
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Lg),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RelationPill("支付工具")
                Text("支付绑定 →", color = PdigV2Colors.PrimaryText, fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold)
                RelationPill("服务")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RelationMeta("能力", "payment", Modifier.weight(1f))
                RelationMeta("关键性", "unknown", Modifier.weight(1f))
                RelationMeta("来源", "manual", Modifier.weight(1f))
            }
            Text(
                "unknown 表示尚未确认它是不是必须路径，不表示“不重要”。",
                color = PdigV2Colors.TextMuted,
                fontSize = 9.sp,
            )
        }
    }
}

@Composable
private fun RelationStep(number: String, title: String, detail: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Md),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Row(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(color = PdigV2Colors.PrimarySoft, shape = RoundedCornerShape(VRadius.Sm)) {
                Text(number, Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                    color = PdigV2Colors.PrimaryText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = PdigV2Colors.TextPrimary, fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold)
                Text(detail, color = PdigV2Colors.TextMuted, fontSize = 9.sp, lineHeight = 14.sp)
            }
        }
    }
}

@Composable
private fun ManualRelationSection(title: String) {
    Text(title, color = PdigV2Colors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun RelationVocabularyRow(label: String, wire: String, capability: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = PdigV2Colors.TextPrimary, fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold)
            Text(wire, color = PdigV2Colors.TextMuted, fontSize = 8.sp)
        }
        Text(capability, color = if (capability == "HOLD") PdigV2Colors.Warning else PdigV2Colors.PrimaryText,
            fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RelationPill(text: String) {
    Surface(
        color = PdigV2Colors.SurfaceRaised,
        shape = RoundedCornerShape(VRadius.Md),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            color = PdigV2Colors.TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RelationMeta(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = PdigV2Colors.SurfaceRaised,
        shape = RoundedCornerShape(VRadius.Sm)) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = PdigV2Colors.TextMuted, fontSize = 8.sp)
            Text(value, color = PdigV2Colors.TextPrimary, fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold)
        }
    }
}
