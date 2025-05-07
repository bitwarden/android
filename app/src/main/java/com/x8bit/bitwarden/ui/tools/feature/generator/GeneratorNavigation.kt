package com.x8bit.bitwarden.ui.tools.feature.generator

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import com.x8bit.bitwarden.ui.tools.feature.generator.model.GeneratorMode
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the generator screen.
 */
@Serializable
sealed class GeneratorRoute {
    /**
     * The type-safe route for the standard generator screen.
     */
    @Serializable
    data object Standard : GeneratorRoute()

    /**
     * The type-safe route for the modal generator screen.
     */
    @Serializable
    data class Modal(
        val type: ModalType,
        val website: String?,
    ) : GeneratorRoute()
}

/**
 * Indicates the type of modal to be displayed.
 */
@Serializable
enum class ModalType {
    PASSWORD,
    USERNAME,
}

/**
 * Class to retrieve vault item listing arguments from the [SavedStateHandle].
 */
data class GeneratorArgs(
    val type: GeneratorMode,
)

/**
 * Constructs a [GeneratorArgs] from the [SavedStateHandle] and internal route data.
 */
fun SavedStateHandle.toGeneratorArgs(): GeneratorArgs {
    return GeneratorArgs(
        type = try {
            this.toModalGeneratorMode()
        } catch (_: Exception) {
            GeneratorMode.Default
        },
    )
}

private fun SavedStateHandle.toModalGeneratorMode(): GeneratorMode.Modal {
    val route = this.toRoute<GeneratorRoute.Modal>()
    return when (route.type) {
        ModalType.PASSWORD -> GeneratorMode.Modal.Password
        ModalType.USERNAME -> GeneratorMode.Modal.Username(website = route.website)
    }
}

/**
 * Add generator destination to the root nav graph.
 */
fun NavGraphBuilder.generatorDestination(
    onNavigateToPasswordHistory: () -> Unit,
    onDimNavBarRequest: (Boolean) -> Unit,
) {
    composable<GeneratorRoute.Standard> {
        GeneratorScreen(
            onNavigateToPasswordHistory = onNavigateToPasswordHistory,
            onNavigateBack = {},
            onDimNavBarRequest = onDimNavBarRequest,
        )
    }
}

/**
 * Add the generator modal destination to the nav graph.
 */
fun NavGraphBuilder.generatorModalDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions<GeneratorRoute.Modal> {
        GeneratorScreen(
            onNavigateToPasswordHistory = {},
            onNavigateBack = onNavigateBack,
            onDimNavBarRequest = {},
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
    navigate(
        route = GeneratorRoute.Modal(
            type = when (mode) {
                GeneratorMode.Modal.Password -> ModalType.PASSWORD
                is GeneratorMode.Modal.Username -> ModalType.USERNAME
            },
            website = (mode as? GeneratorMode.Modal.Username)?.website,
        ),
        navOptions = navOptions,
    )
}
