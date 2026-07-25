plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.toolbox.nativetoolbox"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.toolbox.nativetoolbox"
        minSdk = 29
        targetSdk = 35
        versionCode = 5
        versionName = "0.4.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            // CI 通过环境变量注入(keystore 存 GitHub Secrets,不进版本库)。
            // 本地没有这些变量时回退 debug 签名,便于开发。
            val storePath = System.getenv("RELEASE_STORE_FILE")
            if (storePath != null && file(storePath).exists()) {
                storeFile = file(storePath)
                storePassword = System.getenv("RELEASE_STORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    // ABI 分包:一个 APK 塞所有架构的 native 库是包体大头。
    // 2026 年了,arm64 覆盖绝大多数设备;仍保留 v7a 兼容老机器。
    splits {
        abi {
            isEnable = true
            reset()
            // x86_64 必须留:CI 模拟器是 x86_64,universal 包缺它会让
            // ML Kit 等 native 库在模拟器上加载失败。发布只发 arm 系,x86_64 单包不发。
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 有正式 keystore 就用,否则回退 debug(仅本地开发)
            signingConfig = if (System.getenv("RELEASE_STORE_FILE") != null)
                signingConfigs.getByName("release")
            else signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    lint {
        checkReleaseBuilds = false
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
        )
    }
}

dependencies {
    // Kyant Liquid Glass
    implementation("io.github.kyant0:backdrop:2.0.0")
    implementation("io.github.kyant0:shapes:1.2.0")

    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2026.06.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Compose Core
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    // icons-extended 已停更,固定最后一个版本(与新 Compose 运行时二进制兼容)
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    // Activity & Lifecycle
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.1")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.9.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.7")

    // 相机（镜子/放大镜/文档扫描/扫码取景）
    implementation("androidx.camera:camera-core:1.4.2")
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")

    // 二维码
    implementation("com.google.zxing:core:3.5.3")

    // EXIF 读写
    implementation("androidx.exifinterface:exifinterface:1.4.1")

    // 桌面小组件:预测结果直接出现在桌面,用户不用打开 App
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")

    // 抠图改用自研 SmartCutout(纯算法零依赖)——
    // ML Kit selfie-segmentation 带 mediapipe 共 22MB,只为一个工具不划算
    // OCR 中文识别(模型打包离线)
    implementation("com.google.mlkit:text-recognition-chinese:16.0.1")

    // Core
    implementation("androidx.core:core-ktx:1.16.0")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
