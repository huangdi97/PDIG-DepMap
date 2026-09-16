package com.pdig.core.serialize

import com.pdig.core.db.SqlRow
import com.pdig.core.db.SqliteDriver
import com.pdig.core.json.Json
import com.pdig.core.json.JsonParser
import com.pdig.core.json.JsonWriter
import com.pdig.core.schema.LEGACY_WECHAT_ADAPTER_ID
import com.pdig.core.schema.LEGACY_WECHAT_ADAPTER_VERSION
import com.pdig.core.schema.LEGACY_WECHAT_SOURCE_INSTANCE_ID
import com.pdig.core.schema.SCHEMA_VERSION

/**
 * 逻辑图序列化 —— `.depmap` 备份 payload 层（GOAL MVP02 §15）。
 *
 * 关键分离：**crypto 容器 formatVersion（V1）与 payload schemaVersion（3）互相独立**。
 *  - export：读取全部持久化实体 → payload JSON (schemaVersion=3)
 *  - import：payloadVersion/schemaVersion 校验（未来版本明确拒绝）→
 *    必要时 in-memory migrate → 完整校验 → 单事务原子替换；失败回滚
 *  - 等价性：export → import → export 逐字节一致
 *
 * 移植自 core/src/services/graph-serialize.ts。
 */

const val GRAPH_PAYLOAD_KIND = "depmap-logical-graph"
const val GRAPH_PAYLOAD_VERSION = 3

class GraphImportError(message: String) : RuntimeException(message)

data class PayloadTable(val table: String, val columns: List<String>)

val PAYLOAD_TABLES: List<PayloadTable> = listOf(
    PayloadTable("meta", listOf("key", "value")),
    PayloadTable(
        "source_instances",
        listOf(
            "id", "adapter_id", "adapter_version", "source_kind", "provider_id", "account_node_id",
            "label", "country", "jurisdiction", "currencies_json", "state", "created_at",
            "updated_at", "last_ingested_at",
        ),
    ),
    PayloadTable(
        "nodes",
        listOf(
            "id", "kind", "template_id", "name", "issuer", "last4", "owner", "archived",
            "fields_json", "vault_ref", "wallet_ref", "created_at", "updated_at",
        ),
    ),
    PayloadTable(
        "dependencies",
        listOf(
            "id", "from_node", "relation", "to_node", "capability", "criticality", "group_id",
            "state", "origin", "confirmed_at", "last_verified_at", "retired_at",
            "evidence_refs_json", "verification_basis_type", "verification_basis_json",
            "created_at", "updated_at",
        ),
    ),
    PayloadTable(
        "dependency_groups",
        listOf(
            "id", "group_key", "target_node_id", "capability", "mode", "member_edge_ids_json",
            "state", "confirmed_at", "last_verified_at", "verification_basis_type",
            "verification_basis_json", "created_at", "updated_at",
        ),
    ),
    PayloadTable(
        "dependency_proposals",
        listOf(
            "id", "key", "from_node", "relation", "to_node", "capability", "proposal_type",
            "source", "parser_id", "parser_version", "confidence_score", "path_json", "decision",
            "decided_at", "criticality_decision", "observation_count", "rejected_at",
            "rejected_at_stream_counts_json", "created_at", "updated_at",
        ),
    ),
    PayloadTable("proposal_evidence_refs", listOf("proposal_key", "evidence_id", "position")),
    PayloadTable(
        "dependency_group_proposals",
        listOf(
            "id", "key", "target_node_id", "capability", "mode", "member_dependency_keys_json",
            "decision", "decided_at", "rejected_at", "rejected_at_observation_count",
            "created_at", "updated_at",
        ),
    ),
    PayloadTable(
        "evidence",
        listOf(
            "id", "proposal_key", "source_instance_id", "adapter_id", "adapter_version",
            "evidence_kind", "source_type", "parser_id", "parser_version",
            "last_import_session_id", "first_observed_at", "last_observed_at",
            "observation_count", "created_at", "updated_at",
        ),
    ),
    PayloadTable(
        "observation_fingerprints",
        listOf(
            "fingerprint", "source_instance_id", "source", "fingerprint_version",
            "import_session_id", "first_seen_at",
        ),
    ),
    PayloadTable(
        "import_sessions",
        listOf(
            "id", "source_type", "parser_id", "parser_version", "source_instance_id",
            "adapter_id", "adapter_version", "started_at", "completed_at", "raw_count",
            "new_unique_count", "duplicate_count", "proposal_count", "error_count",
        ),
    ),
)

