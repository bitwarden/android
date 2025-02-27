import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import com.google.protobuf.gradle.proto
import dagger.hilt.android.plugin.util.capitalize
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.crashlytics)
    alias(libs.plugins.detekt)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlinx.kover)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.protobuf)
    alias(libs.plugins.google.services)
    alias(libs.plugins.sonarqube)
}

android {
    namespace = "com.bitwarden.authenticator"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.bitwarden.authenticator"
        minSdk = libs.versions.minSdkBwa.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        setProperty("archivesBaseName", "com.bitwarden.authenticator")

        ksp {
            // The location in which the generated Room Database Schemas will be stored in the repo.
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    androidResources {
        @Suppress("UnstableApiUsage")
        generateLocaleConfig = true
    }

    signingConfigs {
        getByName("debug") {
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            storeFile = file("../keystores/debug-bwa.keystore")
            storePassword = "android"
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".dev"
            manifestPlaceholders["targetBitwardenAppId"] = "com.x8bit.bitwarden.dev"
            buildConfigField(
                type = "com.bitwarden.authenticatorbridge.manager.model.AuthenticatorBridgeConnectionType",
                name = "AUTHENTICATOR_BRIDGE_CONNECTION_TYPE",
                value = "com.bitwarden.authenticatorbridge.manager.model.AuthenticatorBridgeConnectionType.DEV",
            )
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = true
            isMinifyEnabled = false
            buildConfigField(type = "boolean", name = "HAS_DEBUG_MENU", value = "true")
        }

        release {
            manifestPlaceholders["targetBitwardenAppId"] = "com.x8bit.bitwarden"
            buildConfigField(
                type = "com.bitwarden.authenticatorbridge.manager.model.AuthenticatorBridgeConnectionType",
                name = "AUTHENTICATOR_BRIDGE_CONNECTION_TYPE",
                value = "com.bitwarden.authenticatorbridge.manager.model.AuthenticatorBridgeConnectionType.RELEASE",
            )
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField(type = "boolean", name = "HAS_DEBUG_MENU", value = "false")
        }
    }
    applicationVariants.all {
        val bundlesDir = "${layout.buildDirectory.get()}/outputs/bundle"
        outputs
            .mapNotNull { it as? BaseVariantOutputImpl }
            .forEach { output ->
                // Set the APK output filename.
                output.outputFileName = "$applicationId.apk"

                val variantName = name
                val renameTaskName = "rename${variantName.capitalize()}AabFiles"
                tasks.register(renameTaskName) {
                    group = "build"
                    description = "Renames the bundle files for $variantName variant"
                    doLast {
                        renameFile(
                            "$bundlesDir/$variantName/$namespace-${buildType.name}.aab",
                            "$applicationId.aab",
                        )
                    }
                }
                // Force renaming task to execute after the variant is built.
                tasks
                    .getByName("bundle${variantName.capitalize()}")
                    .finalizedBy(renameTaskName)
            }
    }
    compileOptions {
        sourceCompatibility(libs.versions.jvmTarget.get())
        targetCompatibility(libs.versions.jvmTarget.get())
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1,LICENSE*.md}"
        }
    }
    sourceSets {
        getByName("main") {
            proto {
                srcDir("src/main/proto")
            }
        }
    }
    lint {
        disable += listOf(
            "MissingTranslation",
            "ExtraTranslation",
        )
    }
    @Suppress("UnstableApiUsage")
    testOptions {
        // Required for Robolectric
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvmTarget.get()))
    }
}

dependencies {

    implementation(files("libs/authenticatorbridge-1.0.0-release.aar"))

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
    testImplementation(libs.testng)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.bitwarden.sdk)
    implementation(libs.bumptech.glide)
    implementation(platform(libs.google.firebase.bom))
    implementation(libs.google.firebase.cloud.messaging)
    implementation(libs.google.firebase.crashlytics)
    implementation(libs.google.hilt.android)
    ksp(libs.google.hilt.compiler)
    implementation(libs.google.guava)
    implementation(libs.google.protobuf.javalite)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization)
    implementation(libs.square.okhttp)
    implementation(libs.square.okhttp.logging)
    implementation(platform(libs.square.retrofit.bom))
    implementation(libs.square.retrofit)
    implementation(libs.square.retrofit.kotlinx.serialization)
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
    currentProject {
        sources {
            excludeJava = true
        }
    }
    reports {
        filters {
            excludes {
                androidGeneratedClasses()
                annotatedBy(
                    // Compose previews
                    "androidx.compose.ui.tooling.preview.Preview",
                    // Manually excluded classes/files/etc.
                    "com.bitwarden.authenticator.data.platform.annotation.OmitFromCoverage",
                )
                classes(
                    // Navigation helpers
                    "*.*NavigationKt*",
                    // Composable singletons
                    "*.*ComposableSingletons*",
                    // Generated classes related to interfaces with default values
                    "*.*DefaultImpls*",
                    // Databases
                    "*.database.*Database*",
                    "*.dao.*Dao*",
                    // Dagger Hilt
                    "dagger.hilt.*",
                    "hilt_aggregated_deps.*",
                    "*_Factory",
                    "*_Factory\$*",
                    "*_*Factory",
                    "*_*Factory\$*",
                    "*.Hilt_*",
                    "*_HiltModules",
                    "*_HiltModules\$*",
                    "*_Impl",
                    "*_Impl\$*",
                    "*_MembersInjector",
                )
                packages(
                    // Dependency injection
                    "*.di",
                    // Models
                    "*.model",
                    // Custom UI components
                    "com.bitwarden.authenticator.ui.platform.components",
                    // Theme-related code
                    "com.bitwarden.authenticator.ui.platform.theme",
                )
            }
        }
    }
}

protobuf {
    protoc {
        artifact = libs.google.protobuf.protoc.get().toString()
    }
    generateProtoTasks {
        this.all().forEach { task ->
            task.builtins.create("java") {
                option("lite")
            }
        }
    }
}

sonar {
    properties {
        property("sonar.projectKey", "bitwarden_authenticator-android")
        property("sonar.organization", "bitwarden")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.sources", "authenticator/src/")
        property("sonar.tests", "authenticator/src/")
        property("sonar.test.inclusions", "authenticator/src/test/")
        property("sonar.exclusions", "authenticator/src/test/")
    }
}

tasks {
    withType<Test> {
        useJUnitPlatform()
    }
    getByName("sonar") {
        dependsOn("check")
    }
}

private fun renameFile(path: String, newName: String) {
    val originalFile = File(path)
    if (!originalFile.exists()) {
        println("File $originalFile does not exist!")
        return
    }

    val newFile = File(originalFile.parentFile, newName)
    if (originalFile.renameTo(newFile)) {
        println("Renamed $originalFile to $newFile")
    } else {
        throw RuntimeException("Failed to rename $originalFile to $newFile")
    }
}
