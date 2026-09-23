package com.pdig.app.data

import com.pdig.core.sources.Observation

// ---------------------------------------------------------------------------
// 只读投影行（UI 与 Repository 共用的贫血模型；SQL 语义见各 Repository）
// ---------------------------------------------------------------------------

data class NodeRow(val id: String, val kind: String, val name: String, val archived: Boolean, val fieldsJson: String)
data class DependencyRow(
    val id: String,
    val from: String,
    val fromName: String,
    val relation: String,
    val to: String,
    val toName: String,
    val capability: String,
    val criticality: String,
    val state: String,
)

data class PlanRow(
    val id: String,
    val title: String,
    val scenario: String,
    val workflowState: String,
    val lastAnalyzedRevision: Int,
    val effectiveDate: String?,
)

data class DriftRow(
    val id: String,
    val kind: String,
    val targetNodeId: String,
    val capability: String,
    val candidateFrom: String?,
    val candidateRelation: String,
    val relatedDependencyIds: List<String>,
    val observationCount: Int,
    val detectedAt: String,
)
data class CandidateRow(
    val id: String,
    val candidateKind: String,
    val label: String,
    val observationCount: Int,
    val status: String,
)
data class SourceRow(val id: String, val label: String, val adapterId: String, val state: String, val lastIngestedAt: String?)
data class ProposalRow(
    val id: String,
    val key: String,
    val from: String,
    val to: String,
    val relation: String,
    val capability: String,
    val confidence: Double,
    val observationCount: Int,
)

data class ParseOutcome(val observations: List<Observation>, val errors: List<String>)
data class OrphanSummary(val orphanCount: Int)

// ---------------------------------------------------------------------------
// 备份导出的结果模型（F1 修复）
//
// 存在的唯一理由：让"成功/失败"与"失败在哪一步"变成**可断言的数据**，
// 而不是一个 `null`。UI 只负责把结果翻译成中文，不再自己猜。
// ---------------------------------------------------------------------------

/** 备份导出失败阶段。用户可见文案由 UI 映射，不直接暴露枚举（spec §65）。 */
enum class ExportFailureStage {
    /** 生成 .depmap 容器失败（加密封装）。 */
    ENCRYPT,

    /** 在 MediaStore Downloads 建行失败。 */
    INSERT,

    /** 写入输出流失败。 */
    WRITE,

    /** 写完回读校验失败（字节数不一致）。 */
    VERIFY,

    /** 发布（IS_PENDING=0）失败。 */
    PUBLISH,
}

sealed interface ExportBackupResult {

    /**
     * @param displayName MediaStore 回读的**真实**文件名（重名时系统会改写）
     * @param byteCount   落盘并校验通过的字节数
     */
    data class Success(val displayName: String, val byteCount: Int) : ExportBackupResult

    /**
     * @param stage     失败阶段
     * @param errorType 异常类型（或 `null_result`）
     * @param detail    非敏感的诊断细节，仅用于日志与自助排查
     * @param cleanupOk 失败后清理是否真的生效；`null` 表示失败发生在建行之前，无残留可清
     */
    data class Failure(
        val stage: ExportFailureStage,
        val errorType: String,
        val detail: String,
        val cleanupOk: Boolean?,
    ) : ExportBackupResult
}
 
 // ---------------------------------------------------------------------------
 // Import / Plan 共享模型（原 AppContainer 嵌套类；移至包级以便 Android 与 Desktop
 // 共享同一 Repository 层 —— 语义与 UI 引用不变，仅命名空间上移一层）。
 // ---------------------------------------------------------------------------
 
 data class DetectedParty(val label: String, val kind: com.pdig.core.generated.NodeKind, val nodeId: String)
 
 data class ImportPreview(
     val observations: List<com.pdig.core.sources.Observation>,
     val errors: List<String>,
     val adapterId: String,
     val sourceLabel: String,
     val instruments: List<DetectedParty>,
     val counterparties: List<DetectedParty>,
 ) {
     val projectedProposalCount: Int get() = instruments.size * counterparties.size
 }
 
 data class ImportCommitResult(
     val sourceInstanceId: String,
     val importSessionId: String,
     val rawCount: Int,
     val newUniqueCount: Int,
     val duplicateCount: Int,
     val nodeCount: Int,
     val proposalCount: Int,
     val errorCount: Int,
 )
 
 data class ScenarioPlanRequest(
     val scenarioId: String,
     val targetNodeId: String,
     val effectiveDate: String? = null,
 )
 
 data class PlanDetailView(
     val id: String,
     val scenario: String,
     val title: String,
     val workflowState: com.pdig.core.generated.ChangePlanWorkflowState,
     val effectiveState: com.pdig.core.generated.ChangePlanWorkflowState?,
     val baselineGraphRevision: Int,
     val lastAnalyzedGraphRevision: Int,
     val currentGraphRevision: Int,
     val targetNodeId: String?,
     val targetNodeName: String,
     val effectiveDate: String?,
     val actions: List<com.pdig.core.domain.PlanAction>,
     val mustChangeKeys: List<String>,
     val unresolvedMustChangeKeys: List<String>,
     val readiness: com.pdig.core.generated.PlanReadiness,
     val affectedServiceCount: Int,
 )