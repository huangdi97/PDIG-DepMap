// PDIG Android — top-level build file
//
// 模块划分（spec §54）：
//  :core         纯 Kotlin JVM 领域层（禁止依赖 Compose / Context / SQLite / Keystore）
//  :conformance  JVM conformance runner（读取 /fixtures，产出真实证据）
//  :app          Jetpack Compose 原生 UI + Android 平台实现（SQLCipher / Keystore / Biometric）
//
// 生产构建完全不依赖 DCloud / uni-app / UTS。

// ---------------------------------------------------------------------------
// JDK 21 前置检查
//
// 为什么放在这里：Gradle 用的是 PATH 上第一个 `java`。本机默认常常是 JDK 8，
// 于是 AGP 报的是间接错误（"Dependency requires at least JVM runtime version 11" /
// "Cannot find a Java installation ... languageVersion=21"），
// 而不是"你的 JDK 版本不对"—— 这个错误的排查成本已经被支付过不止一次。
//
// 这里把它变成一条前置、明确、带修复方法的失败。
// CI 使用 JDK 21，这条检查在 CI 上恒为 true。
// ---------------------------------------------------------------------------
if (!JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_21)) {
    throw GradleException(
        """
        Requires JDK 21.
          detected : ${JavaVersion.current()} (java.home=${System.getProperty("java.home")})
          expected : JDK 21 or newer (CI uses JDK 21)

        Gradle picked up whatever `java` comes first on PATH. Point JAVA_HOME at a JDK 21:
            JAVA_HOME="<JDK21_HOME>" ./gradlew --no-daemon :core:test :conformance:run
        """.trimIndent(),
    )
}

plugins {
    kotlin("jvm") version "2.0.0" apply false
    kotlin("plugin.serialization") version "2.0.0" apply false
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" apply false
}

allprojects {
    group = "com.pdig"
    version = "0.1.0-milestone"
}
