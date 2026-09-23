package com.pdig.conformance

import com.pdig.core.domain.validateRelationGroupUse
import com.pdig.core.domain.validateRelationUse
import com.pdig.core.generated.GroupMode
import com.pdig.core.generated.NodeKind
import com.pdig.core.json.Json

// ---------------------------------------------------------------------------
// relations —— relation / group 组合合法性（契约测试）
// ---------------------------------------------------------------------------

internal fun runRelations(id: String, input: Json.Obj): Json {
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