package com.pdig.app.data

import com.pdig.core.db.SqlRow
import com.pdig.core.db.SqliteDriver
import com.pdig.core.generated.CandidateStatus
import com.pdig.core.generated.DriftStatus
import com.pdig.core.generated.NodeKind
import com.pdig.core.json.Json
import com.pdig.core.json.JsonParser
import com.pdig.core.json.JsonWriter
import com.pdig.core.sources.Observation
import com.pdig.core.sources.ObservationDirection

/**
 * 共享生成引擎（H-16/H-17 生成侧）：DiscoveryCandidate 与 RealityDrift 保守生成/累计，
 * Android(:app) 与 Desktop 共用同一 :repos 代码，行为一致。
 *
 * 规则来自 spec/domain/entities.md §9–§10 + invariants.md（DR-01..09 / PC-01..06）：
 * 只接受支出型正证据；<2 观测不新建（DR-03）；已确认来源忽略（DR-02）；
 * 同 key 单一 open 行累计（DR-04）；重复 evidence 不计数（DR-05）；
 * dismiss 后 ≥2 条新观测才回到 pending（PC-04）；accepted 不再打扰（PC-05）。
 *
 * SAFETY: 本类不引用 GraphRepository → 结构上不可能 bump revision；
 * 只写 discovery_candidates / reality_drifts，绝不写 dependencies/nodes；
 * 机器不自动 accept / resolve / set required。
 */
class DiscoveryRepository(private val driver: SqliteDriver) {

    /** 新建候选 / 新建漂移的最小正证据条数（spec：观测数 < 2 忽略）；dismiss 重提同阈值。 */
    private val MIN_EVIDENCE = 2

    /** evidence ref 去重后仍设置上限，防止候选/漂移行无限增长。 */
    private val MAX_EVIDENCE_REFS = 64

    /**
     * 在导入事务内调用：从本次导入会话的 observations 生成/累计
     * pending/open 的候选与漂移行。
     *
     * @param observations     本次导入的全部观察（仅内存，不落库）
     * @param adapterId        导入适配器 id（evidence fingerprint，跨源 provenance 引用合并）
     * @param sourceInstanceId 本次导入的 source instance（候选行溯源）
     * @param now              事务时间戳
     */
    fun generateFromObservations(
        observations: List<Observation>,
        adapterId: String,
        sourceInstanceId: String,
        now: String,
    ) {
        // DR-01: 只有"支出"型观察才是 funding 正证据；空字段保守剔除。
        val positive = observations.filter {
            it.direction == ObservationDirection.OUT &&
                it.paymentMethodRaw.isNotBlank() &&
                it.merchantRaw.isNotBlank()
        }
        generateCandidates(positive, adapterId, sourceInstanceId, now)
        generateDrifts(positive, adapterId, now)
    }

    // ------------------------------------------------------------ Candidate

    private fun generateCandidates(
        positive: List<Observation>,
        adapterId: String,
        sourceInstanceId: String,
        now: String,
    ) {
        for ((key, obs) in positive.groupBy { normalizeLabel(it.merchantRaw) }) {
            val refs = evidenceRefs(obs, adapterId)
            val canonicalKey = "svc:$key"
            val existing = driver.prepare(
                """
                SELECT id, status, observation_count, dismissed_at_observation_count, evidence_refs_json
                  FROM discovery_candidates WHERE normalized_key = ?
                """.trimIndent(),
            ).get(canonicalKey)
            when (existing?.str("status")) {
                // 新建：DR-03 / PC-04 阈值（<2 不生成）。
                null -> {
                    if (refs.size >= MIN_EVIDENCE) {
                        insertCandidate(canonicalKey, obs.last().merchantRaw.trim(), sourceInstanceId, refs, now)
                    }
                }
                // 已存在（pending）：始终累计新证据（DR-05 去重后计数）。
                CandidateStatus.PENDING.wire -> accumulateCandidate(existing, refs, now)
                // dismissed：累计新证据；≥ 2 条新观测才回到 pending（PC-04）。
                CandidateStatus.DISMISSED.wire -> reProposeCandidate(existing, refs, now)
                // ACCEPTED / superseded → PC-05：同名新信号不再打扰。
                else -> Unit
            }
        }
    }

