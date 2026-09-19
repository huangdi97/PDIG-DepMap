// expected 与 actual 的语义相等判定。
//
// 为什么不用"两边各自序列化再比字符串"：
//   fixture 的 expected 由 TypeScript oracle 生成，数字的**字面形式**
//   （`1` vs `1.0`、`1e3` vs `1000`）取决于 oracle 的序列化实现，而不是语义。
//   逐字节比字符串会把"实现 A 输出 1、实现 B 输出 1.0"误判成语义差异。
//
// 判定的语义（逐条）：
//   - 对象：键集合相同（与顺序无关），逐键递归相等
//   - 数组：长度与次序相同，逐项递归相等
//   - 数字：**按数值**比较（Double），不是按原始文本
//   - 字符串 / 布尔 / null：直接相等

import Foundation
import PDIGCore

public enum JsonDeepEqual {

    public static func equal(_ a: Json, _ b: Json) -> Bool {
        switch (a, b) {
        case (.null, .null):
            return true
        case (.bool(let x), .bool(let y)):
            return x == y
        case (.str(let x), .str(let y)):
            return x == y
        case (.num(let x), .num(let y)):
            guard let dx = Double(x), let dy = Double(y) else { return false }
            if dx.isNaN || dy.isNaN { return false }
            return dx == dy
        case (.arr(let xs), .arr(let ys)):
            if xs.count != ys.count { return false }
            for i in 0..<xs.count where !equal(xs[i], ys[i]) { return false }
            return true
        case (.obj(let x), .obj(let y)):
            let xk = Set(x.keys)
            let yk = Set(y.keys)
            if xk != yk { return false }
            for k in xk {
                guard let xv = x[k], let yv = y[k] else { return false }
                if !equal(xv, yv) { return false }
            }
            return true
        default:
            return false
        }
    }

    /// 失败时给出最小可读差异（只做一层定位，够定位到字段即可）。
    public static func describeDiff(expected: Json, actual: Json) -> String {
        if equal(expected, actual) { return "" }
        if let e = expected.objectValue, let a = actual.objectValue {
            let ek = Set(e.keys)
            let ak = Set(a.keys)
            let missing = ek.subtracting(ak).sorted()
            let extra = ak.subtracting(ek).sorted()
            var parts: [String] = []
            if !missing.isEmpty { parts.append("missing keys: \(missing.joined(separator: ","))") }
            if !extra.isEmpty { parts.append("unexpected keys: \(extra.joined(separator: ","))") }
            for k in ek.intersection(ak).sorted() {
                if let ev = e[k], let av = a[k], !equal(ev, av) {
                    parts.append("\(k): expected \(JsonWriter.write(ev)) actual \(JsonWriter.write(av))")
                }
            }
            return parts.joined(separator: "; ")
        }
        return "expected \(JsonWriter.write(expected)) actual \(JsonWriter.write(actual))"
    }
}
