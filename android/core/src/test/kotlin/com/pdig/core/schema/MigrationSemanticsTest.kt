package com.pdig.core.schema

import com.pdig.core.db.JdbcTestDriver
import com.pdig.core.db.SqliteDriver
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 迁移语义测试（跑在真实 SQLite 上，不是桩）。
 *
 * 契约（Migrations.kt 头部）：transactional / failure rollback / idempotent /
 * IDs preserved / 未来版本明确 reject。
 */
class MigrationSemanticsTest {

    private lateinit var driver: JdbcTestDriver

    @BeforeTest
    fun setup() {
        driver = JdbcTestDriver()
    }

    private fun tableNames(): Set<String> =
        driver.prepare("SELECT name FROM sqlite_master WHERE type = 'table'").all()
            .mapNotNull { it.str("name") }.toSet()

    @Test
    fun freshDatabaseMigratesToCurrentVersion() {
        val version = migrate(driver, "2026-01-01T00:00:00Z")
        assertEquals(SCHEMA_VERSION, version, "全新库必须迁移到当前 schema version")
        assertEquals(3, getSchemaVersion(driver))
    }

    @Test
    fun repeatedMigrationIsStrictlyNoOp() {
        migrate(driver, "2026-01-01T00:00:00Z")
        val afterFirst = driver.count("SELECT COUNT(*) FROM source_instances")

        // 重复 50 次必须与第一次完全一致
        repeat(50) { migrate(driver, "2026-01-01T00:00:00Z") }

        assertEquals(3, getSchemaVersion(driver))
        assertEquals(
            afterFirst,
            driver.count("SELECT COUNT(*) FROM source_instances"),
            "重复迁移不得重建 legacy SourceInstance",
        )
    }

    @Test
    fun futureSchemaVersionIsRejected() {
        // 先落到当前版本，再伪造"来自未来"的库：这样 meta 表已存在，
        // 被断言的行为只有"版本号比支持版本大时必须明确拒绝"。
        migrate(driver, "2026-01-01T00:00:00Z")
        driver.prepare("UPDATE meta SET value = ? WHERE key = 'schema_version'").run("99")
        val err = assertFailsWith<IllegalStateException> {
            migrate(driver, "2026-01-01T00:00:00Z")
        }
        assertTrue(
            err.message!!.contains("newer than supported"),
            "未来版本必须明确拒绝，而不猜测兼容；实际：${err.message}",
        )
    }

    @Test
    fun v1IntroducesFoundationTables() {
        driver.transaction {
            for (sql in SCHEMA_V1_STATEMENTS) driver.exec(sql)
            driver.prepare("INSERT INTO meta (key, value) VALUES ('schema_version', '1')").run()
        }
        val tables = tableNames()
        for (expected in listOf(
            "meta", "nodes", "dependencies", "dependency_groups",
            "dependency_proposals", "import_sessions", "evidence", "observation_fingerprints",
        )) {
            assertTrue(expected in tables, "v1 必须建立 $expected")
        }
        // v3 专属表此时不应存在
        assertTrue("change_plans" !in tables, "v1 不应有 change_plans")
    }

    @Test
    fun upgradingV1ToV3BringsSourceScopedFingerprintAndPlanTables() {
        // 先造一个 v1 库
        driver.transaction {
            for (sql in SCHEMA_V1_STATEMENTS) driver.exec(sql)
            driver.prepare("INSERT INTO meta (key, value) VALUES ('schema_version', '1')").run()
        }
        assertEquals(1, getSchemaVersion(driver))

        migrate(driver, "2026-01-01T00:00:00Z")

        val tables = tableNames()
        for (expected in listOf("source_instances", "change_plans", "reality_drifts", "discovery_candidates")) {
            assertTrue(expected in tables, "v1→v3 后必须存在 $expected")
        }
        // v2 的表重建后旧表必须被 DROP，不允许并存两套
        assertTrue("dependency_proposals_v2" !in tables, "重建后的临时表必须被 DROP")
        assertTrue("evidence_v2" !in tables, "重建后的临时表必须被 DROP")
    }

