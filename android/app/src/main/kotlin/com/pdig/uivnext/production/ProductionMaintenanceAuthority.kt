package com.pdig.uivnext.production

import com.pdig.app.data.AppContainer
import com.pdig.app.data.MaintenanceWriteResult
import com.pdig.core.domain.MaintenanceFactWrite
import com.pdig.core.domain.MaintenanceScheduleWrite

/**
 * Production mutation seam for governed lifecycle / maintenance Reality.
 *
 * Compose never edits fields_json directly. Every write goes through AppContainer
 * so Canonical applicability/value validation, encrypted persistence and
 * graphRevision mutation remain authoritative.
 */
internal interface VNextMaintenanceActionGateway {
    fun confirmFact(
        nodeId: String,
        request: MaintenanceFactWrite,
    ): MaintenanceWriteResult

    fun confirmFacts(
        nodeId: String,
        requests: List<MaintenanceFactWrite>,
    ): MaintenanceWriteResult

    fun confirmSchedule(
        nodeId: String,
        request: MaintenanceScheduleWrite,
    ): MaintenanceWriteResult
}

internal class AppContainerVNextMaintenanceActionGateway(
    private val app: AppContainer,
) : VNextMaintenanceActionGateway {
    override fun confirmFact(
        nodeId: String,
        request: MaintenanceFactWrite,
    ): MaintenanceWriteResult =
        app.confirmMaintenanceFact(nodeId, request)

    override fun confirmFacts(
        nodeId: String,
        requests: List<MaintenanceFactWrite>,
    ): MaintenanceWriteResult =
        app.confirmMaintenanceFacts(nodeId, requests)

    override fun confirmSchedule(
        nodeId: String,
        request: MaintenanceScheduleWrite,
    ): MaintenanceWriteResult =
        app.confirmMaintenanceSchedule(nodeId, request)
}
