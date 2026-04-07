import com.android.build.api.dsl.LibraryExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.google.services)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

configure<LibraryExtension> {
    namespace = "com.bitwarden.cxf"
    compileSdk {
        version = release(libs.versions.compileSdk.get().toInt())
    }

    defaultConfig {
        minSdk {
            version = release(libs.versions.minSdkBwa.get().toInt())
        }
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
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvmTarget.get()))
    }
}

dependencies {

    implementation(project(":annotation"))
    implementation(project(":core"))
    implementation(project(":ui"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.providerevents)
    implementation(libs.androidx.credentials.providerevents.play.services)
    implementation(libs.google.hilt.android)
    ksp(libs.google.hilt.compiler)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization)
    implementation(libs.timber)

    testImplementation(platform(libs.junit.bom))
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.vintage)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk.mockk)
}
