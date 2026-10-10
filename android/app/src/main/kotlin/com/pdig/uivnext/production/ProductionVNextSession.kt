package com.pdig.uivnext.production

import com.pdig.app.data.AppContainer
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.ui.VAppState

/**
 * Explicit production UI-vNext session.
 *
 * Constructing a production session never changes launcher routing. It only
 * packages the existing VAppState navigation/presentation state together with
 * an authoritative production read source so production-bound Composables cannot
 * accidentally reach UiVNextDemoFixture.
 */
internal data class ProductionVNextSession(
    val appState: VAppState,
    val dataSource: VNextRuntimeDataSource,
) {
    init {
        require(dataSource.mode == VNextRuntimeDataMode.PRODUCTION_REALITY) {
            "ProductionVNextSession requires PRODUCTION_REALITY data"
        }
    }
}

internal fun createProductionVNextSession(
    appContainer: AppContainer,
    initialScreen: VScreen = VScreen.NOW,
    nowIso: String? = null,
): ProductionVNextSession {
    val source = AppContainerVNextReadModelSource(appContainer)
    return ProductionVNextSession(
        appState = VAppState(initialScreen = initialScreen),
        dataSource = ProductionVNextRuntimeDataSource(source, nowIso),
    )
}
