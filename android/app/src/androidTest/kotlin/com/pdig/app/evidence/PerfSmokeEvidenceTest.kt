package com.pdig.app.evidence

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pdig.app.platform.AndroidSqliteDriver
import com.pdig.core.crypto.DepmapContainer
import com.pdig.core.schema.migrate
import com.pdig.core.sources.GenericCsvParser
import com.pdig.core.sources.MappingColumns
import com.pdig.core.sources.MappingOptions
import com.pdig.core.sources.MappingProfile
import com.pdig.core.sources.Observation
import com.pdig.core.timeline.buildTimeline
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import org.json.JSONObject

/**
 * 性能 smoke：不发明 SLA，只记录**本机观测值**（环境 = emulator API34 / x86_64 / swiftshader）。
 * 结果写到 filesDir/perf-smoke.json，并用 logcat tag `PDIG_PERF` 输出。
 */
@RunWith(AndroidJUnit4::class)
class PerfSmokeEvidenceTest {

    private fun ctx() = InstrumentationRegistry.getInstrumentation().targetContext
    private val now = "2026-09-15T00:00:00.000Z"
    private val out = LinkedHashMap<String, Long>()
    private var rows: Int = 0

    private fun measure(name: String, block: () -> Unit) {
        val t0 = System.nanoTime()
        block()
        out[name] = (System.nanoTime() - t0) / 1_000_000
    }

    private fun buildCsv(n: Int): ByteArray {
        val sb = StringBuilder()
        sb.append("date,merchant,amount,currency\n")
        for (i in 0 until n) {
            sb.append("2026-01-")
            sb.append(String.format("%02d", (i % 28) + 1))
            sb.append("T10:00:00Z,Merchant ")
            sb.append(i % 500)
            sb.append(",")
            sb.append(String.format("%.2f", (i % 1000) + 0.5))
            sb.append(",CNY\n")
        }
        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    @Test
    fun perfSmoke() {
        val f = File(ctx().filesDir, "ev-perf.db").apply { if (exists()) delete() }

        var driver: AndroidSqliteDriver? = null
        measure("db_open_encrypted_sqlcipher") { driver = AndroidSqliteDriver.open(f, "pw-perf") }
        val d = driver!!

        measure("migrate_v0_to_v3") { migrate(d, now) }

        val csv10k = buildCsv(10_000)
        val mapping = MappingProfile(
            columns = MappingColumns(
                dateTime = "date",
                amount = "amount",
                description = "merchant",
                counterparty = "merchant",
                currency = "currency",
            ),
            options = MappingOptions(
                delimiter = ",",
                hasHeaderRow = true,
                // 测试数据是完整 ISO 时间戳；dateFormats 默认只有 YYYY-MM-DD，
                // 不匹配会让 1 万行全部落入 "bad date" 错误路径（csvRowsParsed=0）。
                dateFormats = listOf("YYYY-MM-DDTHH:mm:ssZ", "YYYY-MM-DD"),
            ),
        )
        var parsed: List<Observation> = emptyList()
        var parseErrors: Int = -1
        measure("parse_generic_csv_10k_rows") {
            val r = GenericCsvParser.parse(csv10k, mapping)
            parsed = r.observations
            parseErrors = r.errors.size
        }
        rows = parsed.size
        // 若解析结果不是 1 万行，后面的"性能数字"就是空跑出来的，必须让它失败而不是静默记录。
        org.junit.Assert.assertEquals(
            "10k CSV 必须全部解析成功，否则 perf 数字无意义",
            10_000,
            rows,
        )

        measure("insert_10k_rows_in_one_transaction") {
            d.transaction {
                val st = d.prepare(
                    """
                    INSERT INTO observation_fingerprints (fingerprint, source_instance_id, source,
                                                          fingerprint_version, import_session_id, first_seen_at)
                    VALUES (?, 'legacy-wechat-statement', 'generic_csv', 1, 'sess-perf', ?)
                    """.trimIndent(),
                )
                for (i in 0 until 10_000) st.run("fp-perf-$i", now)
            }
        }

        measure("query_nodes_empty") { d.prepare("SELECT id FROM nodes").all() }
        measure("build_timeline") { buildTimeline(d, now) }

        val payload: String = com.pdig.core.serialize.exportGraph(d).payloadJson
        measure("export_graph_bytes=${payload.toByteArray(Charsets.UTF_8).size}") { com.pdig.core.serialize.exportGraph(d) }

        var container: String = ""
        measure("depmap_encrypt_export") {
            container = DepmapContainer.create(payload.toByteArray(Charsets.UTF_8), "pw-perf").json
        }
        measure("depmap_decrypt_and_import") {
            val opened = String(DepmapContainer.openContainer(container, "pw-perf"), Charsets.UTF_8)
            com.pdig.core.serialize.importGraph(d, opened)
        }

        val result = JSONObject()
        result.put("device", "${android.os.Build.MODEL} api=${android.os.Build.VERSION.SDK_INT} abi=${android.os.Build.SUPPORTED_ABIS.firstOrNull()}")
        result.put("csvRowsParsed", rows)
        result.put("csvParseErrors", parseErrors)
        for ((k, v) in out) result.put(k, v)
        val file = File(ctx().filesDir, "perf-smoke.json")
        file.writeText(result.toString(2))
        android.util.Log.d("PDIG_PERF", result.toString(2))
        println("PDIG_PERF " + result.toString(2))

        d.close()
        f.delete()
    }
}
