import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.bitwarden.network"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdkBwa.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility(libs.versions.jvmTarget.get())
        targetCompatibility(libs.versions.jvmTarget.get())
    }
    @Suppress("UnstableApiUsage")
    testFixtures {
        enable = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(libs.versions.jvmTarget.get())
    }
}

dependencies {
    implementation(project(":annotation"))
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.serialization)
    implementation(platform(libs.square.okhttp.bom))
    implementation(libs.square.okhttp)
    implementation(libs.square.okhttp.logging)
    implementation(platform(libs.square.retrofit.bom))
    implementation(libs.square.retrofit)
    implementation(libs.square.retrofit.kotlinx.serialization)
    implementation(libs.timber)

    testImplementation(platform(libs.junit.bom))
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.vintage)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk.mockk)
    testImplementation(libs.square.okhttp.mockwebserver)
    testImplementation(libs.square.turbine)

    testFixturesImplementation(project(":core"))
    testFixturesImplementation(platform(libs.junit.bom))
    testFixturesRuntimeOnly(libs.junit.platform.launcher)
    testFixturesImplementation(libs.junit.jupiter)
    testFixturesImplementation(libs.junit.vintage)
    testFixturesImplementation(libs.kotlinx.serialization)
    testFixturesImplementation(libs.square.okhttp)
    testFixturesImplementation(platform(libs.square.retrofit.bom))
    testFixturesImplementation(libs.square.retrofit)
    testFixturesImplementation(libs.square.retrofit.kotlinx.serialization)
    testFixturesImplementation(libs.square.okhttp.mockwebserver)
}

tasks {
    withType<Test> {
        useJUnitPlatform()
        maxHeapSize = "2g"
        maxParallelForks = Runtime.getRuntime().availableProcessors()
        jvmArgs = jvmArgs.orEmpty() + "-XX:+UseParallelGC"
    }
}
