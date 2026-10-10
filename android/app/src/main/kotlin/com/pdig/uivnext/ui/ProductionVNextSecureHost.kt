package com.pdig.uivnext.ui

import androidx.compose.runtime.Composable
import com.pdig.app.ui.PdigSecureContent
import com.pdig.uivnext.production.ProductionVNextSession

/**
 * Security-preserving host for the production-bound VNext shell.
 *
 * This is intentionally NOT selected by MainActivity yet. It exists so the final
 * cutover can reuse the exact same fail-closed AppLock lifecycle as the current
 * production app instead of inventing a second security gate.
 */
@Composable
internal fun ProductionVNextSecureHost(
    session: ProductionVNextSession,
) {
    PdigSecureContent {
        ProductionVNextShell(session)
    }
}
