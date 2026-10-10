package com.pdig.core.domain

import com.pdig.core.generated.MaintenanceCadenceKind
import com.pdig.core.generated.MaintenanceFactKind
import com.pdig.core.generated.MaintenanceFactState
import com.pdig.core.generated.MaintenanceOverflowPolicy
import com.pdig.core.generated.MaintenanceScheduleKind
import com.pdig.core.generated.MaintenanceScheduleState
import com.pdig.core.generated.MaintenanceValueType
import com.pdig.core.generated.NodeKind
import com.pdig.core.generated.VerificationBasisType
import com.pdig.core.json.Json
import com.pdig.core.json.JsonParser
import java.time.LocalDate
import java.time.YearMonth

data class ConfirmedMaintenanceFact(
    val id: String,
    val kind: MaintenanceFactKind,
    val valueType: MaintenanceValueType,
    val value: String,
    val state: MaintenanceFactState,
    val verificationBasisType: VerificationBasisType,
    val confirmedAt: String,
    val evidenceRefs: List<String>,
    val validFrom: String?,
    val validUntil: String?,
    val retiredAt: String?,
)

data class ConfirmedMaintenanceCadence(
    val kind: MaintenanceCadenceKind,
    val dueAt: String? = null,
    val dayOfMonth: Int? = null,
    val month: Int? = null,
    val day: Int? = null,
    val overflowPolicy: MaintenanceOverflowPolicy? = null,
    val intervalDays: Int? = null,
    val anchorDate: String? = null,
)

data class ConfirmedMaintenanceSchedule(
    val id: String,
    val kind: MaintenanceScheduleKind,
    val state: MaintenanceScheduleState,
    val cadence: ConfirmedMaintenanceCadence,
    val verificationBasisType: VerificationBasisType,
    val confirmedAt: String,
    val evidenceRefs: List<String>,
    val lastCompletedAt: String?,
    val retiredAt: String?,
)

data class ConfirmedMaintenanceProfile(
    val facts: List<ConfirmedMaintenanceFact>,
    val schedules: List<ConfirmedMaintenanceSchedule>,
)

/**
 * R40 governed lifecycle/maintenance decoder.
 *
 * - Provider policy is not Personal Reality.
 * - Time passage never mutates durable state.
 * - Invalid individual items are dropped without destroying valid siblings.
 * - Number lifecycle requires a governed confirmed PHONE_NUMBER identity subtype.
 */
fun governedMaintenanceProfile(
    kind: NodeKind,
    fieldsJson: String,
): ConfirmedMaintenanceProfile {
    val root = runCatching { JsonParser.parse(fieldsJson) }.getOrNull() as? Json.Obj
        ?: return ConfirmedMaintenanceProfile(emptyList(), emptyList())
    val container = root["maintenance_profile"] as? Json.Obj
        ?: return ConfirmedMaintenanceProfile(emptyList(), emptyList())

    val version = (container["version"] as? Json.Num)
        ?.runCatching { asLong() }
        ?.getOrNull()
        ?: return ConfirmedMaintenanceProfile(emptyList(), emptyList())
    if (version != 1L) return ConfirmedMaintenanceProfile(emptyList(), emptyList())

    val phoneAuthority = kind == NodeKind.IDENTITY_ANCHOR &&
        confirmedIdentityAnchorProfile(kind, fieldsJson)?.subtype?.wire == "phone_number"

    val facts = when (val raw = container["facts"]) {
        null -> emptyList()
        is Json.Arr -> raw.items.mapNotNull { item ->
            decodeFact(
                kind = kind,
                phoneAuthority = phoneAuthority,
                item = item as? Json.Obj ?: return@mapNotNull null,
            )
        }
        else -> emptyList()
    }

    val schedules = when (val raw = container["schedules"]) {
        null -> emptyList()
        is Json.Arr -> raw.items.mapNotNull { item ->
            decodeSchedule(
                kind = kind,
                phoneAuthority = phoneAuthority,
                item = item as? Json.Obj ?: return@mapNotNull null,
            )
        }
        else -> emptyList()
    }

    return ConfirmedMaintenanceProfile(facts = facts, schedules = schedules)
}

fun currentMaintenanceFacts(
    kind: NodeKind,
    fieldsJson: String,
): List<ConfirmedMaintenanceFact> =
    governedMaintenanceProfile(kind, fieldsJson).facts
        .filter { it.state == MaintenanceFactState.CONFIRMED }

fun currentMaintenanceSchedules(
    kind: NodeKind,
    fieldsJson: String,
): List<ConfirmedMaintenanceSchedule> =
    governedMaintenanceProfile(kind, fieldsJson).schedules
        .filter { it.state == MaintenanceScheduleState.ACTIVE || it.state == MaintenanceScheduleState.NEEDS_REVIEW }

