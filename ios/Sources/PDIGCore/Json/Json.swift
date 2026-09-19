// PDIG 严格 JSON 解析器 / 序列化器 —— Swift 移植。
//
// 源头：android/core/.../json/Json.kt（Android 已 CORE_FROZEN，是本端口的基准）。
//
// 为什么不用第三方 JSON 库（含 Foundation 的 JSONSerialization）：
//  - Conformance 判定要求"输入 → 输出"完全可控；第三方库在数字格式化、
//    Unicode 转义、对象键顺序上的差异会被误判成平台语义差异。
//  - 本模块必须在三端行为一致；自包含解析器最省依赖。
//
// 保证：
//  - 解析结果保留**原始键顺序**
//  - 数字一律保留 raw 文本（Num.raw），取值时才转 Long / Double
//  - 非法输入抛 JsonException（fail closed，不静默容错）

import Foundation

public struct JsonException: Error, CustomStringConvertible, Equatable {
    public let message: String
    public init(_ message: String) { self.message = message }
    public var description: String { message }
}

/// JSON 对象：保留插入顺序的字段列表 + 惰性 key 索引。
public struct JsonObject: Equatable, Sendable {
    public let fields: [(String, Json)]

    public init(_ fields: [(String, Json)] = []) { self.fields = fields }

    private var index: [String: Json] {
        var m: [String: Json] = [:]
        for (k, v) in fields { m[k] = v }
        return m
    }

    public subscript(key: String) -> Json? { index[key] }

    public func require(_ key: String) throws -> Json {
        guard let v = index[key] else {
            throw JsonException("missing required field: \(key)")
        }
        return v
    }

    public var keys: [String] { fields.map { $0.0 } }

    public func has(_ key: String) -> Bool { index[key] != nil }
}

/// JSON 值模型（无第三方类型）。
public enum Json: Equatable, Sendable {
    case null
    case bool(Bool)
    case num(String)
    case str(String)
    case arr([Json])
    case obj(JsonObject)

    public var asLong: Int64 {
        get throws {
            switch self {
            case .num(let raw):
                guard let v = Int64(raw) else { throw JsonException("expected integer, got \(raw)") }
                return v
            default:
                throw JsonException("expected number, got \(self.describe)")
            }
        }
    }

    public var asDouble: Double {
        get throws {
            switch self {
            case .num(let raw):
                guard let v = Double(raw) else { throw JsonException("expected number, got \(raw)") }
                return v
            default:
                throw JsonException("expected number, got \(self.describe)")
            }
        }
    }

    /// 是否为 RFC 8785 受限域允许的 "safe integer"（raw 与 Long 的十进制表示逐字相同）。
    public var isSafeInteger: Bool {
        guard case .num(let raw) = self, let l = Int64(raw) else { return false }
        return String(l) == raw
    }

    public var stringValue: String? {
        if case .str(let s) = self { return s }
        return nil
    }

    public var boolValue: Bool? {
        if case .bool(let b) = self { return b }
        return nil
    }

    public var arrayValue: [Json]? {
        if case .arr(let a) = self { return a }
        return nil
    }

    public var objectValue: JsonObject? {
        if case .obj(let o) = self { return o }
        return nil
    }

    public var isNull: Bool {
        if case .null = self { return true }
        return false
    }

    private var describe: String {
        switch self {
        case .null: return "null"
        case .bool: return "boolean"
        case .num: return "number"
        case .str: return "string"
        case .arr: return "array"
        case .obj: return "object"
        }
    }
}

public enum JsonParser {

    public static func parse(_ text: String) throws -> Json {
        let state = State(text)
        state.skipWs()
        let value = try state.readValue()
        state.skipWs()
        if !state.atEnd() { throw JsonException("trailing content at \(state.pos)") }
        return value
    }

    final class State {
        private let src: [Character]
        private(set) var pos: Int = 0

        init(_ text: String) { self.src = Array(text) }

        var length: Int { src.count }

        func atEnd() -> Bool { pos >= src.count }

        func skipWs() {
            while pos < src.count && isJavaWhitespace(src[pos]) { pos += 1 }
        }

        private func peek() -> Character { src[pos] }

        private func expect(_ c: Character) throws {
            if atEnd() || src[pos] != c { throw JsonException("expected '\(c)' at \(pos)") }
            pos += 1
        }

        func readValue() throws -> Json {
            if atEnd() { throw JsonException("unexpected end of input") }
            switch peek() {
            case "{": return .obj(try readObject())
            case "[": return .arr(try readArray())
            case "\"": return .str(try readString())
            case "t": try literal("true"); return .bool(true)
            case "f": try literal("false"); return .bool(false)
            case "n": try literal("null"); return .null
            default: return .num(try readNumber())
            }
        }

        private func literal(_ s: String) throws {
            let chars = Array(s)
            if pos + chars.count > src.count || Array(src[pos..<(pos + chars.count)]) != chars {
                throw JsonException("invalid literal at \(pos)")
            }
            pos += chars.count
        }

