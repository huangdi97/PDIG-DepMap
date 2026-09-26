package com.pdig.conformance

import com.pdig.core.domain.ActionDagInput
import com.pdig.core.domain.ActionVerification
import com.pdig.core.domain.ActionVerificationMethod
import com.pdig.core.domain.ActionVerificationStatus
import com.pdig.core.domain.DagAction
import com.pdig.core.domain.Dependency
import com.pdig.core.domain.MakeBeforeBreakInput
import com.pdig.core.domain.PathIndependenceInput
import com.pdig.core.domain.PlanAction
import com.pdig.core.domain.TemporalChangeWindow
import com.pdig.core.domain.classifyTemporalPhase
import com.pdig.core.domain.computePathIndependence
import com.pdig.core.domain.evaluateMakeBeforeBreak
import com.pdig.core.domain.validateActionDag
import com.pdig.core.domain.validateRelationUse
import com.pdig.core.generated.Criticality
import com.pdig.core.generated.DependencyOrigin
import com.pdig.core.generated.DependencyState
import com.pdig.core.generated.NodeKind
import com.pdig.core.generated.PlanActionPhase
import com.pdig.core.generated.Relation
import com.pdig.core.impact.HintedEdge
import com.pdig.core.impact.RecoveryCycleInput
import com.pdig.core.impact.detectRecoveryCycles
import com.pdig.core.json.Json
import com.pdig.core.services.ProviderPolicy
import com.pdig.core.generated.ProviderPolicyState
import com.pdig.core.services.interpretProviderCapability

// ---------------------------------------------------------------------------
// v0.3.0 deterministic engines —— failure-domain / recovery-cycle / action-dag /
// make-before-break / temporal-change / provider-policy / identity-relations
// ---------------------------------------------------------------------------

internal fun runFailureDomain(input: Json.Obj): Json {
    val deps = arr(input, "dependencies").map { parseDep(requireObj(it)) }
    val confirmedDomains = parseDomainMap(input["confirmedDomains"])
    val suspectedDomains = parseDomainMap(input["suspectedDomains"])
    val edgeToDomainKeys = parseStringListMap(requireObj(input.require("edgeToDomainKeys")))
    val r = computePathIndependence(
        PathIndependenceInput(
            targetNodeId = str(input, "targetNodeId"),
            capability = cap(input, "capability"),
            dependencies = deps,
            confirmedDomains = confirmedDomains,
            suspectedDomains = suspectedDomains,
            edgeToDomainKeys = edgeToDomainKeys,
        ),
    )
    return Json.Obj(
        listOf(
            "targetNodeId" to Json.Str(r.targetNodeId),
            "capability" to Json.Str(r.capability.wire),
            "pathCount" to Json.Num(r.pathCount.toString()),
            "independentPathCount" to Json.Num(r.independentPathCount.toString()),
            "sharedFailureDomains" to Json.Arr(r.sharedFailureDomains.map { Json.Str(it) }),
            "unresolvedAssumptions" to Json.Arr(r.unresolvedAssumptions.map { Json.Str(it) }),
        ),
    )
}

internal fun runRecoveryCycle(input: Json.Obj): Json {
    val deps = arr(input, "dependencies").map { parseDep(requireObj(it)) }
    val hinted = arr(input, "hintedEdges").map {
        val o = requireObj(it)
        HintedEdge(from = str(o, "from"), to = str(o, "to"))
    }
    val r = detectRecoveryCycles(
        RecoveryCycleInput(
            capability = cap(input, "capability"),
            dependencies = deps,
            hintedEdges = hinted,
        ),
    )
    return Json.Obj(
        listOf(
            "status" to Json.Str(r.status.wire),
            "confirmedCycles" to Json.Arr(r.confirmedCycles.map { Json.Arr(it.map { Json.Str(it) }) }),
            "potentialCycles" to Json.Arr(r.potentialCycles.map { Json.Arr(it.map { Json.Str(it) }) }),
            "capability" to Json.Str(r.capability.wire),
        ),
    )
}

internal fun runActionDag(input: Json.Obj): Json {
    val actions = arr(input, "actions").map {
        val o = requireObj(it)
        DagAction(
            id = str(o, "id"),
            prerequisiteActionIds = arr(o, "prerequisiteActionIds").map { requireStr(it).value },
        )
    }
    val r = validateActionDag(ActionDagInput(actions))
    return Json.Obj(
        listOf(
            "ok" to Json.Bool(r.ok),
            "violation" to Json.Str(r.violation.wire),
            "offendingActionId" to (r.offendingActionId?.let { Json.Str(it) } ?: Json.Null),
            "topologicalOrder" to Json.Arr(r.topologicalOrder.map { Json.Str(it) }),
        ),
    )
}

internal fun runMakeBeforeBreak(input: Json.Obj): Json {
    val newPaths = arr(input, "newPaths").map { parseMbbAction(requireObj(it)) }
    val verifications = arr(input, "verifications").map { parseMbbAction(requireObj(it)) }
    val r = evaluateMakeBeforeBreak(
        MakeBeforeBreakInput(
            newPathActions = newPaths,
            verificationActions = verifications,
            retireActionId = "",
            retireAlreadyDone = (input["retireDone"] as? Json.Bool)?.value ?: false,
        ),
    )
    return Json.Obj(
        listOf(
            "status" to Json.Str(r.status.wire),
            "unverifiedNewPaths" to Json.Arr(r.unverifiedNewPaths.map { Json.Str(it) }),
            "reason" to Json.Str(r.reason),
        ),
    )
}

