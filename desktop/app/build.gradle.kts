// PDIG Desktop app — Kotlin/JVM + Compose Desktop.
// Reuses :core (pure JVM domain) and :conformance (JdbcSqliteDriver) unchanged;
// Desktop is NOT a canonical truth source.
plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":conformance"))
     implementation(project(":repos"))

    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    // Argon2id / AES-GCM (same lib as :core, declared explicitly for the runtime classpath)
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
    // Windows DPAPI (DesktopSecurityPort) — JNA platform bindings
    implementation("net.java.dev.jna:jna:5.14.0")
    implementation("net.java.dev.jna:jna-platform:5.14.0")
    // slf4j-nop：消除 sqlite-jdbc 的 StaticLoggerBinder 噪音（运行时与测试输出保持干净）。
    runtimeOnly("org.slf4j:slf4j-nop:1.7.36")

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

compose.desktop {
    application {
        mainClass = "com.pdig.desktop.MainKt"
        nativeDistributions {
            packageName = "PDIG"
            packageVersion = "0.3.0"
            description = "PDIG 0.3.0 Preview — 个人数字依赖图 (Windows Desktop)"
            vendor = "PDIG"
            includeAllModules = true
        }
    }
}
 
 // ---------------------------------------------------------------------------
 // 手动打包辅助：把 runtime classpath 与主 jar 收集到一个目录，供 jpackage 直接使用
 // （绕过 Compose downloadWix：Windows 安装器由 NSIS 产出，jpackage 只做 app-image）。
 // ---------------------------------------------------------------------------
 tasks.register<Copy>("collectRuntimeForJpackage") {
     dependsOn("jar")
     val libs = layout.buildDirectory.dir("jpackage-libs")
     into(libs)
     from(configurations.runtimeClasspath)
     from(tasks.named<Jar>("jar").map { it.archiveFile })
 }