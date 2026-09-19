// DEPMAP_CONTAINER_V1 —— 口令派生文件加密，Swift 移植（协议层）。
//
// 源头：android/core/.../crypto/DepmapContainer.kt（Android 已 CORE_FROZEN）。
//
// 协议**冻结**（spec/security/depmap-container-v1.json）。本文件只做协议 glue：
//  - AES-256-GCM 使用 CryptoKit（**禁止手写密码学原语**，spec/security §3）
//  - Argon2id 由调用方注入 `Argon2idProvider`（iOS 侧原生实现属平台安全层，
//    未接入本包前相关用例如实记 BLOCKED，不伪造通过）
//  - CSPRNG 使用 SystemRandomNumberGenerator
//
// 常量全部来自 codegen 的 `DepmapContainerV1`，避免三端各自硬编码导致漂移。
//
// 解密顺序固定：parse → structure → bounds → Argon2id → GCM auth。
// 认证前 header 一律不可信；bounds 必须在 KDF 之前完成
// （恶意容器不得触发超大内存 KDF）。

import Foundation
import CryptoKit

public struct DepmapException: Error, CustomStringConvertible, Equatable {
    public let code: String
    public let message: String
    public init(_ code: String, _ message: String) {
        self.code = code
        self.message = message
    }
    public var description: String { message }
}

/// Argon2id 提供者。iOS 侧原生实现由平台安全层提供（尚未接入本包）。
public protocol Argon2idProvider {
    /// password 使用精确 UTF-8 字节，**不做** Unicode 归一化（spec §38 / DEP-03）。
    func derive(
        password: [UInt8],
        salt: [UInt8],
        memoryKiB: Int,
        iterations: Int,
        parallelism: Int
    ) throws -> [UInt8]
}

public enum DepmapContainer {

    public struct Header: Equatable, Sendable {
        public let format: String
        public let formatVersion: Int64
        public let kdfAlgorithm: String
        public let kdfVersion: Int64
        public let saltB64: String
        public let memoryKiB: Int64
        public let iterations: Int64
        public let parallelism: Int64
        public let cipherAlgorithm: String
        public let nonceB64: String
        public let ciphertextB64: String
        public let tagB64: String

        public init(
            format: String, formatVersion: Int64, kdfAlgorithm: String, kdfVersion: Int64,
            saltB64: String, memoryKiB: Int64, iterations: Int64, parallelism: Int64,
            cipherAlgorithm: String, nonceB64: String, ciphertextB64: String, tagB64: String
        ) {
            self.format = format
            self.formatVersion = formatVersion
            self.kdfAlgorithm = kdfAlgorithm
            self.kdfVersion = kdfVersion
            self.saltB64 = saltB64
            self.memoryKiB = memoryKiB
            self.iterations = iterations
            self.parallelism = parallelism
            self.cipherAlgorithm = cipherAlgorithm
            self.nonceB64 = nonceB64
            self.ciphertextB64 = ciphertextB64
            self.tagB64 = tagB64
        }

        public func copy(ciphertextB64: String? = nil, tagB64: String? = nil) -> Header {
            Header(
                format: format, formatVersion: formatVersion, kdfAlgorithm: kdfAlgorithm,
                kdfVersion: kdfVersion, saltB64: saltB64, memoryKiB: memoryKiB,
                iterations: iterations, parallelism: parallelism,
                cipherAlgorithm: cipherAlgorithm, nonceB64: nonceB64,
                ciphertextB64: ciphertextB64 ?? self.ciphertextB64,
                tagB64: tagB64 ?? self.tagB64
            )
        }
    }

    public struct ContainerResult: Equatable, Sendable {
        public let json: String
        public let header: Header
        public let derivedKeyHex: String
        public init(json: String, header: Header, derivedKeyHex: String) {
            self.json = json
            self.header = header
            self.derivedKeyHex = derivedKeyHex
        }
    }

    // --- base64 (RFC 4648 standard WITH padding) -----------------------------

    public static func toBase64(_ bytes: [UInt8]) -> String { Data(bytes).base64EncodedString() }