    private fun insertCandidate(
        canonicalKey: String,
        label: String,
        sourceInstanceId: String,
        refs: List<String>,
        now: String,
    ) {
        val id = "cand-" + sha256Hex(canonicalKey).take(16)
        driver.prepare(
            """
            INSERT INTO discovery_candidates
              (id, candidate_kind, display_label, normalized_key, source_instance_id, evidence_refs_json,
               observation_count, first_seen_at, last_seen_at, status, created_at, updated_at)
            VALUES (?, 'service', ?, ?, ?, ?, ?, ?, ?, 'pending', ?, ?)
            """.trimIndent(),
        ).run(id, label, canonicalKey, sourceInstanceId, JsonWriter.write(refsJson(refs)), refs.size, now, now, now, now)
    }

    private fun accumulateCandidate(existing: SqlRow, refs: List<String>, now: String) {
        val existingRefs = parseRefs(existing.str("evidence_refs_json"))
        // DR-05: 跨导入重复 evidence ref 不重复计数（只累计本次真正的新引用）。
        val newRefs = refs.filterNot { it in existingRefs }
        val merged = mergeRefs(existingRefs, newRefs)
        driver.prepare(
            """
            UPDATE discovery_candidates
               SET observation_count = ?, evidence_refs_json = ?, last_seen_at = ?, updated_at = ?
             WHERE id = ?
            """.trimIndent(),
        ).run(
            (existing.long("observation_count") ?: 0L) + newRefs.size,
            JsonWriter.write(refsJson(merged)),
            now,
            now,
            existing.str("id"),
        )
    }

    private fun reProposeCandidate(existing: SqlRow, refs: List<String>, now: String) {
        val existingRefs = parseRefs(existing.str("evidence_refs_json"))
        // PC-04: dismiss 后需 ≥ 2 条**新**观测才回到 pending。
        // 新证据始终累计（不丢失），但状态只在 ≥ 2 条新观测后才回到 pending。
        val newRefs = refs.filterNot { it in existingRefs }
        val merged = mergeRefs(existingRefs, newRefs)
        val nextStatus = if (newRefs.size >= MIN_EVIDENCE) "pending" else "dismissed"
        driver.prepare(
            """
            UPDATE discovery_candidates
               SET status = ?, observation_count = ?, evidence_refs_json = ?, last_seen_at = ?, updated_at = ?
             WHERE id = ?
            """.trimIndent(),
        ).run(
            nextStatus,
            (existing.long("observation_count") ?: 0L) + newRefs.size,
            JsonWriter.write(refsJson(merged)),
            now,
            now,
            existing.str("id"),
        )
    }

    // ------------------------------------------------------------ Drift

