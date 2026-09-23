package com.pdig.conformance

import com.pdig.core.domain.ChangePlan
import com.pdig.core.domain.CoverageSourceInfo
import com.pdig.core.domain.Dependency
import com.pdig.core.domain.DependencyGroup
import com.pdig.core.domain.ImpactGraph
import com.pdig.core.domain.ImpactProposalInput
import com.pdig.core.domain.ImpactStateKey
import com.pdig.core.domain.PlanAction
import com.pdig.core.domain.PlanReadinessInput
import com.pdig.core.domain.ScenarioCoverageInput
import com.pdig.core.generated.ChangePlanWorkflowState
import com.pdig.core.generated.Criticality
import com.pdig.core.generated.DependencyOrigin
import com.pdig.core.generated.DependencyState
import com.pdig.core.generated.GroupMode
import com.pdig.core.generated.GroupState
import com.pdig.core.generated.PlanActionPhase
import com.pdig.core.generated.Relation
import com.pdig.core.impact.ImpactResult
import com.pdig.core.impact.simulateScenario
import com.pdig.core.json.Json
import com.pdig.core.plan.computePlanReadiness
import com.pdig.core.plan.computeScenarioCoverage

// ---------------------------------------------------------------------------
// impact
// ---------------------------------------------------------------------------

internal fun runImpact(input: Json.Obj): Json {
    val graphObj = requireObj(input.require("graph"))
    val nodeNames = (graphObj["nodeNames"] as? Json.Obj)?.fields?.associate { (k, v) ->
        k to requireStr(v).value
    } ?: emptyMap()

    val deps = arr(graphObj, "dependencies").map { parseDependency(requireObj(it)) }
    val groups = (graphObj["groups"] as? Json.Arr)?.items?.map { parseGroup(requireObj(it)) } ?: emptyList()
    val proposals = (graphObj["proposals"] as? Json.Arr)?.items?.map {
        val o = requireObj(it)
        ImpactProposalInput(
            key = str(o, "key"),
            from = str(o, "from"),
            to = str(o, "to"),
            capability = cap(o, "capability"),
            confidenceScore = (o["confidenceScore"] as? Json.Num)?.asDouble(),
        )
    } ?: emptyList()

    val unavailable = arr(input, "unavailable").map {
        val o = requireObj(it)
        ImpactStateKey(nodeId = str(o, "nodeId"), capability = cap(o, "capability"))
    }

    val graph = ImpactGraph(deps, groups, proposals, nodeNames)
    val result = simulateScenario(graph, unavailable)
    return serializeImpact(result)
}

private fun parseDependency(o: Json.Obj): Dependency = Dependency(
    id = str(o, "id"),
    from = str(o, "from"),
    relation = Relation.fromWire(str(o, "relation")) ?: error("unknown relation"),
    to = str(o, "to"),
    capability = cap(o, "capability"),
    criticality = Criticality.fromWire(str(o, "criticality")) ?: error("unknown criticality"),
    groupId = (o["groupId"] as? Json.Str)?.value,
    state = DependencyState.fromWire(str(o, "state")) ?: error("unknown state"),
    origin = DependencyOrigin.fromWire(str(o, "origin")) ?: DependencyOrigin.MANUAL,
    lastVerifiedAt = str(o, "lastVerifiedAt"),
)

private fun parseGroup(o: Json.Obj): DependencyGroup = DependencyGroup(
    id = str(o, "id"),
    groupKey = str(o, "groupKey"),
    targetNodeId = str(o, "targetNodeId"),
    capability = cap(o, "capability"),
    mode = GroupMode.fromWire(str(o, "mode")) ?: error("unknown group mode"),
    memberEdgeIds = requireArr(o.require("memberEdgeIds")).items.map { requireStr(it).value },
    state = GroupState.fromWire(str(o, "state")) ?: GroupState.ACTIVE,
)

private fun serializeImpact(r: ImpactResult): Json = Json.Obj(
    listOf(
        "unavailable" to Json.Arr(r.unavailable.map { stateKey(it) }),
        "lostKeys" to Json.Arr(r.lostKeys.map { stateKey(it) }),
        "targets" to Json.Arr(
            r.targets.map { t ->
                Json.Obj(
                    listOf(
                        "nodeId" to Json.Str(t.nodeId),
                        "nodeName" to Json.Str(t.nodeName),
                        "capability" to Json.Str(t.capability.wire),
                        "depth" to Json.Num(t.depth.toString()),
                        "status" to Json.Str(t.status.wire),
                        "available" to Json.Bool(t.available),
                        "redundancyDegraded" to Json.Bool(t.redundancyDegraded),
                        "reasonCode" to Json.Str(t.reasonCode.wire),
                        "reasonText" to Json.Str(t.reasonText),
                        "edgeKeys" to Json.Arr(t.edgeKeys.map { Json.Str(it) }),
                        "groupKeys" to Json.Arr(t.groupKeys.map { Json.Str(it) }),
                        "proposalKeys" to Json.Arr(t.proposalKeys.map { Json.Str(it) }),
                    ),
                )
            },
        ),
        "checklist" to Json.Arr(
            r.checklist.map { c ->
                Json.Obj(
                    listOfNotNull(
                        "level" to Json.Str(c.level.wire),
                        "nodeId" to (c.nodeId?.let { Json.Str(it) } ?: Json.Null),
                        "capability" to (c.capability?.let { Json.Str(it.wire) } ?: Json.Null),
                        "title" to Json.Str(c.title),
                        "detail" to Json.Str(c.detail),
                    ),
                )
            },
        ),
        "processedKeys" to Json.Arr(r.processedKeys.map { Json.Str(it) }),
    ),
)

