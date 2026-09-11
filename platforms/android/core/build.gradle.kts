plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "com.depmap.core"
    compileSdk = 34

    // 共享 Kotlin 源码位于 ../kotlin（platforms/android/kotlin/com/depmap/...）
    sourceSets {
        getByName("main") {
            kotlin.srcDir("../kotlin")
        }
        getByName("test") {
            kotlin.srcDir("../test")
        }
    }

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

    testOptions {
        unitTests.isIncludeAndroidResources = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.fragment:fragment-ktx:1.8.2")
    implementation("net.zetetic:sqlcipher-android:4.6.1")
    implementation("androidx.sqlite:sqlite:2.4.0")
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    // golden vector 互操作测试：与 core/tests/golden 的冻结值对比
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.0.0")
}