    private fun generateDrifts(positive: List<Observation>, adapterId: String, now: String) {
        // 先解析本次会话的 (instrumentId → serviceId) 正证据集合。
        val sessions = LinkedHashMap<String, MutableList<Observation>>()
        for (o in positive) {
            val instrumentId = nodeIdFor(NodeKind.PAYMENT_INSTRUMENT, o.paymentMethodRaw.trim())
            val serviceId = nodeIdFor(NodeKind.SERVICE, o.merchantRaw.trim())
            sessions.getOrPut(driftKey(instrumentId, serviceId)) { mutableListOf() }.add(o)
        }
        for ((key, obs) in sessions) {
            val refs = evidenceRefs(obs, adapterId)
            val parts = key.split('|')
            val instrumentId = parts[0]
            val serviceId = parts[1]
            val existing = driver.prepare(
                """
                SELECT id, status, observation_count, evidence_refs_json FROM reality_drifts
                 WHERE target_node_id = ? AND capability = 'payment' AND candidate_from = ?
                 ORDER BY detected_at DESC LIMIT 1
                """.trimIndent(),
            ).get(serviceId, instrumentId)
            when (existing?.str("status")) {
                // 新建：DR-02/DR-03 门槛 —— 已确认来源忽略；<2 观测忽略；无已确认 Reality 忽略。
                null -> {
                    if (refs.size < MIN_EVIDENCE) continue
                    val confirmed = driver.prepare(
                        """
                        SELECT COUNT(*) AS c FROM dependencies
                         WHERE from_node = ? AND to_node = ? AND relation = 'funding_source'
                           AND capability = 'payment' AND state = 'active'
                        """.trimIndent(),
                    ).get(instrumentId, serviceId)?.long("c") ?: 0L
                    if (confirmed > 0) continue // DR-02
                    val confirmedFunding = confirmedFundingFor(serviceId)
                    if (confirmedFunding.isEmpty()) continue // 无已确认 Reality → candidate 路径
                    insertDrift(
                        "drift-" + sha256Hex(serviceId + "|" + instrumentId).take(16),
                        serviceId,
                        instrumentId,
                        confirmedFunding,
                        refs,
                        now,
                    )
                }
                // open：始终累计新证据（DR-04 单行累计；DR-05 去重）。
                DriftStatus.OPEN.wire -> accumulateDrift(existing, refs, now)
                // dismissed：≥ 2 条新观测才重新 open。
                DriftStatus.DISMISSED.wire -> {
                    val existingRefs = parseRefs(existing.str("evidence_refs_json"))
                    val newRefs = refs.filterNot { it in existingRefs }
                    if (newRefs.size >= MIN_EVIDENCE) {
                        val confirmedFunding = confirmedFundingFor(serviceId)
                        insertDrift(
                            "drift-" + sha256Hex(serviceId + "|" + instrumentId).take(16),
                            serviceId,
                            instrumentId,
                            confirmedFunding,
                            newRefs,
                            now,
                        )
                    }
                }
                // confirmed_change / superseded → Reality 已落定，不再打扰。
                else -> Unit
            }
        }
    }
    /** 服务已有的确认 funding 来源（active 边 id 列表）；无则空。 */
    private fun confirmedFundingFor(serviceId: String): List<String> = driver.prepare(
        """
        SELECT id FROM dependencies
         WHERE to_node = ? AND relation = 'funding_source' AND capability = 'payment' AND state = 'active'
        """.trimIndent(),
    ).all(serviceId).mapNotNull { it.str("id") }
    private fun accumulateDrift(existing: SqlRow, refs: List<String>, now: String) {
        val existingRefs = parseRefs(existing.str("evidence_refs_json"))
        val newRefs = refs.filterNot { it in existingRefs }
        val merged = mergeRefs(existingRefs, newRefs)
        driver.prepare(
            """
            UPDATE reality_drifts
               SET observation_count = ?, evidence_refs_json = ?, updated_at = ?
             WHERE id = ?
            """.trimIndent(),
        ).run(
            (existing.long("observation_count") ?: 0L) + newRefs.size,
            JsonWriter.write(refsJson(merged)),
            now,
            existing.str("id"),
        )
    }

    private fun insertDrift(
        id: String,
        serviceId: String,
        instrumentId: String,
        confirmedFunding: List<String>,
        refs: List<String>,
        now: String,
    ) {
        driver.prepare(
            """
            INSERT INTO reality_drifts
              (id, kind, target_node_id, capability, candidate_from, candidate_relation,
               related_dependency_ids_json, evidence_refs_json, proposal_keys_json, observation_count,
               detected_at, updated_at, status)
            VALUES (?, 'possible_additional_path', ?, 'payment', ?, 'funding_source', ?, ?, '[]', ?, ?, ?, 'open')
            """.trimIndent(),
        ).run(
            id,
            serviceId,
            instrumentId,
            JsonWriter.write(refsJson(confirmedFunding)),
            JsonWriter.write(refsJson(refs)),
            refs.size,
            now,
            now,
        )
    }

    // ------------------------------------------------------------ helpers
    private fun driftKey(instrumentId: String, serviceId: String): String = "$instrumentId|$serviceId"

    /** 归一化 merchant：trim + 小写 + 折叠连续空白；确定性，跨导入稳定。 */
    private fun normalizeLabel(raw: String): String =
        raw.trim().lowercase().replace(Regex("\\s+"), " ")
    /** 本次导入内该组观察的去重 evidence ref（DR-05：fingerprint 级去重）。 */
    private fun evidenceRefs(obs: List<Observation>, adapterId: String): List<String> =
        obs.map { "fp:" + observationFingerprint(adapterId, it) }.distinct()
    private fun refsJson(refs: List<String>): Json.Arr = Json.Arr(refs.map { Json.Str(it) })

    private fun parseRefs(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            (JsonParser.parse(json) as? Json.Arr)?.items?.mapNotNull { (it as? Json.Str)?.value }
                ?: emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
    }
    /** 合并去重 + 上限截断（DR-05/DR-06：provenance 引用合并不复制内容）。 */
    private fun mergeRefs(existing: List<String>, incoming: List<String>): List<String> =
        (existing + incoming).distinct().take(MAX_EVIDENCE_REFS)
}