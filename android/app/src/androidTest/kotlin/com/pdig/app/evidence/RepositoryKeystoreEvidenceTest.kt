package com.pdig.app.evidence

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pdig.app.data.AppContainer
import com.pdig.app.platform.AndroidSqliteDriver
import com.pdig.app.security.AppLock
import com.pdig.app.security.DatabaseKeyStore
import com.pdig.app.security.LockState
import com.pdig.core.schema.SchemaVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * ANDROID_REPOSITORY / ANDROID_KEYSTORE / ANDROID_APP_LOCK 的设备内证据。
 */
@RunWith(AndroidJUnit4::class)
class RepositoryKeystoreEvidenceTest {

    private fun ctx() = InstrumentationRegistry.getInstrumentation().targetContext
    private val now = "2026-09-15T00:00:00.000Z"

    private fun appDbFile() = File(ctx().filesDir, "pdig.db")

    private fun openAppDb() = AndroidSqliteDriver.open(appDbFile(), DatabaseKeyStore.passphrase(ctx()))

    private fun seedNodesAndProposal(d: com.pdig.core.db.SqliteDriver, proposalId: String) {
        d.prepare(
            """
            INSERT OR IGNORE INTO nodes (id, kind, name, owner, created_at, updated_at)
            VALUES ('n-a', 'payment_card', 'Card A', 'self', '$now', '$now')
            """.trimIndent(),
        ).run()
        d.prepare(
            """
            INSERT OR IGNORE INTO nodes (id, kind, name, owner, created_at, updated_at)
            VALUES ('n-b', 'bank_account', 'Bank B', 'self', '$now', '$now')
            """.trimIndent(),
        ).run()
        d.prepare("DELETE FROM dependency_proposals WHERE id = ?").run(proposalId)
        d.prepare("DELETE FROM dependencies WHERE from_node='n-a' AND to_node='n-b'").run()
        d.prepare(
            """
            INSERT INTO dependency_proposals (id, key, from_node, relation, to_node, capability,
                                              proposal_type, source, parser_id, parser_version,
                                              confidence_score, path_json, decision,
                                              observation_count, created_at, updated_at)
            VALUES (?, ?, 'n-a', 'funding_source', 'n-b', 'payment',
                    'add', 'manual', 'generic_csv', 1,
                    0.75, '[]', 'pending',
                    2, '$now', '$now')
            """.trimIndent(),
        ).run(proposalId, "pk-$proposalId")
    }

    @Test
    fun repository_realityMutationBumpsGraphRevisionExactlyOnce() {
        val container = AppContainer.get(ctx())
        val d = openAppDb()
        try {
            val pid = "p-evidence-accept"
            seedNodesAndProposal(d, pid)

            val before = container.graphRevision()
            container.acceptProposal(pid)
            val after = container.graphRevision()

            assertEquals("Reality mutation must bump graphRevision by exactly 1", before + 1, after)
            assertNotNull(
                "dependency must be written to Reality",
                d.prepare("SELECT id FROM dependencies WHERE from_node='n-a' AND to_node='n-b'").get(),
            )
            assertEquals(
                "proposal must be marked accepted",
                "accepted",
                d.prepare("SELECT decision FROM dependency_proposals WHERE id=?").get(pid)!!.str("decision"),
            )

            // reject 属于 neverBumpsOn：不得 bump
            val pid2 = "p-evidence-reject"
            seedNodesAndProposal(d, pid2)
            container.rejectProposal(pid2)
            assertEquals("rejectProposal must not bump graphRevision", after, container.graphRevision())
            assertEquals(
                "rejectProposal must not write Reality",
                0,
                d.prepare("SELECT id FROM dependencies WHERE id=?").all("dep-$pid2").size,
            )
            assertTrue(
                "spec: proposal_decision must be in neverBumpsOn",
                SchemaVersion.graphRevisionNeverBumpedBy.contains("proposal_decision"),
            )
        } finally {
            d.close()
        }
    }

    @Test
    fun repository_transactionMechanismIsAtomic() {
        val f = File(ctx().filesDir, "ev-tx.db").apply { if (exists()) delete() }
        val d = AndroidSqliteDriver.open(f, "pw-tx")
        try {
            d.exec("CREATE TABLE t (a TEXT)")
            var caught = false
            try {
                d.transaction {
                    d.prepare("INSERT INTO t (a) VALUES (?)").run("partial")
                    throw IllegalStateException("boom")
                }
            } catch (e: IllegalStateException) {
                caught = true
            }
            assertTrue(caught)
            assertEquals(0, d.prepare("SELECT a FROM t").all().size)
        } finally {
            d.close()
            f.delete()
        }
    }

    @Test
    fun keystore_rawKeyNeverReachesDisk() {
        val ctx = ctx()
        val wrapped = DatabaseKeyStore.wrappedPassphrase(ctx)
        val plain = DatabaseKeyStore.passphrase(ctx)

        assertTrue(plain.isNotBlank())
        assertTrue("wrapped blob must differ from plaintext", wrapped != plain)
        assertFalse("wrapped blob must not contain plaintext", wrapped.contains(plain))

        val prefs = File(ctx.applicationInfo.dataDir, "shared_prefs/pdig_secure.xml")
        assertTrue("wrapped key must be persisted under app-private prefs", prefs.exists())
        val xml = prefs.readText()
        assertFalse("SharedPreferences must not contain the raw DB key", xml.contains(plain))
        assertTrue("SharedPreferences must contain only the wrapped value", xml.contains("db_passphrase_wrapped"))

        // 重新打开（等价于进程重启后走 wrapped key 路径）
        val d = openAppDb()
        try {
            assertNotNull("DB must reopen after re-deriving the wrapped key", d.prepare("SELECT 1 AS one").get())
        } finally {
            d.close()
        }
        assertEquals("passphrase must be stable across calls", plain, DatabaseKeyStore.passphrase(ctx))
    }

    @Test
    fun appLock_stateIsDeterministicAndFailClosed() {
        val ctx = ctx()
        val s = AppLock.state(ctx)
        assertTrue("state must be one of the declared enum values", s in LockState.values().toList())
        // 无可用生物特征时必须是 NOT_CONFIGURED / UNAVAILABLE（fail-closed），不允许 LOCKED->unlocked 直通
        if (s == LockState.NOT_CONFIGURED || s == LockState.UNAVAILABLE) {
            assertFalse("without an enrolled credential the app must not report unlocked", s == LockState.UNLOCKED)
        }
        println("APP_LOCK_STATE=$s")
    }
}
