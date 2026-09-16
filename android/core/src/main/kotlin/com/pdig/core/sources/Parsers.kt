package com.pdig.core.sources

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import kotlin.math.abs
import kotlin.math.floor

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
// 通用工具
// ---------------------------------------------------------------------------

/** 与 JS `Math.round` 对齐：half 向 +∞ 取整（金额恒为非负数时等价）。 */
internal fun jsRound(x: Double): Double = floor(x + 0.5)

/** 与 JS `Number.isFinite` 对齐。 */
internal fun isFiniteJs(x: Double): Boolean = !x.isNaN() && !x.isInfinite()

/** 两位小数金额归一（与 TS `Math.round(n * 100) / 100` 同语义）。 */
internal fun money2(n: Double): Double = jsRound(n * 100) / 100

internal fun isLeapYear(y: Int): Boolean = (y % 4 == 0 && y % 100 != 0) || y % 400 == 0

internal fun daysInMonth(y: Int, m: Int): Int = when (m) {
    2 -> if (isLeapYear(y)) 29 else 28
    4, 6, 9, 11 -> 30
    else -> 31
}

/** 完整日历校验：拒绝 JS Date 自动纠正产生的"合法但不存在"的日期。 */
internal fun isValidCalendarDate(y: Int, m: Int, d: Int): Boolean =
    y in 1..9999 && m in 1..12 && d >= 1 && d <= daysInMonth(y, m)

internal fun pad2(v: Int): String = if (v < 10) "0$v" else v.toString()
internal fun pad4(v: Int): String = v.toString().padStart(4, '0')

internal fun isoUtc(y: Int, mo: Int, d: Int, h: Int, mi: Int, s: Int): String =
    "${pad4(y)}-${pad2(mo)}-${pad2(d)}T${pad2(h)}:${pad2(mi)}:${pad2(s)}+00:00"

/** 严格 UTF-8 解码；失败返回 null（用于回退到 GB18030）。 */
internal fun decodeStrictUtf8(bytes: ByteArray): String? = try {
    val cs = Charset.forName("UTF-8")
    val decoder = cs.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
    decoder.decode(ByteBuffer.wrap(bytes)).toString()
} catch (_: CharacterCodingException) {
    null
} catch (_: IllegalArgumentException) {
    null
}

internal fun decodeGb18030(bytes: ByteArray): String = try {
    Charset.forName("GB18030").decode(ByteBuffer.wrap(bytes)).toString()
} catch (_: Exception) {
    // 极端兜底：绝不因为编码问题丢掉整次导入
    String(bytes, Charset.forName("UTF-8"))
}

internal fun hasUtf8Bom(bytes: ByteArray): Boolean =
    bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()

internal fun stripBom(bytes: ByteArray): ByteArray =
    if (hasUtf8Bom(bytes)) bytes.copyOfRange(3, bytes.size) else bytes

/** UTF-8 BOM → utf-8；严格 UTF-8 失败 → gb18030。 */
internal fun decodeBillText(bytes: ByteArray): String {
    if (hasUtf8Bom(bytes)) return String(stripBom(bytes), Charset.forName("UTF-8"))
    return decodeStrictUtf8(bytes) ?: decodeGb18030(bytes)
}

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

// ---------------------------------------------------------------------------
// WeChat 账单
// ---------------------------------------------------------------------------

object WechatParser {

    private val EXPECTED_COLUMNS = listOf(
        "交易时间", "交易类型", "交易对方", "商品", "收/支",
        "金额(元)", "支付方式", "当前状态", "交易单号", "商户单号", "备注",
    )

    fun detect(text: String): Double = when {
        text.contains("微信支付账单明细") -> 0.99
        text.contains("交易时间") && text.contains("交易对方") && text.contains("收/支") -> 0.9
        else -> 0.0
    }

    private fun parseAmount(raw: String): Double? {
        val s = raw.trim().replace(Regex("[¥￥,\\s]"), "")
        if (s.isEmpty()) return null
        val n = s.toDoubleOrNull() ?: return null
        if (!isFiniteJs(n)) return null
        return money2(n)
    }

    private fun parseDirection(raw: String): ObservationDirection? = when (val s = raw.trim()) {
        "收入" -> ObservationDirection.IN
        "支出" -> ObservationDirection.OUT
        "/", "中性交易", "" -> ObservationDirection.NEUTRAL
        else -> null
    }