private val DELETE_ORDER = listOf(
    "proposal_evidence_refs",
    "dependency_groups",
    "dependencies",
    "dependency_group_proposals",
    "dependency_proposals",
    "evidence",
    "observation_fingerprints",
    "import_sessions",
    "source_instances",
    "nodes",
    "meta",
)

// ---------------------------------------------------------------------------
// 值序列化（必须与 JS JSON.stringify 的 number 输出对齐）
// ---------------------------------------------------------------------------

internal fun sqlValueToJson(v: Any?): Json = when (v) {
    null -> Json.Null
    is String -> Json.Str(v)
    is Long, is Int, is Short, is Byte -> Json.Num(v.toString())
    is Double -> Json.Num(
        if (v == kotlin.math.floor(v) && kotlin.math.abs(v) < 1e15) {
            v.toLong().toString()
        } else {
            v.toString()
        },
    )
    is Float -> sqlValueToJson(v.toDouble())
    is Boolean -> Json.Num(if (v) "1" else "0")
    is ByteArray -> Json.Str(v.toString(Charsets.UTF_8))
    else -> Json.Str(v.toString())
}

internal fun jsonToSqlValue(v: Json): Any? = when (v) {
    Json.Null -> null
    is Json.Str -> v.value
    is Json.Bool -> if (v.value) 1 else 0
    is Json.Num -> {
        val raw = v.raw
        raw.toLongOrNull() ?: raw.toDoubleOrNull() ?: throw GraphImportError("bad number: $raw")
    }
    else -> throw GraphImportError("unsupported payload value")
}

// ---------------------------------------------------------------------------
// export
// ---------------------------------------------------------------------------

data class GraphExportResult(val payloadJson: String, val counts: Map<String, Int>)

fun exportGraph(driver: SqliteDriver): GraphExportResult {
    val fields = mutableListOf<Pair<String, Json>>(
        "payloadKind" to Json.Str(GRAPH_PAYLOAD_KIND),
        "payloadVersion" to Json.Num(GRAPH_PAYLOAD_VERSION.toString()),
        "schemaVersion" to Json.Num(SCHEMA_VERSION.toString()),
    )
    val counts = LinkedHashMap<String, Int>()
    for (t in PAYLOAD_TABLES) {
        val rows = driver.prepare("SELECT * FROM ${t.table}").all()
        counts[t.table] = rows.size
        fields.add(t.table to Json.Arr(rows.map { row -> rowToJson(row) }))
    }
    return GraphExportResult(JsonWriter.write(Json.Obj(fields)), counts)
}

private fun rowToJson(row: SqlRow): Json =
    Json.Obj(row.columns().map { col -> col to sqlValueToJson(row.raw(col)) })

// ---------------------------------------------------------------------------
// payload 版本迁移（纯函数，不触碰 DB）
// ---------------------------------------------------------------------------

