// GB18030 解码（严格）—— Swift 移植，与 Harmony 侧 `sources/Gb18030.ets` 同口径。
//
// 覆盖（GB18030-2005，全部经 Node ICU 实测并由 CPython 内置 codec 独立复验）：
//   1. 单字节 0x00–0x7F        → ASCII
//   2. 双字节 lead 0x81–0xFE / trail 0x40–0xFE(除 0x7F) → 23,940 个码位，全表
//   3. 四字节 BMP 补漏区（lead 0x81–0x84）→ 50,400 槽位 / 39,420 有效，游程表
//   4. 四字节增补平面（lead 0x90–0xE3）  → 线性公式，cp > 0x10FFFF 即非法
//
// 明确不覆盖（从严判非法，不做宽松替换）：
//   - 单字节 0x80：GB18030-2005 的单字节区只有 0x00–0x7F。ICU 按 CP936 宽松解为
//     U+20AC，CPython 拒绝；本实现取严格一侧。
//   - 单字节 0xFF：无定义。
//
// 码表：Encoding/Gb18030Table.swift（由 tools/encoding/generate-gb18030-table.mjs 生成，
// 与 Harmony 侧同一份数据，sha256 一致）。**不要手改码表。**

import Foundation

public enum Gb18030 {
    private static let trailCount = 190  // trail 0x40–0x7E(63) + 0x80–0xFE(127)

    private static func trailIndex(_ trail: Int) -> Int {
        if trail >= 0x40 && trail <= 0x7E { return trail - 0x40 }
        if trail >= 0x80 && trail <= 0xFE { return trail - 0x80 + 63 }
        return -1
    }

    private static func appendCodePoint(_ out: inout String, _ cp: Int) {
        if cp <= 0xFFFF {
            out.unicodeScalars.append(UnicodeScalar(UInt16(cp))!)
            return
        }
        let v = cp - 0x10000
        let high = 0xD800 + (v >> 10)
        let low = 0xDC00 + (v & 0x3FF)
        out.unicodeScalars.append(UnicodeScalar(UInt16(high))!)
        out.unicodeScalars.append(UnicodeScalar(UInt16(low))!)
    }

    /// 严格 GB18030 解码。任何非法字节序列一律返回 nil。
    public static func decode(_ bytes: [UInt8]) -> String? {
        let twoByte = Gb18030Table.twoByteTable()
        let bmp4 = Gb18030Table.bmp4Table()
        var out = ""
        var i = 0
        while i < bytes.count {
            let b1 = Int(bytes[i])
            if b1 <= 0x7F {
                out.unicodeScalars.append(UnicodeScalar(UInt16(b1))!)
                i += 1
                continue
            }
            if b1 < 0x81 || b1 > 0xFE { return nil }   // 0x80 / 0xFF 无定义
            if i + 1 >= bytes.count { return nil }
            let b2 = Int(bytes[i + 1])

            // 双字节优先：trail 范围与四字节第二字节 0x30–0x39 不重叠，无歧义。
            let ti = trailIndex(b2)
            if ti >= 0 {
                let slot = (b1 - 0x81) * trailCount + ti
                if slot < 0 || slot >= Gb18030Table.twoByteSlots { return nil }
                appendCodePoint(&out, Int(twoByte[slot]))
                i += 2
                continue
            }

            if b2 < 0x30 || b2 > 0x39 { return nil }
            if i + 3 >= bytes.count { return nil }
            let b3 = Int(bytes[i + 2])
            let b4 = Int(bytes[i + 3])
            if b3 < 0x81 || b3 > 0xFE || b4 < 0x30 || b4 > 0x39 { return nil }

            if b1 >= 0x81 && b1 <= 0x84 {
                let slot = ((b1 - 0x81) * 10 + (b2 - 0x30)) * 1260 + ((b3 - 0x81) * 10 + (b4 - 0x30))
                if slot < 0 || slot >= Gb18030Table.bmp4Slots { return nil }
                let cp = bmp4[slot]
                if cp < 0 { return nil }
                appendCodePoint(&out, cp)
                i += 4
                continue
            }
            if b1 >= 0x90 && b1 <= 0xE3 {
                let cp = 0x10000 + ((b1 - 0x90) * 10 + (b2 - 0x30)) * 1260 + ((b3 - 0x81) * 10 + (b4 - 0x30))
                if cp > 0x10FFFF { return nil }
                appendCodePoint(&out, cp)
                i += 4
                continue
            }
            return nil
        }
        return out
    }

    /// 该 encoding 是否由本模块支持（调用方据此决定回退还是抛错）。
    public static func isGb18030Encoding(_ encoding: String) -> Bool {
        switch encoding.trimmingCharacters(in: .whitespaces).lowercased() {
        case "gb18030", "gbk", "gb2312", "cp936", "x-gbk": return true
        default: return false
        }
    }

    /// 严格 UTF-8 解码。与 Harmony 侧 `Utf8.decodeStrictUtf8` 同口径：
    /// 解不出来返回 nil，不替换、不兜底。
    public static func decodeStrictUtf8(_ bytes: [UInt8]) -> String? {
        var out = ""
        var i = 0
        while i < bytes.count {
            let b0 = Int(bytes[i])
            var cp = 0
            var need = 0
            if b0 <= 0x7F {
                cp = b0
                need = 0
            } else if b0 >= 0xC2 && b0 <= 0xDF {
                cp = b0 & 0x1F
                need = 1
            } else if b0 >= 0xE0 && b0 <= 0xEF {
                cp = b0 & 0x0F
                need = 2
            } else if b0 >= 0xF0 && b0 <= 0xF4 {
                cp = b0 & 0x07
                need = 3
            } else {
                return nil
            }
            if i + need >= bytes.count { return nil }
            var k = 1
            while k <= need {
                let bk = Int(bytes[i + k])
                if bk < 0x80 || bk > 0xBF { return nil }
                cp = (cp << 6) | (bk & 0x3F)
                k += 1
            }
            if need == 1 && cp < 0x80 { return nil }
            if need == 2 && (cp < 0x800 || (cp >= 0xD800 && cp <= 0xDFFF)) { return nil }
            if need == 3 && (cp < 0x10000 || cp > 0x10FFFF) { return nil }
            appendCodePoint(&out, cp)
            i += need + 1
        }
        return out
    }

    public static func hasUtf8Bom(_ bytes: [UInt8]) -> Bool {
        bytes.count >= 3 && bytes[0] == 0xEF && bytes[1] == 0xBB && bytes[2] == 0xBF
    }
}

public enum BillTextDecodeError: Error, CustomStringConvertible {
    case bomButNotUtf8
    case notUtf8AndUnsupportedEncoding(String)
    case notUtf8AndNotGb18030(String)

    public var description: String {
        switch self {
        case .bomButNotUtf8:
            return "file has a UTF-8 BOM but is not valid UTF-8"
        case .notUtf8AndUnsupportedEncoding(let enc):
            return "file is not valid UTF-8 and encoding '\(enc)' is not a GB18030 family charset"
        case .notUtf8AndNotGb18030(let enc):
            return "file is not valid UTF-8 and could not be decoded as '\(enc)'"
        }
    }
}
