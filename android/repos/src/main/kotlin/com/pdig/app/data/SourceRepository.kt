package com.pdig.app.data

import com.pdig.core.db.SqliteDriver
import com.pdig.core.domain.dependencyLogicalKey
import com.pdig.core.generated.Capability
import com.pdig.core.generated.NodeKind
import com.pdig.core.generated.Relation
import com.pdig.core.sources.GenericCsvParser
import com.pdig.core.sources.MappingProfile
import com.pdig.core.sources.Observation
import com.pdig.core.sources.OfxParser
import com.pdig.core.sources.WechatParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

/**
 * 导入管道（spec §150：Observation 不落库，只在导入会话内存中）。
 *
 * 落库的是：SourceInstance / ImportSession / Fingerprint / Evidence /
 *           Node（用户在 Node Resolution 步骤显式确认）/ Proposal（待确认）。
 * **绝不**直接写 dependencies —— Proposal ≠ Reality（spec §13）。
 */
class SourceRepository(
    private val driver: SqliteDriver,
    private val graph: GraphRepository,
    private val proposals: ProposalRepository,
) {

    fun sourceInstances(): List<SourceRow> = driver.prepare(
        "SELECT id, label, adapter_id, state, last_ingested_at FROM source_instances ORDER BY created_at",
    ).all().map {
        SourceRow(
            id = it.str("id") ?: "",
            label = it.str("label") ?: "",
            adapterId = it.str("adapter_id") ?: "",
            state = it.str("state") ?: "active",
            lastIngestedAt = it.str("last_ingested_at"),
        )
    }

    // ------------------------------------------------------------------
    // 导入（Observation 仅内存，绝不落库 —— spec §150）
    // ------------------------------------------------------------------

    suspend fun parseFile(bytes: ByteArray, adapterId: String, mapping: MappingProfile?): ParseOutcome =
        withContext(Dispatchers.Default) {
            val result = when (adapterId) {
                "wechat" -> WechatParser.parse(bytes)
                "generic_csv" -> GenericCsvParser.parse(bytes, mapping)
                "ofx_qfx" -> OfxParser.parse(bytes)
                else -> error("unsupported adapter: $adapterId")
            }
            ParseOutcome(result.observations, result.errors.map { "${it.line}: ${it.reason}" })
        }

    /** 预览：**不写任何库**。Node Resolution 所需的解析结果列在内存里。 */
    fun previewImport(
        observations: List<Observation>,
        errors: List<String>,
        adapterId: String,
        sourceLabel: String,
    ): ImportPreview {
        val instruments = observations.asSequence()
            .map { it.paymentMethodRaw.trim() }
            .filter { it.isNotBlank() }
            .distinct().sorted()
            .map { DetectedParty(it, NodeKind.PAYMENT_INSTRUMENT, nodeIdFor(NodeKind.PAYMENT_INSTRUMENT, it)) }
            .toList()
        val counterparties = observations.asSequence()
            .map { it.merchantRaw.trim() }
            .filter { it.isNotBlank() }
            .distinct().sorted()
            .map { DetectedParty(it, NodeKind.SERVICE, nodeIdFor(NodeKind.SERVICE, it)) }
            .toList()
        return ImportPreview(observations, errors, adapterId, sourceLabel, instruments, counterparties)
    }

    /** 用户确认后提交：一次事务内完成，失败整体回滚。 */
    fun commitImport(preview: ImportPreview): ImportCommitResult {
        val now = Instant.now().toString()
        val sourceInstanceId = "src-" + sha256Hex(preview.adapterId + "|" + preview.sourceLabel).take(16)
        val sessionId = "imp-" + sha256Hex(sourceInstanceId + "|" + now).take(16)
        val observedActions = LinkedHashSet<Pair<String, String>>()
        var newUnique = 0
        var duplicates = 0
        var nodesCreated = 0
        val keys = LinkedHashSet<String>()

        driver.transaction {
            driver.prepare(
                """
                INSERT INTO source_instances
                  (id, adapter_id, adapter_version, source_kind, label, currencies_json, state, created_at, updated_at, last_ingested_at)
                VALUES (?, ?, 1, 'statement_file', ?, '[]', 'active', ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET last_ingested_at = excluded.last_ingested_at,
                                              updated_at = excluded.updated_at
                """.trimIndent(),
            ).run(sourceInstanceId, preview.adapterId, preview.sourceLabel, now, now, now)

            driver.prepare(
                """
                INSERT INTO import_sessions
                  (id, source_type, parser_id, parser_version, started_at, raw_count, new_unique_count,
                   duplicate_count, proposal_count, error_count, source_instance_id, adapter_id, adapter_version)
                VALUES (?, ?, ?, 1, ?, ?, 0, 0, 0, ?, ?, ?, 1)
                """.trimIndent(),
            ).run(
                sessionId, preview.adapterId, preview.adapterId, now,
                preview.observations.size, preview.errors.size,
                sourceInstanceId, preview.adapterId,
            )

            for (o in preview.observations) {
                val fp = observationFingerprint(preview.adapterId, o)
                val seen = driver.prepare(
                    """
                    SELECT fingerprint FROM observation_fingerprints
                     WHERE source_instance_id = ? AND fingerprint_version = 1 AND fingerprint = ?
                    """.trimIndent(),
                ).get(sourceInstanceId, fp)
                if (seen != null) {
                    duplicates += 1
                    continue
                }
                newUnique += 1
                driver.prepare(
                    """
                    INSERT INTO observation_fingerprints
                      (fingerprint, source_instance_id, source, fingerprint_version, import_session_id, first_seen_at)
                    VALUES (?, ?, ?, 1, ?, ?)
                    """.trimIndent(),
                ).run(fp, sourceInstanceId, preview.adapterId, sessionId, now)

                val instrument = o.paymentMethodRaw.trim()
                val counterparty = o.merchantRaw.trim()
                if (instrument.isBlank() || counterparty.isBlank()) continue

                observedActions.add(instrument to counterparty)
                val instrumentId = nodeIdFor(NodeKind.PAYMENT_INSTRUMENT, instrument)
                val counterpartyId = nodeIdFor(NodeKind.SERVICE, counterparty)
                nodesCreated += graph.upsertNode(instrumentId, NodeKind.PAYMENT_INSTRUMENT, instrument, now)
                nodesCreated += graph.upsertNode(counterpartyId, NodeKind.SERVICE, counterparty, now)

                val key = dependencyLogicalKey(
                    instrumentId, Relation.MERCHANT_AGREEMENT, counterpartyId, Capability.PAYMENT,
                )
                keys.add(key)
            }

            for (key in keys) proposals.upsertProposal(key, preview.adapterId, sessionId, sourceInstanceId, now)
        }

        driver.prepare(
            """
            UPDATE import_sessions
               SET completed_at = ?, new_unique_count = ?, duplicate_count = ?, proposal_count = ?
             WHERE id = ?
            """.trimIndent(),
        ).run(now, newUnique, duplicates, keys.size, sessionId)

        return ImportCommitResult(
            sourceInstanceId = sourceInstanceId,
            importSessionId = sessionId,
            rawCount = preview.observations.size,
            newUniqueCount = newUnique,
            duplicateCount = duplicates,
            nodeCount = nodesCreated,
            proposalCount = keys.size,
            errorCount = preview.errors.size,
        )
    }
}

/** 跨去重指纹：限定在 (source_instance, fingerprint_version) 作用域内 —— MVP02 § constraints。 */
internal fun observationFingerprint(adapterId: String, o: Observation): String =
    sha256Hex(
        listOf(
            adapterId,
            o.sourceTxnId ?: "",
            o.occurredAt,
            o.amount.toString(),
            o.currency,
            o.merchantRaw,
            o.paymentMethodRaw,
        ).joinToString("|"),
    )
