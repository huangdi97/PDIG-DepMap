package com.pdig.core.sources

import kotlin.math.abs

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
