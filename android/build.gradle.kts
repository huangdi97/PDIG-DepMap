// PDIG Android — top-level build file
//
// 模块划分（spec §54）：
//  :core         纯 Kotlin JVM 领域层（禁止依赖 Compose / Context / SQLite / Keystore）
//  :conformance  JVM conformance runner（读取 /fixtures，产出真实证据）
//  :app          Jetpack Compose 原生 UI + Android 平台实现（SQLCipher / Keystore / Biometric）
//
// 生产构建完全不依赖 DCloud / uni-app / UTS。

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
