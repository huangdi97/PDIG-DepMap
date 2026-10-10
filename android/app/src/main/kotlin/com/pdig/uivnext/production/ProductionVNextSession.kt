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
internal data class ProductionVNextAuthorities(
    val review: ProductionReviewCoordinator,
    val change: VNextChangeActionGateway,
    val manualEstablish: AppContainerVNextManualEstablishGateway,
    val import: AppContainerVNextImportAuthority,
)

/**
 * Host-owned UI actions that must remain outside Compose/domain adapters.
 *
 * File picker / lock re-auth is Activity-scoped and already hardened by
 * FileWorkflowCoordinator, so VNext only requests that host workflow.
 */
internal data class ProductionVNextHostActions(
    val requestFileImport: (() -> Unit)? = null,
)

internal data class ProductionVNextSession(
    val appState: VAppState,
    val dataSource: VNextRuntimeDataSource,
    val authorities: ProductionVNextAuthorities? = null,
    val hostActions: ProductionVNextHostActions = ProductionVNextHostActions(),
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
    appState: VAppState? = null,
    hostActions: ProductionVNextHostActions = ProductionVNextHostActions(),
): ProductionVNextSession {
    val source = AppContainerVNextReadModelSource(appContainer)
    val reviewSource = AppContainerVNextReviewSource(appContainer)
    val reviewGateway = AppContainerVNextReviewActionGateway(appContainer)
    return ProductionVNextSession(
        // Launcher binding may inject a store-backed VAppState so privacy /
        // presentation preferences persist without entering Canonical Reality.
        appState = appState ?: VAppState(initialScreen = initialScreen),
        dataSource = ProductionVNextRuntimeDataSource(source, nowIso),
        authorities = ProductionVNextAuthorities(
            review = ProductionReviewCoordinator(reviewSource, reviewGateway),
            change = AppContainerVNextChangeActionGateway(appContainer),
            manualEstablish = AppContainerVNextManualEstablishGateway(appContainer),
            import = AppContainerVNextImportAuthority(appContainer),
        ),
        hostActions = hostActions,
    )
}