private fun decodeFact(
    kind: NodeKind,
    phoneAuthority: Boolean,
    item: Json.Obj,
): ConfirmedMaintenanceFact? {
    val id = requiredText(item, "id") ?: return null
    if (!validBoundedText(id, 160)) return null

    val factKind = (item["kind"] as? Json.Str)
        ?.value
        ?.let { MaintenanceFactKind.fromWire(it) }
        ?: return null
    if (!factApplicable(kind, phoneAuthority, factKind)) return null

    val valueType = (item["value_type"] as? Json.Str)
        ?.value
        ?.let { MaintenanceValueType.fromWire(it) }
        ?: return null
    val rawValue = requiredText(item, "value") ?: return null
    if (!validFactValue(factKind, valueType, rawValue)) return null

    val state = (item["state"] as? Json.Str)
        ?.value
        ?.let { MaintenanceFactState.fromWire(it) }
        ?: return null
    val basis = (item["verification_basis_type"] as? Json.Str)
        ?.value
        ?.let { VerificationBasisType.fromWire(it) }
        ?: return null
    val confirmedAt = requiredText(item, "confirmed_at") ?: return null
    if (!validBoundedText(confirmedAt, 96)) return null

    val evidenceRefs = stringArray(item, "evidence_refs") ?: return null
    if (evidenceRefs.any { !validBoundedText(it, 512) }) return null

    val validFrom = strictOptionalText(item, "valid_from") ?: if (item["valid_from"] != null) return null else null
    val validUntil = strictOptionalText(item, "valid_until") ?: if (item["valid_until"] != null) return null else null
    val retiredAt = strictOptionalText(item, "retired_at") ?: if (item["retired_at"] != null) return null else null

    return ConfirmedMaintenanceFact(
        id = id,
        kind = factKind,
        valueType = valueType,
        value = rawValue,
        state = state,
        verificationBasisType = basis,
        confirmedAt = confirmedAt,
        evidenceRefs = evidenceRefs,
        validFrom = validFrom,
        validUntil = validUntil,
        retiredAt = retiredAt,
    )
}

private fun decodeSchedule(
    kind: NodeKind,
    phoneAuthority: Boolean,
    item: Json.Obj,
): ConfirmedMaintenanceSchedule? {
    val id = requiredText(item, "id") ?: return null
    if (!validBoundedText(id, 160)) return null

    val scheduleKind = (item["kind"] as? Json.Str)
        ?.value
        ?.let { MaintenanceScheduleKind.fromWire(it) }
        ?: return null
    if (!scheduleApplicable(kind, phoneAuthority, scheduleKind)) return null

    val state = (item["state"] as? Json.Str)
        ?.value
        ?.let { MaintenanceScheduleState.fromWire(it) }
        ?: return null
    val cadenceObj = item["cadence"] as? Json.Obj ?: return null
    val cadence = decodeCadence(cadenceObj) ?: return null
    if (!cadenceApplicable(scheduleKind, cadence.kind)) return null

    val basis = (item["verification_basis_type"] as? Json.Str)
        ?.value
        ?.let { VerificationBasisType.fromWire(it) }
        ?: return null
    val confirmedAt = requiredText(item, "confirmed_at") ?: return null
    if (!validBoundedText(confirmedAt, 96)) return null

    val evidenceRefs = stringArray(item, "evidence_refs") ?: return null
    if (evidenceRefs.any { !validBoundedText(it, 512) }) return null

    val lastCompletedAt = strictOptionalText(item, "last_completed_at")
        ?: if (item["last_completed_at"] != null) return null else null
    val retiredAt = strictOptionalText(item, "retired_at")
        ?: if (item["retired_at"] != null) return null else null

    return ConfirmedMaintenanceSchedule(
        id = id,
        kind = scheduleKind,
        state = state,
        cadence = cadence,
        verificationBasisType = basis,
        confirmedAt = confirmedAt,
        evidenceRefs = evidenceRefs,
        lastCompletedAt = lastCompletedAt,
        retiredAt = retiredAt,
    )
}

