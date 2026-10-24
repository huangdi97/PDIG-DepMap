package com.pdig.core.serialize

import com.pdig.core.db.JdbcTestDriver
import com.pdig.core.json.Json
import com.pdig.core.json.JsonParser
import com.pdig.core.schema.LATEST_SCHEMA_VERSION
import com.pdig.core.schema.getSchemaVersion
import com.pdig.core.schema.migrate
import kotlin.test.Test
import kotlin.test.assertEquals

class GraphSerializeSchemaBoundaryTest {
    @Test
    fun v4ApplicationExportsPortableV3MetaAndRestoreKeepsLocalV4Schema() {
        val driver = JdbcTestDriver()
        migrate(
            driver,
            "2026-10-19T00:00:00Z",
            targetVersion = LATEST_SCHEMA_VERSION,
        )
        assertEquals(4, getSchemaVersion(driver))

        val exported = exportGraph(driver)
        val payload = JsonParser.parse(exported.payloadJson) as Json.Obj
        assertEquals(3L, (payload["schemaVersion"] as Json.Num).asLong())
        val meta = (payload["meta"] as Json.Arr).items.map { it as Json.Obj }
        val exportedSchema = meta.first {
            (it["key"] as? Json.Str)?.value == "schema_version"
        }
        assertEquals("3", (exportedSchema["value"] as Json.Str).value)

        importGraph(driver, exported.payloadJson)

        // Restore replaces logical graph state only. The physical v4 tables
        // remain v4 and must keep their local migration marker.
        assertEquals(4, getSchemaVersion(driver))
        assertEquals(
            4,
            migrate(
                driver,
                "2026-10-19T01:00:00Z",
                targetVersion = LATEST_SCHEMA_VERSION,
            ),
        )
    }
}
