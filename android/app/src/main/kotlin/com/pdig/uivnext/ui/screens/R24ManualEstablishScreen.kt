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
 * R24 Manual Establish reference.
 *
 * This screen freezes the consumer design but exposes no save button because
 * production VNext does not yet own a governed manual-node creation authority.
 */
@Composable
internal fun R24ManualEstablishScreen(
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
                .testTag("pdig.r24.manual-establish"),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "手工记录",
                    color = PdigV2Colors.TextPrimary,
                    fontSize = if (breakpoint == MediaBreakpoint.COMPACT) 22.sp else 27.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "适合你明确知道、但暂时没有可导入来源的基础设施。",
                    color = PdigV2Colors.TextSecondary,
                    fontSize = 11.sp,
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth().testTag("pdig.r24.manual.truth-boundary"),
                color = PdigV2Colors.PrimarySoft.copy(alpha = 0.64f),
                shape = RoundedCornerShape(VRadius.Xl),
                border = BorderStroke(1.dp, PdigV2Colors.Primary.copy(alpha = 0.18f)),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "你确认的对象可以成为 Reality；关系仍要单独确认",
                        color = PdigV2Colors.PrimaryText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "记录“有这张卡 / 这个账户 / 这个服务”不等于自动记录它与其他对象的依赖关系。",
                        color = PdigV2Colors.TextSecondary,
                        fontSize = 10.sp,
                        lineHeight = 16.sp,
                    )
                }
            }

            ManualSectionTitle("当前 Canonical runtime creation set", "设计对齐")
            ManualTypeCard(
                "支付工具",
                "payment_instrument",
                "可表达卡片/支付工具的基础身份；详细生命周期字段仍按 R19 proposal 治理。",
                PdigV2Colors.PrimaryBright,
                "生产 authority 尚未接到 VNext",
            )
            ManualTypeCard(
                "账户",
                "account",
                "记录一个访问/控制对象；认证与恢复关系必须单独确认。",
                PdigV2Colors.Positive,
                "生产 authority 尚未接到 VNext",
            )
            ManualTypeCard(
                "服务",
                "service",
                "记录订阅、银行网银或其他依赖端点；存在对象不证明当前仍订阅。",
                PdigV2Colors.Warning,
                "生产 authority 尚未接到 VNext",
            )

            ManualSectionTitle("其他已知对象类型", "不要制造 ghost capability")
            ManualTypeCard(
                "号码 / 邮箱 / 身份",
                "identity_anchor",
                "当前 Canonical kind 过于粗，不能安全地把每个 identity_anchor 自动当成手机号或邮箱。",
                PdigV2Colors.Warning,
                "等待 governed subtype / mapping",
            )
            ManualTypeCard(
                "设备",
                "device",
                "Canonical 可存储该 kind，但当前 runtime creation set 未把它开放为通用手工创建入口。",
                PdigV2Colors.TextMuted,
                "HOLD",
            )
            ManualTypeCard(
                "会员 / 自定义",
                "membership / custom",
                "保留为存储语义；正式产品入口需先定义用途、验证与跨端行为。",
                PdigV2Colors.TextMuted,
                "HOLD",
            )

            Surface(
                modifier = Modifier.fillMaxWidth().testTag("pdig.r24.manual.preview-disabled"),
                color = PdigV2Colors.SurfaceRaised,
                shape = RoundedCornerShape(VRadius.Lg),
                border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        "当前 Preview 不提供“保存”按钮",
                        color = PdigV2Colors.TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "原因不是 UI 没画完，而是正式 VNext 还没有经过 AppContainer 暴露并测试的手工 Reality mutation authority。" +
                            "在 authority 落地前，添加一个可点击的“保存”会制造假能力。",
                        color = PdigV2Colors.TextMuted,
                        fontSize = 10.sp,
                        lineHeight = 16.sp,
                    )
                }
            }

            ManualSectionTitle("正式版提交语义", "未来 binding")
            ManualFlowLine("1", "选择受支持对象类型")
            ManualFlowLine("2", "填写最小身份字段，并明确哪些字段是未知")
            ManualFlowLine("3", "确认“这个对象存在”")
            ManualFlowLine("4", "通过 authoritative Reality transaction 创建 Node 并 bump graphRevision")
            ManualFlowLine("5", "任何依赖关系继续走单独确认 / Human Review")

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ManualTypeCard(
    title: String,
    canonical: String,
    description: String,
    tint: Color,
    state: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Lg),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, color = PdigV2Colors.TextPrimary, fontSize = 12.sp,
                    fontWeight = FontWeight.Bold)
                Surface(color = tint.copy(alpha = 0.12f), shape = RoundedCornerShape(VRadius.Sm)) {
                    Text(state, Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        color = tint, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Text(canonical, color = PdigV2Colors.TextMuted, fontSize = 8.sp)
            Text(description, color = PdigV2Colors.TextSecondary, fontSize = 10.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun ManualSectionTitle(title: String, trailing: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(title, color = PdigV2Colors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(trailing, color = PdigV2Colors.TextMuted, fontSize = 9.sp)
    }
}

@Composable
private fun ManualFlowLine(number: String, text: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(color = PdigV2Colors.PrimarySoft, shape = RoundedCornerShape(VRadius.Sm)) {
            Text(number, Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                color = PdigV2Colors.PrimaryText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        Text(text, Modifier.weight(1f), color = PdigV2Colors.TextSecondary,
            fontSize = 10.sp, lineHeight = 16.sp)
    }
}
