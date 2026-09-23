// PDIG Desktop build root — reuses android/:core (pure JVM domain) and
// android/:conformance (JdbcSqliteDriver) unchanged.
//
// Non-ASCII repo path workaround (same root cause set as android/settings.gradle.kts):
// redirect build outputs + java.io.tmpdir to a pure-ASCII root so test workers
// and the Kotlin compiler don't see mangled classpath entries.
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(org.gradle.api.initialization.resolve.RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google()
    }
}

rootProject.name = "PDIGDesktop"

val projectPathIsAscii: Boolean = rootDir.absolutePath.all { it.code < 128 }
val explicitAsciiRoot: String? = System.getenv("PDIG_DESKTOP_ASCII_BUILD_ROOT")?.takeIf { it.isNotBlank() }
val asciiBuildRoot: String? = explicitAsciiRoot
    ?: if (projectPathIsAscii) null
    else {
        val home = System.getProperty("user.home")?.replace('\\', '/') ?: "."
        "$home/pdig-desktop-build"
    }

if (asciiBuildRoot != null) {
    val tmp = file("$asciiBuildRoot/tmp").apply { mkdirs() }
    System.setProperty("java.io.tmpdir", tmp.absolutePath)
    gradle.beforeProject {
        val leaf = if (path == ":") "root" else path.trim(':').replace(':', '-')
        layout.buildDirectory.set(file("$asciiBuildRoot/$leaf"))
    }
}

include(":core")
project(":core").projectDir = file("../android/core")
include(":conformance")
project(":conformance").projectDir = file("../android/conformance")
include(":app")
 include(":repos")
 project(":repos").projectDir = file("../android/repos")