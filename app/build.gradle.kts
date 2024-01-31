import com.google.firebase.crashlytics.buildtools.gradle.tasks.InjectMappingFileIdTask
import com.google.firebase.crashlytics.buildtools.gradle.tasks.UploadMappingFileTask
import com.google.gms.googleservices.GoogleServicesTask

plugins {
    alias(libs.plugins.android.application)
    // Crashlytics is enabled for all builds initially but removed for FDroid builds in gradle and
    // standardDebug builds in the merged manifest.
    alias(libs.plugins.crashlytics)
    alias(libs.plugins.detekt)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlinx.kover)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
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

        // This is so the build system only includes language resources in the APK for these
        // languages, preventing translated strings from being included from other libraries that
        // might support languages this app does not.
        resourceConfigurations += arrayOf(
            "af",
            "be",
            "bg",
            "ca",
            "cs",
            "da",
            "de",
            "el",
            "en",
            "en-rGB",
            "es",
            "et",
            "fa",
            "fi",
            "fr",
            "hi",
            "hr",
            "hu",
            "in",
            "it",
            "iw",
            "ja",
            "ko",
            "lv",
            "ml",
            "nb",
            "nl",
            "pl",
            "pt-rBR",
            "pt-rPT",
            "ro",
            "ru",
            "sk",
            "sv",
            "th",
            "tr",
            "uk",
            "vi",
            "zh-rCN",
            "zh-rTW"
        )

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

    flavorDimensions += listOf("mode")
    productFlavors {
        create("standard") {
            isDefault = true
            dimension = "mode"
        }
        create("fdroid") {
            dimension = "mode"
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
        buildConfig = true
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
    fun standardImplementation(dependencyNotation: Any) {
        add("standardImplementation", dependencyNotation)
    }

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.autofill)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.biometrics)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.bitwarden.sdk)
    implementation(libs.bumptech.glide)
    implementation(libs.google.hilt.android)
    kapt(libs.google.hilt.compiler)
    implementation(libs.jakewharton.retrofit.kotlinx.serialization)
    implementation(libs.kotlinx.collections.immutable)
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

    // Standard-specific flavor dependencies
    standardImplementation(libs.google.firebase.cloud.messaging)
    standardImplementation(platform(libs.google.firebase.bom))
    standardImplementation(libs.google.firebase.crashlytics)

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
                "androidx.compose.ui.tooling.preview.Preview",
                // Manually excluded classes/files/etc.
                "com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage"
            )
            classes(
                // Navigation helpers
                "*.*NavigationKt*",
                // Composable singletons
                "*.*ComposableSingletons*",
                // Generated classes related to interfaces with default values
                "*.*DefaultImpls*",
                // OS-level components
                "com.x8bit.bitwarden.BitwardenApplication",
                "com.x8bit.bitwarden.MainActivity*",
                "com.x8bit.bitwarden.WebAuthCallbackActivity*",
                "com.x8bit.bitwarden.data.autofill.BitwardenAutofillService*",
                "com.x8bit.bitwarden.data.push.BitwardenFirebaseMessagingService*",
                // Empty Composables
                "com.x8bit.bitwarden.ui.platform.feature.splash.SplashScreenKt",
                // Databases
                "*.database.*Database*",
                "*.dao.*Dao*",
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
        maxHeapSize = "4g"
        maxParallelForks = Runtime.getRuntime().availableProcessors()
    }
}

afterEvaluate {
    // Disable Fdroid-specific tasks that we want to exclude
    val tasks = tasks.withType<GoogleServicesTask>() +
        tasks.withType<InjectMappingFileIdTask>() +
        tasks.withType<UploadMappingFileTask>()
    tasks
        .filter { it.name.contains("Fdroid") }
        .forEach { it.enabled = false }
}
