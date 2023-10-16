plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.detekt)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlinx.kover)
    alias(libs.plugins.ksp)
    kotlin("kapt")
}

android {
    namespace = "com.x8bit.bitwarden"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.x8bit.bitwarden"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        getByName("debug") {
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            storeFile = file("../keystores/debug.keystore")
            storePassword = "android"
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".dev"
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = true
            isMinifyEnabled = false
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility(libs.versions.jvmTarget.get())
        targetCompatibility(libs.versions.jvmTarget.get())
    }
    kotlinOptions {
        jvmTarget = libs.versions.jvmTarget.get()
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.kotlinCompilerExtensionVersion.get()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    @Suppress("UnstableApiUsage")
    testOptions {
        // Required for Robolectric
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.browser)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.splashscreen)
    implementation(platform(libs.google.firebase.bom))
    implementation(libs.bitwarden.sdk)
    implementation(libs.bumptech.glide)
    implementation(libs.google.firebase.cloud.messaging)
    implementation(libs.google.firebase.crashlytics)
    implementation(libs.google.hilt.android)
    kapt(libs.google.hilt.compiler)
    implementation(libs.jakewharton.retrofit.kotlinx.serialization)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization)
    implementation(libs.nulab.zxcvbn4j)
    implementation(libs.square.okhttp)
    implementation(libs.square.okhttp.logging)
    implementation(libs.square.retrofit)
    implementation(libs.zxing.zxing.core)

    // For now we are restricted to running Compose tests for debug builds only
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.androidx.compose.ui.test)
    testImplementation(libs.google.hilt.android.testing)
    testImplementation(libs.junit.junit5)
    testImplementation(libs.junit.vintage)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk.mockk)
    testImplementation(libs.robolectric.robolectric)
    testImplementation(libs.square.okhttp.mockwebserver)
    testImplementation(libs.square.turbine)

    detektPlugins(libs.detekt.detekt.formatting)
    detektPlugins(libs.detekt.detekt.rules)
}

detekt {
    autoCorrect = true
    config.from(files("$rootDir/detekt-config.yml"))
}

kover {
    excludeJavaCode()
}

koverReport {
    filters {
        excludes {
            annotatedBy(
                // Compose previews
                "androidx.compose.ui.tooling.preview.Preview"
            )
            classes(
                // Navigation helpers
                "*.*NavigationKt*",
                // Composable singletons
                "*.*ComposableSingletons*",

                // OS-level components
                "com.x8bit.bitwarden.BitwardenApplication",
                "com.x8bit.bitwarden.MainActivity*",
                // Empty Composables
                "com.x8bit.bitwarden.ui.platform.feature.splash.SplashScreenKt",
            )
            packages(
                // Dependency injection
                "*.di",
                // Models
                "*.model",
                // Custom UI components
                "com.x8bit.bitwarden.ui.platform.components",
                // Theme-related code
                "com.x8bit.bitwarden.ui.platform.theme",
            )
        }
    }
}

tasks {
    getByName("check") {
        // Add detekt with type resolution to check
        dependsOn("detektMain")
    }

    withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        jvmTarget = libs.versions.jvmTarget.get()
    }
    withType<io.gitlab.arturbosch.detekt.DetektCreateBaselineTask>().configureEach {
        jvmTarget = libs.versions.jvmTarget.get()
    }

    withType<Test> {
        useJUnitPlatform()
    }
}