private fun decodeCadence(obj: Json.Obj): ConfirmedMaintenanceCadence? {
    val kind = (obj["kind"] as? Json.Str)
        ?.value
        ?.let { MaintenanceCadenceKind.fromWire(it) }
        ?: return null

    return when (kind) {
        MaintenanceCadenceKind.ONE_TIME -> {
            val dueAt = requiredText(obj, "due_at") ?: return null
            if (!validBoundedText(dueAt, 96)) return null
            ConfirmedMaintenanceCadence(kind = kind, dueAt = dueAt)
        }

        MaintenanceCadenceKind.MONTHLY_DAY -> {
            val day = intField(obj, "day_of_month") ?: return null
            if (day !in 1..31) return null
            val overflow = overflowPolicy(obj) ?: return null
            ConfirmedMaintenanceCadence(
                kind = kind,
                dayOfMonth = day,
                overflowPolicy = overflow,
            )
        }

        MaintenanceCadenceKind.YEARLY_MONTH_DAY -> {
            val month = intField(obj, "month") ?: return null
            val day = intField(obj, "day") ?: return null
            if (month !in 1..12 || day !in 1..31) return null
            val overflow = overflowPolicy(obj) ?: return null
            ConfirmedMaintenanceCadence(
                kind = kind,
                month = month,
                day = day,
                overflowPolicy = overflow,
            )
        }

        MaintenanceCadenceKind.INTERVAL_DAYS -> {
            val interval = intField(obj, "interval_days") ?: return null
            if (interval !in 1..3660) return null
            val anchor = requiredText(obj, "anchor_date") ?: return null
            if (!DATE_ONLY.matches(anchor)) return null
            ConfirmedMaintenanceCadence(
                kind = kind,
                intervalDays = interval,
                anchorDate = anchor,
            )
        }

        MaintenanceCadenceKind.MANUAL_ONLY ->
            ConfirmedMaintenanceCadence(kind = kind)
    }
}

private fun factApplicable(
    nodeKind: NodeKind,
    phoneAuthority: Boolean,
    factKind: MaintenanceFactKind,
): Boolean = when (factKind) {
    MaintenanceFactKind.CARD_ANNUAL_FEE_AMOUNT,
    MaintenanceFactKind.CARD_ANNUAL_FEE_CURRENCY,
    MaintenanceFactKind.CARD_BILLING_DAY,
    MaintenanceFactKind.CARD_PAYMENT_DUE_DAY,
    MaintenanceFactKind.CARD_AUTOPAY_MODE ->
        nodeKind == NodeKind.PAYMENT_INSTRUMENT

    MaintenanceFactKind.NUMBER_BILLING_MODE,
    MaintenanceFactKind.NUMBER_PLAN_COST,
    MaintenanceFactKind.NUMBER_PLAN_CURRENCY,
    MaintenanceFactKind.NUMBER_RENEWAL_METHOD ->
        phoneAuthority
}

private fun scheduleApplicable(
    nodeKind: NodeKind,
    phoneAuthority: Boolean,
    scheduleKind: MaintenanceScheduleKind,
): Boolean = when (scheduleKind) {
    MaintenanceScheduleKind.CARD_ANNUAL_FEE_CHECKPOINT,
    MaintenanceScheduleKind.CARD_BILLING_CHECKPOINT,
    MaintenanceScheduleKind.CARD_PAYMENT_DUE_CHECKPOINT ->
        nodeKind == NodeKind.PAYMENT_INSTRUMENT

    MaintenanceScheduleKind.NUMBER_KEEP_ALIVE,
    MaintenanceScheduleKind.NUMBER_PLAN_RENEWAL ->
        phoneAuthority

    MaintenanceScheduleKind.FACT_FRESHNESS_REVIEW,
    MaintenanceScheduleKind.CUSTOM_MAINTENANCE ->
        true
}

private fun cadenceApplicable(
    scheduleKind: MaintenanceScheduleKind,
    cadenceKind: MaintenanceCadenceKind,
): Boolean = when (scheduleKind) {
    MaintenanceScheduleKind.CARD_ANNUAL_FEE_CHECKPOINT ->
        cadenceKind == MaintenanceCadenceKind.ONE_TIME ||
            cadenceKind == MaintenanceCadenceKind.YEARLY_MONTH_DAY

    MaintenanceScheduleKind.CARD_BILLING_CHECKPOINT,
    MaintenanceScheduleKind.CARD_PAYMENT_DUE_CHECKPOINT ->
        cadenceKind == MaintenanceCadenceKind.MONTHLY_DAY

    MaintenanceScheduleKind.NUMBER_KEEP_ALIVE ->
        cadenceKind == MaintenanceCadenceKind.ONE_TIME ||
            cadenceKind == MaintenanceCadenceKind.INTERVAL_DAYS ||
            cadenceKind == MaintenanceCadenceKind.MANUAL_ONLY

    MaintenanceScheduleKind.NUMBER_PLAN_RENEWAL ->
        cadenceKind == MaintenanceCadenceKind.ONE_TIME ||
            cadenceKind == MaintenanceCadenceKind.MONTHLY_DAY ||
            cadenceKind == MaintenanceCadenceKind.INTERVAL_DAYS

    MaintenanceScheduleKind.FACT_FRESHNESS_REVIEW ->
        cadenceKind == MaintenanceCadenceKind.INTERVAL_DAYS ||
            cadenceKind == MaintenanceCadenceKind.MANUAL_ONLY

    MaintenanceScheduleKind.CUSTOM_MAINTENANCE ->
        true
}

