package com.pdig.app.evidence

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pdig.app.platform.AndroidSqliteDriver
import com.pdig.core.schema.SCHEMA_V1_STATEMENTS
import com.pdig.core.schema.SCHEMA_V2_STATEMENTS
import com.pdig.core.schema.SCHEMA_VERSION
import com.pdig.core.schema.getSchemaVersion
import com.pdig.core.schema.migrate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * ANDROID_SQLCIPHER / ANDROID_MIGRATION 的**设备内**证据。
 *
 * 与 JVM conformance 的区别：这里跑的是真机上 SQLCipher 的 native 实现，
 * 而不是 sqlite-jdbc。两者都必须过。
 */
@RunWith(AndroidJUnit4::class)
class PersistenceEvidenceTest {

    private fun ctx() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun dbFile(name: String) = File(ctx().filesDir, "ev-$name.db").apply {
        if (exists()) delete()
        parentFile?.mkdirs()
    }

    private val now = "2026-09-15T00:00:00.000Z"

    // ---------------------------------------------------------------- SQLCipher

    @Test
    fun sqlcipher_openCloseReopen_persistsRows() {
        val f = dbFile("reopen")
        val d1 = AndroidSqliteDriver.open(f, "pw-correct-1")
        d1.exec("CREATE TABLE t (a TEXT)")
        d1.prepare("INSERT INTO t (a) VALUES (?)").run("hello")
        d1.close()

        val d2 = AndroidSqliteDriver.open(f, "pw-correct-1")
        val row = d2.prepare("SELECT a FROM t").get()
        d2.close()
        assertNotNull("reopen with same key must read rows", row)
        assertEquals("hello", row!!.str("a"))
    }

    @Test
    fun sqlcipher_wrongKeyIsRejected() {
        val f = dbFile("wrongkey")
        val d1 = AndroidSqliteDriver.open(f, "pw-correct-2")
        d1.exec("CREATE TABLE t (a TEXT)")
        d1.prepare("INSERT INTO t (a) VALUES (?)").run("secret")
        d1.close()

        var threw = false
        try {
            val bad = AndroidSqliteDriver.open(f, "pw-WRONG")
            bad.prepare("SELECT a FROM t").all()
            bad.close()
        } catch (e: Throwable) {
            threw = true
        }
        assertTrue("wrong passphrase must not decrypt the DB", threw)
    }

    @Test
    fun sqlcipher_plainSqliteCannotRead() {
        val f = dbFile("plainread")
        val d1 = AndroidSqliteDriver.open(f, "pw-correct-3")
        d1.exec("CREATE TABLE t (a TEXT)")
        d1.prepare("INSERT INTO t (a) VALUES (?)").run("secret")
        d1.close()

        var threw = false
        try {
            val plain = android.database.sqlite.SQLiteDatabase.openDatabase(
                f.absolutePath,
                null,
                android.database.sqlite.SQLiteDatabase.OPEN_READONLY,
            )
            plain.rawQuery("SELECT a FROM t", null)?.use { it.moveToNext() }
            plain.close()
        } catch (e: Throwable) {
            threw = true
        }
        assertTrue("plain SQLite must not be able to read a SQLCipher DB", threw)
    }

    @Test
    fun sqlcipher_transactionRollbackLeavesNoPartialWrite() {
        val f = dbFile("rollback")
        val d = AndroidSqliteDriver.open(f, "pw-correct-4")
        try {
            d.exec("CREATE TABLE t (a TEXT)")
            var caught = false
            try {
                d.transaction {
                    d.prepare("INSERT INTO t (a) VALUES (?)").run("partial")
                    d.exec("THIS IS NOT VALID SQL")
                }
            } catch (e: Throwable) {
                caught = true
            }
            assertTrue("broken statement must throw", caught)
            assertEquals(0, d.prepare("SELECT a FROM t").all().size)
        } finally {
            d.close()
        }
    }

    // ---------------------------------------------------------------- Migration