fun migratePayloadV1toV2(payloadJson: String): String {
    val parsed = parsePayloadObject(payloadJson)
    if ((parsed["payloadKind"] as? Json.Str)?.value != GRAPH_PAYLOAD_KIND) {
        throw GraphImportError("payloadKind must be $GRAPH_PAYLOAD_KIND")
    }
    val version = (parsed["payloadVersion"] as? Json.Num)?.asLong()?.toInt()
    if (version != 1) {
        throw GraphImportError("migratePayloadV1toV2 expects payloadVersion 1, got $version")
    }
    val legacyNow = "1970-01-01T00:00:00.000Z"
    val legacyInstance = Json.Obj(
        listOf(
            "id" to Json.Str(LEGACY_WECHAT_SOURCE_INSTANCE_ID),
            "adapter_id" to Json.Str(LEGACY_WECHAT_ADAPTER_ID),
            "adapter_version" to Json.Num(LEGACY_WECHAT_ADAPTER_VERSION.toString()),
            "source_kind" to Json.Str("statement_file"),
            "provider_id" to Json.Null,
            "account_node_id" to Json.Null,
            "label" to Json.Str("Legacy WeChat Statement Source"),
            "country" to Json.Null,
            "jurisdiction" to Json.Null,
            "currencies_json" to Json.Str("""["CNY"]"""),
            "state" to Json.Str("active"),
            "created_at" to Json.Str(legacyNow),
            "updated_at" to Json.Str(legacyNow),
            "last_ingested_at" to Json.Null,
        ),
    )
    val fingerprints = rowsOf(parsed, "observation_fingerprints").map { r ->
        Json.Obj(
            listOf(
                "fingerprint" to (r["fingerprint"] ?: Json.Null),
                "source_instance_id" to Json.Str(LEGACY_WECHAT_SOURCE_INSTANCE_ID),
                "source" to Json.Str((r["source"] as? Json.Str)?.value ?: ""),
                "fingerprint_version" to (r["fingerprint_version"] ?: Json.Num("1")),
                "import_session_id" to (r["import_session_id"] ?: Json.Null),
                "first_seen_at" to (r["first_seen_at"] ?: Json.Null),
            ),
        )
    }
    val evidence = rowsOf(parsed, "evidence").map { r ->
        Json.Obj(
            listOf(
                "id" to (r["id"] ?: Json.Null),
                "proposal_key" to (r["proposal_key"] ?: Json.Null),
                "source_instance_id" to Json.Str(LEGACY_WECHAT_SOURCE_INSTANCE_ID),
                "adapter_id" to Json.Str(LEGACY_WECHAT_ADAPTER_ID),
                "adapter_version" to Json.Num(
                    ((r["parser_version"] as? Json.Num)?.asLong() ?: 1L).toString(),
                ),
                "evidence_kind" to Json.Str("transaction_stream"),
                "source_type" to (r["source_type"] ?: Json.Null),
                "parser_id" to (r["parser_id"] ?: Json.Null),
                "parser_version" to (r["parser_version"] ?: Json.Null),
                "last_import_session_id" to (r["last_import_session_id"] ?: Json.Null),
                "first_observed_at" to (r["first_observed_at"] ?: Json.Null),
                "last_observed_at" to (r["last_observed_at"] ?: Json.Null),
                "observation_count" to (r["observation_count"] ?: Json.Num("0")),
                "created_at" to (r["created_at"] ?: Json.Null),
                "updated_at" to (r["updated_at"] ?: Json.Null),
            ),
        )
    }
    val v1Proposals = rowsOf(parsed, "dependency_proposals")
    val proposals = v1Proposals.map { r ->
        Json.Obj(
            r.fields.filter { it.first != "evidence_id" } + ("rejected_at_stream_counts_json" to Json.Null),
        )
    }
    val evidenceRefs = v1Proposals.mapIndexedNotNull { i, r ->
        val ev = r["evidence_id"]
        if (ev == null || ev is Json.Null) return@mapIndexedNotNull null
        Json.Obj(
            listOf(
                "proposal_key" to (r["key"] ?: Json.Null),
                "evidence_id" to ev,
                "position" to Json.Num(i.toString()),
            ),
        )
    }
    val sessions = rowsOf(parsed, "import_sessions").map { r ->
        Json.Obj(
            r.fields +
                ("source_instance_id" to Json.Str(LEGACY_WECHAT_SOURCE_INSTANCE_ID)) +
                ("adapter_id" to Json.Str(LEGACY_WECHAT_ADAPTER_ID)) +
                ("adapter_version" to Json.Num(LEGACY_WECHAT_ADAPTER_VERSION.toString())),
        )
    }
    val deps = rowsOf(parsed, "dependencies").map { withVerificationBasis(it) }
    val groups = rowsOf(parsed, "dependency_groups").map { withVerificationBasis(it) }

    val v2 = Json.Obj(
        listOf(
            "payloadKind" to Json.Str(GRAPH_PAYLOAD_KIND),
            "payloadVersion" to Json.Num("2"),
            "schemaVersion" to Json.Num("2"),
            "meta" to (parsed["meta"] ?: Json.Arr(emptyList())),
            "source_instances" to Json.Arr(listOf(legacyInstance)),
            "nodes" to (parsed["nodes"] ?: Json.Arr(emptyList())),
            "dependencies" to Json.Arr(deps),
            "dependency_groups" to Json.Arr(groups),
            "dependency_proposals" to Json.Arr(proposals),
            "proposal_evidence_refs" to Json.Arr(evidenceRefs),
            "dependency_group_proposals" to (parsed["dependency_group_proposals"] ?: Json.Arr(emptyList())),
            "evidence" to Json.Arr(evidence),
            "observation_fingerprints" to Json.Arr(fingerprints),
            "import_sessions" to Json.Arr(sessions),
        ),
    )
    return JsonWriter.write(v2)
}

