package com.pdig.conformance

import com.pdig.core.crypto.DepmapContainer
import com.pdig.core.crypto.Jcs
import com.pdig.core.domain.CoverageSourceInfo
import com.pdig.core.domain.Dependency
import com.pdig.core.domain.DependencyGroup
import com.pdig.core.domain.ChangePlan
import com.pdig.core.domain.ImpactGraph
import com.pdig.core.domain.ImpactProposalInput
import com.pdig.core.domain.ImpactStateKey
import com.pdig.core.domain.PlanAction
import com.pdig.core.domain.PlanReadinessInput
import com.pdig.core.domain.ScenarioCoverageInput
import com.pdig.core.domain.validateRelationGroupUse
import com.pdig.core.domain.validateRelationUse
import com.pdig.core.db.SqliteDriver
import com.pdig.core.schema.LEGACY_WECHAT_SOURCE_INSTANCE_ID
import com.pdig.core.schema.SCHEMA_V1_STATEMENTS
import com.pdig.core.schema.migrate
import com.pdig.core.timeline.buildTimeline
import com.pdig.core.serialize.checkGraphIntegrity
import com.pdig.core.serialize.exportGraph
import com.pdig.core.serialize.importGraph
import com.pdig.core.generated.ActionVerificationStatus
import com.pdig.core.generated.CandidateStatus
import com.pdig.core.generated.Capability
import com.pdig.core.generated.ChangePlanWorkflowState
import com.pdig.core.generated.Criticality
import com.pdig.core.generated.DependencyOrigin
import com.pdig.core.generated.DependencyState
import com.pdig.core.generated.DriftStatus
import com.pdig.core.generated.GroupMode
import com.pdig.core.generated.GroupState
import com.pdig.core.generated.NodeKind
import com.pdig.core.generated.PlanActionPhase
import com.pdig.core.generated.Relation
import com.pdig.core.impact.ImpactResult
import com.pdig.core.impact.simulateScenario
import com.pdig.core.json.Json
import com.pdig.core.json.JsonParser
import com.pdig.core.json.JsonWriter
import com.pdig.core.plan.computePlanReadiness
import com.pdig.core.plan.computeScenarioCoverage
import com.pdig.core.scenario.ScenarioRegistry
import com.pdig.core.schema.SchemaVersion
import com.pdig.core.statemachine.CandidateMachine
import com.pdig.core.sources.GenericCsvParser
import com.pdig.core.sources.MappingColumns
import com.pdig.core.sources.MappingOptions
import com.pdig.core.sources.MappingProfile
import com.pdig.core.sources.ObservationDirection
import com.pdig.core.sources.OfxParser
import com.pdig.core.sources.ParseResult
import com.pdig.core.sources.WechatParser
import com.pdig.core.statemachine.ChangePlanMachine
import com.pdig.core.statemachine.DriftMachine
import com.pdig.core.statemachine.GraphRevisionMachine
import com.pdig.core.statemachine.VerificationMachine
import java.io.File

/**
 * PDIG Android Conformance Runner。
 *
 * 诚实口径：未实现的用例报 NOT_IMPLEMENTED，**绝不自称 PASS**。
 * 每条结果都附带 actual，便于 harness 独立复核。
 */

private const val PLATFORM = "android"

/** 仓库根（conformance fixture / import 原始文件的基准目录）。 */
private lateinit var ROOT: File

