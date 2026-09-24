package com.pdig.desktop.data

import com.pdig.app.data.CandidateRepository
import com.pdig.app.data.DriftRepository
import com.pdig.app.data.GraphRepository
import com.pdig.app.data.PlanRepository
import com.pdig.app.data.ProposalRepository
import com.pdig.app.data.SourceRepository
import com.pdig.app.data.DiscoveryRepository
 import com.pdig.core.db.SqliteDriver
 import com.pdig.core.schema.migrate
 import com.pdig.core.serialize.importGraph
 import java.time.Instant

/**
 * Desktop 会话装配 —— 与 Android AppContainer 相同的 Repository 组合
 * （共享 :repos 模块，行为字节级一致），仅把 SQLCipher driver 换成 JVM sqlite-jdbc
 * （com.pdig.conformance.JdbcSqliteDriver，内存库）。数据在内存中运行；
 * 落盘只发生在 [com.pdig.desktop.persist.DepmapFileStore]（DEPMAP_CONTAINER_V1 加密）。
 *
 * Desktop 不是 Canonical Truth Source：这里没有任何领域规则实现，
 * 全部来自 android/:core + android/:repos（同一 spec/ 语义）。
 */
class DesktopSession private constructor(
    val driver: SqliteDriver,
) {
    val graph: GraphRepository = GraphRepository(driver) { proposals.pendingProposals() }
    val proposals: ProposalRepository = ProposalRepository(driver, graph)
    val candidates: CandidateRepository = CandidateRepository(driver)
    val drifts: DriftRepository = DriftRepository(driver, graph)
    val discovery: DiscoveryRepository = DiscoveryRepository(driver)
    val plans: PlanRepository = PlanRepository(driver, graph, proposals)
    val sources: SourceRepository = SourceRepository(driver, graph, proposals, discovery)

    companion object {
        /**
         * 打开（或新建）一个内存会话：迁移到 schema v3。调用方随后负责
         * 通过 [com.pdig.desktop.persist.DepmapFileStore] 加载/落盘数据。
         */
        fun open(): DesktopSession {
            val driver = com.pdig.conformance.JdbcSqliteDriver(":memory:")
            migrate(driver, Instant.now().toString())
            return DesktopSession(driver)
        }

        /**
         * Restore：先把 payload 导入到新 driver（原子替换），成功才返回会话。
         * 失败时 driver 已关闭，不留半状态。
         */
        fun restore(payloadJson: String): DesktopSession {
            val session = open()
            try {
                 importGraph(session.driver, payloadJson)
            } catch (e: Throwable) {
                session.close()
                throw e
            }
            return session
        }
    }

    /** exportGraph 的成功路径（payload），由 DepmapFileStore 加密后落盘。 */
    fun exportPayload(): String = com.pdig.core.serialize.exportGraph(driver).payloadJson

    fun close() = driver.close()
}