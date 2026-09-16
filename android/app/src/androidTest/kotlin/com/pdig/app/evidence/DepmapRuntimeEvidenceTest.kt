package com.pdig.app.evidence

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pdig.app.platform.AndroidSqliteDriver
import com.pdig.core.crypto.DepmapContainer
import com.pdig.core.schema.migrate
import com.pdig.core.serialize.checkGraphIntegrity
import com.pdig.core.serialize.exportGraph
import com.pdig.core.serialize.importGraph
import com.pdig.core.serialize.migrateIfNeeded
import com.pdig.core.serialize.migratePayloadV1toV2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * `.depmap` 的**设备内**运行时验证（不修改冻结的 DEPMAP_CONTAINER_V1）。
 */
@RunWith(AndroidJUnit4::class)
class DepmapRuntimeEvidenceTest {

    private fun ctx() = InstrumentationRegistry.getInstrumentation().targetContext
    private val now = "2026-09-15T00:00:00.000Z"

    private fun freshDb(name: String, password: String = "pw-depmap"): AndroidSqliteDriver {
        val f = File(ctx().filesDir, "ev-depmap-$name.db").apply { if (exists()) delete() }
        val d = AndroidSqliteDriver.open(f, password)
        migrate(d, now)
        return d
    }

    private fun seed(d: AndroidSqliteDriver) {
        d.prepare(
            """
            INSERT INTO nodes (id, kind, name, owner, created_at, updated_at)
            VALUES ('n-card', 'payment_card', 'Card A', 'self', '$now', '$now')
            """.trimIndent(),
        ).run()
        d.prepare(
            """
            INSERT INTO nodes (id, kind, name, owner, created_at, updated_at)
            VALUES ('n-bank', 'bank_account', 'Bank B', 'self', '$now', '$now')
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
    }

    @Test
    fun depmap_exportImportRoundTrip_isByteIdentical() {
        val a = freshDb("a")
        val payloadA = exportGraph(a).payloadJson
        a.close()

        val container = DepmapContainer.create(payloadA.toByteArray(Charsets.UTF_8), "pw-备份").json
        val opened = String(DepmapContainer.openContainer(container, "pw-备份"), Charsets.UTF_8)
        assertEquals("container roundtrip must be byte-identical", payloadA, opened)

        val b = freshDb("b")
        importGraph(b, opened)
        val report = checkGraphIntegrity(b)
        val payloadB = exportGraph(b).payloadJson
        b.close()

        assertEquals("restore must reproduce the same logical graph", payloadA, payloadB)
        assertEquals(0, report.orphanDependencies.size + report.orphanGroups.size + report.orphanEvidence.size)
    }

    @Test
    fun depmap_wrongPasswordIsRejected() {
        val a = freshDb("wrongpw")
        val payload = exportGraph(a).payloadJson
        a.close()
        val container = DepmapContainer.create(payload.toByteArray(Charsets.UTF_8), "correct-pw").json

        try {
            DepmapContainer.openContainer(container, "wrong-pw")
            fail("wrong password must be rejected")
        } catch (e: Throwable) {
            assertTrue("expected an auth failure", e !is AssertionError)
        }
    }

    @Test
    fun depmap_tamperedCiphertextIsRejected() {
        val a = freshDb("tamper")
        val payload = exportGraph(a).payloadJson
        a.close()
        val container = DepmapContainer.create(payload.toByteArray(Charsets.UTF_8), "pw").json

        val idx = container.indexOf("ciphertext")
        assertTrue("container must carry a ciphertext field", idx >= 0)
        val at = idx + 20
        val flipped = if (container[at] == 'A') 'B' else 'A'
        val tampered = container.substring(0, at) + flipped + container.substring(at + 1)

        try {
            DepmapContainer.openContainer(tampered, "pw")
            fail("tampered ciphertext must be rejected")
        } catch (e: Throwable) {
            assertTrue("expected an auth failure", e !is AssertionError)
        }
    }

    @Test
    fun depmap_v1AndV2PayloadsMigrateToTheSameV3Graph() {
        val v1 = """
        {
          "payloadKind": "depmap-logical-graph",
          "payloadVersion": 1,
          "schemaVersion": 1,
          "meta": [{"key": "graph_revision", "value": "7"}],
          "nodes": [
            {"id": "n-card", "kind": "payment_card", "template_id": null, "name": "Card A",
             "issuer": null, "last4": null, "owner": "self", "archived": 0, "fields_json": "{}",
             "vault_ref": null, "wallet_ref": null, "created_at": "$now", "updated_at": "$now"}
          ],
          "dependencies": [],
          "dependency_groups": [],
          "dependency_proposals": [
            {"id": "p-1", "key": "pk-1", "from_node": "n-card", "relation": "funding_source",
             "to_node": "n-bank", "capability": "payment", "proposal_type": "add", "source": "wechat",
             "parser_id": "wechat_statement", "parser_version": 1, "confidence_score": 0.9,
             "path_json": "[]", "evidence_id": "e-1", "decision": "accepted", "decided_at": "$now",
             "criticality_decision": "required", "observation_count": 3, "rejected_at": null,
             "rejected_at_observation_count": null, "created_at": "$now", "updated_at": "$now"}
          ],
          "dependency_group_proposals": [],
          "evidence": [
            {"id": "e-1", "proposal_key": "pk-1", "source_type": "statement_file",
             "parser_id": "wechat_statement", "parser_version": 1, "last_import_session_id": "sess-1",
             "first_observed_at": "$now", "last_observed_at": "$now", "observation_count": 3,
             "created_at": "$now", "updated_at": "$now"}
          ],
          "observation_fingerprints": [
            {"fingerprint": "fp-1", "source": "wechat", "fingerprint_version": 1,
             "import_session_id": "sess-1", "first_seen_at": "$now"}
          ],
          "import_sessions": [
            {"id": "sess-1", "source_type": "statement_file", "parser_id": "wechat_statement",
             "parser_version": 1, "started_at": "$now", "completed_at": null, "raw_count": 10,
             "new_unique_count": 1, "duplicate_count": 0, "proposal_count": 1, "error_count": 0}
          ],
          "source_instances": []
        }
        """.trimIndent()

        val v2 = migratePayloadV1toV2(v1)
        val fromV1 = migrateIfNeeded(v1)
        val fromV2 = migrateIfNeeded(v2)
        assertEquals("v1→v3 and v2→v3 must converge", fromV2, fromV1)

        val d1 = freshDb("from-v1")
        importGraph(d1, fromV1)
        val exportedFromV1 = exportGraph(d1).payloadJson
        assertEquals(
            "migrated v1 payload must preserve the node",
            "n-card",
            d1.prepare("SELECT id FROM nodes WHERE id='n-card'").get()!!.str("id"),
        )
        assertEquals(
            "v1 evidence must be re-scoped to the legacy SourceInstance",
            "legacy-wechat-statement",
            d1.prepare("SELECT source_instance_id FROM evidence WHERE id='e-1'").get()!!.str("source_instance_id"),
        )
        assertEquals(
            "v1 decision must survive migration",
            "accepted",
            d1.prepare("SELECT decision FROM dependency_proposals WHERE key='pk-1'").get()!!.str("decision"),
        )
        d1.close()

        val d2 = freshDb("from-v2")
        importGraph(d2, fromV2)
        assertEquals("both paths must import to the same graph", exportedFromV1, exportGraph(d2).payloadJson)
        d2.close()
    }
}
