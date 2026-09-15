// DepMap Android native core — 可独立构建的最小 Gradle 工程。
// 目的：无需 HBuilderX 即可先验证 Kotlin 安全核心
// （Schema DDL、SQLCipher 适配、Keystore、.depmap golden vector）。
//
// 状态：工程文件 IMPLEMENTED。构建验证 = 已执行（JDK 21 + Android SDK 34 + Gradle 8.9）。
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

// ---------------------------------------------------------------------------
// 非 ASCII 工程路径回退
//
// 根因（实测取证，非推测）：
//   本工程位于 `<repo>\...`（含非 ASCII）。当 Test 任务的 classpath 超长时，
//   Gradle 会把 JVM 参数写入 `~/.gradle/.tmp/gradle-worker-classpath*.txt`（argfile），
//   文件内容以 **UTF-8** 编码（实测 hex `e5 8f b7 e5 8d a1` = "号卡"），
//   而 JVM 解析 argfile 使用 `sun.jnu.encoding`（Windows 中文环境 = GBK）。
//   两侧编码不一致 → classpath 中的中文路径 mojibake → 测试 worker 报
//   `java.lang.ClassNotFoundException: com.depmap.core.crypto.DepmapContainerV1GoldenTest`
//   （daemon 侧用 URLClassLoader 手工加载同一 classpath 却成功，可排除源码/类本身问题）。
//
// 处置：classpath 中的非 ASCII 条目全部来自 `build/` 产物目录（实测 5 条）。
//   因此当工程路径含非 ASCII 字符时，把 build 目录整体重定向到 ASCII 路径，
//   classpath 即不再出现非 ASCII 条目，问题从根上消失。
//   可用环境变量 DEPMAP_ANDROID_BUILD_ROOT 覆盖回退位置。
// ---------------------------------------------------------------------------
val asciiBuildRoot: String? = run {
    val root = rootDir.absolutePath
    if (!root.any { it.code > 127 }) return@run null
    System.getenv("DEPMAP_ANDROID_BUILD_ROOT")
        ?: File(System.getProperty("user.home"), "depmap-android-build").absolutePath
}

if (asciiBuildRoot != null) {
    logger.lifecycle("[depmap] 工程路径含非 ASCII 字符，build 目录回退到: $asciiBuildRoot")
    allprojects {
        layout.buildDirectory.set(file("$asciiBuildRoot/${project.name}"))
    }

    // 把产物回收到仓库内（便于交付、哈希校验与留档）。
    tasks.register<Copy>("collectArtifacts") {
        group = "depmap"
        description = "收集 AAR 到 platforms/android/artifacts/"
        dependsOn(":core:assembleDebug", ":core:assembleRelease")
        from("$asciiBuildRoot/core/outputs/aar") { include("*.aar") }
        into(layout.projectDirectory.dir("artifacts"))
    }
}
