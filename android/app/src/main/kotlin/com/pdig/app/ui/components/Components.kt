package com.pdig.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pdig.app.ui.theme.PdigStatus
import com.pdig.app.ui.theme.PdigTokens

/** 卡片：统一圆角/间距（design-tokens）。 */
@Composable
fun PdigCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(PdigTokens.RadiusMd)
    if (onClick == null) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) { Box(Modifier.padding(PdigTokens.SpaceLg)) { content() } }
    } else {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) { Box(Modifier.padding(PdigTokens.SpaceLg)) { content() } }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = PdigTokens.SpaceSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = PdigTokens.Title, color = MaterialTheme.colorScheme.onBackground)
        if (action != null) action()
    }
}

/** 状态标记：状态**不得只靠颜色**传达 —— 必带文字（spec §84 / design-tokens policy）。 */
@Composable
fun StatusChip(statusKey: String, modifier: Modifier = Modifier) {
    val color = PdigStatus.of(statusKey)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(PdigTokens.RadiusFull),
        color = color.copy(alpha = 0.12f),
    ) {
        Text(
            text = PdigStatus.label(statusKey),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = PdigTokens.Label,
            color = color,
        )
    }
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .padding(PdigTokens.SpaceLg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = PdigTokens.Body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun ErrorState(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(PdigTokens.SpaceLg),
        contentAlignment = Alignment.Center,
    ) {
        // 绝不把 SQL 异常 / stack trace 暴露给用户（spec §128）
        Text("出了点问题，请稍后重试。", color = MaterialTheme.colorScheme.error, style = PdigTokens.Body)
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(PdigTokens.SpaceXl), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp))
    }
}

/**
 * 可滚动页面容器（P1-A）。
 *
 * 解决的是一个**真实的可用性缺陷**：页面末尾的按钮贴着滚动视口底边时，
 * 它的可点击区会被视口裁剪，而 uiautomator 报出的 semantics bounds 仍是
 * 未裁剪的布局矩形 —— 两者错位，用户看到"按钮明明在那儿，点中心却没反应，
 * 往下偏一点才生效"。
 *
 * 容器在内容之后固定留出 [PdigTokens.SpaceXl] 的底部空隙，于是：
 *   视觉区 == 真实点击区 == semantics bounds
 * 三者重新重合（末尾元素总能被完整滚入可视区）。
 *
 * 这是唯一的滚动容器实现：断言在 `ImportHitboxTest`，host 侧还有一次
 * 真实 tap 交叉验证（不靠改 tap offset 绕过产品缺陷）。
 */
@Composable
fun PdigScrollingPage(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = verticalArrangement,
    ) {
        content()
        Spacer(Modifier.height(PdigTokens.SpaceXl))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdigTopBar(title: String, onBack: (() -> Unit)? = null) {
    TopAppBar(
        title = { Text(title, style = PdigTokens.Title) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack, modifier = Modifier.size(PdigTokens.MinTouchTarget)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            }
        },
    )
}

@Composable
fun StatTile(label: String, value: String, tint: Color = MaterialTheme.colorScheme.primary, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(tint.copy(alpha = 0.10f), RoundedCornerShape(PdigTokens.RadiusMd))
            .padding(PdigTokens.SpaceMd),
    ) {
        Text(value, style = PdigTokens.Display, color = tint)
        Text(label, style = PdigTokens.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
