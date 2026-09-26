// BackupViewModel —— 备份/恢复（task #12）。
//
// 使用 PDIGCore：
//  - exportGraph / importGraph（GraphSerialize）→ canonical payload
//  - DepmapContainer.create / openContainer（DEPMAP_CONTAINER_V1，Argon2id + AES-GCM）
//  - Argon2idNative（vendored Argon2id，conformance golden 已验证）
//
// 诚实边界：
//  - 加密容器 = 真实实现（Argon2id + CryptoKit AES-GCM + JCS AAD，golden vector 级）
//  - App 本地数据库落盘 = 系统 sqlite（同 conformance host harness）；
//    SQLCipher at-rest 加密尚未接入 → IOS_SQLCIPHER_PERSISTENCE = NOT_RUN。
//    备份文件本身是加密的，但本地工作库不是。

import Foundation
import PDIGCore
import PDIGArgon2

public enum BackupError: Error, CustomStringConvertible {
    case cannotExport(String)
    case cannotImport(String)
    public var description: String {
        switch self {
        case .cannotExport(let s): return "导出失败：\(s)"
        case .cannotImport(let s): return "恢复失败：\(s)"
        }
    }
}

public enum BackupViewModel {

    public static let exportTitle = CopyZh.backupExportTitle
    public static let importTitle = CopyZh.backupImportTitle

    /// 导出 .depmap 容器 JSON 文本（调用方负责写文件/分享）。
    public static func exportDepmap(
        repository: GraphRepository,
        password: String
    ) throws -> String {
        let snapshot = try repository.loadSnapshot()
        let payload = PayloadCodec.toPayloadJson(snapshot)
        guard let bytes = payload.data(using: .utf8) else {
            throw BackupError.cannotExport("payload 编码失败")
        }
        do {
            let container = try DepmapContainer.create(
                plaintext: [UInt8](bytes),
                password: password,
                kdf: Argon2idNative()
            )
            return container.json
        } catch let e {
            throw BackupError.cannotExport("\(e)")
        }
    }

    /// 从 .depmap 容器文本恢复：口令校验 → payload 迁移/校验 → 原子替换。
    public static func importDepmap(
        repository: GraphRepository,
        containerJson: String,
        password: String
    ) throws -> [String: Int] {
        let plaintext: [UInt8]
        do {
            plaintext = try DepmapContainer.openContainer(json: containerJson, password: password, kdf: Argon2idNative())
        } catch {
            throw BackupError.cannotImport(CopyZh.backupWrongPassword)
        }
        guard let payload = String(bytes: plaintext, encoding: .utf8) else {
            throw BackupError.cannotImport("备份内容不是有效文本")
        }
        do {
            return try repository.replacePayload(payload)
        } catch let e {
            throw BackupError.cannotImport("\(e)")
        }
    }

    /// 删除所有数据（task #13，UI 必须显式确认后调用）。
    public static func deleteAllData(repository: GraphRepository) throws {
        try repository.deleteAllData()
    }
}
