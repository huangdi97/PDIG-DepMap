// PDIG core — 纯 Kotlin JVM 领域层。
//
// 硬约束（spec/domain/entities.md + AGENTS §55）：
//   本模块**不得**引入 Compose / Android Context / SQLite / Keystore。
//   因此它是 JVM 模块，可以在普通 `gradle :core:compileKotlin` 下编译，
//   也能被 conformance runner 直接执行，产生真实证据而非声称。
//
// 依赖策略（spec/security §10 优先平台 SDK / 避免大型框架）：
//   - BouncyCastle 仅用于 Argon2id，属于成熟可审计实现
//   - JSON 由本模块自带严格解析器（com.pdig.core.json），避免第三方解析差异影响 conformance

plugins {
    kotlin("jvm")
}

dependencies {
    // Argon2id（DEPMAP_CONTAINER_V1 KDF）。AES-GCM / Base64 / SecureRandom 用 JDK。
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")

    // JVM 单元测试要让 `./gradlew :core:test` 产生真实用例，而不是 NO-SOURCE。
    // sqlite-jdbc 与 :conformance 同版本（已在缓存中），用于跑真实 SQLite 上的迁移语义测试。
    testImplementation("org.xerial:sqlite-jdbc:3.46.1.0")
}

// JDK 21（Android Studio JBR）；用 toolchain 统一 Java/Kotlin 目标，避免
// "Inconsistent JVM-target compatibility"。
kotlin {
    jvmToolchain(21)
}

tasks.withType<Test>().configureEach {
    // 只声明 kotlin("test") 时在 JVM 上没有绑定的测试框架，
    // Gradle 会用默认 JUnit4 runner 加载类，导致 ClassNotFoundException（全部 initializationError）。
    // 这里显式启用 JUnit Platform。
    useJUnitPlatform()
    testLogging {
        events("passed", "failed", "skipped")
        showStandardStreams = false
    }
}
