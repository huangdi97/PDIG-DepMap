package com.pdig.core.timeline

import com.pdig.core.db.SqliteDriver
import com.pdig.core.json.Json
import com.pdig.core.json.JsonParser
import kotlin.math.floor

/**
 * Timeline / Upcoming 投影（MVP03 §42–§45）。
 *
 * **纯投影（derived read model）**：不是 Reality、不持久化、可随时重建。
 * 只包含数字基础设施事项；排序 deterministic（bucket → priority 降序 → scheduledAt → id）。
 *
 * 移植自 core/src/services/timeline.ts。
 */

private val BUCKET_ORDER: List<String> = listOf(
    "attention", "overdue", "today", "7d", "30d", "90d", "later",
)

private const val DAY_MS = 86_400_000.0

data class TimelineItem(
    val id: String,
    val kind: String,
    val title: String,
    val subtitle: String,
    val scheduledAt: String?,
    val bucket: String,
    val priority: Int,
    val sourceType: String,
    val sourceId: String,
    val actionTarget: String?,
    val status: String,
)

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

/** 构建 Timeline 投影（只读；对同一 DB 状态与同一 now 恒定）。 */
fun buildTimeline(
    driver: SqliteDriver,
    nowIso: String,
    freshnessThresholdDays: Int = 45,
): List<TimelineItem> {
    val now = parseIsoEpoch(nowIso) ?: return emptyList()
    val items = mutableListOf<TimelineItem>()
    val currentRevision = getGraphRevision(driver)

    // 1. ChangePlans：needs_revalidation → attention；effectiveDate → upcoming_change / overdue
    val planRows = driver.prepare(
        """
        SELECT id, scenario, title, workflow_state, last_analyzed_graph_revision,
               effective_date, action_items_json
          FROM change_plans
        """.trimIndent(),
    ).all()
    for (row in planRows) {
        val planId = row.str("id") ?: continue
        val workflowState = row.str("workflow_state") ?: continue
        if (workflowState == "completed" || workflowState == "cancelled") continue
        val lastAnalyzed = row.long("last_analyzed_graph_revision") ?: 0
        val effectiveDate = row.str("effective_date")
        val title = row.str("title") ?: ""
        val scenario = row.str("scenario") ?: ""

        if (currentRevision > lastAnalyzed) {
            items.add(
                TimelineItem(
                    id = "tl-plan-stale-$planId",
                    kind = "needs_attention",
                    title = "计划需要重新检查：$title",
                    subtitle = "基础设施在此计划创建后发生了变化，需要重新分析。",
                    scheduledAt = null,
                    bucket = "attention",
                    priority = 3,
                    sourceType = "change_plan",
                    sourceId = planId,
                    actionTarget = planId,
                    status = "needs_revalidation",
                ),
            )
            continue
        }
        val actionsJson = row.str("action_items_json") ?: "[]"
        if (effectiveDate != null) {
            val b = bucketOf(effectiveDate, now)
            items.add(
                TimelineItem(
                    id = "tl-plan-$planId",
                    kind = "upcoming_change",
                    title = title,
                    subtitle = "变更计划（$scenario）",
                    scheduledAt = effectiveDate,
                    bucket = if (b == "attention") "later" else b,
                    priority = 2,
                    sourceType = "change_plan",
                    sourceId = planId,
                    actionTarget = planId,
                    status = workflowState,
                ),
            )
        }
        // 2. Verification pending（verify 阶段动作）
        for (action in parseActions(actionsJson)) {
            val verifStatus = action.verificationStatus
            if (action.phase == "verify" &&
                verifStatus != null &&
                verifStatus != "verified" &&
                verifStatus != "not_required"
            ) {
                val b = bucketOf(effectiveDate, now)
                items.add(
                    TimelineItem(
                        id = "tl-verif-$planId-${action.id}",
                        kind = "verification_pending",
                        title = "待验证：${action.title}",
                        subtitle = "属于计划「$title」",
                        scheduledAt = effectiveDate,
                        bucket = if (b == "attention") "later" else b,
                        priority = 2,
                        sourceType = "action_verification",
                        sourceId = "$planId/${action.id}",
                        actionTarget = planId,
                        status = verifStatus,
                    ),
                )
            }
        }
    }

    // 3. Open RealityDrifts → drift_review（attention）
    for (row in driver.prepare(
        "SELECT id, kind, target_node_id, detected_at, status FROM reality_drifts WHERE status = 'open'",
    ).all()) {
        items.add(
            TimelineItem(
                id = "tl-drift-${row.str("id") ?: continue}",
                kind = "drift_review",
                title = "可能发生了变化",
                subtitle = "${row.str("kind")}（需确认或忽略）",
                scheduledAt = row.str("detected_at"),
                bucket = "attention",
                priority = 3,
                sourceType = "reality_drift",
                sourceId = row.str("id") ?: "",
                actionTarget = row.str("target_node_id"),
                status = row.str("status") ?: "open",
            ),
        )
    }

    // 4. Node expiry（payment_instrument fields.expiryDate）
    for (row in driver.prepare(
        "SELECT id, name, fields_json FROM nodes WHERE archived = 0",
    ).all()) {
        val expiry = expiryOf(row.str("fields_json"))
        if (expiry == null) continue
        items.add(
            TimelineItem(
                id = "tl-expiry-${row.str("id") ?: continue}",
                kind = "expiration",
                title = "${row.str("name")} 即将到期",
                subtitle = "检查仍依赖此对象的支付路径。",
                scheduledAt = expiry,
                bucket = bucketOf(expiry, now),
                priority = 2,
                sourceType = "node_expiry",
                sourceId = row.str("id") ?: "",
                actionTarget = row.str("id"),
                status = "scheduled",
            ),
        )
    }

    // 5. Source freshness
    for (row in driver.prepare(
        "SELECT id, label, state, last_ingested_at FROM source_instances",
    ).all()) {
        if (row.str("state") != "active") continue
        val last = row.str("last_ingested_at")
        val ageDays = if (last != null) {
            val t = parseIsoEpoch(last)
            if (t == null) Double.POSITIVE_INFINITY else (now - t) / DAY_MS
        } else {
            Double.POSITIVE_INFINITY
        }
        if (ageDays > freshnessThresholdDays) {
            items.add(
                TimelineItem(
                    id = "tl-fresh-${row.str("id") ?: continue}",
                    kind = "freshness_review",
                    title = "数据来源需要刷新：${row.str("label")}",
                    subtitle = if (last != null) "最近更新：${last.take(10)}" else "从未导入过数据。",
                    scheduledAt = null,
                    bucket = "attention",
                    priority = 1,
                    sourceType = "source_freshness",
                    sourceId = row.str("id") ?: "",
                    actionTarget = row.str("id"),
                    status = "stale",
                ),
            )
        }
    }

    return items.sortedWith(
        compareBy<TimelineItem> { BUCKET_ORDER.indexOf(it.bucket) }
            .thenByDescending { it.priority }
            .thenBy { it.scheduledAt ?: "" }
            .thenBy { it.id },
    )
}

private data class ParsedAction(
    val id: String,
    val title: String,
    val phase: String?,
    val verificationStatus: String?,
)

private fun parseActions(json: String): List<ParsedAction> {
    val parsed = try {
        JsonParser.parse(json)
    } catch (_: Throwable) {
        return emptyList()
    }
    val arr = parsed as? Json.Arr ?: return emptyList()
    return arr.items.mapNotNull { item ->
        val o = item as? Json.Obj ?: return@mapNotNull null
        val id = (o["id"] as? Json.Str)?.value ?: return@mapNotNull null
        val title = (o["title"] as? Json.Str)?.value ?: ""
        val phase = (o["phase"] as? Json.Str)?.value
        val verif = o["verification"] as? Json.Obj
        val vs = (verif?.get("status") as? Json.Str)?.value
        ParsedAction(id, title, phase, vs)
    }
}

private fun expiryOf(fieldsJson: String?): String? {
    if (fieldsJson == null) return null
    val parsed = try {
        JsonParser.parse(fieldsJson)
    } catch (_: Throwable) {
        return null
    }
    val o = parsed as? Json.Obj ?: return null
    val raw = (o["expiryDate"] as? Json.Str)?.value ?: return null
    return raw.ifEmpty { null }
}
