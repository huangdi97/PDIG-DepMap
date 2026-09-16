// PDIG Android Conformance Runner（JVM 可执行，产出真实证据）
//
// 读取仓库根的 fixtures/ 与 conformance/CONFORMANCE_MANIFEST.json，
// 用 Android（Kotlin）实现逐用例计算，与冻结期望值做**逐字段**比较，
// 结果写入 conformance/reports/android.json。
//
// 运行：
//   ./gradlew :conformance:run --args="<repoRoot>"
// 或（本地 Gradle）：
//   gradle --no-daemon :conformance:run --args="<repo>"

plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":core"))
    // 仅 conformance harness 在 JVM 上使用；Android App 使用平台 SQLite / SQLCipher。
    implementation("org.xerial:sqlite-jdbc:3.46.1.0")
}

application {
    mainClass.set("com.pdig.conformance.MainKt")
}

kotlin {
    jvmToolchain(21)
}

tasks.named<JavaExec>("run") {
    // rootProject 是 android/，仓库根在其上一级（android → 仓库根）。
    // 曾误写为 "../../"（按 conformance 模块推算），导致默认根偏到 <repo_parent> 而 FATAL。
    val defaultRoot = rootProject.layout.projectDirectory.dir("..").asFile.absolutePath
    if (args == null || args!!.isEmpty()) {
        args(defaultRoot)
    }
    standardOutput = System.out
}
