// Argon2id —— 用 vendored C 实现（third_party/argon2）填充 PDIGCore 的
// Argon2idProvider。
//
// 边界：PDIGCore 仍然是纯域层（不链接任何 KDF 实现），容器逻辑只依赖
// Argon2idProvider 协议；本模块是**平台安全层**，由调用方注入。
// 这样"换 KDF / 换参数"不会牵动域层，也让 conformance 可以明确看到
// 用的是哪一个实现。

import Foundation
import PDIGArgon2C
import PDIGCore

public struct Argon2Error: Error, CustomStringConvertible, Equatable {
    public let code: Int32
    public init(_ code: Int32) { self.code = code }
    public var description: String { "argon2id failed with code \(code)" }
}

public struct Argon2idNative: Argon2idProvider {

    public init() {}

    public func derive(
        password: [UInt8],
        salt: [UInt8],
        memoryKiB: Int,
        iterations: Int,
        parallelism: Int
    ) throws -> [UInt8] {
        var out = [UInt8](repeating: 0, count: 32)
        let rc = pdig_argon2id(
            password, password.count,
            salt, salt.count,
            UInt32(memoryKiB), UInt32(iterations), UInt32(parallelism),
            &out, out.count
        )
        guard rc == 0 else { throw Argon2Error(rc) }
        return out
    }
}