    @Test
    fun v2KeepsEvidenceRowsAndAttributesThemToLegacySourceInstance() {
        driver.transaction {
            for (sql in SCHEMA_V1_STATEMENTS) driver.exec(sql)
            driver.prepare("INSERT INTO meta (key, value) VALUES ('schema_version', '1')").run()
            driver.prepare(
                """
                INSERT INTO evidence (id, proposal_key, source_type, parser_id, parser_version,
                                      last_import_session_id, first_observed_at, last_observed_at,
                                      observation_count, created_at, updated_at)
                VALUES ('ev-1', 'k1', 'wechat', 'wechat_statement', 1, 'imp-1',
                        '2026-01-01T00:00:00Z', '2026-01-01T00:00:00Z', 3,
                        '2026-01-01T00:00:00Z', '2026-01-01T00:00:00Z')
                """.trimIndent(),
            ).run()
        }

        migrate(driver, "2026-01-01T00:00:00Z")

        val row = driver.prepare("SELECT * FROM evidence WHERE id = 'ev-1'").get()
        assertEquals("ev-1", row?.str("id"), "Evidence 必须在迁移后保留（spec：Evidence preserved）")
        assertEquals(3L, row?.long("observation_count"))
        assertEquals(LEGACY_WECHAT_SOURCE_INSTANCE_ID, row?.str("source_instance_id"))
    }

    @Test
    fun failureRollsBackAndDoesNotAdvanceVersion() {
        // 在第 2 条语句上抛错，模拟半途失败
        val failing = FailingDriver(delegate = driver, failOnStatementIndex = 1)
        assertFailsWith<RuntimeException> { migrate(failing, "2026-01-01T00:00:00Z") }

        val version = getSchemaVersion(driver)
        assertTrue(version < 3, "迁移失败必须回滚，不得推进到新版：实际 $version")
    }

    @Test
    fun migrationFailureDoesNotCreateTheDangerousHalfState() {
        val failing = FailingDriver(delegate = driver, failOnStatementIndex = 1)
        runCatching { migrate(failing, "2026-01-01T00:00:00Z") }

        // 部分建表后 schema_version 仍是 1 ⇒ 后续正常迁移必须仍能补完
        migrate(driver, "2026-01-01T00:00:00Z")
        assertEquals(3, getSchemaVersion(driver), "失败后补跑必须能收敛到完整状态")
        assertTrue("change_plans" in tableNames())
    }

    @Test
    fun legacyWechatSourceInstanceIsCreatedExactlyOnce() {
        migrate(driver, "2026-01-01T00:00:00Z")
        repeat(5) { migrate(driver, "2026-01-01T00:00:00Z") }
        assertEquals(
            1,
            driver.count("SELECT COUNT(*) FROM source_instances WHERE id = '$LEGACY_WECHAT_SOURCE_INSTANCE_ID'"),
            "legacy 实例必须唯一且确定性（重复迁移不得再多插一个）",
        )
    }

    @Test
    fun schemaVersionUnknownBeforeAnyMigration() {
        // meta 表存在但没有版本记录 ⇒ 视为 0（全新库）
        assertEquals(0, getSchemaVersion(driver))
        assertNull(driver.prepare("SELECT value FROM meta WHERE key = 'schema_version'").get())
    }
}

/** 在指定序号的语句上抛错，用于验证"失败回滚、不留半迁移状态"。 */
private class FailingDriver(
    private val delegate: SqliteDriver,
    private val failOnStatementIndex: Int,
) : SqliteDriver by delegate {
    private var index = 0
    override fun exec(sql: String) {
        if (index == failOnStatementIndex) throw RuntimeException("injected failure")
        index += 1
        delegate.exec(sql)
    }
}
