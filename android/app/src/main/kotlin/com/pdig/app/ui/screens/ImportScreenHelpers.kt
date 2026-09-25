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
import com.pdig.core.sources.MappingColumns
import com.pdig.core.sources.MappingOptions
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
    /** 文件开头（表头 + 首行示例），供 CSV 字段对应步骤取列名与示例值。 */
    val head: String,
    /** 原始文件字节。仅存内存，导入会话结束即释放；绝不持久化（spec §151）。 */
    val bytes: ByteArray,
)

/** 读取/解析失败分类（用户文案由 UI 映射，不暴露异常类型/堆栈 —— spec §24）。 */
internal enum class ImportFailReason { UNREADABLE, UNPARSEABLE }

internal sealed interface ImportFileResult {
    data class Ok(val parsed: ParsedFile) : ImportFileResult
    data class Failed(val reason: ImportFailReason) : ImportFileResult
}

/** 文件读取失败的用户文案。 */
internal const val IMPORT_UNREADABLE_MESSAGE = "无法读取这个文件，请确认文件可访问且未被占用。"

/** 解析异常 / 格式不识别时的用户文案（不暴露异常类型名）。 */
internal const val IMPORT_PARSE_FAILED_MESSAGE =
    "无法识别这个文件的格式。支持微信支付账单、通用 CSV、OFX/QFX 文件。"

/**
 * 读取并解析用户选中的文件。
 *
 * 抽成纯函数（而不是写在 Composable 里）的原因：D-16 之后它会被
 * **用户重新认证之后**再次调用（此时页面刚被重建），必须能在任何组合状态下独立执行。
 * 适配器按内容判定，不按扩展名猜（spec §69）。
 * 读取失败与解析失败被区分成 [ImportFailReason]，UI 据此给出不同的用户文案。
 */
internal suspend fun readAndParse(
    context: android.content.Context,
    uri: Uri,
    container: AppContainer,
    fallbackLabel: String,
): ImportFileResult {
    val bytes = try {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    } catch (_: Throwable) {
        null
    }
    if (bytes == null || bytes.isEmpty()) return ImportFileResult.Failed(ImportFailReason.UNREADABLE)
    val head = String(bytes, Charsets.UTF_8)
    val adapter = when {
        head.contains("微信支付账单明细") ||
            (head.contains("交易时间") && head.contains("收/支")) -> "wechat"
        head.contains("OFX", ignoreCase = true) -> "ofx_qfx"
        else -> "generic_csv"
    }
    val mapping = if (adapter == "generic_csv") defaultCsvMapping(head) else null
    val outcome = try {
        container.parseFile(bytes, adapter, mapping)
    } catch (_: Throwable) {
        return ImportFileResult.Failed(ImportFailReason.UNPARSEABLE)
    }
    return ImportFileResult.Ok(ParsedFile(outcome, adapter, fallbackLabel, mapping, head, bytes))
}

/** 极简 CSV 映射猜测：**仅在用户确认后**使用；绝不静默自动映射（spec §17 禁止 AI 自动映射）。 */
internal fun defaultCsvMapping(head: String): MappingProfile? {
    val cols = csvColumns(head)
    val date = cols.firstOrNull { it.contains("date", true) || it.contains("时间") } ?: return null
    val amount = cols.firstOrNull { it.contains("amount", true) || it.contains("金额") } ?: return null
    return csvMapping(head, date, amount)
}

/** 表头列名（按首行切分，兼容逗号/分号分隔；去掉引号与空列）。 */
internal fun csvColumns(head: String): List<String> {
    val firstLine = head.lineSequence().firstOrNull() ?: return emptyList()
    return firstLine.split(',', ';').map { it.trim().trim('"') }.filter { it.isNotBlank() }
}

/** 表头 → 首行示例值（CSV 字段对应步骤用；只保留列名和一条示例，不保存整份流水）。 */
internal fun csvSampleRow(head: String): Map<String, String> {
    val lines = head.lineSequence().toList()
    if (lines.size < 2) return emptyMap()
    val cols = lines[0].split(',', ';').map { it.trim().trim('"') }
    val data = lines[1].split(',', ';').map { it.trim().trim('"') }
    return cols.mapIndexedNotNull { i, c -> if (c.isBlank()) null else c to (data.getOrElse(i) { "" }) }.toMap()
}

/** 按用户选择的列构造 MappingProfile；列名必须都在表头中，否则返回 null。 */
internal fun csvMapping(head: String, dateColumn: String, amountColumn: String): MappingProfile? {
    val cols = csvColumns(head)
    if (dateColumn !in cols || amountColumn !in cols) return null
    val firstLine = head.lineSequence().firstOrNull() ?: return null
    return MappingProfile(
        columns = MappingColumns(dateTime = dateColumn, amount = amountColumn),
        options = MappingOptions(delimiter = if (firstLine.contains(';')) ";" else ","),
    )
}