private fun validFactValue(
    kind: MaintenanceFactKind,
    valueType: MaintenanceValueType,
    value: String,
): Boolean = when (kind) {
    MaintenanceFactKind.CARD_ANNUAL_FEE_AMOUNT,
    MaintenanceFactKind.NUMBER_PLAN_COST ->
        valueType == MaintenanceValueType.DECIMAL_STRING &&
            DECIMAL_STRING.matches(value)

    MaintenanceFactKind.CARD_ANNUAL_FEE_CURRENCY,
    MaintenanceFactKind.NUMBER_PLAN_CURRENCY ->
        valueType == MaintenanceValueType.CURRENCY_CODE &&
            CURRENCY_CODE.matches(value)

    MaintenanceFactKind.CARD_BILLING_DAY,
    MaintenanceFactKind.CARD_PAYMENT_DUE_DAY ->
        valueType == MaintenanceValueType.INTEGER &&
            value.toIntOrNull()?.let { it in 1..31 && it.toString() == value } == true

    MaintenanceFactKind.CARD_AUTOPAY_MODE,
    MaintenanceFactKind.NUMBER_BILLING_MODE,
    MaintenanceFactKind.NUMBER_RENEWAL_METHOD ->
        valueType == MaintenanceValueType.TEXT &&
            validBoundedText(value, 240)
}

private fun overflowPolicy(obj: Json.Obj): MaintenanceOverflowPolicy? =
    (obj["overflow_policy"] as? Json.Str)
        ?.value
        ?.let { MaintenanceOverflowPolicy.fromWire(it) }

private fun intField(obj: Json.Obj, key: String): Int? =
    (obj[key] as? Json.Num)
        ?.runCatching { asLong() }
        ?.getOrNull()
        ?.takeIf { it in Int.MIN_VALUE..Int.MAX_VALUE }
        ?.toInt()

private fun requiredText(obj: Json.Obj, key: String): String? =
    (obj[key] as? Json.Str)
        ?.value
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

private fun strictOptionalText(obj: Json.Obj, key: String): String? {
    val raw = obj[key] ?: return null
    val value = (raw as? Json.Str)?.value?.trim() ?: return null
    return value.takeIf { validBoundedText(it, 128) }
}

private fun stringArray(obj: Json.Obj, key: String): List<String>? {
    val raw = obj[key] ?: return emptyList()
    if (raw !is Json.Arr) return null
    val values = raw.items.mapNotNull { (it as? Json.Str)?.value }
    return values.takeIf { it.size == raw.items.size }
}

private fun validBoundedText(value: String, max: Int): Boolean =
    value.isNotEmpty() && value.length <= max && !containsControl(value)

private fun containsControl(value: String): Boolean =
    value.any { it.code < 0x20 || it.code == 0x7F }

private val DECIMAL_STRING = Regex("""^(0|[1-9][0-9]*)(\.[0-9]+)?$""")
private val CURRENCY_CODE = Regex("""^[A-Z]{3}$""")
private val DATE_ONLY = Regex("""^[0-9]{4}-[0-9]{2}-[0-9]{2}$""")


data class MaintenanceFactWrite(
    val id: String,
    val kind: MaintenanceFactKind,
    val valueType: MaintenanceValueType,
    val value: String,
    val verificationBasisType: VerificationBasisType = VerificationBasisType.USER_CONFIRMED,
    val evidenceRefs: List<String> = emptyList(),
)

data class MaintenanceCadenceWrite(
    val kind: MaintenanceCadenceKind,
    val dueAt: String? = null,
    val dayOfMonth: Int? = null,
    val month: Int? = null,
    val day: Int? = null,
    val overflowPolicy: MaintenanceOverflowPolicy? = null,
    val intervalDays: Int? = null,
    val anchorDate: String? = null,
)

data class MaintenanceScheduleWrite(
    val id: String,
    val kind: MaintenanceScheduleKind,
    val cadence: MaintenanceCadenceWrite,
    val state: MaintenanceScheduleState = MaintenanceScheduleState.ACTIVE,
    val verificationBasisType: VerificationBasisType = VerificationBasisType.USER_CONFIRMED,
    val evidenceRefs: List<String> = emptyList(),
    val lastCompletedAt: String? = null,
)

