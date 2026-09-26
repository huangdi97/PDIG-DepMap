// KeychainStore —— Keychain 封装（Security framework）。
//
// 平台安全层：把「是否已设置解锁 / 随机盐」等状态放进系统 Keychain
// （kSecClassGenericPassword），不落明文文件。
// 诚实边界：Keychain 访问组 / 共享能力需真机验证；macOS CI 只验证编译。

import Foundation
import Security

public struct KeychainError: Error, CustomStringConvertible {
    public let code: String
    public let message: String
    public init(_ code: String, _ message: String) {
        self.code = code
        self.message = message
    }
    public var description: String { message }
}

public enum KeychainStore {

    public static let service = "com.pdig.depmap"

    /// 写入 generic password 项（覆盖写）。
    @discardableResult
    public static func set(_ value: String, key: String) -> Bool {
        let data = Data(value.utf8)
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key,
        ]
        let attributes: [String: Any] = [
            kSecValueData as String: data,
        ]
        let status = SecItemUpdate(query as CFDictionary, attributes as CFDictionary)
        if status == errSecItemNotFound {
            var add = query
            add[kSecValueData as String] = data
            return SecItemAdd(add as CFDictionary, nil) == errSecSuccess
        }
        return status == errSecSuccess
    }

    public static func read(_ key: String) -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne,
        ]
        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        guard status == errSecSuccess, let data = result as? Data else { return nil }
        return String(data: data, encoding: .utf8)
    }

    public static func delete(_ key: String) {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key,
        ]
        SecItemDelete(query as CFDictionary)
    }

    /// 是否已配置过解锁密钥材料（决定 Onboarding / Lock 分流）。
    public static func hasUnlockMaterial() -> Bool {
        read("unlock.salt") != nil
    }
}
