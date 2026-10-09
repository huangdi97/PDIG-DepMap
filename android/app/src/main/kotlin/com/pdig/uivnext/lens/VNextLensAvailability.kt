package com.pdig.uivnext.lens

/**
 * Product-Lens capability gates.
 *
 * A design can be complete while a user-visible capability remains forbidden.
 * This table prevents future UI cleanup from exposing Identity/Recovery before
 * their governed data/solver exists.
 */
internal enum class VNextLens {
    REGION,
    DEPENDENCY,
    CHANGE,
    IDENTITY,
    RECOVERY,
}

internal enum class VNextLensAvailability {
    VISIBLE,
    HIDDEN_UNTIL_CANONICAL,
    HIDDEN_UNTIL_SOLVER,
}

internal data class VNextLensGate(
    val lens: VNextLens,
    val availability: VNextLensAvailability,
    val requiresNewPrimaryDestination: Boolean = false,
    val reason: String,
)

internal val V_NEXT_LENS_GATES: List<VNextLensGate> = listOf(
    VNextLensGate(
        lens = VNextLens.REGION,
        availability = VNextLensAvailability.VISIBLE,
        reason = "Region is backed by current reference/runtime spatial data.",
    ),
    VNextLensGate(
        lens = VNextLens.DEPENDENCY,
        availability = VNextLensAvailability.VISIBLE,
        reason = "Focused object Impact Lens has a bounded truth projection.",
    ),
    VNextLensGate(
        lens = VNextLens.CHANGE,
        availability = VNextLensAvailability.VISIBLE,
        reason = "Change Center and supported focused scenarios are implemented.",
    ),
    VNextLensGate(
        lens = VNextLens.IDENTITY,
        availability = VNextLensAvailability.HIDDEN_UNTIL_CANONICAL,
        reason = "Identity Context membership has a complete proposal but no governed Canonical source.",
    ),
    VNextLensGate(
        lens = VNextLens.RECOVERY,
        availability = VNextLensAvailability.HIDDEN_UNTIL_SOLVER,
        reason = "Recovery Incident design is complete but post-incident solver/runtime support is absent.",
    ),
)

internal fun lensGate(lens: VNextLens): VNextLensGate =
    V_NEXT_LENS_GATES.first { it.lens == lens }

internal fun canRenderLensEntry(lens: VNextLens): Boolean =
    lensGate(lens).availability == VNextLensAvailability.VISIBLE

internal fun visibleLenses(): Set<VNextLens> =
    V_NEXT_LENS_GATES
        .filter { it.availability == VNextLensAvailability.VISIBLE }
        .mapTo(linkedSetOf()) { it.lens }