/**
 * Canonical writer for an explicitly confirmed maintenance fact.
 *
 * Unknown future maintenance container versions are never overwritten. Raw sibling
 * items are preserved byte-semantically as Json values; only the item with the same
 * id is replaced. The newly built document is decoded again before being returned.
 */
fun upsertConfirmedMaintenanceFact(
    nodeKind: NodeKind,
    fieldsJson: String,
    request: MaintenanceFactWrite,
    confirmedAt: String,
): String {
    require(request.id.isNotBlank() && request.id.length <= 160) { "invalid maintenance fact id" }
    require(request.evidenceRefs.all { validBoundedText(it, 512) }) {
        "invalid maintenance fact evidence ref"
    }
    require(validBoundedText(confirmedAt, 96)) { "invalid confirmedAt" }

    val root = parseWritableRoot(fieldsJson)
    val existing = writableMaintenanceContainer(root)
    val facts = rawArray(existing, "facts").toMutableList()
    val replacement = Json.Obj(
        listOf(
            "id" to Json.Str(request.id),
            "kind" to Json.Str(request.kind.wire),
            "value_type" to Json.Str(request.valueType.wire),
            "value" to Json.Str(request.value),
            "state" to Json.Str(MaintenanceFactState.CONFIRMED.wire),
            "verification_basis_type" to Json.Str(request.verificationBasisType.wire),
            "confirmed_at" to Json.Str(confirmedAt),
            "evidence_refs" to Json.Arr(request.evidenceRefs.map { Json.Str(it) }),
        ),
    )
    replaceRawItemById(facts, request.id, replacement)

    val nextContainer = replaceObjectField(
        replaceObjectField(existing, "version", Json.Num("1")),
        "facts",
        Json.Arr(facts),
    )
    val nextRoot = replaceObjectField(root, "maintenance_profile", nextContainer)
    val output = JsonWriter.write(nextRoot)

    val decoded = governedMaintenanceProfile(nodeKind, output)
        .facts
        .firstOrNull { it.id == request.id && it.state == MaintenanceFactState.CONFIRMED }
        ?: error("maintenance fact failed Canonical applicability/value validation")
    require(
        decoded.kind == request.kind &&
            decoded.valueType == request.valueType &&
            decoded.value == request.value
    ) {
        "maintenance fact did not round-trip through Canonical decoder"
    }
    return output
}

/**
 * Atomically constructs a set of confirmed maintenance facts in memory.
 *
 * Repository callers commit the returned document once, so grouped facts such as
 * amount+currency never become an observable half-written Reality state.
 */
fun upsertConfirmedMaintenanceFacts(
    nodeKind: NodeKind,
    fieldsJson: String,
    requests: List<MaintenanceFactWrite>,
    confirmedAt: String,
): String {
    require(requests.isNotEmpty()) { "maintenance fact batch must not be empty" }
    require(requests.map { it.id }.distinct().size == requests.size) {
        "maintenance fact batch contains duplicate ids"
    }
    return requests.fold(fieldsJson) { current, request ->
        upsertConfirmedMaintenanceFact(
            nodeKind = nodeKind,
            fieldsJson = current,
            request = request,
            confirmedAt = confirmedAt,
        )
    }
}

/**
 * Canonical writer for a user/authority-confirmed maintenance schedule.
 *
 * Passing time is never treated as completion. A completion timestamp is written only
 * when explicitly supplied by the authority layer.
 */
