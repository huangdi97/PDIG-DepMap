package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.demo.demoAttention
import com.pdig.uivnext.demo.demoCards
import com.pdig.uivnext.demo.demoChanges
import com.pdig.uivnext.demo.demoNumbers
import com.pdig.uivnext.demo.demoRegions
import com.pdig.uivnext.demo.demoUpcoming
import com.pdig.uivnext.model.MediaBreakpoint
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VSpacing
import com.pdig.uivnext.theme.VTouchTarget
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.r9.R10MeScreen

/**
 * Medium keeps the same consumer information order as phone but gives it a
 * bounded reading column beside the compact rail. It avoids a 2-column layout
 * in the narrow 600–839dp band.
 */
@Composable
internal fun R19MediumMeScreen(app: VAppState, onHelp: (() -> Unit)? = null) {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .widthIn(max = 620.dp)
                .testTag("pdig.r19.me.medium"),
        ) {
            R10MeScreen(app, onHelp)
        }
    }
}

/**
 * R19 Medium / Expanded "Me" workspace.
 *
 * "我" is an intentional primary destination, so wider Android windows get a
 * first-class workspace instead of stretching the compact consumer feed.
 * Every displayed count or identity comes from the active fixture/read model;
 * missing records remain missing and no safety verdict is inferred.
 */