    /** '2026-01-15 10:23:00' → ISO8601 +08:00（微信账单为中国大陆本地时间）。 */
    fun parseWechatTime(raw: String): String? {
        val m = Regex("^(\\d{4})-(\\d{2})-(\\d{2}) (\\d{2}):(\\d{2}):(\\d{2})\$").find(raw.trim())
            ?: return null
        val y = m.groupValues[1].toInt()
        val mo = m.groupValues[2].toInt()
        val d = m.groupValues[3].toInt()
        val h = m.groupValues[4].toInt()
        val mi = m.groupValues[5].toInt()
        val s = m.groupValues[6].toInt()
        if (mo < 1 || mo > 12) return null
        if (h > 23 || mi > 59 || s > 59) return null
        if (!isValidCalendarDate(y, mo, d)) return null
        return "${pad4(y)}-${pad2(mo)}-${pad2(d)}T${pad2(h)}:${pad2(mi)}:${pad2(s)}+08:00"
    }

    private fun findHeaderRowIndex(lines: List<String>): Int {
        for (i in lines.indices) {
            val cells = parseCsvLine(lines[i])
            if (cells.getOrNull(0)?.trim() == "交易时间") return i
        }
        return -1
    }

    fun parse(raw: ByteArray): ParseResult {
        val text = decodeBillText(raw)
        val lines = text.split(Regex("\\r\\n|\\r|\\n"))
        val observations = mutableListOf<Observation>()
        val errors = mutableListOf<ParseError>()

        val headerIdx = findHeaderRowIndex(lines)
        if (headerIdx == -1) {
            return ParseResult(
                observations,
                listOf(ParseError(0, "missing column header row (交易时间...)")),
                "wechat",
            )
        }
        // 列头行本身校验（列漂移容错：只要求首列匹配 + 最小列数）
        val headerCells = parseCsvLine(lines[headerIdx])
        if (headerCells.getOrNull(0)?.trim() != "交易时间" ||
            headerCells.size < EXPECTED_COLUMNS.size - 3
        ) {
            errors.add(ParseError(headerIdx + 1, "unexpected column header layout"))
        }

        for (i in headerIdx + 1 until lines.size) {
            val line = lines[i]
            if (line.trim().isEmpty()) continue
            val cells = parseCsvLine(line)
            if (cells.size < 9) {
                errors.add(ParseError(i + 1, "expected >=9 columns, got ${cells.size}"))
                continue
            }
            val occurredAt = parseWechatTime(cells.getOrNull(0) ?: "")
            if (occurredAt == null) {
                errors.add(
                    ParseError(
                        i + 1,
                        "bad transaction time: ${(cells.getOrNull(0) ?: "").take(19)}",
                    ),
                )
                continue
            }
            val amount = parseAmount(cells.getOrNull(5) ?: "")
            if (amount == null) {
                errors.add(ParseError(i + 1, "bad amount: ${(cells.getOrNull(5) ?: "").take(12)}"))
                continue
            }
            val direction = parseDirection(cells.getOrNull(4) ?: "")
            if (direction == null) {
                errors.add(ParseError(i + 1, "bad direction: ${(cells.getOrNull(4) ?: "").take(8)}"))
                continue
            }
            val sourceTxnId = (cells.getOrNull(8) ?: "").trim()
            val merchantTxnId = (cells.getOrNull(9) ?: "").trim()
            val status = (cells.getOrNull(7) ?: "").trim()

            observations.add(
                Observation(
                    source = "wechat",
                    sourceTxnId = sourceTxnId.ifEmpty { null },
                    merchantTxnId = merchantTxnId.ifEmpty { null },
                    occurredAt = occurredAt,
                    merchantRaw = (cells.getOrNull(2) ?: "").trim(),
                    description = (cells.getOrNull(3) ?: "").trim(),
                    amount = abs(amount),
                    currency = "CNY",
                    direction = direction,
                    paymentMethodRaw = (cells.getOrNull(6) ?: "").trim(),
                    status = status,
                    note = (cells.getOrNull(10) ?: "").trim(),
                ),
            )
        }
        return ParseResult(observations, errors, "wechat")
    }
}

// ---------------------------------------------------------------------------
// Generic CSV
// ---------------------------------------------------------------------------

object GenericCsvParser {

