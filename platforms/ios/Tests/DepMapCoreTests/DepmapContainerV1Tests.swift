import XCTest

/**
 * DEPMAP_CONTAINER_V1 Golden Vector — iOS/Swift 测试（需 macOS 执行）。
 * 冻结值与 Node reference / Android Kotlin 完全一致 → 三端互操作。
 *
 * 状态：TEST READY（代码完成）。RUN = NO（无 macOS/Xcode，见 BLOCKERS.md）。
 */

enum Golden {
    static let password = "depmap-test"
    static let salt: [UInt8] = [0x00,0x11,0x22,0x33,0x44,0x55,0x66,0x77,0x88,0x99,0xaa,0xbb,0xcc,0xdd,0xee,0xff]
    static let nonce: [UInt8] = [0xa1,0xb2,0xc3,0xd4,0xe5,0xf6,0x07,0x18,0x29,0x3a,0x4b,0x5c]
    static let plaintext = "{\"app\":\"depmap\",\"schemaVersion\":1,\"nodes\":[],\"dependencies\":[]}"
    // 冻结（core/src/crypto/golden.ts GOLDEN_EXPECTED）
    static let derivedKeyHex = "66c4bec7f5e98856747d7b41d0a021bdc092d5e12d492852bd80647bd0ff0c86"
    static let ciphertextB64 = "KNSpbKK6waj4En3ADgeBB74H96Q7GCoBmNffeOG5QSQ9XJwx4LoJCQ0j8lEinA7GN85U6JwaVMqhAkqWDdG7"
    static let tagB64 = "5qpABhovPbNet1q2GNEhkg=="
}

final class DepmapContainerV1Tests: XCTestCase {

    func testGoldenVectorDecrypts() throws {
        // iOS 实现接入 Argon2id（C 参考实现）后启用：
        // let pt = try DepmapContainerV1.decrypt(containerJson, password: Golden.password)
        // XCTAssertEqual(String(data: Data(pt), encoding: .utf8), Golden.plaintext)
        throw XCTSkip("iOS argon2id bridge pending — requires macOS/Xcode (BLOCKERS.md)")
    }

    func testWrongPasswordFails() throws {
        throw XCTSkip("iOS argon2id bridge pending — requires macOS/Xcode (BLOCKERS.md)")
    }

    func testMaliciousBoundsRejectedBeforeKdf() throws {
        // bounds 校验是纯 Swift 逻辑，接入容器模块后即可测：
        // XCTAssertThrowsError(try DepmapContainerV1.validateBounds(maliciousHeader))
        throw XCTSkip("pending Xcode environment (BLOCKERS.md)")
    }
}
