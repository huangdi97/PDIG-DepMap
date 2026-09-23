package com.pdig.core.sources

import java.nio.charset.Charset
import kotlin.math.abs

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
