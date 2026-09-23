package com.pdig.core.sources

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import kotlin.math.floor

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
