package com.pdig.core.timeline

import com.pdig.core.db.SqliteDriver
import kotlin.math.floor

/**
 * ISO-8601 → epoch millis。支持 `Z` / `±HH:MM` / 无偏移（按 UTC 处理）。
 * 与 `com.pdig.core.plan.parseIso` 的区别：**必须正确处理时区偏移** ——
 * 微信账单是 +08:00、OFX 是 +00:00，忽略偏移会把"今天"判成"明天"。
 */
internal fun parseIsoEpoch(value: String): Long? {
    return try {
        val v = value.trim()
        val datePart = v.substringBefore('T').substringBefore(' ')
        val dp = datePart.split('-')
        if (dp.size != 3) return null
        val y = dp[0].toInt()
        val mo = dp[1].toInt()
        val d = dp[2].toInt()
        if (mo !in 1..12) return null
        val dim = when (mo) {
            2 -> if ((y % 4 == 0 && y % 100 != 0) || y % 400 == 0) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
        if (d < 1 || d > dim) return null
        var epoch = daysFromCivil(y, mo, d) * 86_400_000.0
        if (v.contains('T')) {
            var rest = v.substringAfter('T')
            var offsetMinutes = 0
            val plus = rest.indexOf('+')
            val minus = rest.lastIndexOf('-')
            when {
                rest.endsWith("Z") -> rest = rest.removeSuffix("Z")
                plus > 0 -> {
                    val off = rest.substring(plus + 1)
                    rest = rest.substring(0, plus)
                    offsetMinutes = parseOffsetMinutes(off) ?: return null
                }
                minus > 0 -> {
                    val off = rest.substring(minus + 1)
                    rest = rest.substring(0, minus)
                    offsetMinutes = -(parseOffsetMinutes(off) ?: return null)
                }
            }
            val hms = rest.split(':')
            if (hms.size < 2) return null
            val hh = hms[0].toInt()
            val mm = hms[1].toInt()
            val ss = if (hms.size > 2) hms[2].toDouble() else 0.0
            if (hh !in 0..23 || mm !in 0..59 || ss < 0.0 || ss >= 60.0) return null
            epoch += (hh * 3_600_000.0 + mm * 60_000.0 + ss * 1000.0) -
                (offsetMinutes * 60_000.0)
        }
        floor(epoch).toLong()
    } catch (_: Throwable) {
        null
    }
}

private fun parseOffsetMinutes(off: String): Int? {
    val parts = off.split(':')
    return when (parts.size) {
        2 -> {
            val h = parts[0].toIntOrNull() ?: return null
            val m = parts[1].toIntOrNull() ?: return null
            if (h !in 0..23 || m !in 0..59) return null
            h * 60 + m
        }
        1 -> if (off.length == 4) {
            val h = off.substring(0, 2).toIntOrNull() ?: return null
            val m = off.substring(2, 4).toIntOrNull() ?: return null
            h * 60 + m
        } else {
            null
        }
        else -> null
    }
}

/** Howard Hinnant civil-from-days 逆运算（不依赖平台时区/日历纠错）。 */
private fun daysFromCivil(y: Int, m: Int, d: Int): Double {
    val yy = (if (m <= 2) y - 1 else y).toDouble()
    val era = floor(yy / 400.0)
    val yoe = yy - era * 400.0
    val doy = floor((153.0 * (m + if (m > 2) -3 else 9) + 2.0) / 5.0) + d - 1
    val doe = yoe * 365.0 + floor(yoe / 4.0) - floor(yoe / 100.0) + doy
    return era * 146097.0 + doe - 719468.0
}

internal fun bucketOf(scheduledAt: String?, now: Long): String {
    if (scheduledAt == null) return "attention"
    val t = parseIsoEpoch(scheduledAt) ?: return "attention"
    // 当日 00:00 UTC 为分桶基准（与 TS `setUTCHours(0,0,0,0)` 对齐）
    val todayStart = floor(now / DAY_MS) * DAY_MS
    if (t < todayStart) return "overdue"
    if (t < todayStart + DAY_MS) return "today"
    if (t < todayStart + 8 * DAY_MS) return "7d"
    if (t < todayStart + 31 * DAY_MS) return "30d"
    if (t < todayStart + 91 * DAY_MS) return "90d"
    return "later"
}

internal fun getGraphRevision(driver: SqliteDriver): Int {
    val row = driver.prepare("SELECT value FROM meta WHERE key = 'graph_revision'").get()
        ?: return 0
    return row.str("value")?.toIntOrNull() ?: 0
}
