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
