package com.pdig.app.data

import android.content.Context
import com.pdig.app.platform.AndroidSqliteDriver
import com.pdig.app.security.DatabaseKeyStore
import com.pdig.core.db.SqliteDriver
import com.pdig.core.domain.ImpactGraph
import com.pdig.core.generated.ChangePlanWorkflowState
import com.pdig.core.generated.NodeKind
import com.pdig.core.impact.ImpactResult
import com.pdig.core.schema.migrate
import com.pdig.core.sources.MappingProfile
import com.pdig.core.sources.Observation
import com.pdig.core.timeline.TimelineItem
import java.io.File
import java.time.Instant

/**
 * 应用层装配（spec §56/§57）：Domain/Application 不依赖 Android；
 * 这里只做平台实现（SQLCipher / Keystore）与领域层的接线。
 *
 * 公开方法保持原签名与行为不变，仅委托给同包 Repository 类
 * （Graph / Proposal / Candidate / Drift / Source / Plan / Backup）。
 */
class AppContainer private constructor(private val driver: SqliteDriver) {

    companion object {
        @Volatile
        private var instance: AppContainer? = null

        fun get(context: Context): AppContainer = instance ?: synchronized(this) {
            instance ?: run {
                val file = File(context.filesDir, "pdig.db")
                val driver = AndroidSqliteDriver.open(file, DatabaseKeyStore.passphrase(context))
                migrate(driver, Instant.now().toString())
                AppContainer(driver).also { instance = it }
            }
        }

        /**
         * 测试/取证用工厂：用调用方提供的 driver 构造（androidTest 在 app 模块内可访问）。
         * 生产路径只走 [get]。
         */
        internal fun forDriver(driver: SqliteDriver): AppContainer = AppContainer(driver)

        /** 拆除单例（L-37 删除所有数据用）。调用方先 close 当前 driver，否则下次 get 会重开已关闭的库。 */
        internal fun reset() {
            synchronized(this) {
                instance?.let { app ->
                    runCatching { (app.driver as? com.pdig.app.platform.AndroidSqliteDriver)?.close() }
                }
                instance = null
            }
        }
    }

    // ---- repository wiring（同一 driver；跨类的 Reality mutation 与 revision bump 仍同事务）----
    private val graphRepo: GraphRepository = GraphRepository(driver) { proposalRepo.pendingProposals() }
    private val proposalRepo: ProposalRepository = ProposalRepository(driver, graphRepo)
    private val candidateRepo = CandidateRepository(driver)
    private val driftRepo = DriftRepository(driver, graphRepo)
    private val planRepo = PlanRepository(driver, graphRepo, proposalRepo)
    private val sourceRepo = SourceRepository(driver, graphRepo, proposalRepo)
    private val backupRepo = BackupRepository(driver)

    // ------------------------------------------------------------------
    // 查询（只读投影）
    // ------------------------------------------------------------------

    fun nodes(includeArchived: Boolean = false): List<NodeRow> = graphRepo.nodes(includeArchived)

    fun dependencies(): List<DependencyRow> = graphRepo.dependencies()

    fun plans(): List<PlanRow> = planRepo.plans()

    fun openDrifts(): List<DriftRow> = driftRepo.openDrifts()

    fun pendingCandidates(): List<CandidateRow> = candidateRepo.pendingCandidates()

    fun sourceInstances(): List<SourceRow> = sourceRepo.sourceInstances()

    fun graphRevision(): Int = graphRepo.graphRevision()

    fun timeline(nowIso: String = Instant.now().toString()): List<TimelineItem> = graphRepo.timeline(nowIso)

    fun pendingProposals(): List<ProposalRow> = proposalRepo.pendingProposals()

    // ------------------------------------------------------------------
    // 导入（Observation 仅内存，绝不落库 —— spec §150）
    // ------------------------------------------------------------------

    suspend fun parseFile(bytes: ByteArray, adapterId: String, mapping: MappingProfile?): ParseOutcome =
        sourceRepo.parseFile(bytes, adapterId, mapping)

    /** 用户确认后写入 Reality（与 graphRevision bump 同事务 —— spec §26/§27）。 */
    fun acceptProposal(proposalId: String) = proposalRepo.acceptProposal(proposalId)

