package com.pdig.uivnext.capability

/**
 * UI vNext capability / authority matrix.
 *
 * Product design, reference visibility and production mutation authority are
 * intentionally different axes. A screen may be valid as a read-only reference
 * while execution remains forbidden.
 */
internal enum class VNextCapability {
    FILE_IMPORT,
    MANUAL_CREATE,
    MANUAL_RELATIONSHIP,
    HUMAN_REVIEW,
    CHANGE_PHONE,
    CHANGE_PAYMENT_CARD,
    DEVICE_CONTINUITY,
    DIGITAL_RESOURCE_CONTINUITY,
    TRUSTED_HANDOFF,
    LIFECYCLE_PERSISTENCE,
    IDENTITY_CONTEXT,
    RECOVERY_PREPAREDNESS,
    RECOVERY_INCIDENT,
}

internal enum class VNextCapabilityVisibility {
    VISIBLE_REFERENCE,
    HIDDEN_UNTIL_CANONICAL,
    HIDDEN_UNTIL_SOLVER,
}

internal enum class VNextProductionAuthority {
    AVAILABLE,
    NOT_EXPOSED,
    REQUIRES_CANONICAL,
    REQUIRES_SOLVER,
}

internal data class VNextCapabilityGate(
    val capability: VNextCapability,
    val visibility: VNextCapabilityVisibility,
    val productionAuthority: VNextProductionAuthority,
    val requiresNewPrimaryDestination: Boolean = false,
    val reason: String,
)

internal val V_NEXT_CAPABILITY_MATRIX: List<VNextCapabilityGate> = listOf(
    VNextCapabilityGate(
        capability = VNextCapability.FILE_IMPORT,
        visibility = VNextCapabilityVisibility.VISIBLE_REFERENCE,
        productionAuthority = VNextProductionAuthority.AVAILABLE,
        reason = "Production already owns FileWorkflowCoordinator + AppContainer import authority; Preview stays isolated/read-only.",
    ),
    VNextCapabilityGate(
        capability = VNextCapability.MANUAL_CREATE,
        visibility = VNextCapabilityVisibility.VISIBLE_REFERENCE,
        productionAuthority = VNextProductionAuthority.NOT_EXPOSED,
        reason = "Canonical can store supported node kinds, but VNext has no reviewed AppContainer-facing manual-create authority yet.",
    ),
    VNextCapabilityGate(
        capability = VNextCapability.MANUAL_RELATIONSHIP,
        visibility = VNextCapabilityVisibility.VISIBLE_REFERENCE,
        productionAuthority = VNextProductionAuthority.NOT_EXPOSED,
        reason = "Canonical supports DependencyOrigin.MANUAL, but VNext has no reviewed AppContainer manual-dependency creation authority yet.",
    ),
    VNextCapabilityGate(
        capability = VNextCapability.HUMAN_REVIEW,
        visibility = VNextCapabilityVisibility.VISIBLE_REFERENCE,
        productionAuthority = VNextProductionAuthority.AVAILABLE,
        reason = "Proposal/Candidate/Drift production actions exist; Preview deliberately does not execute them.",
    ),
    VNextCapabilityGate(
        capability = VNextCapability.CHANGE_PHONE,
        visibility = VNextCapabilityVisibility.VISIBLE_REFERENCE,
        productionAuthority = VNextProductionAuthority.AVAILABLE,
        reason = "replace_phone_number is an active production scenario and must execute only through the authoritative change gateway.",
    ),
    VNextCapabilityGate(
        capability = VNextCapability.CHANGE_PAYMENT_CARD,
        visibility = VNextCapabilityVisibility.VISIBLE_REFERENCE,
        productionAuthority = VNextProductionAuthority.AVAILABLE,
        reason = "replace_payment_card is an active production scenario and must execute only through the authoritative change gateway.",
    ),
    VNextCapabilityGate(
        capability = VNextCapability.DEVICE_CONTINUITY,
        visibility = VNextCapabilityVisibility.HIDDEN_UNTIL_CANONICAL,
        productionAuthority = VNextProductionAuthority.REQUIRES_CANONICAL,
        reason = "Planned replace_device design requires governed factor/device-subtype semantics and a production scenario before UI exposure.",
    ),
    VNextCapabilityGate(
        capability = VNextCapability.DIGITAL_RESOURCE_CONTINUITY,
        visibility = VNextCapabilityVisibility.HIDDEN_UNTIL_CANONICAL,
        productionAuthority = VNextProductionAuthority.REQUIRES_CANONICAL,
        reason = "Future digital-resource continuity design is frozen but its resource types/capabilities/relations are not current Canonical.",
    ),
    VNextCapabilityGate(
        capability = VNextCapability.TRUSTED_HANDOFF,
        visibility = VNextCapabilityVisibility.HIDDEN_UNTIL_CANONICAL,
        productionAuthority = VNextProductionAuthority.REQUIRES_CANONICAL,
        reason = "Trusted Handoff design is future-only; provider arrangements, trusted-party authority and export semantics are not Canonical.",
    ),
    VNextCapabilityGate(
        capability = VNextCapability.LIFECYCLE_PERSISTENCE,
        visibility = VNextCapabilityVisibility.VISIBLE_REFERENCE,
        productionAuthority = VNextProductionAuthority.REQUIRES_CANONICAL,
        reason = "R19 lifecycle fields are valid product/reference semantics but are not yet governed cross-platform Canonical persistence.",
    ),
    VNextCapabilityGate(
        capability = VNextCapability.IDENTITY_CONTEXT,
        visibility = VNextCapabilityVisibility.HIDDEN_UNTIL_CANONICAL,
        productionAuthority = VNextProductionAuthority.REQUIRES_CANONICAL,
        reason = "Identity Context proposal is design-complete but membership has no Canonical authority yet.",
    ),
    VNextCapabilityGate(
        capability = VNextCapability.RECOVERY_PREPAREDNESS,
        visibility = VNextCapabilityVisibility.HIDDEN_UNTIL_CANONICAL,
        productionAuthority = VNextProductionAuthority.REQUIRES_CANONICAL,
        reason = "Recovery Preparedness UX is frozen but requires governed Factor/SecretLocator projections before the route may appear.",
    ),
    VNextCapabilityGate(
        capability = VNextCapability.RECOVERY_INCIDENT,
        visibility = VNextCapabilityVisibility.HIDDEN_UNTIL_SOLVER,
        productionAuthority = VNextProductionAuthority.REQUIRES_SOLVER,
        reason = "Recovery Incident design is complete but the failure-domain-aware solver/runtime does not exist yet.",
    ),
)

internal fun capabilityGate(capability: VNextCapability): VNextCapabilityGate =
    V_NEXT_CAPABILITY_MATRIX.first { it.capability == capability }

internal fun canShowReference(capability: VNextCapability): Boolean =
    capabilityGate(capability).visibility == VNextCapabilityVisibility.VISIBLE_REFERENCE

internal fun hasProductionAuthority(capability: VNextCapability): Boolean =
    capabilityGate(capability).productionAuthority == VNextProductionAuthority.AVAILABLE

internal fun visibleReferenceCapabilities(): Set<VNextCapability> =
    V_NEXT_CAPABILITY_MATRIX
        .filter { it.visibility == VNextCapabilityVisibility.VISIBLE_REFERENCE }
        .mapTo(linkedSetOf()) { it.capability }
