package com.pdig.core.sources

/**
 * PDIG 账单解析器（WeChat / Generic CSV / OFX-QFX）。
 *
 * 逐行移植自 core/src/parser/wechat/parser.ts、core/src/sources/generic-csv/adapter.ts、
 * core/src/sources/ofx/adapter.ts。**语义必须与 Oracle 完全一致**，不得"重新发明"。
 *
 * 硬约束（来自 Canonical Spec）：
 *  - 坏行保守拒绝：计入 errors，不抛出整次导入
 *  - 金额缺失 ≠ 0 元：空串必须显式拒绝（否则会伪造出一笔不存在的交易）
 *  - 日期必须完整日历校验（拒绝 2026-02-30 这类自动纠正结果）
 *  - Observation 仅存在于导入会话内存，绝不落库
 *
 * 平台无关：只依赖 java.nio.charset（Android / JVM 均可用），不依赖 java.time。
 */

class MissingMappingError(message: String) : RuntimeException(message)

enum class ObservationDirection(val wire: String) {
    IN("in"),
    OUT("out"),
    NEUTRAL("neutral"),
    ;

    companion object {
        fun fromWire(w: String): ObservationDirection =
            entries.firstOrNull { it.wire == w }
                ?: throw IllegalArgumentException("unknown direction: $w")
    }
}

data class Observation(
    val source: String,
    val sourceTxnId: String?,
    val merchantTxnId: String?,
    val occurredAt: String,
    val merchantRaw: String,
    val description: String,
    val amount: Double,
    val currency: String,
    val direction: ObservationDirection,
    val paymentMethodRaw: String,
    val status: String,
    val note: String,
)

data class ParseError(val line: Int, val reason: String)

data class ParseResult(
    val observations: List<Observation>,
    val errors: List<ParseError>,
    val sourceLabel: String = "",
)

/** 显式字段映射（禁止 AI 自动映射；mapping 必须由用户/导入向导明确给出）。 */
data class MappingColumns(
    val transactionId: String? = null,
    val dateTime: String,
    val amount: String? = null,
    val debit: String? = null,
    val credit: String? = null,
    val description: String? = null,
    val counterparty: String? = null,
    val currency: String? = null,
    val balance: String? = null,
    val transactionType: String? = null,
    val paymentMethod: String? = null,
)

data class MappingOptions(
    val delimiter: String = ",",
    val dateFormats: List<String> = listOf("YYYY-MM-DD"),
    val decimalSeparator: Char = '.',
    val amountSignMode: String = "outward_positive",
    val hasHeaderRow: Boolean = true,
    val encoding: String = "utf-8",
    val positiveDirection: String? = null,
)

data class MappingProfile(val columns: MappingColumns, val options: MappingOptions)

// ---------------------------------------------------------------------------
// CSV
// ---------------------------------------------------------------------------

/** 解析单行 CSV（引号转义 + 引号内逗号）。 */
fun parseCsvLine(line: String): List<String> {
    val cells = mutableListOf<String>()
    val cur = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < line.length) {
        val ch = line[i]
        if (inQuotes) {
            when {
                ch == '"' -> {
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        cur.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                }
                else -> cur.append(ch)
            }
        } else when (ch) {
            '"' -> inQuotes = true
            ',' -> { cells.add(cur.toString()); cur.setLength(0) }
            else -> cur.append(ch)
        }
        i++
    }
    cells.add(cur.toString())
    return cells
}