@Composable
internal fun R19AdaptiveMeScreen(
    app: VAppState,
    breakpoint: MediaBreakpoint,
    onHelp: (() -> Unit)? = null,
) {
    val regions = app.demoRegions()
    val cards = app.demoCards()
    val numbers = app.demoNumbers()
    val attention = app.demoAttention()
    val changes = app.demoChanges()
    val upcoming = app.demoUpcoming()
    val accounts = if (app.emptyDemo) emptyList() else UiVNextDemoFixture.accounts
    val services = if (app.emptyDemo) emptyList() else UiVNextDemoFixture.services
    val emails = if (app.emptyDemo) emptyList() else UiVNextDemoFixture.emails
    val devices = if (app.emptyDemo) emptyList() else UiVNextDemoFixture.devices

    val leadNumber = numbers.firstOrNull { it.role == "primary" } ?: numbers.firstOrNull()
    val leadEmail = emails.firstOrNull { it.uniqueRecoveryPath == true }
        ?: emails.firstOrNull { it.recoveryOnly }
        ?: emails.firstOrNull()
    val leadDevice = devices.firstOrNull { "主设备" in it.roles } ?: devices.firstOrNull()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(if (breakpoint == MediaBreakpoint.EXPANDED) 28.dp else 22.dp)
            .testTag("pdig.r19.me.workspace"),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = PdigV2Colors.Surface,
            shape = RoundedCornerShape(VRadius.Xl),
            border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
        ) {
            Row(
                Modifier.padding(horizontal = 22.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Surface(
                    modifier = Modifier.size(58.dp),
                    color = PdigV2Colors.PrimarySoft,
                    shape = CircleShape,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            tint = PdigV2Colors.PrimaryBright,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        "我的数字生活",
                        color = PdigV2Colors.TextPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "个人数字基础设施工作区 · ${regions.size} 个已记录地区",
                        color = PdigV2Colors.TextSecondary,
                        fontSize = 13.sp,
                    )
                }
                Surface(
                    color = PdigV2Colors.PrimarySoft,
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Text(
                        "本机预览",
                        Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        color = PdigV2Colors.PrimaryText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Surface(
                modifier = Modifier
                    .weight(if (breakpoint == MediaBreakpoint.EXPANDED) 1.18f else 1f)
                    .testTag("pdig.r19.me.identity"),
                color = PdigV2Colors.Surface,
                shape = RoundedCornerShape(VRadius.Xl),
                border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
            ) {
                Column(
                    Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MeSectionHeading("关键身份", "只展示已记录对象")
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 112.dp)
                            .clickable {
                                if (leadNumber != null) app.openNumber(leadNumber.id)
                                else app.navigate(VScreen.NUMBERS)
                            },
                        color = Color(0xFF16376D),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("我的主号码", color = Color(0xFFC7E3FF), fontSize = 11.sp)
                            Text(
                                leadNumber?.let {
                                    app.numberDisplayNameForScreen(it.id, it.maskedNumber)
                                } ?: "尚未记录",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                leadNumber?.let {
                                    when {
                                        it.uniqueRecoveryPath == true ->
                                            "${it.carrier} · 已确认唯一恢复路径"
                                        it.recoveryOnly ->
                                            "${it.carrier} · 恢复用途 · 唯一性未知"
                                        else -> "${it.carrier} · 查看已记录依赖"
                                    }
                                } ?: "添加或核对你的通信身份",
                                color = Color(0xFFBDDAF7),
                                fontSize = 10.sp,
                            )
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        MeIdentityMini(
                            title = "恢复邮箱",
                            value = leadEmail?.maskedAddress ?: "尚未记录",
                            hint = when {
                                leadEmail?.uniqueRecoveryPath == true -> "唯一恢复 · 需替代路径"
                                leadEmail?.recoveryOnly == true -> "恢复用途 · 唯一性未知"
                                else -> "查看已记录邮箱"
                            },
                            modifier = Modifier.weight(1f),
                        ) { app.navigate(VScreen.EMAILS) }
                        MeIdentityMini(
                            title = "常用设备",
                            value = leadDevice?.name ?: "尚未记录",
                            hint = leadDevice?.trust ?: "查看已记录设备",
                            modifier = Modifier.weight(1f),
                        ) { app.navigate(VScreen.DEVICES) }
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .testTag("pdig.r19.me.continuity"),
                color = PdigV2Colors.Surface,
                shape = RoundedCornerShape(VRadius.Xl),
                border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
            ) {
                Column(
                    Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MeSectionHeading("连续性概览", "未知关系不计入结论")
                    MeContinuityRow(
                        value = changes.size.toString(),
                        label = "进行中的变更",
                        detail = changes.firstOrNull()?.title ?: "暂无已记录变更",
                        tint = PdigV2Colors.PrimaryBright,
                    ) { app.navigate(VScreen.CHANGE) }
                    MeContinuityRow(
                        value = attention.size.toString(),
                        label = "待处理",
                        detail = attention.firstOrNull()?.title ?: "暂无已记录待处理事项",
                        tint = if (attention.isEmpty()) PdigV2Colors.TextMuted else PdigV2Colors.Critical,
                    ) { app.navigate(VScreen.RECORDS) }
                    MeContinuityRow(
                        value = upcoming.size.toString(),
                        label = "时间节点",
                        detail = upcoming.firstOrNull()?.title ?: "暂无已记录时间节点",
                        tint = PdigV2Colors.Warning,
                    ) { app.navigate(VScreen.RECORDS) }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = PdigV2Colors.Surface,
            shape = RoundedCornerShape(VRadius.Xl),
            border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
        ) {
            Column(
                Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MeSectionHeading("我的基础设施", "进入对象管理")
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MeMetric(cards.size, "银行卡", Modifier.weight(1f)) { app.navigate(VScreen.CARDS) }
                    MeMetric(numbers.size, "号码", Modifier.weight(1f)) { app.navigate(VScreen.NUMBERS) }
                    MeMetric(accounts.size, "账户", Modifier.weight(1f)) { app.navigate(VScreen.ACCOUNTS) }
                    MeMetric(services.size, "服务", Modifier.weight(1f)) { app.navigate(VScreen.SERVICES) }
                    MeMetric(regions.size, "地区", Modifier.weight(1f)) { app.navigate(VScreen.OVERVIEW) }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Surface(
                modifier = Modifier.weight(1.16f),
                color = PdigV2Colors.Surface,
                shape = RoundedCornerShape(VRadius.Xl),
                border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
            ) {
                Column(
                    Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MeSectionHeading("全球分布", "地区只是上下文，不等于风险结论")
                    if (regions.isEmpty()) {
                        Text(
                            "尚无已记录地区。",
                            color = PdigV2Colors.TextSecondary,
                            fontSize = 12.sp,
                        )
                    } else {
                        regions.take(6).forEach { region ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = VTouchTarget.Min)
                                    .clickable {
                                        app.selectRegion(region.regionCode)
                                        app.navigate(VScreen.OVERVIEW)
                                    }
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(meRegionFlag(region.regionCode), fontSize = 20.sp)
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        region.displayName,
                                        color = PdigV2Colors.TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        "${region.cardCount} 张卡 · ${region.phoneCount} 个号码 · ${region.serviceCount} 项服务",
                                        color = PdigV2Colors.TextMuted,
                                        fontSize = 10.sp,
                                    )
                                }
                                Icon(
                                    Icons.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = PdigV2Colors.TextMuted,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.weight(0.84f),
                color = PdigV2Colors.Surface,
                shape = RoundedCornerShape(VRadius.Xl),
                border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
            ) {
                Column(
                    Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MeSectionHeading("我的管理", "个人工作区")
                    MeActionRow("敏感信息遮蔽", if (app.privacyMask) "已开启" else "已关闭") {
                        app.privacyMask = !app.privacyMask
                    }
                    MeActionRow("号码与名称", "自定义号码称呼") { app.navigate(VScreen.NUMBERS) }
                    MeActionRow("卡面图片", "在卡片详情中更换") { app.navigate(VScreen.CARDS) }
                    MeActionRow("数据源与记录范围", "查看来源与未知边界") { app.navigate(VScreen.SOURCES) }
                    MeActionRow("偏好设置", "隐私、显示、减弱动态效果") { app.navigate(VScreen.SETTINGS) }
                    if (onHelp != null) {
                        MeActionRow("新手引导", "重新查看使用说明") { onHelp() }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = PdigV2Colors.PrimarySoft.copy(alpha = 0.72f),
            shape = RoundedCornerShape(VRadius.Lg),
        ) {
            Row(
                Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    tint = PdigV2Colors.PrimaryBright,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    "当前是预览数据。未记录 ≠ 不存在，恢复用途 ≠ 唯一恢复路径，计划投影 ≠ 已执行。",
                    color = PdigV2Colors.TextSecondary,
                    fontSize = 11.sp,
                )
            }
        }
        Spacer(Modifier.height(VSpacing.Sm))
    }
}

@Composable
private fun MeSectionHeading(title: String, subtitle: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            title,
            color = PdigV2Colors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(subtitle, color = PdigV2Colors.TextMuted, fontSize = 10.sp)
    }
}

@Composable
private fun MeIdentityMini(
    title: String,
    value: String,
    hint: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 82.dp)
            .clickable(onClick = onClick),
        color = PdigV2Colors.SurfaceRaised,
        shape = RoundedCornerShape(VRadius.Lg),
    ) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, color = PdigV2Colors.TextMuted, fontSize = 10.sp)
            Text(
                value,
                color = PdigV2Colors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(hint, color = PdigV2Colors.TextSecondary, fontSize = 9.sp, maxLines = 1)
        }
    }
}

@Composable
private fun MeContinuityRow(
    value: String,
    label: String,
    detail: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clickable(onClick = onClick),
        color = PdigV2Colors.SurfaceRaised,
        shape = RoundedCornerShape(VRadius.Lg),
    ) {
        Row(
            Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Text(value, color = tint, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, color = PdigV2Colors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    detail,
                    color = PdigV2Colors.TextMuted,
                    fontSize = 10.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text("›", color = PdigV2Colors.PrimaryBright, fontSize = 18.sp)
        }
    }
}

@Composable
private fun MeMetric(
    value: Int,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 76.dp)
            .clickable(onClick = onClick),
        color = PdigV2Colors.SurfaceRaised,
        shape = RoundedCornerShape(VRadius.Lg),
    ) {
        Column(
            Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(value.toString(), color = PdigV2Colors.PrimaryText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(label, color = PdigV2Colors.TextSecondary, fontSize = 10.sp)
        }
    }
}

@Composable
private fun MeActionRow(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = VTouchTarget.Min)
            .clickable(onClick = onClick),
        color = PdigV2Colors.SurfaceRaised,
        shape = RoundedCornerShape(VRadius.Md),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = PdigV2Colors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = PdigV2Colors.TextMuted, fontSize = 9.sp)
            }
            Icon(
                Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = PdigV2Colors.TextMuted,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private fun meRegionFlag(code: String): String = when (code) {
    "CN" -> "🇨🇳"
    "HK" -> "🇭🇰"
    "MO" -> "🇲🇴"
    "GB" -> "🇬🇧"
    "US" -> "🇺🇸"
    "SG" -> "🇸🇬"
    else -> "🌐"
}