fun main(args: Array<String>) {
    val root = if (args.isNotEmpty()) File(args[0]) else File("../../").canonicalFile
    ROOT = root
    println("PDIG Android conformance runner")
    println("repo root: ${root.absolutePath}")

    val manifestFile = File(root, "conformance/CONFORMANCE_MANIFEST.json")
    if (!manifestFile.exists()) {
        System.err.println("FATAL: ${manifestFile.absolutePath} not found")
        kotlin.system.exitProcess(2)
    }
    val manifest = JsonParser.parse(manifestFile.readText()) as Json.Obj
    val fixtures = manifest.require("fixtures") as Json.Arr

    val results = linkedMapOf<String, Json>()
    var pass = 0
    var fail = 0
    var notImplemented = 0

    for (item in fixtures.items) {
        val entry = item as Json.Obj
        val id = (entry.require("id") as Json.Str).value
        val category = (entry.require("category") as Json.Str).value
        val path = (entry.require("path") as Json.Str).value
        val file = File(root, path)
        val case = JsonParser.parse(file.readText()) as Json.Obj
        val input = case.require("input")
        val expected = case.require("expected")

        val outcome: Pair<String, Json?> = try {
            val actual = compute(category, id, input)
            val a = JsonWriter.write(actual)
            val b = JsonWriter.write(expected)
            if (a == b) "PASS" to actual else "FAIL" to actual
        } catch (e: NotImplementedError) {
            "NOT_IMPLEMENTED" to null
        } catch (e: Throwable) {
            "FAIL" to Json.Obj(
                listOf("error" to Json.Str(e::class.simpleName ?: "error"), "message" to Json.Str(e.message ?: "")),
            )
        }

        when (outcome.first) {
            "PASS" -> pass++
            "FAIL" -> {
                fail++
                System.err.println("FAIL $id")
                System.err.println("  expected: ${JsonWriter.write(expected)}")
                val actual = outcome.second
                if (actual != null) System.err.println("  actual  : ${JsonWriter.write(actual)}")
            }
            else -> notImplemented++
        }

        results[id] = Json.Obj(
            listOfNotNull(
                "status" to Json.Str(outcome.first),
                "category" to Json.Str(category),
                outcome.second?.let { "actual" to it },
            ),
        )
    }

    val report = Json.Obj(
        listOf(
            "platform" to Json.Str(PLATFORM),
            "specVersion" to Json.Str("1.0.0"),
            "generatedAt" to Json.Str(java.time.Instant.now().toString()),
            "summary" to Json.Obj(
                listOf(
                    "pass" to Json.Num(pass.toString()),
                    "fail" to Json.Num(fail.toString()),
                    "notImplemented" to Json.Num(notImplemented.toString()),
                    "total" to Json.Num(results.size.toString()),
                ),
            ),
            "results" to Json.Obj(results.map { it.key to it.value }),
        ),
    )

    val outDir = File(root, "conformance/reports")
    outDir.mkdirs()
    val outFile = File(outDir, "android.json")
    outFile.writeText(JsonWriter.write(report) + "\n")
    println("pass=$pass fail=$fail notImplemented=$notImplemented total=${results.size}")
    println("report: ${outFile.absolutePath}")
    if (fail > 0) kotlin.system.exitProcess(1)
}

// ---------------------------------------------------------------------------
// 分发
// ---------------------------------------------------------------------------

private fun compute(category: String, id: String, input: Json): Json = when (category) {
    "impact" -> runImpact(input as Json.Obj)
    "readiness" -> runReadiness(input as Json.Obj)
    "coverage" -> runCoverage(input as Json.Obj)
    "relations" -> runRelations(id, input as Json.Obj)
    "depmap" -> runDepmap(id, input as Json.Obj)
    "jcs" -> runJcs(input as Json.Obj)
    "scenario" -> runScenario()
    "state-machine" -> runStateMachine(id)
    "parser" -> runParser(input as Json.Obj)
    "timeline" -> runTimeline(id, input as Json.Obj)
    "migration" -> when (id) {
        "migration-db-v1-to-v3" -> runMigrationV1ToV3()
        else -> runMigration()
    }
    "backup" -> runBackup(input as Json.Obj)
    else -> throw NotImplementedError("no runner for category $category")
}

// ---------------------------------------------------------------------------
// backup —— .depmap 导出 / 恢复往返（Cutover 核心要求）
// ---------------------------------------------------------------------------