/** 解析完整 CSV（支持引号内换行、CRLF / CR-only / LF）。 */
fun parseCsv(text: String, delimiter: String): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    val row = mutableListOf<String>()
    val cur = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < text.length) {
        val ch = text[i]
        if (inQuotes) {
            when {
                ch == '"' -> {
                    if (i + 1 < text.length && text[i + 1] == '"') {
                        cur.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                }
                else -> cur.append(ch)
            }
        } else when {
            ch == '"' -> inQuotes = true
            ch.toString() == delimiter -> { row.add(cur.toString()); cur.setLength(0) }
            ch == '\r' && i + 1 < text.length && text[i + 1] == '\n' -> {
                row.add(cur.toString()); rows.add(row.toList()); row.clear(); cur.setLength(0); i++
            }
            ch == '\r' || ch == '\n' -> {
                row.add(cur.toString()); rows.add(row.toList()); row.clear(); cur.setLength(0)
            }
            else -> cur.append(ch)
        }
        i++
    }
    if (cur.isNotEmpty() || row.isNotEmpty()) {
        row.add(cur.toString())
        rows.add(row.toList())
    }
    return rows
}

// ---------------------------------------------------------------------------
// 日期 / 金额（Generic CSV）
// ---------------------------------------------------------------------------

private val DATE_TOKENS: List<Pair<String, String>> = listOf(
    "YYYY" to "(\\d{4})",
    "MM" to "(\\d{1,2})",
    "DD" to "(\\d{1,2})",
    "HH" to "(\\d{1,2})",
    "mm" to "(\\d{1,2})",
    "ss" to "(\\d{1,2})",
)

/**
 * 把 format 字符串编译为正则 + 捕获组顺序。
 *
 * 不得回退的实现要点（legacy 曾踩过）：**先按 token 切分并逐段转义，再拼装捕获组**。
 * 若先把 token 替换成 `(\d{4})`、再对整体做正则元字符转义，会连捕获组自身的括号一起
 * 转义成 `\(\d\{4\}\)`，导致任何含日期 token 的格式永远匹配失败 —— 表现为"所有行
 * 都是 bad date"的静默数据丢失。
 */
private fun compileFormat(fmt: String): Pair<String, List<String>> {
    val order = mutableListOf<String>()
    val source = StringBuilder()
    var i = 0
    while (i < fmt.length) {
        val hit = DATE_TOKENS.firstOrNull { fmt.startsWith(it.first, i) }
        if (hit != null) {
            order.add(hit.first)
            source.append(hit.second)
            i += hit.first.length
        } else {
            val c = fmt[i]
            if (".*+?^\${}()|[]\\".indexOf(c) >= 0) source.append('\\')
            source.append(c)
            i++
        }
    }
    return source.toString() to order
}

private fun matchFormat(s: String, fmt: String): String? {
    val (source, order) = compileFormat(fmt)
    if (order.isEmpty()) return null
    val m = Regex("^$source\$").find(s) ?: return null
    val values = m.groupValues.drop(1).map { it.toIntOrNull() ?: 0 }
    fun get(t: String): Int {
        val idx = order.indexOf(t)
        return if (idx == -1) 0 else values.getOrElse(idx) { 0 }
    }
    val y = get("YYYY")
    val mo = get("MM")
    val d = get("DD")
    val h = get("HH")
    val mi = get("mm")
    val sec = get("ss")
    if (mo < 1 || mo > 12 || h > 23 || mi > 59 || sec > 59) return null
    if (!isValidCalendarDate(y, mo, d)) return null
    return isoUtc(y, mo, d, h, mi, sec)
}

fun parseMappingDate(raw: String, formats: List<String>): String? {
    val s = raw.trim()
    for (fmt in formats) {
        val r = matchFormat(s, fmt)
        if (r != null) return r
    }
    return null
}

/** 按显式 decimalSeparator 解析金额；空串返回 null（缺失金额 ≠ 0 元）。 */
fun parseMappingAmount(raw: String, decimalSeparator: Char): Double? {
    var s = raw.trim().replace(Regex("[\\s¥￥\$€£]"), "")
    if (s.isEmpty()) return null
    s = if (decimalSeparator == ',') s.replace(".", "").replace(",", ".") else s.replace(",", "")
    val n = s.toDoubleOrNull() ?: return null
    if (!isFiniteJs(n)) return null
    return money2(n)
}