        private func readObject() throws -> JsonObject {
            try expect("{")
            var fields: [(String, Json)] = []
            skipWs()
            if !atEnd() && peek() == "}" { pos += 1; return JsonObject(fields) }
            while true {
                skipWs()
                try expect("\"")
                let key = try readStringBody()
                skipWs()
                try expect(":")
                skipWs()
                let value = try readValue()
                fields.append((key, value))
                skipWs()
                if atEnd() { throw JsonException("unterminated object") }
                if peek() == "," {
                    pos += 1
                } else if peek() == "}" {
                    pos += 1
                    return JsonObject(fields)
                } else {
                    throw JsonException("expected ',' or '}' at \(pos)")
                }
            }
        }

        private func readArray() throws -> [Json] {
            try expect("[")
            var items: [Json] = []
            skipWs()
            if !atEnd() && peek() == "]" { pos += 1; return items }
            while true {
                skipWs()
                items.append(try readValue())
                skipWs()
                if atEnd() { throw JsonException("unterminated array") }
                if peek() == "," {
                    pos += 1
                } else if peek() == "]" {
                    pos += 1
                    return items
                } else {
                    throw JsonException("expected ',' or ']' at \(pos)")
                }
            }
        }

        private func readString() throws -> String {
            try expect("\"")
            return try readStringBody()
        }

        private func readStringBody() throws -> String {
            var sb = ""
            while true {
                if atEnd() { throw JsonException("unterminated string") }
                let c = src[pos]
                pos += 1
                if c == "\"" { return sb }
                if c == "\\" {
                    if atEnd() { throw JsonException("unterminated escape") }
                    let e = src[pos]
                    pos += 1
                    switch e {
                    case "\"": sb.append("\"")
                    case "\\": sb.append("\\")
                    case "/": sb.append("/")
                    case "b": sb.append(Character(UnicodeScalar(0x08)))
                    case "f": sb.append(Character(UnicodeScalar(0x0C)))
                    case "n": sb.append("\n")
                    case "r": sb.append("\r")
                    case "t": sb.append("\t")
                    case "u":
                        if pos + 4 > src.count { throw JsonException("bad \\u escape") }
                        let hex = String(src[pos..<(pos + 4)])
                        guard let cp = UInt32(hex, radix: 16), hex.count == 4 else {
                            throw JsonException("bad \\u escape: \(hex)")
                        }
                        pos += 4
                        // 与 Kotlin `cp.toChar()` 一致：不组合代理对（孤立代理按原样落成标量）
                        if let scalar = UnicodeScalar(cp) {
                            sb.append(Character(scalar))
                        } else {
                            throw JsonException("bad \\u escape: \(hex)")
                        }
                    default:
                        throw JsonException("bad escape: \\\(e)")
                    }
                } else {
                    sb.append(c)
                }
            }
        }

        private func readNumber() throws -> String {
            let start = pos
            if !atEnd() && src[pos] == "-" { pos += 1 }
            while pos < src.count && (isAsciiDigit(src[pos]) || "+-.eE".contains(src[pos])) { pos += 1 }
            let raw = String(src[start..<pos])
            if raw.isEmpty { throw JsonException("invalid value at \(start)") }
            return raw
        }
    }
}

func isAsciiDigit(_ c: Character) -> Bool {
    guard let v = c.unicodeScalars.first?.value, c.unicodeScalars.count == 1 else { return false }
    return v >= 0x30 && v <= 0x39
}

public enum JsonWriter {
    /// 紧凑序列化（无空格）。键顺序保持插入顺序。
    public static func write(_ value: Json) -> String {
        var sb = ""
        emit(&sb, value)
        return sb
    }

    private static func emit(_ sb: inout String, _ v: Json) {
        switch v {
        case .null: sb += "null"
        case .bool(let b): sb += b ? "true" : "false"
        case .num(let raw): sb += raw
        case .str(let s): emitString(&sb, s)
        case .arr(let items):
            sb += "["
            for (i, item) in items.enumerated() {
                if i > 0 { sb += "," }
                emit(&sb, item)
            }
            sb += "]"
        case .obj(let o):
            sb += "{"
            for (i, f) in o.fields.enumerated() {
                if i > 0 { sb += "," }
                emitString(&sb, f.0)
                sb += ":"
                emit(&sb, f.1)
            }
            sb += "}"
        }
    }

    private static func emitString(_ sb: inout String, _ s: String) {
        sb += "\""
        for ch in s {
            switch ch {
            case "\"": sb += "\\\""
            case "\\": sb += "\\\\"
            case "\n": sb += "\\n"
            case "\r": sb += "\\r"
            case "\t": sb += "\\t"
            case Character(UnicodeScalar(0x08)): sb += "\\b"
            case Character(UnicodeScalar(0x0C)): sb += "\\f"
            default:
                if let v = ch.unicodeScalars.first?.value, v < 0x20 {
                    sb += String(format: "\\u%04x", v)
                } else {
                    sb.append(ch)
                }
            }
        }
        sb += "\""
    }
}
