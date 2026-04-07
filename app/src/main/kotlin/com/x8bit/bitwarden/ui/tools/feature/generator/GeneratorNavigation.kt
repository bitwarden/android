package com.x8bit.bitwarden.ui.tools.feature.generator

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import com.bitwarden.ui.platform.base.util.composableWithRootPushTransitions
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import com.bitwarden.ui.platform.util.ParcelableRouteSerializer
import com.x8bit.bitwarden.ui.tools.feature.generator.model.GeneratorMode
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the generator screen.
 */
@Parcelize
@Serializable(with = GeneratorRoute.Serializer::class)
sealed class GeneratorRoute : Parcelable {

    /**
     * Custom serializer to support polymorphic routes.
     */
    class Serializer : ParcelableRouteSerializer<GeneratorRoute>(GeneratorRoute::class)

    /**
     * The type-safe route for the standard generator screen.
     */
    @Parcelize
    @Serializable(with = Standard.Serializer::class)
    data object Standard : GeneratorRoute() {
        /**
         * Custom serializer to support polymorphic routes.
         */
        class Serializer : ParcelableRouteSerializer<Standard>(Standard::class)
    }

    /**
     * The type-safe route for the modal generator screen.
     */
    @Parcelize
    @Serializable(with = Modal.Serializer::class)
    data class Modal(
        val type: ModalType,
        val website: String?,
    ) : GeneratorRoute() {
        /**
         * Custom serializer to support polymorphic routes.
         */
        class Serializer : ParcelableRouteSerializer<Modal>(Modal::class)
    }
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
        type = when (val route = this.toRoute<GeneratorRoute>()) {
            is GeneratorRoute.Modal -> when (route.type) {
                ModalType.PASSWORD -> GeneratorMode.Modal.Password
                ModalType.USERNAME -> GeneratorMode.Modal.Username(website = route.website)
            }

            GeneratorRoute.Standard -> GeneratorMode.Default
        },
    )
}

/**
 * Add generator destination to the root nav graph.
 */
fun NavGraphBuilder.generatorDestination(
    onNavigateToPasswordHistory: () -> Unit,
    onDimNavBarRequest: (Boolean) -> Unit,
) {
    composableWithRootPushTransitions<GeneratorRoute.Standard> {
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
