import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.bitwarden.ui"
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
    lint {
        disable += listOf(
            "MissingTranslation",
            "ExtraTranslation",
        )
    }
    testOptions {
        // Required for Robolectric
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
    }
    @Suppress("UnstableApiUsage")
    testFixtures {
        enable = true
    }
}

dependencies {
    implementation(project(":annotation"))
    implementation(project(":core"))

    implementation(libs.androidx.appcompat)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.bumptech.glide)
    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.collections.immutable)

    // For now we are restricted to running Compose tests for debug builds only
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.junit5)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.junit.vintage)
    testImplementation(libs.mockk.mockk)
    testImplementation(libs.robolectric.robolectric)
    testImplementation(libs.androidx.compose.ui.test)

    testFixturesImplementation(libs.androidx.activity.compose)
    testFixturesImplementation(libs.androidx.compose.ui.test)
    testFixturesImplementation(libs.androidx.navigation.compose)
    testFixturesImplementation(libs.google.hilt.android.testing)
    testFixturesImplementation(platform(libs.junit.bom))
    testFixturesImplementation(libs.junit.junit5)
    testFixturesImplementation(libs.junit.vintage)
    testFixturesImplementation(libs.kotlinx.coroutines.test)
    testFixturesImplementation(libs.mockk.mockk)
    testFixturesImplementation(libs.robolectric.robolectric)
    testFixturesImplementation(libs.square.turbine)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(libs.versions.jvmTarget.get())
    }
}

tasks {
    withType<Test> {
        useJUnitPlatform()
        maxHeapSize = "2g"
        maxParallelForks = Runtime.getRuntime().availableProcessors()
        @Suppress("UselessCallOnNotNull")
        jvmArgs = jvmArgs.orEmpty() + "-XX:+UseParallelGC"
        android.sourceSets["main"].res.srcDirs("src/test/res")
    }
}