private fun runBackup(input: Json.Obj): Json {
    val password = str(input, "password")
    val salt = hexToBytes(str(input, "saltHex"))
    val nonce = hexToBytes(str(input, "nonceHex"))

    // 源库
    val srcResult = withTempDb("bak") { driver ->
        migrate(driver, T0)
        for (nodeId in arr(input, "seededNodes")) {
            val id = (nodeId as Json.Str).value
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
// timeline / migration（临时 DB，跑完即删）
// ---------------------------------------------------------------------------

/** 与 core/scripts/generate-conformance.ts 的 T0 保持一致。 */
private const val T0 = "2026-09-13T00:00:00.000Z"

private fun insertNode(driver: JdbcSqliteDriver, id: String, kind: String, name: String, fields: String = "{}") {
    driver.prepare(
        """
        INSERT INTO nodes (id, kind, template_id, name, issuer, last4, owner, archived, fields_json, vault_ref, wallet_ref, created_at, updated_at)
        VALUES (?, ?, NULL, ?, NULL, NULL, 'self', 0, ?, NULL, NULL, ?, ?)
        """.trimIndent(),
    ).run(id, kind, name, fields, T0, T0)
}

private fun insertPlan(
    driver: JdbcSqliteDriver,
    id: String,
    title: String,
    effectiveDate: String?,
    lastAnalyzed: Int,
    workflowState: String = "analyzed",
) {
    driver.prepare(
        """
        INSERT INTO change_plans (id, template_id, scenario, title, workflow_state, baseline_graph_revision, last_analyzed_graph_revision, target_node_id, effective_date, params_json, impact_snapshot_json, action_items_json, created_at, updated_at)
        VALUES (?, 'replace_payment_card', 'replace_payment_card', ?, ?, 0, ?, NULL, ?, '{}', NULL, '[]', ?, ?)
        """.trimIndent(),
    ).run(id, title, workflowState, lastAnalyzed, effectiveDate, T0, T0)
}

private fun <T> withTempDb(tag: String, fn: (JdbcSqliteDriver) -> T): T {
    val dir = java.nio.file.Files.createTempDirectory("pdig-cf-$tag").toFile()
    val driver = JdbcSqliteDriver(File(dir, "fx.db"))
    return try {
        fn(driver)
    } finally {
        driver.close()
        dir.deleteRecursively()
    }
}

private fun runTimeline(id: String, input: Json.Obj): Json {
    val now = str(input, "now")
    val freshness = (input["freshnessThresholdDays"] as? Json.Num)?.asLong()?.toInt() ?: 45
    return withTempDb("tl") { driver ->
        migrate(driver, T0)
        when (id) {
            // 镜像 core/scripts/generate-conformance.ts §12 的三种种子状态
            "timeline-buckets-and-ordering" -> {
                insertNode(driver, "n-card", "payment_instrument", "招行 4417")
                val dates = arr(input, "plans").map { (it as Json.Str).value }
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

private fun runMigrationV1ToV3(): Json = withTempDb("mig") { driver ->
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

// ---------------------------------------------------------------------------
// parser
// ---------------------------------------------------------------------------

private fun runParser(input: Json.Obj): Json {
    val adapter = str(input, "adapterId")
    val fileName = str(input, "file")
    val file = File(ROOT, "fixtures/import/$fileName")
    if (!file.exists()) error("import fixture not found: ${file.absolutePath}")
    val data = file.readBytes()
    val result: ParseResult = when (adapter) {
        "wechat" -> WechatParser.parse(data)
        "ofx_qfx" -> OfxParser.parse(data)
        "generic_csv" -> GenericCsvParser.parse(data, parseMapping(input["mapping"]))
        else -> error("unknown adapter: $adapter")
    }
    return Json.Obj(
        listOf(
            "observationCount" to Json.Num(result.observations.size.toString()),
            "observations" to Json.Arr(
                result.observations.map {
                    Json.Obj(
                        listOf(
                            "occurredAt" to Json.Str(it.occurredAt),
                            "amount" to num(it.amount),
                            "currency" to Json.Str(it.currency),
                            "direction" to Json.Str(it.direction.wire),
                            "merchantRaw" to Json.Str(it.merchantRaw),
                            "status" to Json.Str(it.status),
                        ),
                    )
                },
            ),
            "errorCount" to Json.Num(result.errors.size.toString()),
            "errors" to Json.Arr(
                result.errors.map {
                    Json.Obj(
                        listOf(
                            "line" to Json.Num(it.line.toString()),
                            "reason" to Json.Str(it.reason),
                        ),
                    )
                },
            ),
        ),
    )
}

private fun parseMapping(v: Json?): MappingProfile? {
    if (v == null || v is Json.Null) return null
    val o = v as Json.Obj
    val cols = o.require("columns") as Json.Obj
    val opts = o.require("options") as Json.Obj
    fun col(key: String): String? = (cols[key] as? Json.Str)?.value
    return MappingProfile(
        columns = MappingColumns(
            transactionId = col("transactionId"),
            dateTime = col("dateTime") ?: error("mapping.columns.dateTime required"),
            amount = col("amount"),
            debit = col("debit"),
            credit = col("credit"),
            description = col("description"),
            counterparty = col("counterparty"),
            currency = col("currency"),
            balance = col("balance"),
            transactionType = col("transactionType"),
            paymentMethod = col("paymentMethod"),
        ),
        options = MappingOptions(
            delimiter = (opts["delimiter"] as? Json.Str)?.value ?: ",",
            dateFormats = ((opts["dateFormats"] as? Json.Arr)?.items ?: emptyList()).map { (it as Json.Str).value },
            decimalSeparator = ((opts["decimalSeparator"] as? Json.Str)?.value ?: ".").first(),
            amountSignMode = (opts["amountSignMode"] as? Json.Str)?.value ?: "outward_positive",
            hasHeaderRow = (opts["hasHeaderRow"] as? Json.Bool)?.value ?: true,
            encoding = (opts["encoding"] as? Json.Str)?.value ?: "utf-8",
            positiveDirection = (opts["positiveDirection"] as? Json.Str)?.value,
        ),
    )
}

/** JS `JSON.stringify(number)` 等价：整数不输出小数点，否则输出最短往返表示。 */
private fun num(d: Double): Json.Num {
    val s = if (d == kotlin.math.floor(d) && kotlin.math.abs(d) < 1e15) {
        d.toLong().toString()
    } else {
        d.toString()
    }
    return Json.Num(s)
}

/** 尚未实现：报 NOT_IMPLEMENTED，绝不自称 PASS。 */
private fun notYet(id: String): Nothing =
    throw NotImplementedError("$id requires persistence/parser implementation not yet ported")

// ---------------------------------------------------------------------------
// impact
// ---------------------------------------------------------------------------

private fun runImpact(input: Json.Obj): Json {
    val graphObj = input.require("graph") as Json.Obj
    val nodeNames = (graphObj["nodeNames"] as? Json.Obj)?.fields?.associate { (k, v) ->
        k to (v as Json.Str).value
    } ?: emptyMap()

    val deps = arr(graphObj, "dependencies").map { parseDependency(it as Json.Obj) }
    val groups = (graphObj["groups"] as? Json.Arr)?.items?.map { parseGroup(it as Json.Obj) } ?: emptyList()
    val proposals = (graphObj["proposals"] as? Json.Arr)?.items?.map {
        val o = it as Json.Obj
        ImpactProposalInput(
            key = str(o, "key"),
            from = str(o, "from"),
            to = str(o, "to"),
            capability = cap(o, "capability"),
            confidenceScore = (o["confidenceScore"] as? Json.Num)?.asDouble(),
        )
    } ?: emptyList()

    val unavailable = arr(input, "unavailable").map {
        val o = it as Json.Obj
        ImpactStateKey(nodeId = str(o, "nodeId"), capability = cap(o, "capability"))
    }

    val graph = ImpactGraph(deps, groups, proposals, nodeNames)
    val result = simulateScenario(graph, unavailable)
    return serializeImpact(result)
}

private fun parseDependency(o: Json.Obj): Dependency = Dependency(
    id = str(o, "id"),
    from = str(o, "from"),
    relation = Relation.fromWire(str(o, "relation")) ?: error("unknown relation"),
    to = str(o, "to"),
    capability = cap(o, "capability"),
    criticality = Criticality.fromWire(str(o, "criticality")) ?: error("unknown criticality"),
    groupId = (o["groupId"] as? Json.Str)?.value,
    state = DependencyState.fromWire(str(o, "state")) ?: error("unknown state"),
    origin = DependencyOrigin.fromWire(str(o, "origin")) ?: DependencyOrigin.MANUAL,
    lastVerifiedAt = str(o, "lastVerifiedAt"),
)

private fun parseGroup(o: Json.Obj): DependencyGroup = DependencyGroup(
    id = str(o, "id"),
    groupKey = str(o, "groupKey"),
    targetNodeId = str(o, "targetNodeId"),
    capability = cap(o, "capability"),
    mode = GroupMode.fromWire(str(o, "mode")) ?: error("unknown group mode"),
    memberEdgeIds = (o.require("memberEdgeIds") as Json.Arr).items.map { (it as Json.Str).value },
    state = GroupState.fromWire(str(o, "state")) ?: GroupState.ACTIVE,
)

private fun serializeImpact(r: ImpactResult): Json = Json.Obj(
    listOf(
        "unavailable" to Json.Arr(r.unavailable.map { stateKey(it) }),
        "lostKeys" to Json.Arr(r.lostKeys.map { stateKey(it) }),
        "targets" to Json.Arr(
            r.targets.map { t ->
                Json.Obj(
                    listOf(
                        "nodeId" to Json.Str(t.nodeId),
                        "nodeName" to Json.Str(t.nodeName),
                        "capability" to Json.Str(t.capability.wire),
                        "depth" to Json.Num(t.depth.toString()),
                        "status" to Json.Str(t.status.wire),
                        "available" to Json.Bool(t.available),
                        "redundancyDegraded" to Json.Bool(t.redundancyDegraded),
                        "reasonCode" to Json.Str(t.reasonCode.wire),
                        "reasonText" to Json.Str(t.reasonText),
                        "edgeKeys" to Json.Arr(t.edgeKeys.map { Json.Str(it) }),
                        "groupKeys" to Json.Arr(t.groupKeys.map { Json.Str(it) }),
                        "proposalKeys" to Json.Arr(t.proposalKeys.map { Json.Str(it) }),
                    ),
                )
            },
        ),
        "checklist" to Json.Arr(
            r.checklist.map { c ->
                Json.Obj(
                    listOfNotNull(
                        "level" to Json.Str(c.level.wire),
                        "nodeId" to (c.nodeId?.let { Json.Str(it) } ?: Json.Null),
                        "capability" to (c.capability?.let { Json.Str(it.wire) } ?: Json.Null),
                        "title" to Json.Str(c.title),
                        "detail" to Json.Str(c.detail),
                    ),
                )
            },
        ),
        "processedKeys" to Json.Arr(r.processedKeys.map { Json.Str(it) }),
    ),
)

private fun stateKey(k: ImpactStateKey): Json = Json.Obj(
    listOf("nodeId" to Json.Str(k.nodeId), "capability" to Json.Str(k.capability.wire)),
)

// ---------------------------------------------------------------------------
// readiness
// ---------------------------------------------------------------------------

private fun runReadiness(input: Json.Obj): Json {
    val planObj = input.require("plan") as Json.Obj
    val actions = arr(planObj, "actions").map { a ->
        val o = a as Json.Obj
        PlanAction(
            id = str(o, "id"),
            title = (o["title"] as? Json.Str)?.value ?: "",
            phase = PlanActionPhase.fromWire(str(o, "phase")) ?: error("unknown phase"),
            done = (o["done"] as? Json.Bool)?.value ?: false,
            resolvesImpactKeys = (o["resolvesImpactKeys"] as? Json.Arr)?.items?.map { (it as Json.Str).value }
                ?: emptyList(),
        )
    }
    val plan = ChangePlan(
        id = str(planObj, "id"),
        templateId = (planObj["templateId"] as? Json.Str)?.value,
        scenario = (planObj["scenario"] as? Json.Str)?.value ?: "",
        title = (planObj["title"] as? Json.Str)?.value ?: "",
        workflowState = ChangePlanWorkflowState.fromWire(str(planObj, "workflowState"))
            ?: error("unknown workflow state"),
        baselineGraphRevision = (planObj.require("baselineGraphRevision") as Json.Num).asLong().toInt(),
        lastAnalyzedGraphRevision = (planObj.require("lastAnalyzedGraphRevision") as Json.Num).asLong().toInt(),
        targetNodeId = (planObj["targetNodeId"] as? Json.Str)?.value,
        effectiveDate = (planObj["effectiveDate"] as? Json.Str)?.value,
        actions = actions,
    )
    val readinessInput = PlanReadinessInput(
        plan = plan,
        currentGraphRevision = (input.require("currentGraphRevision") as Json.Num).asLong().toInt(),
        pendingMustChange = (input.require("pendingMustChange") as Json.Num).asLong().toInt(),
        pendingNeedsReview = (input.require("pendingNeedsReview") as Json.Num).asLong().toInt(),
        unresolvedCandidates = (input.require("unresolvedCandidates") as Json.Num).asLong().toInt(),
        pendingRelevantProposals = (input.require("pendingRelevantProposals") as Json.Num).asLong().toInt(),
        staleRelevantDependencies = (input.require("staleRelevantDependencies") as Json.Num).asLong().toInt(),
        unfinishedChangeActions = (input.require("unfinishedChangeActions") as Json.Num).asLong().toInt(),
    )
    return Json.Str(computePlanReadiness(readinessInput).wire)
}

// ---------------------------------------------------------------------------
// coverage
// ---------------------------------------------------------------------------

private fun runCoverage(input: Json.Obj): Json {
    val sources = arr(input, "sources").map { s ->
        val o = s as Json.Obj
        CoverageSourceInfo(
            id = str(o, "id"),
            label = str(o, "label"),
            lastIngestedAt = (o["lastIngestedAt"] as? Json.Str)?.value,
        )
    }
    val cov = computeScenarioCoverage(
        ScenarioCoverageInput(
            scenarioId = str(input, "scenarioId"),
            sources = sources,
            confirmedDirectDependencies = int(input, "confirmedDirectDependencies"),
            confirmedIndirectDependencies = int(input, "confirmedIndirectDependencies"),
            pendingProposals = int(input, "pendingProposals"),
            unresolvedCandidates = int(input, "unresolvedCandidates"),
            staleDependencies = int(input, "staleDependencies"),
            unknownCriticalityCount = int(input, "unknownCriticalityCount"),
            unverifiedActions = int(input, "unverifiedActions"),
            freshnessThresholdDays = int(input, "freshnessThresholdDays"),
            now = str(input, "now"),
        ),
    )
    return Json.Obj(
        listOf(
            "scenarioId" to Json.Str(cov.scenarioId),
            "coverageLevel" to Json.Str(cov.coverageLevel.wire),
            "explanations" to Json.Arr(cov.explanations.map { Json.Str(it) }),
            "counts" to Json.Obj(
                listOf(
                    "confirmedDirectDependencies" to Json.Num(cov.counts.confirmedDirectDependencies.toString()),
                    "confirmedIndirectDependencies" to Json.Num(cov.counts.confirmedIndirectDependencies.toString()),
                    "pendingProposals" to Json.Num(cov.counts.pendingProposals.toString()),
                    "unresolvedCandidates" to Json.Num(cov.counts.unresolvedCandidates.toString()),
                    "staleDependencies" to Json.Num(cov.counts.staleDependencies.toString()),
                    "unknownCriticalityCount" to Json.Num(cov.counts.unknownCriticalityCount.toString()),
                    "unverifiedActions" to Json.Num(cov.counts.unverifiedActions.toString()),
                ),
            ),
        ),
    )
}

// ---------------------------------------------------------------------------
// relations
// ---------------------------------------------------------------------------

private fun runRelations(id: String, input: Json.Obj): Json {
    val result = if (id.startsWith("relation-group-")) {
        validateRelationGroupUse(
            relation = str(input, "relation"),
            mode = GroupMode.fromWire(str(input, "mode")) ?: error("unknown mode"),
        )
    } else {
        validateRelationUse(
            fromKind = (input["fromKind"] as? Json.Str)?.value?.let { NodeKind.fromWire(it) },
            relation = str(input, "relation"),
            toKind = (input["toKind"] as? Json.Str)?.value?.let { NodeKind.fromWire(it) },
            capability = str(input, "capability"),
        )
    }
    return Json.Obj(
        listOfNotNull(
            "ok" to Json.Bool(result.ok),
            result.reason?.let { "reason" to Json.Str(it) },
        ),
    )
}

// ---------------------------------------------------------------------------
// depmap
// ---------------------------------------------------------------------------

private fun runDepmap(id: String, input: Json.Obj): Json = when (id) {
    "depmap-golden-v1" -> {
        val salt = hexToBytes(str(input, "saltHex"))
        val nonce = hexToBytes(str(input, "nonceHex"))
        val plaintext = str(input, "plaintext").toByteArray(Charsets.UTF_8)
        val kdf = input.require("kdf") as Json.Obj
        val created = DepmapContainer.create(
            plaintext,
            str(input, "password"),
            DepmapContainer.CreateOptions(
                salt = salt,
                nonce = nonce,
                memoryKiB = int(kdf, "memoryKiB"),
                iterations = int(kdf, "iterations"),
                parallelism = int(kdf, "parallelism"),
            ),
        )
        val reopened = String(DepmapContainer.openContainer(created.json, str(input, "password")), Charsets.UTF_8)
        val wrongPassword = try {
            DepmapContainer.openContainer(created.json, "wrong-password"); "opened"
        } catch (e: DepmapContainer.DepmapException) {
            e.code
        }
        val tampered = created.json.replace(
            Regex("\"ciphertext\":\"[^\"]+\""),
            "\"ciphertext\":\"AAAAAAAAAAAAAAAAAAAAAA==\"",
        )
        val tamperOutcome = try {
            DepmapContainer.openContainer(tampered, str(input, "password")); "opened"
        } catch (e: DepmapContainer.DepmapException) {
            e.code
        }
        Json.Obj(
            listOf(
                "derivedKeyHex" to Json.Str(created.derivedKeyHex),
                "ciphertextBase64" to Json.Str(created.header.ciphertextB64),
                "tagBase64" to Json.Str(created.header.tagB64),
                "containerJson" to Json.Str(created.json),
                "reopenedPlaintext" to Json.Str(reopened),
                "wrongPasswordOutcome" to Json.Str(wrongPassword),
                "tamperedCiphertextOutcome" to Json.Str(tamperOutcome),
            ),
        )
    }

    "depmap-utf8-password-normalization" -> {
        val salt = hexToBytes("00112233445566778899aabbccddeeff")
        val nonce = hexToBytes("a1b2c3d4e5f60718293a4b5c")
        val plaintext = str(input, "plaintext").toByteArray(Charsets.UTF_8)
        val cases = arr(input, "cases")
        val entries = cases.map { c ->
            val o = c as Json.Obj
            val caseId = str(o, "id")
            val derived = DepmapContainer.create(
                plaintext,
                str(o, "password"),
                DepmapContainer.CreateOptions(salt = salt, nonce = nonce),
            ).derivedKeyHex
            caseId to Json.Str(derived)
        }
        val combining = (entries.first { it.first == "combining" }.second as Json.Str).value
        val nfc = (entries.first { it.first == "nfc" }.second as Json.Str).value
        Json.Obj(
            listOf(
                "derivedKeyHexByCase" to Json.Obj(entries),
                "combiningDiffersFromNfc" to Json.Bool(combining != nfc),
            ),
        )
    }

    "depmap-bounds-and-structure-rejection" -> {
        val mutations = arr(input, "mutations")
        Json.Obj(
            mutations.map { m ->
                val o = m as Json.Obj
                val caseId = str(o, "id")
                val json = str(o, "json")
                val code = try {
                    DepmapContainer.openContainer(json, "depmap-test"); "opened"
                } catch (e: DepmapContainer.DepmapException) {
                    e.code
                }
                caseId to Json.Str(code)
            },
        )
    }

    else -> throw NotImplementedError("no depmap runner for $id")
}

// ---------------------------------------------------------------------------
// jcs
// ---------------------------------------------------------------------------

private fun runJcs(input: Json.Obj): Json {
    val canonical = arr(input, "cases").map { c ->
        val o = c as Json.Obj
        val caseId = str(o, "id")
        caseId to Json.Str(Jcs.stringify(o.require("input")))
    }
    val rejections = arr(input, "rejectCases").map { c ->
        val o = c as Json.Obj
        val caseId = str(o, "id")
        val outcome = try {
            Jcs.stringify(o.require("input")); "serialized"
        } catch (e: Jcs.JcsError) {
            "Error"
        }
        caseId to Json.Str(outcome)
    }
    return Json.Obj(
        listOf(
            "canonical" to Json.Obj(canonical),
            "rejections" to Json.Obj(rejections),
        ),
    )
}

// ---------------------------------------------------------------------------
// scenario / migration（产品配置与版本契约）
// ---------------------------------------------------------------------------

private fun runScenario(): Json = Json.Obj(
    listOf(
        "activeIds" to Json.Arr(ScenarioRegistry.active.map { Json.Str(it.id) }),
        "activeCategories" to Json.Arr(ScenarioRegistry.active.map { Json.Str(it.category) }),
        "plannedIds" to Json.Arr(ScenarioRegistry.planned.map { Json.Str(it.id) }),
        "plannedExecutable" to Json.Arr(
            ScenarioRegistry.planned.map { Json.Bool(it.availability == "active") },
        ),
        "leadingTimeByTemplate" to Json.Obj(
            ScenarioRegistry.active.map {
                it.id to (it.recommendedLeadTimeDays?.let { d -> Json.Num(d.toString()) } ?: Json.Null)
            },
        ),
    ),
)

private fun runMigration(): Json = Json.Obj(
    listOf(
        "migrations" to Json.Arr(SchemaVersion.migrations.map { Json.Num(it.toString()) }),
        "payloadKind" to Json.Str(SchemaVersion.payloadKind),
        "payloadTables" to Json.Arr(SchemaVersion.payloadTables.map { Json.Str(it) }),
        "rejectedSchemaVersions" to Json.Arr(SchemaVersion.rejectedSchemaVersions.map { Json.Num(it.toString()) }),
        "rejectedPayloadVersions" to Json.Arr(SchemaVersion.rejectedPayloadVersions.map { Json.Num(it.toString()) }),
        "graphRevisionNeverBumpedBy" to Json.Arr(SchemaVersion.graphRevisionNeverBumpedBy.map { Json.Str(it) }),
    ),
)

// ---------------------------------------------------------------------------
// state machines（Kotlin 权威定义 → 与 spec 比对）
// ---------------------------------------------------------------------------

private fun runStateMachine(id: String): Json = when (id) {
    "state-machine-change-plan" -> Json.Obj(
        listOf(
            "transitions" to Json.Obj(
                ChangePlanMachine.transitions.map { (from, tos) ->
                    from.wire to Json.Arr(tos.map { Json.Str(it.wire) })
                },
            ),
            "terminal" to Json.Arr(ChangePlanMachine.terminal.map { Json.Str(it.wire) }),
            "derivedStatus" to Json.Obj(
                listOf(
                    "name" to Json.Str(ChangePlanMachine.derivedStatus.name),
                    "value" to Json.Str(ChangePlanMachine.derivedStatus.value),
                    "rule" to Json.Str(ChangePlanMachine.derivedStatus.rule),
                    "persisted" to Json.Bool(ChangePlanMachine.derivedStatus.persisted),
                ),
            ),
            "guards" to Json.Arr(
                ChangePlanMachine.guards.map {
                    Json.Obj(
                        listOf(
                            "id" to Json.Str(it.id),
                            "rule" to Json.Str(it.rule),
                            "errorCode" to Json.Str(it.errorCode),
                        ),
                    )
                },
            ),
        ),
    )

    "state-machine-reality-drift" -> Json.Obj(
        listOf(
            "initial" to Json.Str(DriftMachine.initial.wire),
            "transitions" to Json.Obj(
                DriftMachine.transitions.map { (from, tos) -> from.wire to Json.Arr(tos.map { Json.Str(it.wire) }) },
            ),
            "creationRule" to Json.Obj(
                listOf(
                    "requires" to Json.Str(DriftMachine.creationRule.requires),
                    "minObservations" to Json.Num(DriftMachine.creationRule.minObservations.toString()),
                    "absenceOnly" to Json.Str(DriftMachine.creationRule.absenceOnly),
                    "alreadyConfirmedSource" to Json.Str(DriftMachine.creationRule.alreadyConfirmedSource),
                    "belowThreshold" to Json.Str(DriftMachine.creationRule.belowThreshold),
                    "duplicateEvidenceRef" to Json.Str(DriftMachine.creationRule.duplicateEvidenceRef),
                    "upsert" to Json.Str(DriftMachine.creationRule.upsert),
                ),
            ),
            "guards" to Json.Arr(
                DriftMachine.guards.map {
                    Json.Obj(
                        listOf("id" to Json.Str(it.id), "rule" to Json.Str(it.rule), "errorCode" to Json.Str(it.errorCode)),
                    )
                },
            ),
        ),
    )

    "state-machine-discovery-candidate" -> Json.Obj(
        listOf(
            "initial" to Json.Str(CandidateMachine.initial.wire),
            "transitions" to Json.Obj(
                CandidateMachine.transitions.map { (from, tos) -> from.wire to Json.Arr(tos.map { Json.Str(it.wire) }) },
            ),
            "accept" to Json.Obj(
                listOf(
                    "mutatesReality" to Json.Bool(CandidateMachine.accept.mutatesReality),
                    "effects" to Json.Arr(CandidateMachine.accept.effects.map { Json.Str(it) }),
                    "bumpsGraphRevision" to Json.Bool(CandidateMachine.accept.bumpsGraphRevision),
                    "replay" to Json.Str(CandidateMachine.accept.replay),
                ),
            ),
            "dismiss" to Json.Obj(
                listOf(
                    "mutatesReality" to Json.Bool(CandidateMachine.dismiss.mutatesReality),
                    "effects" to Json.Arr(CandidateMachine.dismiss.effects.map { Json.Str(it) }),
                    "reappeal" to Json.Str(CandidateMachine.dismiss.reappeal),
                ),
            ),
            "guards" to Json.Arr(
                CandidateMachine.guards.map {
                    Json.Obj(
                        listOf("id" to Json.Str(it.id), "rule" to Json.Str(it.rule), "errorCode" to Json.Str(it.errorCode)),
                    )
                },
            ),
        ),
    )

    "state-machine-action-verification" -> Json.Obj(
        listOf(
            "initial" to Json.Str(VerificationMachine.initial.wire),
            "terminal" to Json.Arr(VerificationMachine.terminal.map { Json.Str(it.wire) }),
            "transitions" to Json.Obj(
                VerificationMachine.transitions.map { (from, tos) -> from.wire to Json.Arr(tos.map { Json.Str(it.wire) }) },
            ),
            "evidenceSignalRule" to Json.Obj(
                listOf(
                    "appliesOnlyTo" to Json.Arr(
                        VerificationMachine.evidenceSignalRule.appliesOnlyTo.map { Json.Str(it.wire) },
                    ),
                    "requiresMethod" to Json.Str(VerificationMachine.evidenceSignalRule.requiresMethod),
                    "requiresMatch" to Json.Str(VerificationMachine.evidenceSignalRule.requiresMatch),
                    "effect" to Json.Str(VerificationMachine.evidenceSignalRule.effect),
                    "never" to Json.Arr(VerificationMachine.evidenceSignalRule.never.map { Json.Str(it) }),
                ),
            ),
            "guards" to Json.Arr(
                VerificationMachine.guards.map {
                    Json.Obj(
                        listOf("id" to Json.Str(it.id), "rule" to Json.Str(it.rule), "errorCode" to Json.Str(it.errorCode)),
                    )
                },
            ),
        ),
    )

    "state-machine-graph-revision" -> Json.Obj(
        listOf(
            "initial" to Json.Num(GraphRevisionMachine.initial.toString()),
            "monotonic" to Json.Bool(GraphRevisionMachine.monotonic),
            "atomicity" to Json.Str(GraphRevisionMachine.atomicity),
            "bumpsOn" to Json.Arr(GraphRevisionMachine.bumpsOn.map { Json.Str(it) }),
            "neverBumpsOn" to Json.Arr(GraphRevisionMachine.neverBumpsOn.map { Json.Str(it) }),
        ),
    )

    else -> throw NotImplementedError("no state machine runner for $id")
}

// ---------------------------------------------------------------------------
// 小工具
// ---------------------------------------------------------------------------

private fun str(o: Json.Obj, key: String): String = (o.require(key) as Json.Str).value

private fun int(o: Json.Obj, key: String): Int = ((o.require(key)) as Json.Num).asLong().toInt()

private fun arr(o: Json.Obj, key: String): List<Json> =
    (o[key] as? Json.Arr)?.items ?: emptyList()

private fun cap(o: Json.Obj, key: String): Capability =
    Capability.fromWire(str(o, key)) ?: error("unknown capability: ${str(o, key)}")

private fun hexToBytes(hex: String): ByteArray {
    require(hex.length % 2 == 0) { "bad hex length" }
    return ByteArray(hex.length / 2) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
}
