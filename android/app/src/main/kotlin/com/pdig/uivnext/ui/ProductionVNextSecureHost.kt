package com.pdig.uivnext.ui

import androidx.compose.runtime.Composable
import com.pdig.app.ui.PdigSecureContent
import com.pdig.uivnext.production.ProductionVNextSession

/**
 * Security-preserving host for the production-bound VNext shell.
 *
 * R32 selects this host only for the explicit productionDebug Reality rehearsal.
 * The release/default production route still uses the legacy shell. Both paths
 * reuse the exact same fail-closed AppLock lifecycle, so final cutover changes the
 * unlocked content rather than inventing a second security gate.
 */
@Composable
internal fun ProductionVNextSecureHost(
    session: ProductionVNextSession,
) {
    PdigSecureContent {
        ProductionVNextShell(session)
    }
}
