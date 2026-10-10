package com.pdig.app

internal enum class VNextLaunchTarget {
    REFERENCE_PREVIEW,
    PRODUCTION_REALITY_DEBUG,
    LEGACY_PRODUCTION,
}

/**
 * UI vNext launch policy.
 *
 * - Preview flavor always opens the synthetic reference candidate.
 * - Production release never exposes synthetic or production-VNext experimental
 *   routes through Intent extras.
 * - Production DEBUG may explicitly open:
 *   - vnext_demo       -> synthetic reference evidence
 *   - vnext_production -> real AppContainer-backed VNext behind the same LockGate
 *
 * Production default stays on the current lock-gated legacy application until the
 * explicit cutover gate is accepted.
 */
internal fun resolveVNextLaunchTarget(
    flavor: String,
    explicitDemo: Boolean,
    explicitProductionVNext: Boolean,
    debugBuild: Boolean,
): VNextLaunchTarget = when {
    flavor == "preview" -> VNextLaunchTarget.REFERENCE_PREVIEW
    debugBuild && explicitProductionVNext -> VNextLaunchTarget.PRODUCTION_REALITY_DEBUG
    debugBuild && explicitDemo -> VNextLaunchTarget.REFERENCE_PREVIEW
    else -> VNextLaunchTarget.LEGACY_PRODUCTION
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
