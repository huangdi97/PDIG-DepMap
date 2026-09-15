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
    // SQLCipher：源码（net/sqlcipher/database）实现的是 android-database-sqlcipher 的 API
    // （loadLibs(Context)、SQLiteOpenHelper.getWritableDatabase(String password)）。
    // 原先声明的 net.zetetic:sqlcipher-android 使用 net.zetetic.database.sqlcipher 命名空间
    // 且不含口令重载，导致安全层无法编译。此处对齐为源码所实现的制品（同为 SQLCipher 引擎）。
    implementation("net.zetetic:android-database-sqlcipher:4.5.4")
    implementation("androidx.sqlite:sqlite:2.4.0")
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    // golden vector 互操作测试：与 core/tests/golden 的冻结值对比
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.0.0")
}

// ---------------------------------------------------------------------------
// Kotlin 2.0 + AGP 8.5 集成缺口（本工程首次真实构建时实测）：
// `android.sourceSets.test.kotlin.srcDir("../test")` 的编译产物落在
// `build/tmp/kotlin-classes/<variant>/`，该目录会进入 Test 任务的
// `testClassesDirs`（Gradle 能**发现**测试类名），但没有进入运行时 `classpath`，
// 结果是 `java.lang.ClassNotFoundException: ...DepmapContainerV1GoldenTest`。
// 此处显式把对应 variant 的 Kotlin 测试产物目录补进 classpath。
// ---------------------------------------------------------------------------
tasks.withType<Test>().configureEach {
    val variant = name.removePrefix("test").replaceFirstChar { it.lowercase() }
    val kotlinTestClasses = layout.buildDirectory.dir("tmp/kotlin-classes/$variant")
    val kotlinTestCompile = "compile${variant.replaceFirstChar { it.uppercase() }}Kotlin"
    dependsOn(kotlinTestCompile)
    // 在任务真正执行前注入：AGP 会在自身配置回调中重置 Test 任务的 classpath，
    // 仅在配置阶段 += 会被覆盖，故必须在 doFirst 中追加。
    doFirst {
        testClassesDirs += files(kotlinTestClasses)
        classpath += files(kotlinTestClasses)
    }
}
