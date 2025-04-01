plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.bitwarden.network"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    kotlinOptions {
        jvmTarget = libs.versions.jvmTarget.get()
    }
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.serialization)
    implementation(libs.square.okhttp)
    implementation(libs.square.okhttp.logging)
    implementation(platform(libs.square.retrofit.bom))
    implementation(libs.square.retrofit)
    implementation(libs.square.retrofit.kotlinx.serialization)
    implementation(libs.timber)

    testImplementation(platform(libs.junit.bom))
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.junit.junit5)
    testImplementation(libs.junit.vintage)
    testImplementation(libs.mockk.mockk)
    testImplementation(libs.square.okhttp.mockwebserver)
    testImplementation(libs.square.turbine)
}

tasks {
    withType<Test> {
        useJUnitPlatform()
        maxHeapSize = "2g"
        maxParallelForks = Runtime.getRuntime().availableProcessors()
        jvmArgs = jvmArgs.orEmpty() + "-XX:+UseParallelGC"
    }
}
