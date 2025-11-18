import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.bitwarden.testharness"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.bitwarden.testharness"
        // API 28 - CredentialManager with Play Services support
        minSdk = libs.versions.minSdkBwa.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".dev"
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        // Required for Android framework classes in unit tests
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
    }

    compileOptions {
        sourceCompatibility(libs.versions.jvmTarget.get())
        targetCompatibility(libs.versions.jvmTarget.get())
    }

    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.fromTarget(libs.versions.jvmTarget.get())
        }
    }
}

dependencies {
    // Internal modules
    implementation(project(":annotation"))
    implementation(project(":core"))
    implementation(project(":ui"))

    // AndroidX Credentials - PRIMARY DEPENDENCY
    implementation(libs.androidx.credentials)

    // Hilt DI
    implementation(libs.google.hilt.android)
    ksp(libs.google.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Compose UI
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.splashscreen)

    // Kotlin essentials
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlinx.collections.immutable)

    // Testing
    testImplementation(libs.androidx.compose.ui.test)
    testImplementation(libs.google.hilt.android.testing)
    testImplementation(platform(libs.junit.bom))
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.vintage)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk.mockk)
    // Robolectric reserved for future Android framework tests if needed
    testImplementation(libs.robolectric.robolectric)
    testImplementation(libs.square.turbine)

    testImplementation(testFixtures(project(":ui")))
}

tasks {
    withType<Test> {
        useJUnitPlatform()
        maxHeapSize = "2g"
        maxParallelForks = Runtime.getRuntime().availableProcessors()
    }
}