private fun withVerificationBasis(r: Json.Obj): Json.Obj = Json.Obj(
    r.fields +
        ("verification_basis_type" to (r["verification_basis_type"] ?: Json.Str("user_confirmed"))) +
        ("verification_basis_json" to (r["verification_basis_json"] ?: Json.Null)),
)

fun migratePayloadV2toV3(payloadJson: String): String {
    val parsed = parsePayloadObject(payloadJson)
    val version = (parsed["payloadVersion"] as? Json.Num)?.asLong()?.toInt()
    if (version != 2) {
        throw GraphImportError("migratePayloadV2toV3 expects payloadVersion 2, got $version")
    }
    val meta = rowsOf(parsed, "meta")
    val hasRevision = meta.any { (it["key"] as? Json.Str)?.value == "graph_revision" }
    val newMeta = if (hasRevision) meta else meta + Json.Obj(
        listOf("key" to Json.Str("graph_revision"), "value" to Json.Str("0")),
    )
    val v3 = Json.Obj(
        listOf(
            "payloadKind" to Json.Str(GRAPH_PAYLOAD_KIND),
            "payloadVersion" to Json.Num(GRAPH_PAYLOAD_VERSION.toString()),
            "schemaVersion" to Json.Num(SCHEMA_VERSION.toString()),
            "meta" to Json.Arr(newMeta),
            "source_instances" to (parsed["source_instances"] ?: Json.Arr(emptyList())),
            "nodes" to (parsed["nodes"] ?: Json.Arr(emptyList())),
            "dependencies" to (parsed["dependencies"] ?: Json.Arr(emptyList())),
            "dependency_groups" to (parsed["dependency_groups"] ?: Json.Arr(emptyList())),
            "dependency_proposals" to (parsed["dependency_proposals"] ?: Json.Arr(emptyList())),
            "proposal_evidence_refs" to (parsed["proposal_evidence_refs"] ?: Json.Arr(emptyList())),
            "dependency_group_proposals" to (parsed["dependency_group_proposals"] ?: Json.Arr(emptyList())),
            "evidence" to (parsed["evidence"] ?: Json.Arr(emptyList())),
            "observation_fingerprints" to (parsed["observation_fingerprints"] ?: Json.Arr(emptyList())),
            "import_sessions" to (parsed["import_sessions"] ?: Json.Arr(emptyList())),
        ),
    )
    return JsonWriter.write(v3)
}

fun migrateIfNeeded(payloadJson: String): String {
    val parsed = try {
        JsonParser.parse(payloadJson)
    } catch (e: Throwable) {
        throw GraphImportError("payload is not valid JSON: ${e.message}")
    }
    if (parsed is Json.Obj) {
        when ((parsed["payloadVersion"] as? Json.Num)?.asLong()?.toInt()) {
            1 -> return migratePayloadV2toV3(migratePayloadV1toV2(payloadJson))
            2 -> return migratePayloadV2toV3(payloadJson)
        }
    }
    return payloadJson
}

private fun parsePayloadObject(payloadJson: String): Json.Obj {
    val parsed = try {
        JsonParser.parse(payloadJson)
    } catch (_: Throwable) {
        throw GraphImportError("payload is not valid JSON")
    }
    if (parsed !is Json.Obj) throw GraphImportError("payload must be an object")
    return parsed
}

private fun rowsOf(payload: Json.Obj, table: String): List<Json.Obj> {
    val v = payload[table] ?: return emptyList()
    if (v !is Json.Arr) throw GraphImportError("payload.$table must be an array")
    return v.items.map {
        if (it !is Json.Obj) throw GraphImportError("payload.$table rows must be objects")
        it
    }
}

// ---------------------------------------------------------------------------
// import
// ---------------------------------------------------------------------------

private fun tableRows(payload: Json.Obj, table: String, required: List<String>): List<Json.Obj> {
    val rows = rowsOf(payload, table)
    for (row in rows) {
        for (col in required) {
            if (!row.has(col)) throw GraphImportError("payload.$table row missing column $col")
        }
    }
    return rows
}