    fun parse(data: ByteArray, mapping: MappingProfile?): ParseResult {
        if (mapping == null) {
            throw MissingMappingError("generic_csv requires an explicit MappingProfile")
        }
        val errors = mutableListOf<ParseError>()
        val text = decodeText(data, mapping.options.encoding)
        val rawRows = parseCsv(text, mapping.options.delimiter)
        val rows = if (mapping.options.hasHeaderRow) rawRows.drop(1) else rawRows
        val header = rawRows.firstOrNull() ?: emptyList()

        fun colIndexOf(name: String?): Int {
            if (name == null) return -1
            val idx = header.indexOf(name)
            if (idx == -1) throw MissingMappingError("mapping column not found in header: $name")
            return idx
        }

        val cTxn = colIndexOf(mapping.columns.transactionId)
        val cDate = colIndexOf(mapping.columns.dateTime)
        val cAmount = colIndexOf(mapping.columns.amount)
        val cDebit = colIndexOf(mapping.columns.debit)
        val cCredit = colIndexOf(mapping.columns.credit)
        val cDesc = colIndexOf(mapping.columns.description)
        val cCounterparty = colIndexOf(mapping.columns.counterparty)
        val cCurrency = colIndexOf(mapping.columns.currency)
        val cBalance = colIndexOf(mapping.columns.balance)
        val cType = colIndexOf(mapping.columns.transactionType)
        val cMethod = colIndexOf(mapping.columns.paymentMethod)

        val observations = mutableListOf<Observation>()
        val signMode = mapping.options.amountSignMode
        val dec = mapping.options.decimalSeparator

        rows.forEachIndexed { rowIdx, cells ->
            if (cells.all { it.trim().isEmpty() }) return@forEachIndexed
            val line = rowIdx + if (mapping.options.hasHeaderRow) 2 else 1

            if (cDate == -1 || cDate >= cells.size) {
                errors.add(ParseError(line, "missing required date column"))
                return@forEachIndexed
            }
            val rawDate = cells[cDate]
            val occurredAt = parseMappingDate(rawDate, mapping.options.dateFormats)
            if (occurredAt == null) {
                errors.add(ParseError(line, "bad date: ${rawDate.take(24)}"))
                return@forEachIndexed
            }

            var amount: Double? = null
            var direction: ObservationDirection = ObservationDirection.NEUTRAL
            if (signMode == "debit_credit") {
                val debit = if (cDebit >= 0) parseMappingAmount(cells.getOrElse(cDebit) { "" }, dec) else null
                val credit = if (cCredit >= 0) parseMappingAmount(cells.getOrElse(cCredit) { "" }, dec) else null
                when {
                    debit != null && debit > 0 -> { amount = debit; direction = ObservationDirection.OUT }
                    credit != null && credit > 0 -> { amount = credit; direction = ObservationDirection.IN }
                    else -> {
                        errors.add(ParseError(line, "missing debit/credit amount"))
                        return@forEachIndexed
                    }
                }
            } else {
                amount = if (cAmount >= 0) parseMappingAmount(cells.getOrElse(cAmount) { "" }, dec) else null
                if (amount == null) {
                    // TS 用 cells[-1] → undefined → "undefined"；此处必须复现同一字符串
                    val shown = if (cAmount in cells.indices) cells[cAmount].take(16) else "undefined"
                    errors.add(ParseError(line, "bad amount: $shown"))
                    return@forEachIndexed
                }
                when (signMode) {
                    "signed" -> {
                        // signed：金额自带符号。positiveDirection 默认 in（负数=支出）。
                        // 绝不根据数据分布自动猜测方向 —— 必须由 mapping 显式声明。
                        val positiveDirection = mapping.options.positiveDirection ?: "in"
                        direction = when {
                            amount < 0 -> if (positiveDirection == "in") ObservationDirection.OUT else ObservationDirection.IN
                            amount > 0 -> ObservationDirection.fromWire(positiveDirection)
                            else -> ObservationDirection.NEUTRAL
                        }
                        amount = abs(amount)
                    }
                    else -> {
                        direction = ObservationDirection.fromWire(mapping.options.positiveDirection ?: "out")
                    }
                }
            }

            val currency = if (cCurrency >= 0) cells.getOrElse(cCurrency) { "" }.trim().ifEmpty { null } else null

            observations.add(
                Observation(
                    source = "generic_csv",
                    sourceTxnId = if (cTxn >= 0) cells.getOrElse(cTxn) { "" }.trim().ifEmpty { null } else null,
                    merchantTxnId = null,
                    occurredAt = occurredAt,
                    merchantRaw = if (cCounterparty >= 0) cells.getOrElse(cCounterparty) { "" }.trim() else "",
                    description = if (cDesc >= 0) cells.getOrElse(cDesc) { "" }.trim() else "",
                    amount = amount,
                    currency = currency ?: "XXX",
                    direction = direction,
                    paymentMethodRaw = if (cMethod >= 0) cells.getOrElse(cMethod) { "" }.trim() else "",
                    status = if (cType >= 0) cells.getOrElse(cType) { "" }.trim() else "",
                    note = "",
                ),
            )
        }
        return ParseResult(observations, errors, "generic_csv")
    }

    private fun decodeText(data: ByteArray, encoding: String): String {
        if (hasUtf8Bom(data)) return String(stripBom(data), Charset.forName("UTF-8"))
        return decodeStrictUtf8(data)
            ?: if (encoding == "gb18030") decodeGb18030(data) else String(data, Charset.forName("UTF-8"))
    }
}

// ---------------------------------------------------------------------------
// OFX / QFX
// ---------------------------------------------------------------------------

