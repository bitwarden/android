import com.android.build.api.dsl.LibraryExtension
import com.android.build.gradle.tasks.BundleAar
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// For more info on versioning, see the README.
val version = "1.0.2"

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
}

configure<LibraryExtension> {
    namespace = "com.bitwarden.authenticatorbridge"
    setVersion(version)
    compileSdk {
        version = release(libs.versions.compileSdk.get().toInt())
    }

    defaultConfig {
        // This min value is selected to accommodate known consumers
        minSdk {
            version = release(libs.versions.minSdkBwa.get().toInt())
        }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        buildConfigField("String", "VERSION", "\"$version\"")
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
    buildFeatures {
        buildConfig = true
        aidl = true
    }
}

androidComponents {
    onVariants { libVariant ->
        val bundleTaskName = "bundle${libVariant.name.uppercaseFirstChar()}Aar"
        tasks
            .withType<BundleAar>()
            .named { it == bundleTaskName }
            .configureEach {
                archiveFileName.set("authenticatorbridge-$version-${libVariant.name}.aar")
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
    implementation(project(":annotation"))
    implementation(project(":core"))
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.timber)

    // Test environment dependencies:
    testImplementation(platform(libs.junit.bom))
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk.mockk)
    testImplementation(libs.square.turbine)
}
