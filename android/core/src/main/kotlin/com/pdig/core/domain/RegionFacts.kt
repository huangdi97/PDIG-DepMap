package com.pdig.core.domain

import com.pdig.core.generated.CanonicalSpec
import com.pdig.core.generated.RegionFacet
import com.pdig.core.generated.RegionFactState
import com.pdig.core.generated.VerificationBasisType
import com.pdig.core.json.Json
import com.pdig.core.json.JsonParser

/**
 * R39 governed RegionFact decoder.
 *
 * Geography is explicit Reality. This decoder consumes only Node.fields.region_facts;
 * it never looks at currency, issuer/provider names, identifier prefixes, locale,
 * timezone, IP, presentation centroids or the user's current device location.
 *
 * Malformed items fail closed independently so one bad RegionFact cannot erase
 * valid sibling facts.
 */
data class ConfirmedRegionFact(
    val id: String,
    val facet: RegionFacet,
    val territoryCode: String,
    val subdivisionCode: String?,
    val state: RegionFactState,
    val verificationBasisType: VerificationBasisType,
    val confirmedAt: String,
    val evidenceRefs: List<String>,
    val validFrom: String?,
    val validUntil: String?,
    val retiredAt: String?,
)

enum class RegionLensSelectionStatus {
    SELECTED,
    NEEDS_REVIEW,
    UNKNOWN,
}

data class RegionLensSelection(
    val status: RegionLensSelectionStatus,
    val territoryCode: String? = null,
    val facet: RegionFacet? = null,
)

/**
 * Decode every individually valid governed RegionFact from the Node.fields envelope.
 *
 * Container-level corruption/version drift returns an empty list. Item-level
 * corruption drops only the invalid item.
 */
fun governedRegionFacts(fieldsJson: String): List<ConfirmedRegionFact> {
    val root = runCatching { JsonParser.parse(fieldsJson) }.getOrNull() as? Json.Obj
        ?: return emptyList()
    val container = root["region_facts"] as? Json.Obj ?: return emptyList()

    val version = (container["version"] as? Json.Num)
        ?.runCatching { asLong() }
        ?.getOrNull()
        ?: return emptyList()
    if (version != 1L) return emptyList()

    val items = container["items"] as? Json.Arr ?: return emptyList()
    return items.items.mapNotNull { raw ->
        decodeRegionFact(raw as? Json.Obj ?: return@mapNotNull null)
    }
}

fun currentConfirmedRegionFacts(fieldsJson: String): List<ConfirmedRegionFact> =
    governedRegionFacts(fieldsJson).filter { it.state == RegionFactState.CONFIRMED }

/**
 * Deterministic default consumer lens.
 *
 * PHYSICAL_LOCATION is deliberately excluded from the default lens. It is a
 * distinct, volatile and privacy-sensitive fact that requires an explicit view.
 *
 * At the first precedence facet with any confirmed facts:
 * - exactly one distinct territory => SELECTED;
 * - more than one => NEEDS_REVIEW (never pick silently);
 * - no supported facts => UNKNOWN.
 */
fun defaultRegionLensSelection(fieldsJson: String): RegionLensSelection {
    val facts = currentConfirmedRegionFacts(fieldsJson)
    val precedence = listOf(
        RegionFacet.NUMBERING_TERRITORY,
        RegionFacet.ISSUANCE_JURISDICTION,
        RegionFacet.SERVICE_MARKET,
        RegionFacet.PROVIDER_JURISDICTION,
        RegionFacet.USER_CONFIRMED_CONTEXT,
    )
    for (facet in precedence) {
        val territories = facts
            .filter { it.facet == facet }
            .map { it.territoryCode }
            .distinct()
            .sorted()
        if (territories.isEmpty()) continue
        if (territories.size == 1) {
            return RegionLensSelection(
                status = RegionLensSelectionStatus.SELECTED,
                territoryCode = territories.single(),
                facet = facet,
            )
        }
        return RegionLensSelection(
            status = RegionLensSelectionStatus.NEEDS_REVIEW,
            facet = facet,
        )
    }
    return RegionLensSelection(RegionLensSelectionStatus.UNKNOWN)
}

private fun decodeRegionFact(item: Json.Obj): ConfirmedRegionFact? {
    val id = requiredText(item, "id") ?: return null
    if (id.length > 160 || containsControl(id)) return null

    val facet = (item["facet"] as? Json.Str)
        ?.value
        ?.let(RegionFacet::fromWire)
        ?: return null

    val territory = requiredText(item, "territory_code")?.uppercase() ?: return null
    // Lower-case input is not accepted through accidental normalization: the stored
    // confirmed value itself must already be a canonical alpha-2 code.
    val rawTerritory = (item["territory_code"] as? Json.Str)?.value?.trim() ?: return null
    if (rawTerritory != territory) return null
    if (territory !in CanonicalSpec.ISO_3166_ALPHA2_TERRITORY_CODES) return null

    val subdivision = optionalText(item, "subdivision_code") ?: if (item["subdivision_code"] != null) return null else null
    if (subdivision != null && !validSubdivision(territory, subdivision)) return null

    val state = (item["state"] as? Json.Str)
        ?.value
        ?.let(RegionFactState::fromWire)
        ?: return null

    val basis = (item["verification_basis_type"] as? Json.Str)
        ?.value
        ?.let(VerificationBasisType::fromWire)
        ?: return null

    val confirmedAt = requiredText(item, "confirmed_at") ?: return null
    if (containsControl(confirmedAt)) return null

    val evidenceRefs = stringArray(item, "evidence_refs") ?: return null
    if (evidenceRefs.any { it.length > 512 || containsControl(it) }) return null

    val validFrom = optionalTextStrict(item, "valid_from") ?: if (item["valid_from"] != null) return null else null
    val validUntil = optionalTextStrict(item, "valid_until") ?: if (item["valid_until"] != null) return null else null
    val retiredAt = optionalTextStrict(item, "retired_at") ?: if (item["retired_at"] != null) return null else null

    return ConfirmedRegionFact(
        id = id,
        facet = facet,
        territoryCode = territory,
        subdivisionCode = subdivision,
        state = state,
        verificationBasisType = basis,
        confirmedAt = confirmedAt,
        evidenceRefs = evidenceRefs,
        validFrom = validFrom,
        validUntil = validUntil,
        retiredAt = retiredAt,
    )
}

private fun requiredText(obj: Json.Obj, key: String): String? =
    (obj[key] as? Json.Str)
        ?.value
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

private fun optionalText(obj: Json.Obj, key: String): String? =
    (obj[key] as? Json.Str)
        ?.value
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

private fun optionalTextStrict(obj: Json.Obj, key: String): String? {
    val raw = obj[key] ?: return null
    val value = (raw as? Json.Str)?.value?.trim() ?: return null
    return value.takeIf { it.isNotEmpty() }
}

private fun stringArray(obj: Json.Obj, key: String): List<String>? {
    val raw = obj[key] ?: return emptyList()
    if (raw !is Json.Arr) return null
    val values = raw.items.mapNotNull { (it as? Json.Str)?.value }
    return values.takeIf { it.size == raw.items.size }
}

private fun validSubdivision(territory: String, value: String): Boolean {
    if (!value.startsWith("$territory-")) return false
    val suffix = value.removePrefix("$territory-")
    return suffix.length in 1..3 && suffix.all { it in 'A'..'Z' || it in '0'..'9' }
}

private fun containsControl(value: String): Boolean =
    value.any { it.code < 0x20 || it.code == 0x7F }
