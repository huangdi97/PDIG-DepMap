package com.pdig.conformance

import com.pdig.core.crypto.DepmapContainer
import com.pdig.core.json.Json
import com.pdig.core.schema.LEGACY_WECHAT_SOURCE_INSTANCE_ID
import com.pdig.core.schema.SCHEMA_V1_STATEMENTS
import com.pdig.core.schema.migrate
import com.pdig.core.serialize.checkGraphIntegrity
import com.pdig.core.serialize.exportGraph
import com.pdig.core.serialize.importGraph
import com.pdig.core.timeline.buildTimeline

// ---------------------------------------------------------------------------
// backup —— .depmap 导出 / 恢复往返（Cutover 核心要求）
// ---------------------------------------------------------------------------

internal fun runBackup(input: Json.Obj): Json {
    val password = str(input, "password")
    val salt = hexToBytes(str(input, "saltHex"))
    val nonce = hexToBytes(str(input, "nonceHex"))

    // 源库
    val srcResult = withTempDb("bak") { driver ->
        migrate(driver, T0)
        for (nodeId in arr(input, "seededNodes")) {
            val id = requireStr(nodeId).value
            val kind = if (id == "b-card") "payment_instrument" else "account"
            val name = if (id == "b-card") "招行 4417" else "微信支付"
            insertNode(driver, id, kind, name)
        }
        driver.prepare(
            """
            INSERT INTO dependencies (id, from_node, relation, to_node, capability, criticality, state, origin, confirmed_at, last_verified_at, evidence_refs_json, verification_basis_type, created_at, updated_at)
            VALUES ('b-dep', 'b-card', 'funding_source', 'b-wechat', 'payment', 'required', 'active', 'manual', ?, ?, '[]', 'user_confirmed', ?, ?)
            """.trimIndent(),
        ).run(T0, T0, T0, T0)
        exportGraph(driver)
    }

    // 加密 → 解密
    val created = DepmapContainer.create(
        srcResult.payloadJson.toByteArray(Charsets.UTF_8),
        password,
        DepmapContainer.CreateOptions(salt = salt, nonce = nonce),
    )
    val plaintext = DepmapContainer.openContainer(created.json, password)
    val decrypted = plaintext.toString(Charsets.UTF_8)

    // 恢复到全新库 → 再导出
    val restored = withTempDb("bak-restore") { driver ->
        migrate(driver, T0)
        val imported = importGraph(driver, decrypted)
        val reExported = exportGraph(driver)
        val integrity = checkGraphIntegrity(driver)
        Triple(imported, reExported, integrity)
    }
    val imported = restored.first
    val reExported = restored.second
    val integrity = restored.third

    return Json.Obj(
        listOf(
            "payloadJson" to Json.Str(srcResult.payloadJson),
            "counts" to Json.Obj(srcResult.counts.map { (k, v) -> k to Json.Num(v.toString()) }),
            "containerJson" to Json.Str(created.json),
            "decryptedEqualsPayload" to Json.Bool(decrypted == srcResult.payloadJson),
            "imported" to Json.Obj(imported.map { (k, v) -> k to Json.Num(v.toString()) }),
            "roundtripPayloadJson" to Json.Str(reExported.payloadJson),
            "roundtripEqual" to Json.Bool(reExported.payloadJson == srcResult.payloadJson),
            "integrity" to Json.Obj(
                listOf(
                    "orphanDependencies" to Json.Arr(integrity.orphanDependencies.map { Json.Str(it) }),
                    "orphanGroups" to Json.Arr(integrity.orphanGroups.map { Json.Str(it) }),
                    "danglingGroupMembers" to Json.Arr(integrity.danglingGroupMembers.map { Json.Str(it) }),
                    "orphanEvidence" to Json.Arr(integrity.orphanEvidence.map { Json.Str(it) }),
                    "orphanFingerprints" to Json.Arr(integrity.orphanFingerprints.map { Json.Str(it) }),
                ),
            ),
        ),
    )
}

// ---------------------------------------------------------------------------
// timeline（临时 DB，跑完即删）
// ---------------------------------------------------------------------------

