package com.pdig.app.data

import com.pdig.core.domain.PlanAction
import com.pdig.core.generated.PlanActionPhase
import com.pdig.core.plan.unresolvedMustChangeKeys
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlanSupportClaimingTest {
    @Test
    fun reanalysisDropsClaimsForImpactsThatNoLongerExist() {
        val actions = listOf(
            PlanAction(
                id = "rpn-change-1",
                title = "迁移关键账户",
                phase = PlanActionPhase.CHANGE,
                done = true,
                resolvesImpactKeys = listOf("account-old|recovery"),
            ),
            PlanAction(
                id = "rpn-change-2",
                title = "停用旧手机号",
                phase = PlanActionPhase.CHANGE,
                done = false,
            ),
        )

        val reconciled = claimScenarioMustChangeKeys(
            scenario = "replace_phone_number",
            actions = actions,
            mustChangeKeys = emptyList(),
        )

        assertTrue(reconciled.first { it.id == "rpn-change-1" }.resolvesImpactKeys.isEmpty())
    }

    @Test
    fun impactThatReappearsCannotBeResolvedByHistoricalDoneClaim() {
        val historical = listOf(
            PlanAction(
                id = "rpn-change-1",
                title = "迁移关键账户",
                phase = PlanActionPhase.CHANGE,
                done = true,
                resolvesImpactKeys = listOf("account-a|recovery"),
            ),
            PlanAction(
                id = "rpn-change-2",
                title = "停用旧手机号",
                phase = PlanActionPhase.CHANGE,
                done = false,
            ),
        )

        val absent = claimScenarioMustChangeKeys(
            "replace_phone_number",
            historical,
            emptyList(),
        )
        val reappeared = claimScenarioMustChangeKeys(
            "replace_phone_number",
            absent,
            listOf("account-a|recovery"),
        )

        assertTrue(reappeared.first { it.id == "rpn-change-1" }.resolvesImpactKeys.isEmpty())
        assertTrue(reappeared.first { it.id == "rpn-change-2" }.resolvesImpactKeys.isEmpty())
        assertEquals(
            listOf("account-a|recovery"),
            unresolvedMustChangeKeys(reappeared, listOf("account-a|recovery")),
        )
    }

    @Test
    fun stillCurrentClaimsRemainAttachedAndNewWorkStaysExplicit() {
        val actions = listOf(
            PlanAction(
                id = "rpn-change-1",
                title = "迁移关键账户",
                phase = PlanActionPhase.CHANGE,
                done = false,
                resolvesImpactKeys = listOf("account-a|recovery"),
            ),
        )

        val reconciled = claimScenarioMustChangeKeys(
            "replace_phone_number",
            actions,
            listOf("account-a|recovery", "service-b|authentication"),
        )

        assertEquals(
            listOf("account-a|recovery"),
            reconciled.single().resolvesImpactKeys,
        )
        assertEquals(
            listOf("account-a|recovery", "service-b|authentication"),
            unresolvedMustChangeKeys(
                reconciled,
                listOf("account-a|recovery", "service-b|authentication"),
            ),
        )
    }
}
