package com.pdig.uivnext.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius

/**
 * Production-safe local presentation preferences.
 *
 * These switches mutate VAppState presentation preferences only. They never
 * write Canonical / PersonalReality or claim a domain action completed.
 * Persistence is provided when the host injects a store-backed VAppState.
 */
@Composable
internal fun ProductionPreferencesScreen(
    app: VAppState,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("pdig.production-vnext.preferences"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "隐私与偏好",
                    color = PdigV2Colors.TextPrimary,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "这些设置只改变本机显示方式，不修改已确认的基础设施数据。",
                    color = PdigV2Colors.TextMuted,
                    fontSize = 12.sp,
                )
            }
        }

        item {
            PreferenceToggleCard(
                title = "敏感信息遮蔽",
                body = "隐藏号码、卡号等敏感呈现；不会改写对象身份。",
                enabled = app.privacyMask,
                tag = "pdig.production-vnext.preference.privacy",
            ) { app.privacyMask = !app.privacyMask }
        }

        item {
            PreferenceToggleCard(
                title = "减少动态效果",
                body = "降低 Globe 与界面动效强度；不会改变数据或计划状态。",
                enabled = app.reduceMotion,
                tag = "pdig.production-vnext.preference.motion",
            ) { app.reduceMotion = !app.reduceMotion }
        }

        item {
            PreferenceToggleCard(
                title = "显示即将到来",
                body = "控制 Now 是否突出未来维护/计划项；隐藏不等于事项不存在。",
                enabled = app.showUpcoming,
                tag = "pdig.production-vnext.preference.upcoming",
            ) { app.showUpcoming = !app.showUpcoming }
        }

        item {
            PreferenceToggleCard(
                title = "展开宽屏导航",
                body = "仅影响 Expanded 布局的导航宽度。",
                enabled = app.railExpanded,
                tag = "pdig.production-vnext.preference.rail",
            ) { app.railExpanded = !app.railExpanded }
        }

        item {
            PresentationBoundary(
                "这些都是本机显示偏好，不属于基础设施事实。正式入口接入持久化存储后才能承诺跨启动保存；临时状态不能冒充“已保存”。"
            )
        }
    }
}

@Composable
private fun PreferenceToggleCard(
    title: String,
    body: String,
    enabled: Boolean,
    tag: String,
    onToggle: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .testTag(tag),
        color = PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Lg),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                title,
                color = PdigV2Colors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(body, color = PdigV2Colors.TextSecondary, fontSize = 10.sp)
            Text(
                if (enabled) "已开启" else "已关闭",
                color = if (enabled) PdigV2Colors.PrimaryText else PdigV2Colors.TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun PresentationBoundary(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PdigV2Colors.PrimarySoft.copy(alpha = 0.62f),
        shape = RoundedCornerShape(VRadius.Lg),
    ) {
        Text(
            text,
            Modifier.padding(13.dp),
            color = PdigV2Colors.TextSecondary,
            fontSize = 10.sp,
        )
    }
}
