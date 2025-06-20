package com.x8bit.bitwarden.ui.tools.feature.generator.passwordhistory

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import com.x8bit.bitwarden.ui.tools.feature.generator.model.GeneratorPasswordHistoryMode
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the password history screen.
 */
@Serializable
data class PasswordHistoryRoute(
    val passwordHistoryType: PasswordHistoryType,
    val itemId: String?,
)

/**
 * Indicates the type of password to be displayed.
 */
@Serializable
enum class PasswordHistoryType {
    DEFAULT,
    ITEM,
}

/**
 * Class to retrieve password history arguments from the [SavedStateHandle].
 */
data class PasswordHistoryArgs(
    val passwordHistoryMode: GeneratorPasswordHistoryMode,
)

/**
 * Constructs a [PasswordHistoryArgs] from the [SavedStateHandle] and internal route data.
 */
fun SavedStateHandle.toPasswordHistoryArgs(): PasswordHistoryArgs {
    val route = this.toRoute<PasswordHistoryRoute>()
    return PasswordHistoryArgs(
        passwordHistoryMode = when (route.passwordHistoryType) {
            PasswordHistoryType.DEFAULT -> GeneratorPasswordHistoryMode.Default
            PasswordHistoryType.ITEM -> GeneratorPasswordHistoryMode.Item(
                itemId = requireNotNull(route.itemId),
            )
        },
    )
}

/**
 * Add password history destination to the graph.
 */
fun NavGraphBuilder.passwordHistoryDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions<PasswordHistoryRoute> {
        PasswordHistoryScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the Password History Screen.
 */
fun NavController.navigateToPasswordHistory(
    passwordHistoryMode: GeneratorPasswordHistoryMode,
    navOptions: NavOptions? = null,
) {
    navigate(
        route = PasswordHistoryRoute(
            passwordHistoryType = when (passwordHistoryMode) {
                GeneratorPasswordHistoryMode.Default -> PasswordHistoryType.DEFAULT
                is GeneratorPasswordHistoryMode.Item -> PasswordHistoryType.ITEM
            },
            itemId = (passwordHistoryMode as? GeneratorPasswordHistoryMode.Item)?.itemId,
        ),
        navOptions = navOptions,
    )
}
