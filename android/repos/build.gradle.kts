// PDIG 共享 Repository 层（纯 Kotlin JVM）—— 从 android/app 提出，Android 与 Desktop 共用。
//
// 内容：Graph / Proposal / Candidate / Drift / Plan / Source 六个 repository + 共享 models/helpers。
// 约束（与 :core 相同）：不得引入 Compose / Android Context / MediaStore / Keystore。
// BackupRepository 因 MediaStore 绑定留在 :app；Desktop 用自己的文件层（:core export/import + container）。
//
// 包名保持 com.pdig.app.data（为避免改 UI/AppContainer 的 import；跨模块同包在 Gradle classpath 模型下合法）。
plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":core"))
    // SourceRepository 的 parseFile/previewImport/commitImport 使用 Dispatchers.IO
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("passed", "failed", "skipped")
        showStandardStreams = false
    }
}