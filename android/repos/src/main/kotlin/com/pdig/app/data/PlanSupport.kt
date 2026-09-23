package com.pdig.app.data

import com.pdig.core.domain.ActionVerificationMethod
import com.pdig.core.domain.ActionVerificationStatus
import com.pdig.core.domain.PlanAction
import com.pdig.core.generated.ImpactTargetStatus
import com.pdig.core.generated.PlanActionPhase
import com.pdig.core.impact.ImpactResult
import com.pdig.core.json.Json
import com.pdig.core.json.JsonParser

// ---------------------------------------------------------------------------
// ChangePlan 专用文件级辅助（纯函数，仅 PlanRepository 使用）
// ---------------------------------------------------------------------------

/** 依赖"长期未验证"阈值（天），用于 readiness 的 staleRelevantDependencies。 */
const val STALE_CUTOFF_DAYS = 90L

internal fun keyString(nodeId: String, capabilityWire: String): String = "$nodeId|$capabilityWire"

internal fun keysOfStatus(
    impact: ImpactResult,
    status: ImpactTargetStatus,
): List<String> = impact.targets
    .filter { it.status == status }
    .map { keyString(it.nodeId, it.capability.wire) }
    .distinct()
    .sorted()

internal fun buildActionsFor(
    planId: String,
    impact: ImpactResult,
    keys: List<String>,
): List<PlanAction> = keys.mapIndexed { i, key ->
    val t = impact.targets.first { keyString(it.nodeId, it.capability.wire) == key }
    PlanAction(
        id = "act-$planId-${i + 1}",
        title = "把「${t.nodeName}」的支付来源换成新卡",
        phase = PlanActionPhase.CHANGE,
        // 显式 resolution：这个动作"声明"自己解决哪个 must_change key（spec §43，禁止减法）
        resolvesImpactKeys = listOf(key),
        verification = com.pdig.core.domain.ActionVerification(
            method = ActionVerificationMethod.MANUAL_CONFIRMATION,
            status = ActionVerificationStatus.PENDING,
        ),
    )
}

internal fun snapshotKeys(snapshot: String?, field: String): List<String> {
    if (snapshot.isNullOrBlank()) return emptyList()
    return try {
        val obj = JsonParser.parse(snapshot) as? Json.Obj ?: return emptyList()
        (obj[field] as? Json.Arr)?.items?.mapNotNull { (it as? Json.Str)?.value } ?: emptyList()
    } catch (_: Throwable) {
        emptyList()
    }
}

internal fun involvedNodeIds(targetNodeId: String?, snapshot: String?): Set<String> {
    val out = LinkedHashSet<String>()
    targetNodeId?.let { if (it.isNotBlank()) out.add(it) }
    for (k in snapshotKeys(snapshot, "targetKeys")) out.add(k.substringBefore('|'))
    return out
}

internal fun planActionToJson(a: PlanAction): Json = Json.Obj(
    listOf(
        "id" to Json.Str(a.id),
        "title" to Json.Str(a.title),
        "phase" to Json.Str(a.phase.wire),
        "done" to Json.Bool(a.done),
        "resolvesImpactKeys" to Json.Arr(a.resolvesImpactKeys.map { Json.Str(it) }),
        "verification" to (a.verification?.let { v ->
            Json.Obj(
                listOf(
                    "method" to Json.Str(v.method.wire),
                    "status" to Json.Str(v.status.wire),
                    "evidenceRefs" to Json.Arr(v.evidenceRefs.map { Json.Str(it) }),
                ),
            )
        } ?: Json.Null),
    ),
)

internal fun planActionsFromJson(raw: String?): List<PlanAction> {
    if (raw.isNullOrBlank()) return emptyList()
    return try {
        val arr = JsonParser.parse(raw) as? Json.Arr ?: return emptyList()
        arr.items.mapNotNull { it as? Json.Obj }.map { o ->
            val v = (o["verification"] as? Json.Obj)?.let { vo ->
                com.pdig.core.domain.ActionVerification(
                    method = ActionVerificationMethod.fromWire((vo["method"] as? Json.Str)?.value ?: "")
                        ?: ActionVerificationMethod.MANUAL_CONFIRMATION,
                    status = ActionVerificationStatus.fromWire((vo["status"] as? Json.Str)?.value ?: "")
                        ?: ActionVerificationStatus.PENDING,
                    evidenceRefs = ((vo["evidenceRefs"] as? Json.Arr)?.items)
                        ?.mapNotNull { (it as? Json.Str)?.value } ?: emptyList(),
                )
            }
            PlanAction(
                id = (o["id"] as? Json.Str)?.value ?: "",
                title = (o["title"] as? Json.Str)?.value ?: "",
                phase = PlanActionPhase.fromWire((o["phase"] as? Json.Str)?.value ?: "")
                    ?: PlanActionPhase.CHANGE,
                done = (o["done"] as? Json.Bool)?.value ?: false,
                resolvesImpactKeys = ((o["resolvesImpactKeys"] as? Json.Arr)?.items)
                    ?.mapNotNull { (it as? Json.Str)?.value } ?: emptyList(),
                verification = v,
            )
        }
    } catch (_: Throwable) {
        emptyList()
    }
}
