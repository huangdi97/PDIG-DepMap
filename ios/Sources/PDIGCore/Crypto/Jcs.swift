// RFC 8785 JSON Canonicalization Scheme (JCS) —— 受限域实现，Swift 移植。
//
// 源头：android/core/.../crypto/Jcs.kt（Android 已 CORE_FROZEN，是本端口的基准）。
//
// 与 core/src/crypto/jcs.ts 同策略：
//  - 值域：string / safe integer / 纯对象 / 数组 / null / boolean
//  - 浮点、bigint → **直接抛错**，绝不静默产出非规范输出
//  - 对象键按 UTF-16 code unit 排序（RFC 8785 §3.2.3）
//  - 转义遵循 RFC 8785 §3.2.2.2（仅 \b \t \n \f \r \" \\ 与 <0x20 的 \u00xx）
//
// 跨端要求：三端必须对同一逻辑值产出**相同 bytes**（DEP-02）。

import Foundation

public struct JcsError: Error, CustomStringConvertible, Equatable {
    public let message: String
    public init(_ message: String) { self.message = message }
    public var description: String { message }
}

public enum Jcs {

    public static func stringify(_ value: Json) throws -> String {
        var sb = ""
        try emit(&sb, value)
        return sb
    }

    private static func emit(_ sb: inout String, _ v: Json) throws {
        switch v {
        case .null:
            sb += "null"
        case .bool(let b):
            sb += b ? "true" : "false"
        case .num(let raw):
            if !Json.num(raw).isSafeInteger {
                throw JcsError("JCS restricted domain: only safe integers allowed, got \(raw)")
            }
            sb += raw
        case .str(let s):
            emitString(&sb, s)
        case .arr(let items):
            sb += "["
            for (i, item) in items.enumerated() {
                if i > 0 { sb += "," }
                try emit(&sb, item)
            }
            sb += "]"
        case .obj(let o):
            let sorted = o.fields.sorted { utf16Less($0.0, $1.0) }
            sb += "{"
            for (i, f) in sorted.enumerated() {
                if i > 0 { sb += "," }
                emitString(&sb, f.0)
                sb += ":"
                try emit(&sb, f.1)
            }
            sb += "}"
        }
    }

    /// RFC 8785 §3.2.2.2 转义；非 ASCII 原样输出（UTF-8）。
    private static func emitString(_ sb: inout String, _ s: String) {
        sb += "\""
        for ch in s {
            switch ch {
            case "\"": sb += "\\\""
            case "\\": sb += "\\\\"
            case Character(UnicodeScalar(0x08)): sb += "\\b"
            case "\t": sb += "\\t"
            case "\n": sb += "\\n"
            case Character(UnicodeScalar(0x0C)): sb += "\\f"
            case "\r": sb += "\\r"
            default:
                if let v = ch.unicodeScalars.first?.value, v < 0x20 {
                    sb += "\\u00" + String(v, radix: 16).leftPadded(to: 2, with: "0")
                } else {
                    sb.append(ch)
                }
            }
        }
        sb += "\""
    }

    /// **必须用 UTF-16 code unit 排序，不能用 Swift 的 `String` 比较。**
    ///
    /// RFC 8785 §3.2.3 要求按 UTF-16 code unit 排序。Java / Kotlin 的
    /// `String.compareTo`、JS 的 `<` 天然就是 UTF-16 code unit 序；
    /// 但 Swift 的 `String` 比较走 Unicode 规范化 + 语义排序（例如 "\u{1F600}"
    /// 与 "\u{FFFD}" 的相对顺序会与 UTF-16 序不同）。这里显式按 UTF-16 逐单元比较，
    /// 否则三端 canonical bytes 会静默分叉。
    static func utf16Less(_ a: String, _ b: String) -> Bool {
        let au = Array(a.utf16)
        let bu = Array(b.utf16)
        let n = min(au.count, bu.count)
        var i = 0
        while i < n {
            if au[i] != bu[i] { return au[i] < bu[i] }
            i += 1
        }
        return au.count < bu.count
    }
}

extension String {
    func leftPadded(to width: Int, with pad: Character) -> String {
        if count >= width { return self }
        return String(repeating: pad, count: width - count) + self
    }
}