object OfxParser {

    data class Txn(
        val fitid: String?,
        val dtposted: String?,
        val trnamt: String?,
        val trntype: String?,
        val name: String?,
        val memo: String?,
        val currency: String?,
    )

    /** OFX v1 是 SGML 风格：<TAG>VALUE 成对出现，可能带 </TAG>。 */
    fun parseTransactions(text: String): Pair<List<Txn>, Int> {
        val transactions = mutableListOf<Txn>()
        var malformed = 0
        val blocks = text.split(Regex("<STMTTRN>", RegexOption.IGNORE_CASE)).drop(1)
        if (blocks.isEmpty()) return transactions to 0
        for (block in blocks) {
            val end = Regex("</STMTTRN>|<STMTTRN>", RegexOption.IGNORE_CASE).find(block)?.range?.first ?: -1
            val body = if (end == -1) block else block.substring(0, end)
            fun tag(name: String): String? =
                Regex("<$name>\\s*([^<\\r\\n]+)", RegexOption.IGNORE_CASE)
                    .find(body)?.groupValues?.getOrNull(1)?.trim()
            val fitid = tag("FITID")
            val trnamt = tag("TRNAMT")
            // 无 FITID 且无 TRNAMT → 坏块
            if (fitid == null && trnamt == null) {
                malformed += 1
                continue
            }
            transactions.add(
                Txn(
                    fitid = fitid,
                    dtposted = tag("DTPOSTED"),
                    trnamt = trnamt,
                    trntype = tag("TRNTYPE"),
                    name = tag("NAME"),
                    memo = tag("MEMO"),
                    currency = tag("CURDEF") ?: tag("CURRENCY"),
                ),
            )
        }
        return transactions to malformed
    }

    /** OFX DTPOSTED YYYYMMDD[HHMMSS.XXX[gmt offset]] → ISO(UTC)。 */
    fun parseOfxDate(raw: String): String? {
        val m = Regex("^(\\d{4})(\\d{2})(\\d{2})(?:(\\d{2})(\\d{2})(\\d{2}))?").find(raw.trim())
            ?: return null
        val y = m.groupValues[1].toInt()
        val mo = m.groupValues[2].toInt()
        val d = m.groupValues[3].toInt()
        val h = m.groupValues[4].toIntOrNull() ?: 0
        val mi = m.groupValues[5].toIntOrNull() ?: 0
        val s = m.groupValues[6].toIntOrNull() ?: 0
        if (mo < 1 || mo > 12 || h > 23 || mi > 59 || s > 59) return null
        if (!isValidCalendarDate(y, mo, d)) return null
        return isoUtc(y, mo, d, h, mi, s)
    }

    /** 必须显式拒绝空串：Number('') === 0 会把"缺失金额"伪造成一笔 0 元交易。 */
    fun parseOfxAmount(raw: String): Double? {
        val s = raw.trim()
        if (s.isEmpty()) return null
        val n = s.toDoubleOrNull() ?: return null
        if (!isFiniteJs(n)) return null
        return money2(n)
    }

    fun parse(data: ByteArray): ParseResult {
        val text = decodeBillText(data)
        val (transactions, malformed) = parseTransactions(text)
        val errors = mutableListOf<ParseError>()
        repeat(malformed) { i ->
            errors.add(ParseError(i + 1, "malformed STMTTRN block (no FITID/TRNAMT)"))
        }
        val observations = mutableListOf<Observation>()
        for (txn in transactions) {
            val occurredAt = txn.dtposted?.let { parseOfxDate(it) }
            if (occurredAt == null) {
                errors.add(
                    ParseError(
                        observations.size + 1,
                        "bad DTPOSTED: ${txn.dtposted?.take(20) ?: "missing"}",
                    ),
                )
                continue
            }
            val amount = txn.trnamt?.let { parseOfxAmount(it) }
            if (amount == null) {
                errors.add(
                    ParseError(
                        observations.size + 1,
                        "bad TRNAMT: ${txn.trnamt?.take(20) ?: "missing"}",
                    ),
                )
                continue
            }
            observations.add(
                Observation(
                    source = "ofx_qfx",
                    sourceTxnId = txn.fitid,
                    merchantTxnId = null,
                    occurredAt = occurredAt,
                    merchantRaw = txn.name ?: txn.memo ?: "",
                    description = txn.memo ?: "",
                    amount = abs(amount),
                    currency = txn.currency ?: "XXX",
                    direction = if (amount < 0) ObservationDirection.OUT else ObservationDirection.IN,
                    paymentMethodRaw = "",
                    status = txn.trntype ?: "",
                    note = "",
                ),
            )
        }
        return ParseResult(observations, errors, "ofx_qfx")
    }
}