private fun stateKey(k: ImpactStateKey): Json = Json.Obj(
    listOf("nodeId" to Json.Str(k.nodeId), "capability" to Json.Str(k.capability.wire)),
)

// ---------------------------------------------------------------------------
// readiness
// ---------------------------------------------------------------------------

internal fun runReadiness(input: Json.Obj): Json {
    val planObj = requireObj(input.require("plan"))
    val actions = arr(planObj, "actions").map { a ->
        val o = requireObj(a)
        PlanAction(
            id = str(o, "id"),
            title = (o["title"] as? Json.Str)?.value ?: "",
            phase = PlanActionPhase.fromWire(str(o, "phase")) ?: error("unknown phase"),
            done = (o["done"] as? Json.Bool)?.value ?: false,
            resolvesImpactKeys = (o["resolvesImpactKeys"] as? Json.Arr)?.items?.map { requireStr(it).value }
                ?: emptyList(),
        )
    }
    val plan = ChangePlan(
        id = str(planObj, "id"),
        templateId = (planObj["templateId"] as? Json.Str)?.value,
        scenario = (planObj["scenario"] as? Json.Str)?.value ?: "",
        title = (planObj["title"] as? Json.Str)?.value ?: "",
        workflowState = ChangePlanWorkflowState.fromWire(str(planObj, "workflowState"))
            ?: error("unknown workflow state"),
        baselineGraphRevision = int(planObj, "baselineGraphRevision"),
        lastAnalyzedGraphRevision = int(planObj, "lastAnalyzedGraphRevision"),
        targetNodeId = (planObj["targetNodeId"] as? Json.Str)?.value,
        effectiveDate = (planObj["effectiveDate"] as? Json.Str)?.value,
        actions = actions,
    )
    val readinessInput = PlanReadinessInput(
        plan = plan,
        currentGraphRevision = int(input, "currentGraphRevision"),
        pendingMustChange = int(input, "pendingMustChange"),
        pendingNeedsReview = int(input, "pendingNeedsReview"),
        unresolvedCandidates = int(input, "unresolvedCandidates"),
        pendingRelevantProposals = int(input, "pendingRelevantProposals"),
        staleRelevantDependencies = int(input, "staleRelevantDependencies"),
        unfinishedChangeActions = int(input, "unfinishedChangeActions"),
    )
    return Json.Str(computePlanReadiness(readinessInput).wire)
}

// ---------------------------------------------------------------------------
// coverage
// ---------------------------------------------------------------------------

internal fun runCoverage(input: Json.Obj): Json {
    val sources = arr(input, "sources").map { s ->
        val o = requireObj(s)
        CoverageSourceInfo(
            id = str(o, "id"),
            label = str(o, "label"),
            lastIngestedAt = (o["lastIngestedAt"] as? Json.Str)?.value,
        )
    }
    val cov = computeScenarioCoverage(
        ScenarioCoverageInput(
            scenarioId = str(input, "scenarioId"),
            sources = sources,
            confirmedDirectDependencies = int(input, "confirmedDirectDependencies"),
            confirmedIndirectDependencies = int(input, "confirmedIndirectDependencies"),
            pendingProposals = int(input, "pendingProposals"),
            unresolvedCandidates = int(input, "unresolvedCandidates"),
            staleDependencies = int(input, "staleDependencies"),
            unknownCriticalityCount = int(input, "unknownCriticalityCount"),
            unverifiedActions = int(input, "unverifiedActions"),
            freshnessThresholdDays = int(input, "freshnessThresholdDays"),
            now = str(input, "now"),
        ),
    )
    return Json.Obj(
        listOf(
            "scenarioId" to Json.Str(cov.scenarioId),
            "coverageLevel" to Json.Str(cov.coverageLevel.wire),
            "explanations" to Json.Arr(cov.explanations.map { Json.Str(it) }),
            "counts" to Json.Obj(
                listOf(
                    "confirmedDirectDependencies" to Json.Num(cov.counts.confirmedDirectDependencies.toString()),
                    "confirmedIndirectDependencies" to Json.Num(cov.counts.confirmedIndirectDependencies.toString()),
                    "pendingProposals" to Json.Num(cov.counts.pendingProposals.toString()),
                    "unresolvedCandidates" to Json.Num(cov.counts.unresolvedCandidates.toString()),
                    "staleDependencies" to Json.Num(cov.counts.staleDependencies.toString()),
                    "unknownCriticalityCount" to Json.Num(cov.counts.unknownCriticalityCount.toString()),
                    "unverifiedActions" to Json.Num(cov.counts.unverifiedActions.toString()),
                ),
            ),
        ),
    )
}