    /// 严格 base64：仅接受标准字母表 + 可选填充。非法字符抛 invalid_structure。
    /// 与 Java `Base64.getDecoder()` 的宽容度对齐：允许省略末尾填充。
    public static func fromBase64(_ s: String) throws -> [UInt8] {
        let allowed = Set("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=")
        for ch in s where !allowed.contains(ch) {
            throw DepmapException("invalid_structure", "field is not valid base64")
        }
        var t = s
        let rem = t.count % 4
        if rem != 0 { t += String(repeating: "=", count: 4 - rem) }
        guard let data = Data(base64Encoded: t) else {
            throw DepmapException("invalid_structure", "field is not valid base64")
        }
        return [UInt8](data)
    }

    public static func toHex(_ bytes: [UInt8]) -> String {
        bytes.map { String(format: "%02x", $0) }.joined()
    }

    // --- AAD ---------------------------------------------------------------

    public static func computeAad(_ h: Header) throws -> [UInt8] {
        let aad = Json.obj(JsonObject([
            ("format", .str(h.format)),
            ("formatVersion", .num(String(h.formatVersion))),
            ("kdf", .obj(JsonObject([
                ("algorithm", .str(h.kdfAlgorithm)),
                ("version", .num(String(h.kdfVersion))),
                ("salt", .str(h.saltB64)),
                ("memoryKiB", .num(String(h.memoryKiB))),
                ("iterations", .num(String(h.iterations))),
                ("parallelism", .num(String(h.parallelism))),
            ]))),
            ("cipher", .obj(JsonObject([
                ("algorithm", .str(h.cipherAlgorithm)),
                ("nonce", .str(h.nonceB64)),
            ]))),
        ]))
        let s = try Jcs.stringify(aad)
        return Array(s.utf8)
    }

    // --- 解析 / 结构 / 边界 --------------------------------------------------

    public static func parseHeader(_ json: String) throws -> Header {
        let raw: Json
        do {
            raw = try JsonParser.parse(json)
        } catch {
            throw DepmapException("invalid_json", "container is not valid JSON")
        }
        guard let o = raw.objectValue else {
            throw DepmapException("invalid_structure", "container must be a JSON object")
        }
        let kdf = o["kdf"]
        let cipher = o["cipher"]
        guard let kdfObj = kdf?.objectValue, let cipherObj = cipher?.objectValue else {
            throw DepmapException("invalid_structure", "kdf/cipher must be objects")
        }
        return Header(
            format: try str(o, "format"),
            formatVersion: try int(o, "formatVersion"),
            kdfAlgorithm: try str(kdfObj, "algorithm"),
            kdfVersion: try int(kdfObj, "version"),
            saltB64: try str(kdfObj, "salt"),
            memoryKiB: try int(kdfObj, "memoryKiB"),
            iterations: try int(kdfObj, "iterations"),
            parallelism: try int(kdfObj, "parallelism"),
            cipherAlgorithm: try str(cipherObj, "algorithm"),
            nonceB64: try str(cipherObj, "nonce"),
            ciphertextB64: try str(o, "ciphertext"),
            tagB64: try str(o, "tag")
        )
    }

    private static func str(_ o: JsonObject, _ key: String) throws -> String {
        guard let v = o[key]?.stringValue else {
            throw DepmapException("invalid_structure", "header field \(key) must be a string")
        }
        return v
    }

    private static func int(_ o: JsonObject, _ key: String) throws -> Int64 {
        guard let v = o[key], case .num(let raw) = v else {
            throw DepmapException("invalid_structure", "header field \(key) must be an integer")
        }
        guard Json.num(raw).isSafeInteger else {
            throw DepmapException("invalid_structure", "header field \(key) must be an integer")
        }
        return try Json.num(raw).asLong
    }

