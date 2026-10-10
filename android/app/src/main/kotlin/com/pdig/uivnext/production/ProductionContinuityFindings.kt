package com.pdig.uivnext.production

import com.pdig.app.data.AppContainer
import com.pdig.core.domain.Dependency
import com.pdig.core.generated.Capability
import com.pdig.core.generated.DependencyState
import com.pdig.core.impact.RecoveryCycleInput
import com.pdig.core.impact.detectRecoveryCycles

internal enum class VNextProductionFindingType {
    SINGLE_POINT_OF_FAILURE,
    RECOVERY_CYCLE,
    UNCONFIRMED_FALLBACK,
    PENDING_VERIFICATION,
}

internal data class VNextProductionFinding(
    val id: String,
    val type: VNextProductionFindingType,
    val title: String,
    val why: String,
    val confirmedBasis: String,
    val unknowns: String,
    val recommendedNextAction: String,
    val evidenceRefs: List<String>,
    val truth: VNextProjectionTruth,
)

internal data class VNextProductionFindingReport(
    val findings: List<VNextProductionFinding>,
    val supportedTypes: List<String>,
    val unsupportedTypes: List<String>,
)

internal fun buildProductionContinuityFindings(
    app: AppContainer,
): VNextProductionFindingReport {
    val graph = app.loadImpactGraph()
    val activeRecovery = graph.dependencies
        .filter {
            it.capability == Capability.RECOVERY &&
                it.state == DependencyState.ACTIVE
        }
        .sortedBy { it.id }

    val findings = mutableListOf<VNextProductionFinding>()

    activeRecovery
        .groupBy { it.to }
        .filterValues { it.size == 1 }
        .toSortedMap()
        .forEach { (targetId, deps) ->
            val dep = deps.single()
            val targetName = graph.nodeNames[targetId] ?: targetId
            findings += VNextProductionFinding(
                id = "spof:$targetId:${dep.id}",
                type = VNextProductionFindingType.SINGLE_POINT_OF_FAILURE,
                title = "「$targetName」只有一条已确认恢复来源",
                why = "当前已确认 Reality 中，该对象只有一条 active recovery 入边。",
                confirmedBasis = "已确认恢复关系：${dep.id}",
                unknowns = "未记录的恢复方式仍可能存在，因此这不是“外部一定没有备用”的结论。",
                recommendedNextAction = "核对是否还有其他恢复方式，并实际验证可用性。",
                evidenceRefs = listOf(dep.id),
                truth = VNextProjectionTruth.DERIVED,
            )
        }

    val cycles = detectRecoveryCycles(
        RecoveryCycleInput(
            capability = Capability.RECOVERY,
            dependencies = activeRecovery,
            hintedEdges = emptyList(),
        ),
    ).confirmedCycles

    cycles.forEachIndexed { index, path ->
        val evidence = cycleEvidenceRefs(path, activeRecovery)
        val label = path.joinToString(" → ") { graph.nodeNames[it] ?: it }
        findings += VNextProductionFinding(
            id = "recovery-cycle:${index + 1}:${path.joinToString(":")}",
            type = VNextProductionFindingType.RECOVERY_CYCLE,
            title = "发现恢复循环：$label",
            why = "恢复路径回到了它所依赖的对象，因此不能把这条循环当成独立备用。",
            confirmedBasis = "core RecoveryCycle 在已确认 active recovery 关系上检测到 confirmed cycle。",
            unknowns = "是否还存在不经过该循环的独立恢复路径仍需单独确认。",
            recommendedNextAction = "建立并验证一条不经过该循环的独立恢复路径。",
            evidenceRefs = evidence,
            truth = VNextProjectionTruth.DERIVED,
        )
    }

    app.pendingProposals()
        .filter { it.capability == Capability.RECOVERY.wire }
        .sortedBy { it.id }
        .forEach { proposal ->
            val fromName = graph.nodeNames[proposal.from] ?: proposal.from
            val toName = graph.nodeNames[proposal.to] ?: proposal.to
            findings += VNextProductionFinding(
                id = "unconfirmed-fallback:${proposal.id}",
                type = VNextProductionFindingType.UNCONFIRMED_FALLBACK,
                title = "存在待复核的恢复候选：$fromName → $toName",
                why = "导入/观察记录提出了恢复关系候选，但用户尚未确认它进入 Reality。",
                confirmedBasis = "Proposal ${proposal.id} 处于待复核状态。",
                unknowns = "该候选是否真实、当前是否可用、是否独立都尚未确认。",
                recommendedNextAction = "进入待复核，确认、拒绝或继续保留为待处理。",
                evidenceRefs = listOf(proposal.id),
                truth = VNextProjectionTruth.PENDING_REVIEW,
            )
        }

    app.plans()
        .sortedBy { it.id }
        .forEach { row ->
            val detail = app.planDetail(row.id) ?: return@forEach
            detail.actions
                .filter { action ->
                    action.done && (
                        action.verification?.status?.wire == "pending" ||
                            action.verification?.status?.wire == "evidence_suggested"
                        )
                }
                .sortedBy { it.id }
                .forEach { action ->
                    findings += VNextProductionFinding(
                        id = "pending-verification:${detail.id}:${action.id}",
                        type = VNextProductionFindingType.PENDING_VERIFICATION,
                        title = "「${detail.title}」有动作已完成但仍待验证",
                        why = "动作已记录完成，但验证状态尚未达到 verified。",
                        confirmedBasis = "ChangePlan ${detail.id} / Action ${action.id}",
                        unknowns = "实际变更是否生效仍未知；不能因为 done=true 就停用旧路径。",
                        recommendedNextAction = "完成对应验证并记录证据，再推进依赖它的后续动作。",
                        evidenceRefs = action.verification?.evidenceRefs.orEmpty(),
                        truth = VNextProjectionTruth.DERIVED,
                    )
                }
        }

    return VNextProductionFindingReport(
        findings = findings.sortedWith(
            compareBy<VNextProductionFinding> { it.type.ordinal }.thenBy { it.id },
        ),
        supportedTypes = listOf(
            "SINGLE_POINT_OF_FAILURE",
            "RECOVERY_CYCLE",
            "UNCONFIRMED_FALLBACK",
            "PENDING_VERIFICATION",
        ),
        unsupportedTypes = listOf(
            "SHARED_FAILURE_DOMAIN",
            "STALE_RECOVERY_INFORMATION",
            "UNKNOWN_CRITICAL_PATH",
        ),
    )
}

private fun cycleEvidenceRefs(
    path: List<String>,
    dependencies: List<Dependency>,
): List<String> {
    if (path.size < 2) return emptyList()
    return path.indices.mapNotNull { index ->
        val from = path[index]
        val to = path[(index + 1) % path.size]
        dependencies.firstOrNull { it.from == from && it.to == to }?.id
    }.distinct().sorted()
}
