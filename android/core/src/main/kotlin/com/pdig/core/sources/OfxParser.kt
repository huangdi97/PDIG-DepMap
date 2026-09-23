package com.pdig.core.sources

import kotlin.math.abs

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
