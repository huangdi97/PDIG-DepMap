package com.pdig.app

/**
 * UI vNext launch policy.
 *
 * - Preview flavor always opens the synthetic reference candidate.
 * - Production release must never expose synthetic reference data, even if an
 *   external caller supplies the historical instrumentation extra.
 * - Production DEBUG builds may retain the explicit demo extra for local/device
 *   evidence only.
 */
internal fun shouldLaunchVNext(
    flavor: String,
    explicitDemo: Boolean,
    debugBuild: Boolean,
): Boolean =
    flavor == "preview" || (explicitDemo && debugBuild)
