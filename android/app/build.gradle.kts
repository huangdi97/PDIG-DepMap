// PDIG Android 原生 App —— Jetpack Compose / Material 3 / Navigation Compose。
//
// 依赖策略（spec §238）：优先平台 SDK，避免大型第三方框架。
//  - 无网络请求库（产品无业务网络）
//  - 无 analytics / crash SDK
//  - SQLCipher 用于本地库加密（持久化安全要求）

plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.pdig.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.pdig.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0-milestone"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // ⚠️ 非生产测试签名（NON_PRODUCTION_TEST_SIGNING）
    // 用途只有一个：验证 release 签名流水线本身可用。
    // 它**不是** production signing；生产签名必须使用用户提供的真实 keystore。
    signingConfigs {
        create("nonProductionTest") {
            storeFile = rootProject.file("../local_private/build-chain/pdig-nonprod.jks")
            storePassword = "<REDACTED_NONPROD_TEST_SECRET>"
            keyAlias = "<REDACTED_NONPROD_TEST_SECRET>"
            keyPassword = "<REDACTED_NONPROD_TEST_SECRET>"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // 默认 release **保持未签名**。
            // 只有显式传入 -PpdigNonProdSigning=true 且本地测试 keystore 存在时才套用，
            // 防止把测试密钥或 debug 密钥冒充生产签名。
            val nonProdKs = rootProject.file("../local_private/build-chain/pdig-nonprod.jks")
            if (project.hasProperty("pdigNonProdSigning") && nonProdKs.exists()) {
                signingConfig = signingConfigs.getByName("nonProductionTest")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":core"))

    val composeBom = platform("androidx.compose:compose-bom:2024.09.02")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.navigation:navigation-compose:2.8.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.5")
    implementation("androidx.biometric:biometric:1.1.0")
    // ⚠ 显式钉住 androidx.fragment 的版本，**不能不写**。
    //
    // `androidx.biometric:biometric:1.1.0` 传递依赖的是 `androidx.fragment:fragment:1.2.5`（2020 年），
    // 而本模块的 `androidx.activity` 是 1.9.1（2024 年）。两代混用会在真机上直接崩溃：
    //
    //   java.lang.IllegalArgumentException: Can only use lower 16 bits for requestCode
    //     at androidx.fragment.app.FragmentActivity.checkForValidRequestCode(FragmentActivity.java:714)
    //     at androidx.fragment.app.FragmentActivity.startActivityForResult(FragmentActivity.java:672)
    //     at ... ImportScreen 的 picker.launch("*/*")
    //
    // 机制：`ActivityResultRegistry` 自动生成的 requestCode 由 activity 1.9.x 决定，
    // 而 `FragmentActivity`（1.2.5）对 requestCode 强制 16 位上限。
    // 因为 `MainActivity` 必须是 FragmentActivity（BiometricPrompt 的硬要求），
    // 这条路径才会被走到 —— 即"App Lock 接线"与"文件选择器"通过宿主基类耦合在一起。
    // 升级到与 activity 同代的 fragment 即可消除。
    implementation("androidx.fragment:fragment:1.7.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.sqlite:sqlite-framework:2.4.0")
    implementation("net.zetetic:sqlcipher-android:4.5.5")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    debugImplementation("androidx.compose.ui:ui-tooling")

    // ---- 运行时证据测试（androidTest，仅测试源集，不进入生产代码） ----
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    // Compose UI 测试：P0-A（锁不可绕过）/ P1-A（按钮 hitbox）要求
    // "Compose UI test + Emulator 实际 tap" 交叉验证，两个通道都要有。
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    // createAndroidComposeRule 需要一个可调试的空 Activity 载体
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
