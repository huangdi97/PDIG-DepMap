package com.pdig.app.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.pdig.app.data.AppContainer
import com.pdig.app.data.ImportCommitResult
import com.pdig.app.data.ParseOutcome
import com.pdig.app.ui.Route
import com.pdig.app.ui.components.PdigScrollingPage
import com.pdig.app.ui.components.PdigTopBar
import com.pdig.app.ui.theme.PdigTokens
import com.pdig.core.sources.MappingProfile
/**
 * 导入完成态。
 *
 * 单独成函数而不是在 ImportScreen 内部 `return@Scaffold`：后者写在嵌套 Column
 * lambda 中会造成 Compose start/end 失衡并在重组时崩溃。
 */
@Composable
internal fun ImportDonePanel(nav: NavController, done: ImportCommitResult, onLeave: () -> Unit) {
    Scaffold(
        topBar = { PdigTopBar("导入账单", onBack = { onLeave(); nav.popBackStack() }) },
    ) { pad ->
        PdigScrollingPage(
            modifier = Modifier
                .padding(pad)
                .padding(PdigTokens.SpaceLg),
            verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceMd),
        ) {
            Text("导入完成", style = PdigTokens.Title)
            Text(
                "记录 ${done.rawCount} 行；新增不重复 ${done.newUniqueCount} 条，重复跳过 ${done.duplicateCount} 条。",
                style = PdigTokens.Body,
            )
            Text(
                "记录 ${done.nodeCount} 个对象，生成 ${done.proposalCount} 条待确认关系。",
                style = PdigTokens.Body,
            )
            Text(
                "这些关系还只是候选，需要你确认后才会成为事实。",
                style = PdigTokens.Body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = { onLeave(); nav.navigate(Route.REVIEW) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = PdigTokens.MinTouchTarget),
            ) { Text("去确认候选关系") }
        }
    }
}

/** 一次"读文件 + 解析"的结果。**只在内存中流转**，不落库、不进 Bundle。 */
internal data class ParsedFile(
    val outcome: ParseOutcome,
    val adapterId: String,
    val sourceLabel: String,
    val mapping: MappingProfile?,
)

/**
 * 读取并解析用户选中的文件。
 *
 * 抽成纯函数（而不是写在 Composable 里）的原因：D-16 之后它会被
 * **用户重新认证之后**再次调用（此时页面刚被重建），必须能在任何组合状态下独立执行。
 * 适配器按内容判定，不按扩展名猜（spec §69）。
 */
internal suspend fun readAndParse(
    context: android.content.Context,
    uri: Uri,
    container: AppContainer,
    fallbackLabel: String,
): ParsedFile? {
    val bytes = try {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    } catch (_: Throwable) {
        null
    } ?: return null
    val head = String(bytes, Charsets.UTF_8)
    val adapter = when {
        head.contains("微信支付账单明细") ||
            (head.contains("交易时间") && head.contains("收/支")) -> "wechat"
        head.contains("OFX", ignoreCase = true) -> "ofx_qfx"
        else -> "generic_csv"
    }
    val mapping = if (adapter == "generic_csv") defaultCsvMapping(head) else null
    val outcome = runCatching {
        container.parseFile(bytes, adapter, mapping)
    }.getOrNull() ?: return null
    return ParsedFile(outcome, adapter, fallbackLabel, mapping)
}

/** 极简 CSV 映射猜测：**仅在用户确认后**使用；绝不静默自动映射（spec §17 禁止 AI 自动映射）。 */
internal fun defaultCsvMapping(head: String): com.pdig.core.sources.MappingProfile? {
    val firstLine = head.lineSequence().firstOrNull() ?: return null
    val cols = firstLine.split(',', ';').map { it.trim().trim('"') }
    val date = cols.firstOrNull { it.contains("date", true) || it.contains("时间") } ?: return null
    val amount = cols.firstOrNull { it.contains("amount", true) || it.contains("金额") } ?: return null
    return com.pdig.core.sources.MappingProfile(
        columns = com.pdig.core.sources.MappingColumns(dateTime = date, amount = amount),
        options = com.pdig.core.sources.MappingOptions(
            delimiter = if (firstLine.contains(';')) ";" else ",",
        ),
    )
}
