package com.pdig.conformance

import com.pdig.core.generated.Capability
import com.pdig.core.json.Json
import com.pdig.core.json.JsonParser
import com.pdig.core.json.JsonWriter
import com.pdig.core.sources.MappingColumns
import com.pdig.core.sources.MappingOptions
import com.pdig.core.sources.MappingProfile
import java.io.File

/**
 * PDIG Android Conformance Runner。
 *
 * 诚实口径：未实现的用例报 NOT_IMPLEMENTED，**绝不自称 PASS**。
 * 每条结果都附带 actual，便于 harness 独立复核。
 *
 * Runner 按 category 拆分（本文件只保留入口与共享小工具）：
 * RunnerImpact.kt / RunnerRelations.kt / RunnerDepmap.kt /
 * RunnerParser.kt / RunnerBackupTimeline.kt。
 */

private const val PLATFORM = "android"

/** 仓库根（conformance fixture / import 原始文件的基准目录）。 */
internal lateinit var ROOT: File

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
    val manifest = parseObj(manifestFile.readText())
    val fixtures = requireArr(manifest.require("fixtures")).items

    val results = linkedMapOf<String, Json>()
    var pass = 0
    var fail = 0
    var notImplemented = 0

    for (item in fixtures) {
        val entry = requireObj(item)
        val id = requireStr(entry.require("id")).value
        val category = requireStr(entry.require("category")).value
        val path = requireStr(entry.require("path")).value
        val file = File(root, path)
        val case = parseObj(file.readText())
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
    "impact" -> runImpact(requireObj(input))
    "readiness" -> runReadiness(requireObj(input))
    "coverage" -> runCoverage(requireObj(input))
    "relations" -> runRelations(id, requireObj(input))
    "depmap" -> runDepmap(id, requireObj(input))
    "jcs" -> runJcs(requireObj(input))
    "scenario" -> runScenario()
    "state-machine" -> runStateMachine(id)
    "parser" -> runParser(requireObj(input))
    "timeline" -> runTimeline(id, requireObj(input))
    "migration" -> when (id) {
        "migration-db-v1-to-v3" -> runMigrationV1ToV3()
        else -> runMigration()
    }
    "backup" -> runBackup(requireObj(input))
    "failure-domain" -> runFailureDomain(requireObj(input))
    "recovery-cycle" -> runRecoveryCycle(requireObj(input))
    "action-dag" -> runActionDag(requireObj(input))
    "make-before-break" -> runMakeBeforeBreak(requireObj(input))
    "temporal-change" -> runTemporalChange(requireObj(input))
    "provider-policy" -> runProviderPolicy(requireObj(input))
    "identity-relations" -> runIdentityRelations(requireObj(input))
    else -> throw NotImplementedError("no runner for category $category")
}


// ---------------------------------------------------------------------------
// 临时 DB 与种子数据（RunnerBackupTimeline.kt 的 runner 共用）
// ---------------------------------------------------------------------------

/** 与 core/scripts/generate-conformance.ts 的 T0 保持一致。 */
internal const val T0 = "2026-09-13T00:00:00.000Z"

internal fun insertNode(driver: JdbcSqliteDriver, id: String, kind: String, name: String, fields: String = "{}") {
    driver.prepare(
        """
        INSERT INTO nodes (id, kind, template_id, name, issuer, last4, owner, archived, fields_json, vault_ref, wallet_ref, created_at, updated_at)
        VALUES (?, ?, NULL, ?, NULL, NULL, 'self', 0, ?, NULL, NULL, ?, ?)
        """.trimIndent(),
    ).run(id, kind, name, fields, T0, T0)
}

internal fun insertPlan(
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

internal fun <T> withTempDb(tag: String, fn: (JdbcSqliteDriver) -> T): T {
    val dir = java.nio.file.Files.createTempDirectory("pdig-cf-$tag").toFile()
    val driver = JdbcSqliteDriver(File(dir, "fx.db"))
    return try {
        fn(driver)
    } finally {
        driver.close()
        dir.deleteRecursively()
    }
}

// ---------------------------------------------------------------------------
// parser（adapter 选择 + mapping 解析）—— runner 主体见 RunnerParser.kt
// ---------------------------------------------------------------------------

internal fun parseMapping(v: Json?): MappingProfile? {
    if (v == null || v is Json.Null) return null
    val o = requireObj(v)
    val cols = requireObj(o.require("columns"))
    val opts = requireObj(o.require("options"))
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
            dateFormats = ((opts["dateFormats"] as? Json.Arr)?.items ?: emptyList()).map { requireStr(it).value },
            decimalSeparator = ((opts["decimalSeparator"] as? Json.Str)?.value ?: ".").first(),
            amountSignMode = (opts["amountSignMode"] as? Json.Str)?.value ?: "outward_positive",
            hasHeaderRow = (opts["hasHeaderRow"] as? Json.Bool)?.value ?: true,
            encoding = (opts["encoding"] as? Json.Str)?.value ?: "utf-8",
            positiveDirection = (opts["positiveDirection"] as? Json.Str)?.value,
        ),
    )
}

/** JS `JSON.stringify(number)` 等价：整数不输出小数点，否则输出最短往返表示。 */
internal fun num(d: Double): Json.Num {
    val s = if (d == kotlin.math.floor(d) && kotlin.math.abs(d) < 1e15) {
        d.toLong().toString()
    } else {
        d.toString()
    }
    return Json.Num(s)
}

/** 尚未实现：报 NOT_IMPLEMENTED，绝不自称 PASS。 */
internal fun notYet(id: String): Nothing =
    throw NotImplementedError("$id requires persistence/parser implementation not yet ported")

// ---------------------------------------------------------------------------
// 小工具
// ---------------------------------------------------------------------------

internal fun str(o: Json.Obj, key: String): String =
    (o.require(key) as? Json.Str)?.value ?: throw RuntimeException("expected JSON string at $key")

internal fun int(o: Json.Obj, key: String): Int =
    (o.require(key) as? Json.Num)?.asLong()?.toInt() ?: throw RuntimeException("expected JSON number at $key")

internal fun arr(o: Json.Obj, key: String): List<Json> =
    (o[key] as? Json.Arr)?.items ?: emptyList()

internal fun cap(o: Json.Obj, key: String): Capability =
    Capability.fromWire(str(o, key)) ?: error("unknown capability: ${str(o, key)}")

internal fun hexToBytes(hex: String): ByteArray {
    require(hex.length % 2 == 0) { "bad hex length" }
    return ByteArray(hex.length / 2) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
}

/** 安全强制转换：类型不符时报错而不是 ClassCastException。 */
internal fun requireStr(v: Json): Json.Str =
    v as? Json.Str ?: throw RuntimeException("expected JSON string")

internal fun requireObj(v: Json): Json.Obj =
    v as? Json.Obj ?: throw RuntimeException("expected JSON object")

internal fun requireArr(v: Json): Json.Arr =
    v as? Json.Arr ?: throw RuntimeException("expected JSON array")

private fun parseObj(text: String): Json.Obj =
    JsonParser.parse(text) as? Json.Obj ?: throw RuntimeException("expected JSON object")