    private fun seedV1(d: com.pdig.core.db.SqliteDriver) {
        for (s in SCHEMA_V1_STATEMENTS) d.exec(s)
        d.prepare("INSERT INTO meta (key, value) VALUES ('schema_version', '1')").run()
        d.prepare("INSERT INTO meta (key, value) VALUES ('graph_revision', '7')").run()

        d.prepare(
            """
            INSERT INTO nodes (id, kind, name, owner, created_at, updated_at)
            VALUES ('n-card', 'payment_card', 'Card A', 'self', '$now', '$now')
            """.trimIndent(),
        ).run()
        d.prepare(
            """
            INSERT INTO nodes (id, kind, name, owner, created_at, updated_at)
            VALUES ('n-bank', 'bank_account', 'Bank A', 'self', '$now', '$now')
            """.trimIndent(),
        ).run()
        d.prepare(
            """
            INSERT INTO dependencies (id, from_node, relation, to_node, capability, criticality,
                                      state, origin, confirmed_at, last_verified_at, created_at, updated_at)
            VALUES ('dep-1', 'n-card', 'funding_source', 'n-bank', 'payment', 'required',
                    'active', 'manual', '$now', '$now', '$now', '$now')
            """.trimIndent(),
        ).run()
        d.prepare(
            """
            INSERT INTO dependency_groups (id, group_key, target_node_id, capability, mode,
                                           member_edge_ids_json, confirmed_at, last_verified_at, created_at, updated_at)
            VALUES ('g-1', 'gk-1', 'n-card', 'payment', 'ANY', '["dep-1"]', '$now', '$now', '$now', '$now')
            """.trimIndent(),
        ).run()
        d.prepare(
            """
            INSERT INTO dependency_proposals (id, key, from_node, relation, to_node, capability,
                                              proposal_type, source, parser_id, parser_version,
                                              confidence_score, path_json, decision, decided_at,
                                              criticality_decision, observation_count, created_at, updated_at)
            VALUES ('p-1', 'pk-1', 'n-card', 'funding_source', 'n-bank', 'payment',
                    'add', 'wechat', 'wechat_statement', 1,
                    0.9, '[]', 'accepted', '$now',
                    'required', 3, '$now', '$now')
            """.trimIndent(),
        ).run()
        d.prepare(
            """
            INSERT INTO evidence (id, proposal_key, source_type, parser_id, parser_version,
                                  last_import_session_id, first_observed_at, last_observed_at,
                                  observation_count, created_at, updated_at)
            VALUES ('e-1', 'pk-1', 'statement_file', 'wechat_statement', 1,
                    'sess-1', '$now', '$now', 3, '$now', '$now')
            """.trimIndent(),
        ).run()
        d.prepare(
            """
            INSERT INTO observation_fingerprints (fingerprint, source, fingerprint_version, import_session_id, first_seen_at)
            VALUES ('fp-1', 'wechat', 1, 'sess-1', '$now')
            """.trimIndent(),
        ).run()
        d.prepare(
            """
            INSERT INTO import_sessions (id, source_type, parser_id, parser_version, started_at, raw_count)
            VALUES ('sess-1', 'statement_file', 'wechat_statement', 1, '$now', 10)
            """.trimIndent(),
        ).run()
        d.prepare(
            """
            INSERT INTO dependency_group_proposals (id, key, target_node_id, capability, mode,
                                                    member_dependency_keys_json, created_at, updated_at)
            VALUES ('gp-1', 'gpk-1', 'n-card', 'payment', 'ANY', '[]', '$now', '$now')
            """.trimIndent(),
        ).run()
    }

