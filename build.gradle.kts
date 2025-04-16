plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
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

    kover(project(":app"))
    kover(project(":authenticator"))
    kover(project(":authenticatorbridge"))
    kover(project(":core"))
    kover(project(":data"))
    kover(project(":network"))
    kover(project(":ui"))
}

detekt {
    autoCorrect = true
    config.from(files("detekt-config.yml"))
    source.from(
        "app/src",
        "authenticator/src",
        "authenticatorbridge/src",
        "core/src",
        "data/src",
        "network/src",
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
                    "com.bitwarden.core.annotation.OmitFromCoverage",
                    // DI modules
                    "dagger.Module",
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

    withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        jvmTarget = libs.versions.jvmTarget.get()
    }
    withType<io.gitlab.arturbosch.detekt.DetektCreateBaselineTask>().configureEach {
        jvmTarget = libs.versions.jvmTarget.get()
    }
}