internal fun runTimeline(id: String, input: Json.Obj): Json {
    val now = str(input, "now")
    val freshness = (input["freshnessThresholdDays"] as? Json.Num)?.asLong()?.toInt() ?: 45
    return withTempDb("tl") { driver ->
        migrate(driver, T0)
        when (id) {
            // 镜像 core/scripts/generate-conformance.ts §12 的三种种子状态
            "timeline-buckets-and-ordering" -> {
                insertNode(driver, "n-card", "payment_instrument", "招行 4417")
                val dates = arr(input, "plans").map { requireStr(it).value }
                dates.forEachIndexed { i, d -> insertPlan(driver, "plan-${i + 1}", "计划 ${i + 1}", d, 5) }
                val items = buildTimeline(driver, now, freshness)
                val again = buildTimeline(driver, now, freshness)
                val planCount = driver.prepare("SELECT COUNT(*) AS c FROM change_plans").get()?.long("c") ?: 0
                Json.Obj(
                    listOf(
                        "buckets" to Json.Arr(items.map { Json.Str(it.bucket) }),
                        "ids" to Json.Arr(items.map { Json.Str(it.id) }),
                        "kinds" to Json.Arr(items.map { Json.Str(it.kind) }),
                        "deterministic" to Json.Bool(items == again),
                        "planCountUnchanged" to Json.Num(planCount.toString()),
                    ),
                )
            }
            "timeline-attention-signals" -> {
                insertNode(driver, "n-card", "payment_instrument", "招行 4417", """{"expiryDate":"2026-10-05"}""")
                insertNode(driver, "n-wechat", "account", "微信支付")
                driver.prepare(
                    """
                    INSERT INTO reality_drifts (id, kind, target_node_id, capability, candidate_from, candidate_relation, related_dependency_ids_json, evidence_refs_json, proposal_keys_json, observation_count, detected_at, updated_at, status)
                    VALUES ('drift-1', 'possible_replacement', 'n-wechat', 'payment', 'n-card', 'funding_source', '[]', '[]', '[]', 2, ?, ?, 'open')
                    """.trimIndent(),
                ).run(T0, T0)
                driver.prepare(
                    """
                    INSERT INTO source_instances (id, adapter_id, adapter_version, source_kind, provider_id, account_node_id, label, country, jurisdiction, currencies_json, state, created_at, updated_at, last_ingested_at)
                    VALUES ('inst-stale', 'generic_csv', 1, 'statement_file', NULL, NULL, '陈旧来源', NULL, NULL, '["CNY"]', 'active', ?, ?, '2026-01-01T00:00:00.000Z')
                    """.trimIndent(),
                ).run(T0, T0)
                insertPlan(driver, "plan-stale", "落后计划", null, 0)
                val items = buildTimeline(driver, now, freshness)
                Json.Obj(
                    listOf(
                        "kinds" to Json.Arr(items.map { Json.Str(it.kind) }),
                        "buckets" to Json.Arr(items.map { Json.Str(it.bucket) }),
                        "sourceTypes" to Json.Arr(items.map { Json.Str(it.sourceType) }),
                        "priorities" to Json.Arr(items.map { Json.Num(it.priority.toString()) }),
                        "statuses" to Json.Arr(items.map { Json.Str(it.status) }),
                        "allTraceable" to Json.Bool(items.all { it.sourceId.isNotEmpty() }),
                    ),
                )
            }
            "timeline-terminal-plans-excluded" -> {
                insertNode(driver, "n-card", "payment_instrument", "招行 4417")
                insertPlan(driver, "plan-done", "已完成", "2026-09-20", 5, "completed")
                insertPlan(driver, "plan-cancel", "已取消", "2026-09-21", 5, "cancelled")
                val items = buildTimeline(driver, now, freshness)
                Json.Obj(
                    listOf(
                        "count" to Json.Num(items.size.toString()),
                        "kinds" to Json.Arr(items.map { Json.Str(it.kind) }),
                    ),
                )
            }
            else -> throw NotImplementedError("unknown timeline fixture: $id")
        }
    }
}

// ---------------------------------------------------------------------------
// migration v1 → v3（旧库数据保全 + 幂等）
// ---------------------------------------------------------------------------