    @Test
    fun migration_v1_to_v3_preservesEverything() {
        val f = dbFile("v1v3")
        val d = AndroidSqliteDriver.open(f, "pw-mig")
        try {
            seedV1(d)
            val v = migrate(d, now)
            assertEquals(SCHEMA_VERSION, v)
            assertEquals(3, getSchemaVersion(d))

            assertEquals("node id preserved", "n-card", d.prepare("SELECT id FROM nodes WHERE id='n-card'").get()!!.str("id"))
            assertEquals(
                "decision preserved",
                "accepted",
                d.prepare("SELECT decision FROM dependency_proposals WHERE key='pk-1'").get()!!.str("decision"),
            )
            assertEquals(
                "evidence preserved and scoped to legacy SourceInstance",
                "legacy-wechat-statement",
                d.prepare("SELECT source_instance_id FROM evidence WHERE id='e-1'").get()!!.str("source_instance_id"),
            )
            assertEquals(
                "group preserved",
                "g-1",
                d.prepare("SELECT id FROM dependency_groups WHERE id='g-1'").get()!!.str("id"),
            )
            assertEquals(
                "SourceInstance created deterministically",
                1,
                d.prepare("SELECT id FROM source_instances WHERE id='legacy-wechat-statement'").all().size,
            )
            assertEquals(
                "graphRevision semantics preserved",
                "7",
                d.prepare("SELECT value FROM meta WHERE key='graph_revision'").get()!!.str("value"),
            )

            // 幂等：重复 50 次严格 no-op
            repeat(50) { migrate(d, now) }
            assertEquals(3, getSchemaVersion(d))
            assertEquals(
                1,
                d.prepare("SELECT id FROM source_instances WHERE id='legacy-wechat-statement'").all().size,
            )
            assertEquals(2, d.prepare("SELECT id FROM nodes").all().size)
        } finally {
            d.close()
        }
    }

    @Test
    fun migration_v2_to_v3() {
        val f = dbFile("v2v3")
        val d = AndroidSqliteDriver.open(f, "pw-mig2")
        try {
            for (s in SCHEMA_V1_STATEMENTS) d.exec(s)
            for (s in SCHEMA_V2_STATEMENTS) d.exec(s)
            d.prepare("INSERT INTO meta (key, value) VALUES ('schema_version', '2')").run()
            d.prepare(
                """
                INSERT INTO nodes (id, kind, name, owner, created_at, updated_at)
                VALUES ('n-x', 'payment_card', 'X', 'self', '$now', '$now')
                """.trimIndent(),
            ).run()

            val v = migrate(d, now)
            assertEquals(3, v)
            assertEquals("n-x", d.prepare("SELECT id FROM nodes WHERE id='n-x'").get()!!.str("id"))
            assertTrue(
                "v3 tables must exist",
                d.prepare("SELECT id FROM change_plans").all().isEmpty() &&
                    d.prepare("SELECT id FROM reality_drifts").all().isEmpty(),
            )
        } finally {
            d.close()
        }
    }

    @Test
    fun migration_futureSchemaVersionIsRejected_andDatabaseNotWiped() {
        val f = dbFile("future")
        val d = AndroidSqliteDriver.open(f, "pw-future")
        try {
            for (s in SCHEMA_V1_STATEMENTS) d.exec(s)
            d.prepare("INSERT INTO meta (key, value) VALUES ('schema_version', '99')").run()
            d.prepare(
                """
                INSERT INTO nodes (id, kind, name, owner, created_at, updated_at)
                VALUES ('n-keep', 'payment_card', 'Keep', 'self', '$now', '$now')
                """.trimIndent(),
            ).run()

            var message: String? = null
            try {
                migrate(d, now)
                fail("future schema version must be rejected")
            } catch (e: IllegalStateException) {
                message = e.message
            }
            assertTrue("reject reason must mention newer schema: $message", message?.contains("newer") == true)
            assertEquals(
                "DB must not be wiped by a rejected migration",
                1,
                d.prepare("SELECT id FROM nodes").all().size,
            )
        } finally {
            d.close()
        }
    }

    @Test
    fun migration_failureRollsBack_andDatabaseNotWiped() {
        val f = dbFile("failroll")
        val d = AndroidSqliteDriver.open(f, "pw-fail")
        try {
            seedV1(d)
            // 注入冲突：v2 的 CREATE TABLE source_instances 没有 IF NOT EXISTS → 必然失败
            d.exec("CREATE TABLE source_instances (id TEXT PRIMARY KEY)")

            var threw = false
            try {
                migrate(d, now)
            } catch (e: Throwable) {
                threw = true
            }
            assertTrue("conflicting migration must throw", threw)
            assertEquals(
                "failed migration must not leave half-migrated schema version",
                1,
                getSchemaVersion(d),
            )
            assertEquals(
                "failed migration must not wipe existing data",
                2,
                d.prepare("SELECT id FROM nodes").all().size,
            )
        } finally {
            d.close()
        }
    }
}
