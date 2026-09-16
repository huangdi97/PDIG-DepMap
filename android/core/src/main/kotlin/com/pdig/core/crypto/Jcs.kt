package com.pdig.core.crypto

import com.pdig.core.json.Json
import com.pdig.core.json.JsonException

/**
 * RFC 8785 JSON Canonicalization Scheme (JCS) —— 受限域实现。
 *
 * 与 core/src/crypto/jcs.ts 同策略：
 *  - 值域：string / safe integer / 纯对象 / 数组 / null / boolean
 *  - 浮点、bigint → **直接抛错**，绝不静默产出非规范输出
 *  - 对象键按 UTF-16 code unit 排序
 *  - 转义遵循 RFC 8785 §3.2.2.2（仅 \b \t \n \f \r \" \\ 与 <0x20 的 \u00xx）
 *
 * 跨端要求：三端必须对同一逻辑值产出**相同 bytes**（DEP-02）。
 */
object Jcs {

    class JcsError(message: String) : RuntimeException(message)

    fun stringify(value: Json): String = buildString { emit(this, value) }

    private fun emit(sb: StringBuilder, v: Json) {
        when (v) {
            Json.Null -> sb.append("null")
            is Json.Bool -> sb.append(if (v.value) "true" else "false")
            is Json.Num -> {
                if (!v.isSafeInteger()) {
                    throw JcsError("JCS restricted domain: only safe integers allowed, got ${v.raw}")
                }
                sb.append(v.raw)
            }
            is Json.Str -> emitString(sb, v.value)
            is Json.Arr -> {
                sb.append('[')
                v.items.forEachIndexed { i, item -> if (i > 0) sb.append(','); emit(sb, item) }
                sb.append(']')
            }
            is Json.Obj -> {
                val sorted = v.fields.sortedBy { it.first }
                sb.append('{')
                sorted.forEachIndexed { i, (k, value) ->
                    if (i > 0) sb.append(',')
                    emitString(sb, k)
                    sb.append(':')
                    emit(sb, value)
                }
                sb.append('}')
            }
        }
    }

    /** RFC 8785 §3.2.2.2 转义；非 ASCII 原样输出（UTF-8）。 */
    private fun emitString(sb: StringBuilder, s: String) {
        sb.append('"')
        for (ch in s) {
            when (ch) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\b' -> sb.append("\\b")
                '\t' -> sb.append("\\t")
                '\n' -> sb.append("\\n")
                '\u000C' -> sb.append("\\f")
                '\r' -> sb.append("\\r")
                else -> if (ch.code < 0x20) {
                    sb.append("\\u00" + ch.code.toString(16).padStart(2, '0'))
                } else {
                    sb.append(ch)
                }
            }
        }
        sb.append('"')
    }
}