internal fun runMigrationV1ToV3(): Json = withTempDb("mig") { driver ->
    // 手工构造 v1 库（不用 migrate，直接执行 v1 DDL）
    for (sql in SCHEMA_V1_STATEMENTS) driver.exec(sql)
    driver.prepare("INSERT INTO meta (key, value) VALUES ('schema_version', '1')").run()
    driver.prepare(
        """
        INSERT INTO nodes (id, kind, name, owner, archived, fields_json, created_at, updated_at)
        VALUES ('node-1', 'payment_instrument', '招行 4417', 'self', 0, '{}', ?, ?)
        """.trimIndent(),
    ).run(T0, T0)
    driver.prepare(
        """
        INSERT INTO dependencies (id, from_node, relation, to_node, capability, criticality, state, origin, confirmed_at, last_verified_at, evidence_refs_json, created_at, updated_at)
        VALUES ('dep-1', 'node-1', 'funding_source', 'node-1', 'payment', 'required', 'active', 'manual', ?, ?, '[]', ?, ?)
        """.trimIndent(),
    ).run(T0, T0, T0, T0)
    driver.prepare(
        """
        INSERT INTO evidence (id, proposal_key, source_type, parser_id, parser_version, last_import_session_id, first_observed_at, last_observed_at, observation_count, created_at, updated_at)
        VALUES ('ev-1', 'k1', 'wechat_bill', 'wechat', 1, 'sess-1', ?, ?, 3, ?, ?)
        """.trimIndent(),
    ).run(T0, T0, T0, T0)
    driver.prepare(
        """
        INSERT INTO observation_fingerprints (fingerprint, source, fingerprint_version, import_session_id, first_seen_at)
        VALUES ('fp-abc', 'wechat', 1, 'sess-1', ?)
        """.trimIndent(),
    ).run(T0)
    driver.prepare(
        """
        INSERT INTO dependency_proposals (id, key, from_node, relation, to_node, capability, proposal_type, source, parser_id, parser_version, confidence_score, path_json, evidence_id, decision, observation_count, created_at, updated_at)
        VALUES ('prop-1', 'k1', 'node-1', 'funding_source', 'node-1', 'payment', 'recurring_payment_route', 'statement', 'wechat', 1, 0.9, '[]', 'ev-1', 'pending', 3, ?, ?)
        """.trimIndent(),
    ).run(T0, T0)
    driver.prepare(
        """
        INSERT INTO import_sessions (id, source_type, parser_id, parser_version, started_at, completed_at, raw_count, new_unique_count, duplicate_count, proposal_count, error_count)
        VALUES ('sess-1', 'wechat_bill', 'wechat', 1, ?, ?, 6, 6, 0, 1, 0)
        """.trimIndent(),
    ).run(T0, T0)

    val finalVersion = migrate(driver, T0)
    val tables = driver.prepare(
        "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name",
    ).all().map { it.str("name") ?: "" }
    val dep = driver.prepare("SELECT id, state, criticality FROM dependencies WHERE id = ?").get("dep-1")
    val node = driver.prepare("SELECT id, name FROM nodes WHERE id = ?").get("node-1")
    val ev = driver.prepare(
        "SELECT id, proposal_key, source_instance_id, evidence_kind FROM evidence WHERE id = ?",
    ).get("ev-1")
    val fp = driver.prepare(
        "SELECT fingerprint, source_instance_id FROM observation_fingerprints WHERE fingerprint = ?",
    ).get("fp-abc")
    val prop = driver.prepare("SELECT id, decision FROM dependency_proposals WHERE id = ?").get("prop-1")
    val legacyCount = driver.prepare(
        "SELECT COUNT(*) AS c FROM source_instances WHERE id = ?",
    ).get(LEGACY_WECHAT_SOURCE_INSTANCE_ID)?.long("c") ?: 0

    // 幂等：重复 migrate 50 次不漂移
    var idempotent = true
    repeat(50) { if (migrate(driver, T0) != finalVersion) idempotent = false }

    Json.Obj(
        listOf(
            "finalSchemaVersion" to Json.Num(finalVersion.toString()),
            "tables" to Json.Arr(tables.map { Json.Str(it) }),
            "legacySourceInstanceId" to Json.Str(LEGACY_WECHAT_SOURCE_INSTANCE_ID),
            "legacySourceInstanceRows" to Json.Num(legacyCount.toString()),
            "preserved" to Json.Obj(
                listOf(
                    "node" to Json.Obj(
                        listOf(
                            "id" to Json.Str(node?.str("id") ?: ""),
                            "name" to Json.Str(node?.str("name") ?: ""),
                        ),
                    ),
                    "dependency" to Json.Obj(
                        listOf(
                            "id" to Json.Str(dep?.str("id") ?: ""),
                            "state" to Json.Str(dep?.str("state") ?: ""),
                            "criticality" to Json.Str(dep?.str("criticality") ?: ""),
                        ),
                    ),
                    "evidence" to Json.Obj(
                        listOf(
                            "id" to Json.Str(ev?.str("id") ?: ""),
                            "proposal_key" to Json.Str(ev?.str("proposal_key") ?: ""),
                            "source_instance_id" to Json.Str(ev?.str("source_instance_id") ?: ""),
                            "evidence_kind" to Json.Str(ev?.str("evidence_kind") ?: ""),
                        ),
                    ),
                    "fingerprint" to Json.Obj(
                        listOf(
                            "fingerprint" to Json.Str(fp?.str("fingerprint") ?: ""),
                            "source_instance_id" to Json.Str(fp?.str("source_instance_id") ?: ""),
                        ),
                    ),
                    "proposal" to Json.Obj(
                        listOf(
                            "id" to Json.Str(prop?.str("id") ?: ""),
                            "decision" to Json.Str(prop?.str("decision") ?: ""),
                        ),
                    ),
                ),
            ),
            "idempotentAfter50" to Json.Bool(idempotent),
        ),
    )
}