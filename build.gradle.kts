import io.gitlab.arturbosch.detekt.Detekt

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.androidx.room) apply false
    alias(libs.plugins.detekt) apply true
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose.compiler) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.kotlinx.kover) apply true
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.sonarqube) apply true
}

dependencies {
    detektPlugins(libs.detekt.detekt.formatting)
    detektPlugins(libs.detekt.detekt.rules)

    kover(project(":annotation"))
    kover(project(":app"))
    kover(project(":authenticator"))
    kover(project(":authenticatorbridge"))
    kover(project(":core"))
    kover(project(":cxf"))
    kover(project(":data"))
    kover(project(":network"))
    kover(project(":testharness"))
    kover(project(":ui"))
}

detekt {
    autoCorrect = true
    config.from(files("detekt-config.yml"))
    source.from(
        "annotation/src",
        "app/src",
        "authenticator/src",
        "authenticatorbridge/src",
        "core/src",
        "cxf/src",
        "data/src",
        "network/src",
        "testharness/src",
        "ui/src",
    )
}

kover {
    merge {
        allProjects()
        createVariant("mergedCoverage") {
            // Only run tests on the `StandardDebug` variant of the :app module.
            add("standardDebug", optional = true)

            // Only run tests on the `debug` variants in all other modules.
            add("debug", optional = true)
        }
    }
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
                    "com.bitwarden.annotation.OmitFromCoverage",
                    // Dagger modules
                    "dagger.Module",
                )
                files(
                    // Navigation helpers
                    "*.*Navigation.kt",
                )
                classes(
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
                    "*.ui.platform.components",
                    // Theme-related code
                    "*.ui.platform.theme",
                )
            }
        }
    }
}

sonar {
    properties {
        property("sonar.projectKey", "bitwarden_android")
        property("sonar.organization", "bitwarden")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.sources", "*/src/")
        property("sonar.tests", "*/src/")
        property("sonar.test.inclusions", "*/src/test/")
        property("sonar.exclusions", "*/src/test/")
    }
}

tasks {
    getByName("sonar") {
        dependsOn("koverXmlReportMergedCoverage")
    }

    withType<Detekt>().configureEach {
        jvmTarget = libs.versions.jvmTarget.get()
        // If run as a precommit hook, only run on staged files.
        // This can be manually trigger by adding `-Pprecommit=true` to the gradle command.
        if (project.hasProperty("precommit")) {
            val rootDir = project.rootDir
            val projectDir = projectDir

            val fileCollection = files()

            setSource(
                getGitStagedFiles(rootDir)
                    .map { stagedFiles ->
                        val stagedFilesFromThisProject = stagedFiles
                            .filter { it.startsWith(projectDir) }

                        fileCollection.setFrom(*stagedFilesFromThisProject.toTypedArray())

                        fileCollection.asFileTree
                    },
            )
        }
    }
    withType<io.gitlab.arturbosch.detekt.DetektCreateBaselineTask>().configureEach {
        jvmTarget = libs.versions.jvmTarget.get()
    }
}

/**
 * Gets the staged files in the current Git repository.
 */
fun Project.getGitStagedFiles(rootDir: File): Provider<List<File>> {
    return providers
        .exec { commandLine("git", "--no-pager", "diff", "--name-only", "--cached") }
        .standardOutput.asText
        .map { outputText ->
            outputText
                .trim()
                .split("\n")
                .filter { it.isNotBlank() }
                .map { File(rootDir, it) }
        }
}

afterEvaluate {
    tasks.withType(Detekt::class.java).configureEach {
        val typeResolutionEnabled = !classpath.isEmpty
        if (typeResolutionEnabled && project.hasProperty("precommit")) {
            // We must exclude kts files from pre-commit hook to prevent detekt from crashing
            // This is a workaround for the https://github.com/detekt/detekt/issues/5501
            exclude("*.gradle.kts")
        }
    }
}
