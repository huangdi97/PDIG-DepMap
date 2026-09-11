// DepMap Android native core — 可独立构建的最小 Gradle 工程。
// 目的：在拿到 JDK17 + Android SDK（见 BLOCKERS.md）后，无需 HBuilderX 即可
// 先验证 Kotlin 安全核心（Schema DDL、SQLCipher 适配、Keystore、.depmap golden vector）。
//
// 状态：工程文件 IMPLEMENTED。构建验证 = 未执行（本机无 JDK17/SDK）。
plugins {
    id("com.android.library") version "8.5.2" apply false
    kotlin("android") version "2.0.0" apply false
}

subprojects {
    repositories {
        google()
        mavenCentral()
    }
}
