package com.pdig.core.domain

import com.pdig.core.generated.IdentityAnchorSubtype
import com.pdig.core.generated.NodeKind
import com.pdig.core.generated.VerificationBasisType
import com.pdig.core.json.Json
import com.pdig.core.json.JsonParser

/**
 * Canonical R37 identity-anchor profile decoder.
 *
 * A free-form/bare fields_json.subtype key is intentionally ignored. Production
 * identity surfaces may consume a subtype only when the governed profile carries
 * explicit confirmation provenance.
 */
data class ConfirmedIdentityAnchorProfile(
    val subtype: IdentityAnchorSubtype,
    val verificationBasisType: VerificationBasisType,
    val confirmedAt: String,
    val evidenceRefs: List<String>,
)

fun confirmedIdentityAnchorProfile(
    kind: NodeKind,
    fieldsJson: String,
): ConfirmedIdentityAnchorProfile? {
    if (kind != NodeKind.IDENTITY_ANCHOR) return null

    val root = runCatching { JsonParser.parse(fieldsJson) }.getOrNull() as? Json.Obj
        ?: return null
    val profile = root["identity_anchor_profile"] as? Json.Obj ?: return null

    val version = (profile["version"] as? Json.Num)
        ?.runCatching { asLong() }
        ?.getOrNull()
        ?: return null
    if (version != 1L) return null

    val subtypeWire = (profile["subtype"] as? Json.Str)?.value ?: return null
    val subtype = IdentityAnchorSubtype.fromWire(subtypeWire) ?: return null

    val basisWire = (profile["verification_basis_type"] as? Json.Str)?.value ?: return null
    val basis = VerificationBasisType.fromWire(basisWire) ?: return null

    val confirmedAt = (profile["confirmed_at"] as? Json.Str)
        ?.value
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: return null

    val evidenceRefs = when (val raw = profile["evidence_refs"]) {
        null -> emptyList()
        is Json.Arr -> {
            val refs = raw.items.mapNotNull { (it as? Json.Str)?.value }
            if (refs.size != raw.items.size) return null
            refs
        }
        else -> return null
    }

    return ConfirmedIdentityAnchorProfile(
        subtype = subtype,
        verificationBasisType = basis,
        confirmedAt = confirmedAt,
        evidenceRefs = evidenceRefs,
    )
}