    public static func validateBounds(_ h: Header) throws {
        func fail(_ m: String) throws -> Never {
            throw DepmapException("bounds", m)
        }
        if h.format != DepmapContainerV1.format {
            try fail("format must be \"\(DepmapContainerV1.format)\"")
        }
        if h.formatVersion != Int64(DepmapContainerV1.formatVersion) {
            try fail("formatVersion must be 1")
        }
        if h.kdfAlgorithm != DepmapContainerV1.kdfAlgorithm { try fail("kdf.algorithm must be argon2id") }
        if h.kdfVersion != Int64(DepmapContainerV1.kdfVersion) { try fail("kdf.version must be 19") }
        if h.memoryKiB < Int64(DepmapContainerV1.memoryKiBMin) ||
            h.memoryKiB > Int64(DepmapContainerV1.memoryKiBMax) {
            try fail("kdf.memoryKiB out of bounds [\(DepmapContainerV1.memoryKiBMin),\(DepmapContainerV1.memoryKiBMax)]")
        }
        if h.iterations < Int64(DepmapContainerV1.iterationsMin) ||
            h.iterations > Int64(DepmapContainerV1.iterationsMax) {
            try fail("kdf.iterations out of bounds [\(DepmapContainerV1.iterationsMin),\(DepmapContainerV1.iterationsMax)]")
        }
        if h.parallelism < Int64(DepmapContainerV1.parallelismMin) ||
            h.parallelism > Int64(DepmapContainerV1.parallelismMax) {
            try fail("kdf.parallelism out of bounds [\(DepmapContainerV1.parallelismMin),\(DepmapContainerV1.parallelismMax)]")
        }
        if try fromBase64(h.saltB64).count != DepmapContainerV1.saltBytes {
            try fail("kdf.salt must decode to 16 bytes")
        }
        if try fromBase64(h.nonceB64).count != DepmapContainerV1.nonceBytes {
            try fail("cipher.nonce must decode to 12 bytes")
        }
        if try fromBase64(h.tagB64).count != DepmapContainerV1.tagBytes {
            try fail("tag must decode to 16 bytes")
        }
        let ct = try fromBase64(h.ciphertextB64)
        if ct.isEmpty { try fail("ciphertext must not be empty") }
        if ct.count > DepmapContainerV1.ciphertextMaxBytes { try fail("ciphertext exceeds 64 MiB limit") }
        if h.cipherAlgorithm != DepmapContainerV1.cipherAlgorithm {
            try fail("cipher.algorithm must be AES-256-GCM")
        }
    }

    // --- 创建 / 打开 ---------------------------------------------------------

    public struct CreateOptions {
        public let salt: [UInt8]?
        public let nonce: [UInt8]?
        public let memoryKiB: Int
        public let iterations: Int
        public let parallelism: Int
        public init(
            salt: [UInt8]? = nil, nonce: [UInt8]? = nil,
            memoryKiB: Int = DepmapContainerV1.memoryKiB,
            iterations: Int = DepmapContainerV1.iterations,
            parallelism: Int = DepmapContainerV1.parallelism
        ) {
            self.salt = salt
            self.nonce = nonce
            self.memoryKiB = memoryKiB
            self.iterations = iterations
            self.parallelism = parallelism
        }
    }

    public static func create(
        plaintext: [UInt8],
        password: String,
        kdf: Argon2idProvider,
        opts: CreateOptions = CreateOptions()
    ) throws -> ContainerResult {
        let salt = opts.salt ?? randomBytes(DepmapContainerV1.saltBytes)
        let nonce = opts.nonce ?? randomBytes(DepmapContainerV1.nonceBytes)
        guard salt.count == DepmapContainerV1.saltBytes else {
            throw DepmapException("invalid_structure", "salt must be 16 bytes")
        }
        guard nonce.count == DepmapContainerV1.nonceBytes else {
            throw DepmapException("invalid_structure", "nonce must be 12 bytes")
        }

        let key = try kdf.derive(
            password: Array(password.utf8),
            salt: salt,
            memoryKiB: opts.memoryKiB,
            iterations: opts.iterations,
            parallelism: opts.parallelism
        )

        let base = Header(
            format: DepmapContainerV1.format,
            formatVersion: Int64(DepmapContainerV1.formatVersion),
            kdfAlgorithm: DepmapContainerV1.kdfAlgorithm,
            kdfVersion: Int64(DepmapContainerV1.kdfVersion),
            saltB64: toBase64(salt),
            memoryKiB: Int64(opts.memoryKiB),
            iterations: Int64(opts.iterations),
            parallelism: Int64(opts.parallelism),
            cipherAlgorithm: DepmapContainerV1.cipherAlgorithm,
            nonceB64: toBase64(nonce),
            ciphertextB64: "",
            tagB64: ""
        )

        let aad = try computeAad(base)
        let sealed = try aesGcmSeal(
            plaintext: plaintext, key: key, nonce: nonce, aad: aad
        )
        let ct = Array(sealed.ciphertext)
        let tag = Array(sealed.tag)

        let full = base.copy(ciphertextB64: toBase64(ct), tagB64: toBase64(tag))
        // 容器本身也用 JCS 序列化 → deterministic 输出
        return ContainerResult(
            json: try Jcs.stringify(toJson(full)),
            header: full,
            derivedKeyHex: toHex(key)
        )
    }