internal fun runTemporalChange(input: Json.Obj): Json {
    val w = requireObj(input.require("window"))
    val window = TemporalChangeWindow(
        effectiveAt = str(w, "effectiveAt"),
        verificationNotBefore = (w["verificationNotBefore"] as? Json.Str)?.value,
        verificationDueAt = (w["verificationDueAt"] as? Json.Str)?.value,
        retireOldPathAfter = (w["retireOldPathAfter"] as? Json.Str)?.value,
    )
    val r = classifyTemporalPhase(
        window = window,
        now = str(input, "now"),
        allKeyNewPathsVerified = (input["verified"] as? Json.Bool)?.value ?: false,
    )
    return Json.Obj(
        listOf(
            "phase" to Json.Str(r.phase.wire),
            "validOrder" to Json.Bool(r.validOrder),
            "retireAllowedAt" to (r.retireAllowedAt?.let { Json.Str(it) } ?: Json.Null),
            "retireBlockedReason" to (r.retireBlockedReason?.let { Json.Str(it) } ?: Json.Null),
        ),
    )
}

internal fun runProviderPolicy(input: Json.Obj): Json {
    val policy = (input["policy"] as? Json.Obj)?.let { parsePolicy(it) }
    val userConfigured = (input["userConfigured"] as? Json.Bool)?.value ?: false
    val r = interpretProviderCapability(policy, userConfigured)
    return Json.Obj(
        listOf(
            "provider" to Json.Str(r.provider),
            "supports" to Json.Bool(r.supports),
            "configured" to Json.Bool(r.configured),
            "explanation" to Json.Str(r.explanation),
            "influences" to Json.Arr(r.influences.map { Json.Str(it.wire) }),
            "state" to Json.Str(r.state.wire),
        ),
    )
}

/** identity-relations：与 relations 同一 registry 校验（recovers/authenticates/controls）。 */
internal fun runIdentityRelations(input: Json.Obj): Json {
    val result = validateRelationUse(
        fromKind = (input["fromKind"] as? Json.Str)?.value?.let { NodeKind.fromWire(it) },
        relation = str(input, "relation"),
        toKind = (input["toKind"] as? Json.Str)?.value?.let { NodeKind.fromWire(it) },
        capability = str(input, "capability"),
    )
    return Json.Obj(
        listOfNotNull(
            "ok" to Json.Bool(result.ok),
            result.reason?.let { "reason" to Json.Str(it) },
        ),
    )
}

// ---------------------------------------------------------------------------
// 输入解析
// ---------------------------------------------------------------------------

private fun parseDep(o: Json.Obj): Dependency = Dependency(
    id = str(o, "id"),
    from = str(o, "from"),
    relation = Relation.fromWire(str(o, "relation")) ?: error("unknown relation"),
    to = str(o, "to"),
    capability = cap(o, "capability"),
    criticality = Criticality.fromWire(str(o, "criticality")) ?: error("unknown criticality"),
    groupId = (o["groupId"] as? Json.Str)?.value,
    state = DependencyState.fromWire(str(o, "state")) ?: error("unknown state"),
    origin = DependencyOrigin.fromWire(str(o, "origin")) ?: DependencyOrigin.MANUAL,
    lastVerifiedAt = (o["lastVerifiedAt"] as? Json.Str)?.value ?: "",
)

private fun parseDomainMap(v: Json?): Map<String, String> {
    if (v !is Json.Arr) return emptyMap()
    return v.items.mapNotNull { it as? Json.Obj }.associate { o -> str(o, "key") to str(o, "id") }
}

private fun parseStringListMap(o: Json.Obj): Map<String, List<String>> =
    o.fields.associate { (k, v) ->
        k to ((v as? Json.Arr)?.items?.mapNotNull { (it as? Json.Str)?.value } ?: emptyList())
    }

private fun parseMbbAction(o: Json.Obj): PlanAction {
    val verification = (o["verification"] as? Json.Obj)?.let { v ->
        ActionVerification(
            method = ActionVerificationMethod.fromWire(str(v, "method")) ?: error("unknown method"),
            status = ActionVerificationStatus.fromWire(str(v, "status")) ?: error("unknown status"),
        )
    }
    return PlanAction(
        id = str(o, "id"),
        title = (o["title"] as? Json.Str)?.value ?: "",
        phase = PlanActionPhase.fromWire(str(o, "phase")) ?: error("unknown phase"),
        done = (o["done"] as? Json.Bool)?.value ?: false,
        resolvesImpactKeys = (o["resolvesImpactKeys"] as? Json.Arr)?.items?.map { requireStr(it).value }
            ?: emptyList(),
        verification = verification,
    )
}

private fun parsePolicy(o: Json.Obj): ProviderPolicy = ProviderPolicy(
    provider = str(o, "provider"),
    policyType = str(o, "policyType"),
    sourceUrl = str(o, "sourceUrl"),
    retrievedAt = str(o, "retrievedAt"),
    lastVerifiedAt = (o["lastVerifiedAt"] as? Json.Str)?.value,
    effectiveFrom = (o["effectiveFrom"] as? Json.Str)?.value,
    effectiveTo = (o["effectiveTo"] as? Json.Str)?.value,
    jurisdiction = (o["jurisdiction"] as? Json.Str)?.value,
    accountTypeScope = (o["accountTypeScope"] as? Json.Str)?.value,
    policyRevision = (o["policyRevision"] as? Json.Num)?.asLong()?.toInt() ?: 0,
    state = ProviderPolicyState.fromWire(str(o, "state")) ?: error("unknown policy state"),
)
