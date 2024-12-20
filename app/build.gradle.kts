import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import com.android.utils.cxx.io.removeExtensionIfPresent
import com.google.firebase.crashlytics.buildtools.gradle.tasks.InjectMappingFileIdTask
import com.google.firebase.crashlytics.buildtools.gradle.tasks.UploadMappingFileTask
import com.google.gms.googleservices.GoogleServicesTask
import dagger.hilt.android.plugin.util.capitalize
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    // Crashlytics is enabled for all builds initially but removed for FDroid builds in gradle and
    // standardDebug builds in the merged manifest.
    alias(libs.plugins.crashlytics)
    alias(libs.plugins.detekt)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlinx.kover)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.sonarqube)
}

/**
 * Loads local user-specific build properties that are not checked into source control.
 */
val userProperties = Properties().apply {
    val buildPropertiesFile = File(rootDir, "user.properties")
    if (buildPropertiesFile.exists()) {
        FileInputStream(buildPropertiesFile).use { load(it) }
    }
}

/**
 * Loads CI-specific build properties that are not checked into source control.
 */
val ciProperties = Properties().apply {
    val ciPropsFile = File(rootDir, "ci.properties")
    if (ciPropsFile.exists()) {
        FileInputStream(ciPropsFile).use { load(it) }
    }
}

android {
    namespace = "com.x8bit.bitwarden"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.x8bit.bitwarden"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "2024.9.0"

        setProperty("archivesBaseName", "com.x8bit.bitwarden")

        ksp {
            // The location in which the generated Room Database Schemas will be stored in the repo.
            arg("room.schemaLocation", "$projectDir/schemas")
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            type = "String",
            name = "CI_INFO",
            value = "${ciProperties.getOrDefault("ci.info", "\"local\"")}"
        )
    }

    androidResources {
        @Suppress("UnstableApiUsage")
        generateLocaleConfig = true
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

            buildConfigField(type = "boolean", name = "HAS_DEBUG_MENU", value = "true")
            buildConfigField(type = "boolean", name = "HAS_LOGS_ENABLED", value = "true")
        }

        // Beta and Release variants are identical except beta has a different package name
        create("beta") {
            applicationIdSuffix = ".beta"
            isDebuggable = false
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            buildConfigField(type = "boolean", name = "HAS_DEBUG_MENU", value = "false")
            buildConfigField(type = "boolean", name = "HAS_LOGS_ENABLED", value = "false")
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            buildConfigField(type = "boolean", name = "HAS_DEBUG_MENU", value = "false")
            buildConfigField(type = "boolean", name = "HAS_LOGS_ENABLED", value = "false")
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

    applicationVariants.all {
        val bundlesDir = "${layout.buildDirectory.get()}/outputs/bundle"
        outputs
            .mapNotNull { it as? BaseVariantOutputImpl }
            .forEach { output ->
                val fileNameWithoutExtension = when (flavorName) {
                    "fdroid" -> "$applicationId-$flavorName"
                    "standard" -> "$applicationId"
                    else -> output.outputFileName.removeExtensionIfPresent(".apk")
                }

                // Set the APK output filename.
                output.outputFileName = "$fileNameWithoutExtension.apk"

                val variantName = name
                val renameTaskName = "rename${variantName.capitalize()}AabFiles"
                tasks.register(renameTaskName) {
                    group = "build"
                    description = "Renames the bundle files for $variantName variant"
                    doLast {
                        renameFile(
                            "$bundlesDir/$variantName/$namespace-$flavorName-${buildType.name}.aab",
                            "$fileNameWithoutExtension.aab",
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
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    @Suppress("UnstableApiUsage")
    testOptions {
        // Required for Robolectric
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
    }
    lint {
        disable += listOf(
            "MissingTranslation",
            "ExtraTranslation",
        )
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvmTarget.get()))
    }
}

configurations.all {
    resolutionStrategy.dependencySubstitution {
        if ((userProperties["localSdk"] as String?).toBoolean()) {
            substitute(module("com.bitwarden:sdk-android"))
                .using(module("com.bitwarden:sdk-android:LOCAL"))
        }
    }
}

dependencies {
    fun standardImplementation(dependencyNotation: Any) {
        add("standardImplementation", dependencyNotation)
    }

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
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.bitwarden.sdk)
    implementation(libs.bumptech.glide)
    implementation(libs.androidx.credentials)
    implementation(libs.google.hilt.android)
    ksp(libs.google.hilt.compiler)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization)
    implementation(libs.nulab.zxcvbn4j)
    implementation(libs.square.okhttp)
    implementation(libs.square.okhttp.logging)
    implementation(platform(libs.square.retrofit.bom))
    implementation(libs.square.retrofit)
    implementation(libs.square.retrofit.kotlinx.serialization)
    implementation(libs.timber)
    implementation(libs.zxing.zxing.core)

    // For now we are restricted to running Compose tests for debug builds only
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Standard-specific flavor dependencies
    standardImplementation(libs.google.firebase.cloud.messaging)
    standardImplementation(platform(libs.google.firebase.bom))
    standardImplementation(libs.google.firebase.crashlytics)
    standardImplementation(libs.google.play.review)

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
                    "androidx.compose.ui.tooling.preview.PreviewScreenSizes",
                    // Manually excluded classes/files/etc.
                    "com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage",
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
                    "com.x8bit.bitwarden.ui.platform.components",
                    // Theme-related code
                    "com.x8bit.bitwarden.ui.platform.theme",
                )
            }
        }
    }
}

tasks {
    getByName("check") {
        // Add detekt with type resolution to check
        dependsOn("detekt")
    }

    getByName("sonar") {
        dependsOn("check")
    }

    withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        jvmTarget = libs.versions.jvmTarget.get()
    }
    withType<io.gitlab.arturbosch.detekt.DetektCreateBaselineTask>().configureEach {
        jvmTarget = libs.versions.jvmTarget.get()
    }

    withType<Test> {
        useJUnitPlatform()
        maxHeapSize = "2g"
        maxParallelForks = Runtime.getRuntime().availableProcessors()
        jvmArgs = jvmArgs.orEmpty() + "-XX:+UseParallelGC"
        android.sourceSets["main"].res.srcDirs("src/test/res")
    }
}

afterEvaluate {
    // Disable Fdroid-specific tasks that we want to exclude
    val fdroidTasksToDisable = tasks.withType<GoogleServicesTask>() +
        tasks.withType<InjectMappingFileIdTask>() +
        tasks.withType<UploadMappingFileTask>()
    fdroidTasksToDisable
        .filter { it.name.contains("Fdroid") }
        .forEach { it.enabled = false }
}

sonar {
    properties {
        property("sonar.projectKey", "bitwarden_android")
        property("sonar.organization", "bitwarden")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.sources", "app/src/")
        property("sonar.tests", "app/src/")
        property("sonar.test.inclusions", "app/src/test/")
        property("sonar.exclusions", "app/src/test/")
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
