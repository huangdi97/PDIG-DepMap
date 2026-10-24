package com.pdig.app

internal enum class VNextLaunchTarget {
    REFERENCE_PREVIEW,
    PRODUCTION_REALITY_DEBUG,
    PRODUCTION_REALITY_RELEASE,
    LEGACY_PRODUCTION,
}

internal enum class ProductionUiGeneration {
    LEGACY,
    VNEXT,
}

/**
 * Parses a build-time production UI generation fail-closed.
 *
 * Unknown values are intentionally LEGACY: a typo in a release command must not
 * switch the production UI generation.
 */
internal fun productionUiGeneration(raw: String): ProductionUiGeneration =
    when (raw.trim().lowercase()) {
        "vnext" -> ProductionUiGeneration.VNEXT
        else -> ProductionUiGeneration.LEGACY
    }

/**
 * Release cutover is a two-key decision:
 *
 * generation=vnext
 * AND
 * explicit cutover approval=true
 *
 * The approval bit is intentionally independent from the generation string so an
 * inherited/accidental Gradle property cannot switch production by itself.
 */
internal fun productionVNextReleaseEnabled(
    generation: String,
    cutoverApproved: Boolean,
): Boolean =
    productionUiGeneration(generation) == ProductionUiGeneration.VNEXT &&
        cutoverApproved

/**
 * UI vNext launch policy.
 *
 * - Preview flavor always opens the synthetic reference candidate.
 * - Production release ignores ALL Intent extras.
 * - Production release opens VNext only when the two build-time cutover keys are
 *   both present; otherwise it fails closed to the legacy production shell.
 * - Production DEBUG may explicitly open:
 *   - vnext_demo       -> synthetic reference evidence
 *   - vnext_production -> real AppContainer-backed VNext behind the same LockGate
 * - A debug build may also rehearse the exact release-default path by supplying
 *   the same two build-time cutover keys and no explicit Intent override.
 *
 * No runtime/user preference, deep link, Intent extra or synthetic Preview state
 * can activate Production VNext release.
 */
internal fun resolveVNextLaunchTarget(
    flavor: String,
    explicitDemo: Boolean,
    explicitProductionVNext: Boolean,
    debugBuild: Boolean,
    productionUiGeneration: String = "legacy",
    productionVNextCutoverApproved: Boolean = false,
): VNextLaunchTarget {
    if (flavor == "preview") return VNextLaunchTarget.REFERENCE_PREVIEW

    val releaseVNext = productionVNextReleaseEnabled(
        generation = productionUiGeneration,
        cutoverApproved = productionVNextCutoverApproved,
    )

    if (debugBuild) {
        return when {
            explicitProductionVNext -> VNextLaunchTarget.PRODUCTION_REALITY_DEBUG
            explicitDemo -> VNextLaunchTarget.REFERENCE_PREVIEW
            releaseVNext -> VNextLaunchTarget.PRODUCTION_REALITY_RELEASE
            else -> VNextLaunchTarget.LEGACY_PRODUCTION
        }
    }

    // Production release Intent extras are deliberately ignored.
    return if (releaseVNext) {
        VNextLaunchTarget.PRODUCTION_REALITY_RELEASE
    } else {
        VNextLaunchTarget.LEGACY_PRODUCTION
    }
}

internal fun shouldLaunchVNext(
    flavor: String,
    explicitDemo: Boolean,
    debugBuild: Boolean,
): Boolean =
    resolveVNextLaunchTarget(
        flavor = flavor,
        explicitDemo = explicitDemo,
        explicitProductionVNext = false,
        debugBuild = debugBuild,
    ) == VNextLaunchTarget.REFERENCE_PREVIEW
