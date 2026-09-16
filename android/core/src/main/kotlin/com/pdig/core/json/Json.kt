package com.pdig.core.json

/**
 * PDIG 严格 JSON 解析器 / 序列化器。
 *
 * 为什么不用第三方 JSON 库：
 *  - Conformance 判定要求"输入 → 输出"完全可控；第三方库在数字格式化、
 *    Unicode 转义、对象键顺序上的差异会被误判成平台语义差异。
 *  - 本模块必须能在 JVM / Android / 单测中行为一致；自包含解析器最省依赖。
 *
 * 保证：
 *  - 解析结果保留**原始键顺序**（fixture 的 input 依赖顺序无关，但便于 diff 输出）
 *  - 数字一律解析为 Double 或 Long；本模块的业务数值只使用 Long / Double
 *  - 非法输入抛 JsonException（fail closed，不静默容错）
 */

class JsonException(message: String) : RuntimeException(message)

/** JSON 值模型（sealed；无第三方类型）。 */
sealed class Json {
    object Null : Json()
    data class Bool(val value: Boolean) : Json()
    data class Num(val raw: String) : Json() {
        fun asLong(): Long = raw.toLongOrNull()
            ?: throw JsonException("expected integer, got $raw")
        fun asDouble(): Double = raw.toDoubleOrNull()
            ?: throw JsonException("expected number, got $raw")
        fun isSafeInteger(): Boolean {
            val l = raw.toLongOrNull() ?: return false
            return l.toString() == raw
        }
    }
    data class Str(val value: String) : Json()
    data class Arr(val items: List<Json>) : Json()
    data class Obj(val fields: List<Pair<String, Json>>) : Json() {
        private val index by lazy { fields.toMap() }
        operator fun get(key: String): Json? = index[key]
        fun require(key: String): Json = index[key]
            ?: throw JsonException("missing required field: $key")
        fun keys(): List<String> = fields.map { it.first }
        fun has(key: String): Boolean = index.containsKey(key)
    }
}

object JsonParser {

    fun parse(text: String): Json {
        val state = State(text)
        state.skipWs()
        val value = state.readValue()
        state.skipWs()
        if (!state.atEnd()) throw JsonException("trailing content at ${state.pos}")
        return value
    }

    private class State(private val src: String) {
        var pos = 0

        fun atEnd(): Boolean = pos >= src.length

        fun skipWs() {
            while (pos < src.length && src[pos].isWhitespace()) pos++
        }

        private fun peek(): Char = src[pos]

        private fun expect(c: Char) {
            if (atEnd() || src[pos] != c) {
                throw JsonException("expected '$c' at $pos")
            }
            pos++
        }

        fun readValue(): Json {
            if (atEnd()) throw JsonException("unexpected end of input")
            return when (val c = peek()) {
                '{' -> readObject()
                '[' -> readArray()
                '"' -> Json.Str(readString())
                't' -> { literal("true"); Json.Bool(true) }
                'f' -> { literal("false"); Json.Bool(false) }
                'n' -> { literal("null"); Json.Null }
                else -> readNumber()
            }
        }

        private fun literal(s: String) {
            if (pos + s.length > src.length || src.substring(pos, pos + s.length) != s) {
                throw JsonException("invalid literal at $pos")
            }
            pos += s.length
        }

        private fun readObject(): Json.Obj {
            expect('{')
            val fields = mutableListOf<Pair<String, Json>>()
            skipWs()
            if (!atEnd() && peek() == '}') { pos++; return Json.Obj(fields) }
            while (true) {
                skipWs()
                expect('"')
                val key = readStringBody()
                skipWs()
                expect(':')
                skipWs()
                val value = readValue()
                fields.add(key to value)
                skipWs()
                when {
                    atEnd() -> throw JsonException("unterminated object")
                    peek() == ',' -> pos++
                    peek() == '}' -> { pos++; return Json.Obj(fields) }
                    else -> throw JsonException("expected ',' or '}' at $pos")
                }
            }
        }

        private fun readArray(): Json.Arr {
            expect('[')
            val items = mutableListOf<Json>()
            skipWs()
            if (!atEnd() && peek() == ']') { pos++; return Json.Arr(items) }
            while (true) {
                skipWs()
                items.add(readValue())
                skipWs()
                when {
                    atEnd() -> throw JsonException("unterminated array")
                    peek() == ',' -> pos++
                    peek() == ']' -> { pos++; return Json.Arr(items) }
                    else -> throw JsonException("expected ',' or ']' at $pos")
                }
            }
        }

        private fun readString(): String {
            expect('"')
            return readStringBody()
        }

        private fun readStringBody(): String {
            val sb = StringBuilder()
            while (true) {
                if (atEnd()) throw JsonException("unterminated string")
                when (val c = src[pos++]) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (atEnd()) throw JsonException("unterminated escape")
                        when (val e = src[pos++]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (pos + 4 > src.length) throw JsonException("bad \\u escape")
                                val hex = src.substring(pos, pos + 4)
                                val cp = hex.toIntOrNull(16)
                                    ?: throw JsonException("bad \\u escape: $hex")
                                pos += 4
                                sb.append(cp.toChar())
                            }
                            else -> throw JsonException("bad escape: \\$e")
                        }
                    }
                    else -> sb.append(c)
                }
            }
        }

        private fun readNumber(): Json.Num {
            val start = pos
            if (!atEnd() && src[pos] == '-') pos++
            while (pos < src.length && (src[pos].isDigit() || src[pos] in "+-.eE")) pos++
            val raw = src.substring(start, pos)
            if (raw.isEmpty()) throw JsonException("invalid value at $start")
            return Json.Num(raw)
        }
    }
}

object JsonWriter {
    /** 紧凑序列化（无空格）。键顺序保持插入顺序。 */
    fun write(value: Json): String = buildString { emit(this, value) }

    private fun emit(sb: StringBuilder, v: Json) {
        when (v) {
            Json.Null -> sb.append("null")
            is Json.Bool -> sb.append(if (v.value) "true" else "false")
            is Json.Num -> sb.append(v.raw)
            is Json.Str -> emitString(sb, v.value)
            is Json.Arr -> {
                sb.append('[')
                v.items.forEachIndexed { i, item ->
                    if (i > 0) sb.append(',')
                    emit(sb, item)
                }
                sb.append(']')
            }
            is Json.Obj -> {
                sb.append('{')
                v.fields.forEachIndexed { i, (k, value) ->
                    if (i > 0) sb.append(',')
                    emitString(sb, k)
                    sb.append(':')
                    emit(sb, value)
                }
                sb.append('}')
            }
        }
    }

    private fun emitString(sb: StringBuilder, s: String) {
        sb.append('"')
        for (ch in s) {
            when {
                ch == '"' -> sb.append("\\\"")
                ch == '\\' -> sb.append("\\\\")
                ch == '\n' -> sb.append("\\n")
                ch == '\r' -> sb.append("\\r")
                ch == '\t' -> sb.append("\\t")
                ch == '\b' -> sb.append("\\b")
                ch == '\u000C' -> sb.append("\\f")
                ch.code < 0x20 -> sb.append("\\u%04x".format(ch.code))
                else -> sb.append(ch)
            }
        }
        sb.append('"')
    }
}
