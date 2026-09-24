// PDIG Desktop root build — plugin versions pinned to the Android build's Kotlin 2.0.0
// and Compose Multiplatform 1.6.11 (matches Kotlin 2.0.0; stable for JDK 21 / Gradle 8.9).
plugins {
    kotlin("jvm") version "2.0.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" apply false
    id("org.jetbrains.compose") version "1.6.11" apply false
}

allprojects {
    group = "com.pdig"
    version = "0.1.2"
}