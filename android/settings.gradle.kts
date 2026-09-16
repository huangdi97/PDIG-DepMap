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
// Windows 非 ASCII 工程路径的构建输出重定向（环境适配，默认不生效）
//
// 缺陷（已在 Windows 10 26200 + 中文 ANSI 代码页 GBK 上最小复现）：
//   Gradle 8.9 在 Windows 上把 test worker 的 classpath 以 **UTF-8** 写进
//   @argfile（%TEMP%\gradle-worker-classpath*.txt），而 JVM launcher 按**平台编码**
//   （本机 GBK）解析该 argfile —— 所有含非 ASCII 字符的 classpath 条目被破坏，
//   表现为 :core:test 里每一个测试类都 ClassNotFoundException：
//
//     java.lang.ClassNotFoundException: com.pdig.core.db.JdbcStatement
//
//   复现结论：命令行 -cp 含中文 → 成功；@argfile UTF-8 含中文 → 失败；
//   @argfile GBK 含中文 → 成功。mklink /J 无效（Gradle 会把 junction 规范化回真实路径）。
//
// 规避：把 **build 输出目录**重定向到纯 ASCII 路径，使 test worker classpath 全 ASCII。
//   源码与产物路径以外的构建脚本零改动。
//
// 触发方式（opt-in，必须显式设置环境变量）：
//   export PDIG_ASCII_BUILD_ROOT=C:/Users/<you>/pdig-build
//   cd android && ./gradlew :core:test
//
// 未设置时使用默认的 android/**/build（纯 ASCII 检出环境下本就正常）。
// 这段逻辑原先只存在于机器本地 init script，现已内置，构建不再依赖仓库外文件。
// ---------------------------------------------------------------------------
val asciiBuildRoot: String? = System.getenv("PDIG_ASCII_BUILD_ROOT")
if (!asciiBuildRoot.isNullOrBlank()) {
    gradle.beforeProject {
        val leaf = if (path == ":") "root" else path.trim(':').replace(':', '-')
        layout.buildDirectory.set(file("$asciiBuildRoot/$leaf"))
    }
}

include(":core")
include(":conformance")
include(":app")
