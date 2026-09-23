package com.pdig.app.data

import com.pdig.core.generated.NodeKind
import com.pdig.core.json.Json
import com.pdig.core.json.JsonParser
import java.security.MessageDigest

// ---------------------------------------------------------------------------
// 文件级辅助（纯函数，便于 JVM 侧复用与审计）
// ---------------------------------------------------------------------------

/** Observation-only 置信度：机器永不据此写 Reality，也永不据此产生 required。 */
const val OBSERVED_CONFIDENCE = 0.5

internal fun sha256Hex(value: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
}

/** 稳定 node id：同一 kind+name 重复导入不会生成第二个节点。 */
internal fun nodeIdFor(kind: NodeKind, name: String): String =
    "nd-" + sha256Hex(kind.wire + "|" + name).take(12)

internal fun parseStringList(json: String?): List<String> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        (JsonParser.parse(json) as? Json.Arr)?.items?.mapNotNull { (it as? Json.Str)?.value } ?: emptyList()
    } catch (_: Throwable) {
        emptyList()
    }
}