private fun validatePayload(payloadJson: String): Json.Obj {
    val payload = parsePayloadObject(payloadJson)
    if ((payload["payloadKind"] as? Json.Str)?.value != GRAPH_PAYLOAD_KIND) {
        throw GraphImportError("payloadKind must be $GRAPH_PAYLOAD_KIND")
    }
    val pv = (payload["payloadVersion"] as? Json.Num)?.asLong()?.toInt()
    if (pv != GRAPH_PAYLOAD_VERSION) {
        throw GraphImportError("unsupported payloadVersion: $pv (expected $GRAPH_PAYLOAD_VERSION)")
    }
    val svRaw = payload["schemaVersion"] as? Json.Num
        ?: throw GraphImportError("schemaVersion must be an integer")
    if (!svRaw.isSafeInteger()) throw GraphImportError("schemaVersion must be an integer")
    val sv = svRaw.asLong().toInt()
    if (sv > SCHEMA_VERSION) {
        throw GraphImportError("payload schemaVersion ($sv) newer than supported ($SCHEMA_VERSION)")
    }
    for (t in PAYLOAD_TABLES) tableRows(payload, t.table, t.columns)
    return payload
}

/** 原子导入：校验通过后单事务替换全部数据；任何失败回滚，不留半恢复状态。 */
fun importGraph(driver: SqliteDriver, payloadJson: String): Map<String, Int> {
    val normalized = migrateIfNeeded(payloadJson)
    val payload = validatePayload(normalized)
    return driver.transaction {
        val imported = LinkedHashMap<String, Int>()
        for (table in DELETE_ORDER) driver.exec("DELETE FROM $table")
        for (t in PAYLOAD_TABLES) {
            val rows = tableRows(payload, t.table, t.columns)
            if (rows.isEmpty()) {
                imported[t.table] = 0
                continue
            }
            val placeholders = t.columns.joinToString(", ") { "?" }
            val stmt = driver.prepare(
                "INSERT INTO ${t.table} (${t.columns.joinToString(", ")}) VALUES ($placeholders)",
            )
            for (row in rows) {
                stmt.run(*t.columns.map { jsonToSqlValue(row.require(it)) }.toTypedArray())
            }
            imported[t.table] = rows.size
        }
        imported
    }
}

// ---------------------------------------------------------------------------
// 完整性（孤儿检测）
// ---------------------------------------------------------------------------

data class OrphanReport(
    val orphanDependencies: List<String>,
    val orphanGroups: List<String>,
    val danglingGroupMembers: List<String>,
    val orphanEvidence: List<String>,
    val orphanFingerprints: List<String>,
)

fun checkGraphIntegrity(driver: SqliteDriver): OrphanReport = OrphanReport(
    orphanDependencies = driver.prepare(
        """
        SELECT d.id FROM dependencies d
         WHERE d.from_node NOT IN (SELECT id FROM nodes) OR d.to_node NOT IN (SELECT id FROM nodes)
        """.trimIndent(),
    ).all().map { it.str("id") ?: "" },
    orphanGroups = driver.prepare(
        """
        SELECT g.id FROM dependency_groups g WHERE g.target_node_id NOT IN (SELECT id FROM nodes)
        """.trimIndent(),
    ).all().map { it.str("id") ?: "" },
    danglingGroupMembers = driver.prepare(
        """
        SELECT g.id FROM dependency_groups g
         WHERE EXISTS (SELECT 1 FROM json_each(g.member_edge_ids_json) je
                        WHERE je.value NOT IN (SELECT id FROM dependencies))
        """.trimIndent(),
    ).all().map { it.str("id") ?: "" },
    orphanEvidence = driver.prepare(
        """
        SELECT e.id FROM evidence e WHERE e.source_instance_id NOT IN (SELECT id FROM source_instances)
        """.trimIndent(),
    ).all().map { it.str("id") ?: "" },
    orphanFingerprints = driver.prepare(
        """
        SELECT f.rowid FROM observation_fingerprints f
         WHERE f.source_instance_id NOT IN (SELECT id FROM source_instances)
        """.trimIndent(),
    ).all().map { it.str("rowid") ?: "" },
)
