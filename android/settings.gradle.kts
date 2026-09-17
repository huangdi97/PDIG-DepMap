pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(org.gradle.api.initialization.resolve.RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "PDIG"

// ---------------------------------------------------------------------------
// Windows 非 ASCII 工程路径的构建适配（仓库内置，**不再依赖仓库外文件**）
//
// 两个实测缺陷，同一个根因（工程路径含非 ASCII）：
//
//  (1) test worker classpath 被破坏
//      Gradle 8.9 在 Windows 上把 test worker 的 classpath 以 **UTF-8** 写进
//      @argfile（%TEMP%\gradle-worker-classpath*.txt），而 JVM launcher 按**平台编码**
//      （本机 GBK）解析该 argfile —— 含非 ASCII 的 classpath 条目全部损坏，表现为
//      :core:test 里每一个测试类都：
//        java.lang.ClassNotFoundException: com.pdig.core.db.JdbcStatement
//      复现结论：命令行 -cp 含中文 → 成功；@argfile UTF-8 含中文 → 失败；GBK → 成功。
//      mklink /J 无效（Gradle 会把 junction 规范化回真实路径）。
//
//  (2) Kotlin 编译器的临时目录不可写
//      编译 Android 源码时 Kotlin 编译器把存活标记写进一个解析为
//      **%WINDIR%\kotlin-compiler-in--<hash>.alive** 的目录，普通用户无写权限：
//        java.nio.file.AccessDeniedException: %WINDIR%\kotlin-compiler-in--….alive
//      （手动运行 `java -XshowSettings:properties` 显示 java.io.tmpdir 正常，
//        只有被 fork 出去的那一层走错；该错误与沙箱无关，也复现于非沙箱运行。）
//
// 统一规避：把 **构建输出目录** 与 **java.io.tmpdir** 都钉到纯 ASCII 路径。
// 触发条件（任一）：
//   - 环境变量 PDIG_ASCII_BUILD_ROOT 显式指定
//   - 工程路径本身含非 ASCII 字符（本仓库即如此）
// 未触发时完全保持 Gradle 默认行为（`android/**/build`），对 ASCII 检出零影响。
//
// 用法：
//   cd android && ./gradlew :core:test              # 自动检测，无需额外参数
//   PDIG_ASCII_BUILD_ROOT=<ASCII_BUILD_ROOT>/pdig-build ./gradlew ...   # 显式指定位置
// ---------------------------------------------------------------------------
val projectPathIsAscii: Boolean = rootDir.absolutePath.all { it.code < 128 }
val explicitAsciiRoot: String? = System.getenv("PDIG_ASCII_BUILD_ROOT")?.takeIf { it.isNotBlank() }
val asciiBuildRoot: String? = explicitAsciiRoot
    ?: if (projectPathIsAscii) {
        null
    } else {
        val home = System.getProperty("user.home")?.replace('\\', '/') ?: "."
        "$home/pdig-build"
    }

if (asciiBuildRoot != null) {
    // (2) 把编译器/工具的临时目录也钉到 ASCII 路径（必须在任务执行前生效）
    val tmp = file("$asciiBuildRoot/tmp").apply { mkdirs() }
    System.setProperty("java.io.tmpdir", tmp.absolutePath)

    // (1) + (2) 构建输出重定向，使所有 fork 出去的 JVM 看到全 ASCII 路径
    gradle.beforeProject {
        val leaf = if (path == ":") "root" else path.trim(':').replace(':', '-')
        layout.buildDirectory.set(file("$asciiBuildRoot/$leaf"))
    }
}

include(":core")
include(":conformance")
include(":app")
