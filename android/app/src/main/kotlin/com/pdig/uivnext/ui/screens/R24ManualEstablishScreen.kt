package com.pdig.uivnext.ui.screens

import com.pdig.uivnext.capability.VNextCapability
import com.pdig.uivnext.capability.hasProductionAuthority

import androidx.compose.foundation.BorderStroke
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
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius

/**
 * R24 Manual Establish reference.
 *
 * This screen freezes the consumer design and intentionally exposes no Save button
 * in Preview. Production now has a governed manual-node authority/gateway, but
 * Preview stays isolated and production screen binding is a separate cutover gate.
 */
@Composable
internal fun R24ManualEstablishScreen(
    app: com.pdig.uivnext.ui.VAppState,
    breakpoint: MediaBreakpoint,
) {
    val productionAuthorityReady = hasProductionAuthority(VNextCapability.MANUAL_CREATE)
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
                        "你确认的对象会进入正式数据；关系仍要单独确认",
                        color = PdigV2Colors.PrimaryText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "记录“有这个对象”不等于自动记录它与其他对象的依赖关系；手机号/邮箱的类型与具体标识也不会自动变成恢复方式。",
                        color = PdigV2Colors.TextSecondary,
                        fontSize = 10.sp,
                        lineHeight = 16.sp,
                    )
                }
            }

            ManualSectionTitle("当前可直接记录的对象", "按正式规则")
            ManualTypeCard(
                "支付工具",
                "payment_instrument",
                "可表达卡片/支付工具的基础身份；详细生命周期字段仍按正式提案流程处理。",
                PdigV2Colors.PrimaryBright,
                if (productionAuthorityReady) "可正式记录" else "正式记录能力待接入",
            )
            ManualTypeCard(
                "账户",
                "account",
                "记录一个访问/控制对象；认证与恢复关系必须单独确认。",
                PdigV2Colors.Positive,
                if (productionAuthorityReady) "可正式记录" else "正式记录能力待接入",
            )
            ManualTypeCard(
                "服务",
                "service",
                "记录订阅、银行网银或其他依赖端点；存在对象不证明当前仍订阅。",
                PdigV2Colors.Warning,
                if (productionAuthorityReady) "可正式记录" else "正式记录能力待接入",
            )

            ManualSectionTitle("可记录的身份对象", "手机号 / 邮箱")
            ManualTypeCard(
                "手机号 / 邮箱",
                "专用身份页",
                "正式生产能力已支持在一次确认中建立对象、手机号/邮箱类型与具体标识；Preview 仍保持只读，不会写入正式数据。",
                PdigV2Colors.PrimaryBright,
                if (productionAuthorityReady) "可正式记录 · Preview 只读"
                else "正式记录能力待接入 · Preview 只读",
            )

            ManualSectionTitle("其他已知对象类型", "不要制造 ghost capability")
            ManualTypeCard(
                "其他身份",
                "identity_anchor(other_identity / unknown)",
                "未确认手机号/邮箱 subtype 与 identifier 的通用身份仍保持通用对象，不会靠名称、格式或关系猜测。",
                PdigV2Colors.Warning,
                "HOLD",
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
                modifier = Modifier.fillMaxWidth()
                    .testTag("pdig.r25.manual-relation.entry")
                    .clickable { app.navigate(VScreen.MANUAL_RELATION) },
                color = PdigV2Colors.Surface,
                shape = RoundedCornerShape(VRadius.Lg),
                border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
            ) {
                Row(
                    Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("已经有对象？设计一条关系", color = PdigV2Colors.TextPrimary,
                            fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("From → Relation → To；关系确认与对象创建保持分离",
                            color = PdigV2Colors.TextMuted, fontSize = 9.sp)
                    }
                    Text("查看 →", color = PdigV2Colors.PrimaryText, fontSize = 10.sp)
                }
            }

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
                        if (productionAuthorityReady)
                            "正式记录能力已存在，但 Preview 仍保持只读，不能把参考环境当成正式数据。"
                        else
                            "原因不是界面没画完，而是正式 VNext 还没有经过完整验证的对象建立能力。" +
                                "在能力落地前，添加一个可点击的“保存”会制造假能力。",
                        color = PdigV2Colors.TextMuted,
                        fontSize = 10.sp,
                        lineHeight = 16.sp,
                    )
                }
            }

            ManualSectionTitle("正式版提交语义", "production binding")
            ManualFlowLine("1", "选择对象类型")
            ManualFlowLine("2", "填写最少信息；手机号/邮箱同时确认类型与具体标识")
            ManualFlowLine("3", "确认“这个对象存在”以及你明确填写的信息")
            ManualFlowLine("4", "通过正式数据事务创建对象并更新数据版本")
            ManualFlowLine("5", "任何依赖关系继续单独确认与复核")

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