fun upsertConfirmedMaintenanceSchedule(
    nodeKind: NodeKind,
    fieldsJson: String,
    request: MaintenanceScheduleWrite,
    confirmedAt: String,
): String {
    require(request.id.isNotBlank() && request.id.length <= 160) { "invalid maintenance schedule id" }
    require(request.evidenceRefs.all { validBoundedText(it, 512) }) {
        "invalid maintenance schedule evidence ref"
    }
    require(validBoundedText(confirmedAt, 96)) { "invalid confirmedAt" }

    val root = parseWritableRoot(fieldsJson)
    val existing = writableMaintenanceContainer(root)
    val schedules = rawArray(existing, "schedules").toMutableList()

    val cadence = maintenanceCadenceJson(request.cadence)
    val fields = mutableListOf<Pair<String, Json>>(
        "id" to Json.Str(request.id),
        "kind" to Json.Str(request.kind.wire),
        "state" to Json.Str(request.state.wire),
        "cadence" to cadence,
        "verification_basis_type" to Json.Str(request.verificationBasisType.wire),
        "confirmed_at" to Json.Str(confirmedAt),
        "evidence_refs" to Json.Arr(request.evidenceRefs.map { Json.Str(it) }),
    )
    request.lastCompletedAt?.let {
        require(validBoundedText(it, 128)) { "invalid lastCompletedAt" }
        fields += "last_completed_at" to Json.Str(it)
    }
    replaceRawItemById(schedules, request.id, Json.Obj(fields))

    val nextContainer = replaceObjectField(
        replaceObjectField(existing, "version", Json.Num("1")),
        "schedules",
        Json.Arr(schedules),
    )
    val nextRoot = replaceObjectField(root, "maintenance_profile", nextContainer)
    val output = JsonWriter.write(nextRoot)

    val decoded = governedMaintenanceProfile(nodeKind, output)
        .schedules
        .firstOrNull { it.id == request.id }
        ?: error("maintenance schedule failed Canonical applicability/cadence validation")
    require(decoded.kind == request.kind && decoded.state == request.state) {
        "maintenance schedule did not round-trip through Canonical decoder"
    }
    return output
}

private fun parseWritableRoot(fieldsJson: String): Json.Obj =
    runCatching { JsonParser.parse(fieldsJson) as? Json.Obj }
        .getOrNull()
        ?: error("node fields_json is not a writable object")

private fun writableMaintenanceContainer(root: Json.Obj): Json.Obj {
    val raw = root["maintenance_profile"] ?: return Json.Obj(emptyList())
    val container = raw as? Json.Obj ?: error("maintenance_profile is not an object")
    val version = container["version"]
    if (version != null) {
        val n = (version as? Json.Num)?.runCatching { asLong() }?.getOrNull()
            ?: error("maintenance_profile version is invalid")
        require(n == 1L) {
            "refusing to overwrite unsupported maintenance_profile version $n"
        }
    }
    return container
}

private fun rawArray(container: Json.Obj, key: String): List<Json> {
    val raw = container[key] ?: return emptyList()
    return (raw as? Json.Arr)?.items
        ?: error("maintenance_profile.$key must be an array")
}

private fun replaceRawItemById(
    items: MutableList<Json>,
    id: String,
    replacement: Json.Obj,
) {
    var replaced = false
    for (i in items.indices) {
        val obj = items[i] as? Json.Obj ?: continue
        if ((obj["id"] as? Json.Str)?.value == id) {
            if (!replaced) {
                items[i] = replacement
                replaced = true
            } else {
                // Duplicate ids are not allowed to survive an authoritative upsert.
                items[i] = Json.Null
            }
        }
    }
    items.removeAll { it === Json.Null }
    if (!replaced) items += replacement
}

private fun replaceObjectField(obj: Json.Obj, key: String, value: Json): Json.Obj {
    val out = mutableListOf<Pair<String, Json>>()
    var replaced = false
    obj.fields.forEach { (name, existing) ->
        if (name == key) {
            if (!replaced) {
                out += key to value
                replaced = true
            }
        } else {
            out += name to existing
        }
    }
    if (!replaced) out += key to value
    return Json.Obj(out)
}

private fun maintenanceCadenceJson(write: MaintenanceCadenceWrite): Json.Obj {
    val fields = mutableListOf<Pair<String, Json>>(
        "kind" to Json.Str(write.kind.wire),
    )
    when (write.kind) {
        MaintenanceCadenceKind.ONE_TIME -> {
            val dueAt = requireNotNull(write.dueAt) { "one_time requires dueAt" }
            require(validBoundedText(dueAt, 96)) { "invalid one_time dueAt" }
            fields += "due_at" to Json.Str(dueAt)
        }
        MaintenanceCadenceKind.MONTHLY_DAY -> {
            val day = requireNotNull(write.dayOfMonth) { "monthly_day requires dayOfMonth" }
            require(day in 1..31) { "monthly day out of range" }
            val overflow = requireNotNull(write.overflowPolicy) {
                "monthly_day requires overflowPolicy"
            }
            fields += "day_of_month" to Json.Num(day.toString())
            fields += "overflow_policy" to Json.Str(overflow.wire)
        }
        MaintenanceCadenceKind.YEARLY_MONTH_DAY -> {
            val month = requireNotNull(write.month) { "yearly_month_day requires month" }
            val day = requireNotNull(write.day) { "yearly_month_day requires day" }
            require(month in 1..12 && day in 1..31) { "yearly month/day out of range" }
            val overflow = requireNotNull(write.overflowPolicy) {
                "yearly_month_day requires overflowPolicy"
            }
            fields += "month" to Json.Num(month.toString())
            fields += "day" to Json.Num(day.toString())
            fields += "overflow_policy" to Json.Str(overflow.wire)
        }
        MaintenanceCadenceKind.INTERVAL_DAYS -> {
            val interval = requireNotNull(write.intervalDays) { "interval_days requires intervalDays" }
            require(interval in 1..3660) { "intervalDays out of range" }
            val anchor = requireNotNull(write.anchorDate) { "interval_days requires anchorDate" }
            require(DATE_ONLY.matches(anchor)) { "invalid interval anchorDate" }
            fields += "interval_days" to Json.Num(interval.toString())
            fields += "anchor_date" to Json.Str(anchor)
        }
        MaintenanceCadenceKind.MANUAL_ONLY -> Unit
    }
    return Json.Obj(fields)
}


