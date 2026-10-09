package com.pdig.uivnext.production

import com.pdig.app.data.AppContainer
import com.pdig.app.data.ScenarioPlanRequest

/**
 * Production mutation seam for the VNext continuity UI.
 *
 * Compose must never mutate local presentation state and then pretend a domain
 * action completed. Every mutation delegates to AppContainer and immediately
 * re-reads the authoritative plan.
 */
internal interface VNextChangeActionGateway {
    fun createPlan(
        scenarioId: String,
        targetNodeId: String,
        effectiveDate: String? = null,
    ): VNextProductionPlan

    fun completeAction(planId: String, actionId: String): VNextProductionPlan

    fun verifyAction(planId: String, actionId: String): VNextProductionPlan
}

internal class AppContainerVNextChangeActionGateway(
    private val app: AppContainer,
) : VNextChangeActionGateway {
    override fun createPlan(
        scenarioId: String,
        targetNodeId: String,
        effectiveDate: String?,
    ): VNextProductionPlan {
        val planId = app.createPlanForScenario(
            ScenarioPlanRequest(
                scenarioId = scenarioId,
                targetNodeId = targetNodeId,
                effectiveDate = effectiveDate,
            ),
        )
        return requirePlan(planId)
    }

    override fun completeAction(planId: String, actionId: String): VNextProductionPlan {
        app.completeAction(planId, actionId)
        return requirePlan(planId)
    }

    override fun verifyAction(planId: String, actionId: String): VNextProductionPlan {
        app.verifyAction(planId, actionId)
        return requirePlan(planId)
    }

    private fun requirePlan(planId: String): VNextProductionPlan =
        app.planDetail(planId)?.let(::mapProductionPlan)
            ?: error("Production change plan disappeared after authoritative mutation: $planId")
}
