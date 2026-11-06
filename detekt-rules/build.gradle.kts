import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // The Kotlin Gradle plugin is already on the root build classpath, so no version is needed.
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.toVersion(libs.versions.jvmTarget.get())
    targetCompatibility = JavaVersion.toVersion(libs.versions.jvmTarget.get())
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvmTarget.get()))
    }
}

dependencies {
    compileOnly(libs.detekt.detekt.api)

    testImplementation(libs.detekt.detekt.api)
    testImplementation(libs.detekt.detekt.test)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