enum class MaintenanceOccurrenceStatus {
    UPCOMING,
    DUE,
    OVERDUE,
    NEEDS_REVIEW,
}

data class MaintenanceOccurrence(
    val scheduleId: String,
    val scheduleKind: MaintenanceScheduleKind,
    val dueDate: String?,
    val status: MaintenanceOccurrenceStatus,
    val explanation: String,
)

/**
 * Derive the next actionable occurrence without mutating Reality.
 *
 * Crucial invariant: an elapsed due date becomes OVERDUE, never COMPLETED. Only an
 * explicit authority write may move lastCompletedAt / durable schedule state.
 */
fun nextMaintenanceOccurrence(
    schedule: ConfirmedMaintenanceSchedule,
    todayIso: String,
): MaintenanceOccurrence? {
    if (schedule.state == MaintenanceScheduleState.PAUSED ||
        schedule.state == MaintenanceScheduleState.RETIRED
    ) {
        return null
    }
    val today = parseDateOnly(todayIso) ?: return reviewOccurrence(
        schedule,
        "当前日期无法解析，需要人工核对维护节点",
    )
    if (schedule.state == MaintenanceScheduleState.NEEDS_REVIEW) {
        return reviewOccurrence(schedule, "维护计划已标记为需要核对")
    }

    val due = when (schedule.cadence.kind) {
        MaintenanceCadenceKind.ONE_TIME ->
            schedule.cadence.dueAt?.let(::parseDateOnly)
                ?: return reviewOccurrence(schedule, "一次性维护日期缺失或无效")

        MaintenanceCadenceKind.INTERVAL_DAYS -> {
            val interval = schedule.cadence.intervalDays
                ?: return reviewOccurrence(schedule, "维护间隔缺失")
            val base = schedule.lastCompletedAt?.let(::parseDateOnly)
                ?: schedule.cadence.anchorDate?.let(::parseDateOnly)
                ?: return reviewOccurrence(schedule, "维护周期锚点缺失")
            base.plusDays(interval.toLong())
        }

        MaintenanceCadenceKind.MONTHLY_DAY ->
            nextMonthlyOccurrence(
                today = today,
                day = schedule.cadence.dayOfMonth
                    ?: return reviewOccurrence(schedule, "每月维护日缺失"),
                overflow = schedule.cadence.overflowPolicy
                    ?: return reviewOccurrence(schedule, "每月溢出策略缺失"),
            ) ?: return reviewOccurrence(schedule, "本周期日期需要人工确认")

        MaintenanceCadenceKind.YEARLY_MONTH_DAY ->
            nextYearlyOccurrence(
                today = today,
                month = schedule.cadence.month
                    ?: return reviewOccurrence(schedule, "年度维护月缺失"),
                day = schedule.cadence.day
                    ?: return reviewOccurrence(schedule, "年度维护日缺失"),
                overflow = schedule.cadence.overflowPolicy
                    ?: return reviewOccurrence(schedule, "年度溢出策略缺失"),
            ) ?: return reviewOccurrence(schedule, "本周期日期需要人工确认")

        MaintenanceCadenceKind.MANUAL_ONLY -> return null
    }

    val status = when {
        due.isBefore(today) -> MaintenanceOccurrenceStatus.OVERDUE
        due.isEqual(today) -> MaintenanceOccurrenceStatus.DUE
        else -> MaintenanceOccurrenceStatus.UPCOMING
    }
    return MaintenanceOccurrence(
        scheduleId = schedule.id,
        scheduleKind = schedule.kind,
        dueDate = due.toString(),
        status = status,
        explanation = occurrenceExplanation(schedule, due),
    )
}

