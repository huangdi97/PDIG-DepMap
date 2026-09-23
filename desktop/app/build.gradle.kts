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
            packageVersion = "0.1.0"
            description = "PDIG 0.1.0 Developer Preview — 个人数字基础设施图谱 (Windows Desktop)"
            vendor = "PDIG"
            includeAllModules = true
        }
    }
}