    fun rejectProposal(proposalId: String) = proposalRepo.rejectProposal(proposalId)

    // ------------------------------------------------------------------
    // DiscoveryCandidate（H-16：发现 → Review → Confirm → Node / Dismiss / Later）
    // ------------------------------------------------------------------

    fun acceptCandidate(candidateId: String): String = candidateRepo.acceptCandidate(candidateId)

    fun dismissCandidate(candidateId: String) = candidateRepo.dismissCandidate(candidateId)

    // ------------------------------------------------------------------
    // RealityDrift（H-17：发现 → Review → 用户选择 → Reality mutation）
    // ------------------------------------------------------------------

    fun resolveDriftAsReplacement(driftId: String) = driftRepo.resolveDriftAsReplacement(driftId)

    fun resolveDriftAsAdditionalPath(driftId: String) = driftRepo.resolveDriftAsAdditionalPath(driftId)

    fun dismissDrift(driftId: String) = driftRepo.dismissDrift(driftId)

    /** Observation-only 置信度常量：机器永不据此写 Reality，也不据此产生 required。 */
    val observedConfidence: Double get() = OBSERVED_CONFIDENCE

    /** 预览：**不写任何库**。Node Resolution 所需的解析结果列在内存里。 */
    fun previewImport(
        observations: List<Observation>,
        errors: List<String>,
        adapterId: String,
        sourceLabel: String,
    ): ImportPreview = sourceRepo.previewImport(observations, errors, adapterId, sourceLabel)

    /** 用户确认后提交：一次事务内完成，失败整体回滚。 */
    fun commitImport(preview: ImportPreview): ImportCommitResult = sourceRepo.commitImport(preview)

    // ------------------------------------------------------------------
    // Impacts（复用 core.impact，UI 不自行推导）
    // ------------------------------------------------------------------

    /**
     * Reality 确认：criticality unknown → required 只能由用户显式完成（机器永不产生 required，spec §12）。
     * 这属于 GraphRevisionMachine.bumpsOn 的 dependency_confirm_update，必须 bump graphRevision。
     */
    fun setDependencyCriticality(dependencyId: String, required: Boolean) =
        graphRepo.setDependencyCriticality(dependencyId, required)

    fun loadImpactGraph(): ImpactGraph = graphRepo.loadImpactGraph()

    fun impactFor(nodeId: String): ImpactResult = graphRepo.impactFor(nodeId)

    fun integrity(): OrphanSummary = graphRepo.integrity()

    // ------------------------------------------------------------------
    // ChangePlan / Action / Verification
    // 纯复用 core.plan 的显式 resolution 规则与 core.statemachine 的迁移表，
    // UI 侧不做"完成动作数减法"，也不把 done 当成 verified（spec §48/§49）。
    // ------------------------------------------------------------------

    /** 从 scenario 创建真实变更计划：must_change 每条生成一个显式 CHANGE 动作。 */
    fun createPlanForScenario(req: ScenarioPlanRequest): String = planRepo.createPlanForScenario(req)

    fun planDetail(planId: String): PlanDetailView? = planRepo.planDetail(planId)

    /** 完成动作：done=true。verification 仍为 pending —— done ≠ verified。 */
    fun completeAction(planId: String, actionId: String) = planRepo.completeAction(planId, actionId)

    /** 用户人工确认验证 → verified（VF-FROZEN：不得覆盖 verified / failed / not_required）。 */
    fun verifyAction(planId: String, actionId: String) = planRepo.verifyAction(planId, actionId)

    // ------------------------------------------------------------------
    // Backup / Restore（.depmap）
    // ------------------------------------------------------------------

    /**
     * 把 .depmap 容器写成用户可见的 Downloads 文件（MediaStore，scoped storage）。
     * 分级失败结果见 [ExportBackupResult] / [ExportFailureStage]（实现位于 BackupRepository）。
     */
    fun exportBackupToFile(context: Context, password: String, fileName: String): ExportBackupResult =
        backupRepo.exportBackupToFile(context, password, fileName)

    suspend fun exportBackup(password: String): String = backupRepo.exportBackup(password)

    suspend fun restoreBackup(containerJson: String, password: String): Int =
        backupRepo.restoreBackup(containerJson, password)

}