    public static func openContainer(
        json: String, password: String, kdf: Argon2idProvider
    ) throws -> [UInt8] {
        let header = try parseHeader(json)
        try validateBounds(header)   // KDF 之前完成全部边界检查
        let key = try kdf.derive(
            password: Array(password.utf8),
            salt: try fromBase64(header.saltB64),
            memoryKiB: Int(header.memoryKiB),
            iterations: Int(header.iterations),
            parallelism: Int(header.parallelism)
        )
        let aad = try computeAad(header)
        let ct = try fromBase64(header.ciphertextB64)
        let tag = try fromBase64(header.tagB64)
        return try aesGcmOpen(
            ciphertext: ct, tag: tag, key: key, nonce: try fromBase64(header.nonceB64), aad: aad
        )
    }

    // --- AES-256-GCM（CryptoKit；禁止手写原语） --------------------------------

    private static func aesGcmSeal(
        plaintext: [UInt8], key: [UInt8], nonce: [UInt8], aad: [UInt8]
    ) throws -> AES.GCM.SealedBox {
        let sym = SymmetricKey(data: key)
        let box: AES.GCM.SealedBox
        do {
            box = try AES.GCM.seal(
                plaintext, using: sym,
                nonce: try AES.GCM.Nonce(data: nonce),
                authenticating: aad
            )
        } catch {
            throw DepmapException("auth_failed", "encryption failed: \(error)")
        }
        return box
    }

    private static func aesGcmOpen(
        ciphertext: [UInt8], tag: [UInt8], key: [UInt8], nonce: [UInt8], aad: [UInt8]
    ) throws -> [UInt8] {
        do {
            let box = try AES.GCM.SealedBox(
                nonce: AES.GCM.Nonce(data: nonce),
                ciphertext: ciphertext,
                tag: tag
            )
            let opened = try AES.GCM.open(box, using: SymmetricKey(data: key), authenticating: aad)
            return Array(opened)
        } catch {
            throw DepmapException(
                "auth_failed",
                "decryption authentication failed (wrong password or tampered container)"
            )
        }
    }

    // --- JSON ----------------------------------------------------------------

    public static func toJson(_ h: Header) -> Json {
        .obj(JsonObject([
            ("format", .str(h.format)),
            ("formatVersion", .num(String(h.formatVersion))),
            ("kdf", .obj(JsonObject([
                ("algorithm", .str(h.kdfAlgorithm)),
                ("version", .num(String(h.kdfVersion))),
                ("salt", .str(h.saltB64)),
                ("memoryKiB", .num(String(h.memoryKiB))),
                ("iterations", .num(String(h.iterations))),
                ("parallelism", .num(String(h.parallelism))),
            ]))),
            ("cipher", .obj(JsonObject([
                ("algorithm", .str(h.cipherAlgorithm)),
                ("nonce", .str(h.nonceB64)),
            ]))),
            ("ciphertext", .str(h.ciphertextB64)),
            ("tag", .str(h.tagB64)),
        ]))
    }

    public static func headerToPrettyJson(_ h: Header) -> String { JsonWriter.write(toJson(h)) }

    public static func randomBytes(_ n: Int) -> [UInt8] {
        var out = [UInt8](repeating: 0, count: n)
        for i in 0..<n { out[i] = UInt8.random(in: 0...255) }
        return out
    }
}