fun maintenanceOccurrences(
    profile: ConfirmedMaintenanceProfile,
    todayIso: String,
): List<MaintenanceOccurrence> =
    profile.schedules
        .mapNotNull { nextMaintenanceOccurrence(it, todayIso) }
        .sortedWith(
            compareBy<MaintenanceOccurrence> {
                when (it.status) {
                    MaintenanceOccurrenceStatus.OVERDUE -> 0
                    MaintenanceOccurrenceStatus.DUE -> 1
                    MaintenanceOccurrenceStatus.NEEDS_REVIEW -> 2
                    MaintenanceOccurrenceStatus.UPCOMING -> 3
                }
            }.thenBy { it.dueDate ?: "9999-12-31" }
                .thenBy { it.scheduleId },
        )

private fun nextMonthlyOccurrence(
    today: LocalDate,
    day: Int,
    overflow: MaintenanceOverflowPolicy,
): LocalDate? {
    var cursor = YearMonth.from(today)
    repeat(24) {
        val resolved = resolveDay(cursor.year, cursor.monthValue, day, overflow)
        if (resolved.needsReview) return null
        val date = resolved.date
        if (date != null && !date.isBefore(today)) return date
        cursor = cursor.plusMonths(1)
    }
    return null
}

private fun nextYearlyOccurrence(
    today: LocalDate,
    month: Int,
    day: Int,
    overflow: MaintenanceOverflowPolicy,
): LocalDate? {
    var year = today.year
    repeat(8) {
        val resolved = resolveDay(year, month, day, overflow)
        if (resolved.needsReview) return null
        val date = resolved.date
        if (date != null && !date.isBefore(today)) return date
        year += 1
    }
    return null
}

private data class ResolvedMaintenanceDate(
    val date: LocalDate?,
    val needsReview: Boolean = false,
)

private fun resolveDay(
    year: Int,
    month: Int,
    requestedDay: Int,
    overflow: MaintenanceOverflowPolicy,
): ResolvedMaintenanceDate {
    if (month !in 1..12 || requestedDay !in 1..31) {
        return ResolvedMaintenanceDate(null, needsReview = true)
    }
    val ym = runCatching { YearMonth.of(year, month) }.getOrNull()
        ?: return ResolvedMaintenanceDate(null, needsReview = true)
    val maxDay = ym.lengthOfMonth()
    if (requestedDay <= maxDay) {
        return ResolvedMaintenanceDate(LocalDate.of(year, month, requestedDay))
    }
    return when (overflow) {
        MaintenanceOverflowPolicy.CLAMP_TO_LAST_DAY ->
            ResolvedMaintenanceDate(LocalDate.of(year, month, maxDay))
        MaintenanceOverflowPolicy.SKIP_OCCURRENCE ->
            ResolvedMaintenanceDate(null)
        MaintenanceOverflowPolicy.USER_CONFIRM ->
            ResolvedMaintenanceDate(null, needsReview = true)
    }
}

private fun parseDateOnly(raw: String): LocalDate? {
    val value = raw.trim().take(10)
    if (!DATE_ONLY.matches(value)) return null
    return runCatching { LocalDate.parse(value) }.getOrNull()
}

private fun reviewOccurrence(
    schedule: ConfirmedMaintenanceSchedule,
    explanation: String,
): MaintenanceOccurrence =
    MaintenanceOccurrence(
        scheduleId = schedule.id,
        scheduleKind = schedule.kind,
        dueDate = null,
        status = MaintenanceOccurrenceStatus.NEEDS_REVIEW,
        explanation = explanation,
    )

private fun occurrenceExplanation(
    schedule: ConfirmedMaintenanceSchedule,
    due: LocalDate,
): String = when (schedule.kind) {
    MaintenanceScheduleKind.CARD_ANNUAL_FEE_CHECKPOINT ->
        "依据已确认年费维护计划，下一检查节点为 $due"
    MaintenanceScheduleKind.CARD_BILLING_CHECKPOINT ->
        "依据已确认账单周期，下一账单节点为 $due"
    MaintenanceScheduleKind.CARD_PAYMENT_DUE_CHECKPOINT ->
        "依据已确认还款周期，下一还款节点为 $due"
    MaintenanceScheduleKind.NUMBER_KEEP_ALIVE ->
        "依据已确认保号周期，下一保号节点为 $due"
    MaintenanceScheduleKind.NUMBER_PLAN_RENEWAL ->
        "依据已确认号码套餐周期，下一续费节点为 $due"
    MaintenanceScheduleKind.FACT_FRESHNESS_REVIEW ->
        "依据资料新鲜度计划，下一复核节点为 $due"
    MaintenanceScheduleKind.CUSTOM_MAINTENANCE ->
        "依据用户确认的维护计划，下一节点为 $due"
}
