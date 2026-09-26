// FileStore —— 文件存取（应用沙盒内）。
//
// macOS：~/Library/Application Support/PDIG（CI 上即 runner 家目录）。
// iOS：Documents/PDIG。备份导出/导入走同一目录。

import Foundation

public enum FileStore {

    /// App 数据目录。
    public static func dataDirectory() -> URL {
        let base: URL
        #if os(macOS)
        let appSupport = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first
            ?? URL(fileURLWithPath: NSTemporaryDirectory())
        base = appSupport.appendingPathComponent("PDIG", isDirectory: true)
        #else
        base = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
            .appendingPathComponent("PDIG", isDirectory: true)
        #endif
        try? FileManager.default.createDirectory(at: base, withIntermediateDirectories: true)
        return base
    }

    /// 写文本文件（原子）。
    @discardableResult
    public static func write(_ text: String, name: String, directory: URL = dataDirectory()) throws -> URL {
        let url = directory.appendingPathComponent(name)
        try text.write(to: url, atomically: true, encoding: .utf8)
        return url
    }

    /// 读文本文件。
    public static func read(name: String, directory: URL = dataDirectory()) -> String? {
        let url = directory.appendingPathComponent(name)
        return try? String(contentsOf: url, encoding: .utf8)
    }

    /// 列出目录内文件。
    public static func listFiles(directory: URL = dataDirectory()) -> [String] {
        (try? FileManager.default.contentsOfDirectory(atPath: directory.path))?.sorted() ?? []
    }

    /// 删除文件。
    public static func delete(name: String, directory: URL = dataDirectory()) {
        let url = directory.appendingPathComponent(name)
        try? FileManager.default.removeItem(at: url)
    }
}
