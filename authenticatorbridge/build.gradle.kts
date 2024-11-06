import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// For more info on versioning, see the README.
val version = "1.0.0"

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.bitwarden.authenticatorbridge"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        // This min value is selected to accommodate known consumers
        minSdk = 28

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        buildConfigField("String", "VERSION", "\"$version\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    buildFeatures {
        buildConfig = true
        aidl = true
    }
    // Add version name to the output .aar file:
    libraryVariants.all {
        val variant = this
        outputs
            .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
            .forEach { output ->
                val outputFileName = "authenticatorbridge-${version}-${variant.baseName}.aar"
                output.outputFileName = outputFileName
            }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvmTarget.get()))
    }
}

dependencies {
    // SDK dependencies:
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlinx.coroutines.core)

    // Test environment dependencies:
    testImplementation(libs.junit.junit5)
    testImplementation(libs.mockk.mockk)
    testImplementation(libs.square.turbine)
}

tasks {
    withType<Test> {
        useJUnitPlatform()
    }
}
