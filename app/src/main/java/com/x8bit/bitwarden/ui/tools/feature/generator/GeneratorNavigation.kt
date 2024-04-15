package com.x8bit.bitwarden.ui.tools.feature.generator

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import com.x8bit.bitwarden.ui.tools.feature.generator.model.GeneratorMode

/**
 * The functions below pertain to entry into the [GeneratorScreen].
 */
private const val GENERATOR_MODAL_ROUTE_PREFIX: String = "generator_modal"
private const val GENERATOR_MODE_TYPE: String = "generator_mode_type"
private const val GENERATOR_WEBSITE: String = "generator_website"
private const val USERNAME_GENERATOR: String = "username_generator"
private const val PASSWORD_GENERATOR: String = "password_generator"

const val GENERATOR_ROUTE: String = "generator"
private const val GENERATOR_MODAL_ROUTE: String =
    "$GENERATOR_MODAL_ROUTE_PREFIX/{$GENERATOR_MODE_TYPE}?$GENERATOR_WEBSITE={$GENERATOR_WEBSITE}"

/**
 * Class to retrieve vault item listing arguments from the [SavedStateHandle].
 */
@OmitFromCoverage
data class GeneratorArgs(
    val type: GeneratorMode,
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        type = when (savedStateHandle.get<String>(GENERATOR_MODE_TYPE)) {
            USERNAME_GENERATOR -> GeneratorMode.Modal.Username(
                website = savedStateHandle[GENERATOR_WEBSITE],
            )

            PASSWORD_GENERATOR -> GeneratorMode.Modal.Password
            else -> GeneratorMode.Default
        },
    )
}

/**
 * Add generator destination to the root nav graph.
 */
fun NavGraphBuilder.generatorDestination(
    onNavigateToPasswordHistory: () -> Unit,
) {
    composable(GENERATOR_ROUTE) {
        GeneratorScreen(
            onNavigateToPasswordHistory = onNavigateToPasswordHistory,
            onNavigateBack = {},
        )
    }
}

/**
 * Add the generator modal destination to the nav graph.
 */
fun NavGraphBuilder.generatorModalDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions(
        route = GENERATOR_MODAL_ROUTE,
        arguments = listOf(
            navArgument(GENERATOR_MODE_TYPE) { type = NavType.StringType },
            navArgument(GENERATOR_WEBSITE) {
                type = NavType.StringType
                nullable = true
            },
        ),
    ) {
        GeneratorScreen(
            onNavigateToPasswordHistory = {},
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the generator screen in the given mode with the corresponding website, if one exists.
 */
fun NavController.navigateToGeneratorModal(
    mode: GeneratorMode.Modal,
    navOptions: NavOptions? = null,
) {
    val generatorModeType = when (mode) {
        GeneratorMode.Modal.Password -> PASSWORD_GENERATOR
        is GeneratorMode.Modal.Username -> USERNAME_GENERATOR
    }
    val website = (mode as? GeneratorMode.Modal.Username)?.website
    navigate(
        route = "$GENERATOR_MODAL_ROUTE_PREFIX/$generatorModeType?$GENERATOR_WEBSITE=$website",
        navOptions = navOptions,
    )
}
