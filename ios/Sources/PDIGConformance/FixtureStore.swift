// canonical fixture 定位与加载。
//
// fixture 是**冻结契约**（conformance/CONFORMANCE_MANIFEST.json 的 $comment：
// 同一输入的 expected 必须被 Kotlin/Swift/ArkTS 实现逐字节复现）。
// 因此本模块从仓库直接读冻结原件，**不在 Swift 包里内嵌副本** ——
// 内嵌副本会漂移，而漂移后的"验证"是在拿变质的数据做断言。

import Foundation
import PDIGCore

public struct FixtureEntry: Equatable, Sendable {
    public let id: String
    public let category: String
    public let path: String
    public let sha256: String
}

public struct FixtureStore {
    public let root: String

    /// 从当前工作目录向上找 `conformance/CONFORMANCE_MANIFEST.json`。
    /// SwiftPM 跑测试时 cwd 是包目录，往上两级即仓库根；逐级上溯比写死层数稳。
    public static func locate() throws -> FixtureStore {
        var dir = FileManager.default.currentDirectoryPath
        for _ in 0..<8 {
            let candidate = (dir as NSString).appendingPathComponent("conformance/CONFORMANCE_MANIFEST.json")
            if FileManager.default.fileExists(atPath: candidate) {
                return FixtureStore(root: dir)
            }
            dir = (dir as NSString).deletingLastPathComponent
            if dir == "/" || dir.isEmpty { break }
        }
        throw FixtureError("cannot locate repo root (conformance/CONFORMANCE_MANIFEST.json not found)")
    }

    public func read(_ relativePath: String) throws -> String {
        let p = (root as NSString).appendingPathComponent(relativePath)
        guard let data = FileManager.default.contents(atPath: p) else {
            throw FixtureError("fixture file missing: \(relativePath)")
        }
        return String(decoding: data, as: UTF8.self)
    }

    public func manifest() throws -> [FixtureEntry] {
        let text = try read("conformance/CONFORMANCE_MANIFEST.json")
        let parsed = try JsonParser.parse(text)
        guard let obj = parsed.objectValue, let arr = obj["fixtures"]?.arrayValue else {
            throw FixtureError("manifest has no fixtures array")
        }
        return arr.compactMap { item -> FixtureEntry? in
            guard let o = item.objectValue,
                  let id = o["id"]?.stringValue,
                  let cat = o["category"]?.stringValue,
                  let path = o["path"]?.stringValue else { return nil }
            let sha = o["sha256"]?.stringValue ?? ""
            return FixtureEntry(id: id, category: cat, path: path, sha256: sha)
        }
    }

    public func fixture(_ entry: FixtureEntry) throws -> JsonObject {
        let text = try read(entry.path)
        let parsed = try JsonParser.parse(text)
        guard let o = parsed.objectValue else {
            throw FixtureError("fixture \(entry.id) is not a JSON object")
        }
        return o
    }
}

public struct FixtureError: Error, CustomStringConvertible, Equatable {
    public let message: String
    public init(_ message: String) { self.message = message }
    public var description: String { message }
}
