package com.pdig.app

/**
 * Preview is deliberately a separate, synthetic-data UI evaluation package.
 * Production launch remains lock-gated; the explicit demo extra is retained for instrumentation.
 */
internal fun shouldLaunchVNext(flavor: String, explicitDemo: Boolean): Boolean =
    flavor == "preview" || explicitDemo
