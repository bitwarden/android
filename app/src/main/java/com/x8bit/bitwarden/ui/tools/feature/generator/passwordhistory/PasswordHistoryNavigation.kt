package com.x8bit.bitwarden.ui.tools.feature.generator.passwordhistory

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import com.x8bit.bitwarden.ui.tools.feature.generator.model.GeneratorPasswordHistoryMode

private const val DEFAULT_MODE: String = "default"
private const val ITEM_MODE: String = "item"

private const val PASSWORD_HISTORY_PREFIX: String = "password_history"
private const val PASSWORD_HISTORY_MODE: String = "password_history_mode"
private const val PASSWORD_HISTORY_ITEM_ID: String = "password_history_id"

private const val PASSWORD_HISTORY_ROUTE: String =
    PASSWORD_HISTORY_PREFIX +
        "/{$PASSWORD_HISTORY_MODE}" +
        "?$PASSWORD_HISTORY_ITEM_ID={$PASSWORD_HISTORY_ITEM_ID}"

/**
 * Class to retrieve password history arguments from the [SavedStateHandle].
 */
@OmitFromCoverage
data class PasswordHistoryArgs(
    val passwordHistoryMode: GeneratorPasswordHistoryMode,
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        passwordHistoryMode = when (requireNotNull(savedStateHandle[PASSWORD_HISTORY_MODE])) {
            DEFAULT_MODE -> GeneratorPasswordHistoryMode.Default
            ITEM_MODE -> GeneratorPasswordHistoryMode.Item(
                requireNotNull(savedStateHandle[PASSWORD_HISTORY_ITEM_ID]),
            )

            else -> throw IllegalStateException("Unknown VaultAddEditType.")
        },
    )
}

/**
 * Add password history destination to the graph.
 */
fun NavGraphBuilder.passwordHistoryDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions(
        route = PASSWORD_HISTORY_ROUTE,
        arguments = listOf(
            navArgument(PASSWORD_HISTORY_MODE) { type = NavType.StringType },
        ),
    ) {
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
        route = "$PASSWORD_HISTORY_PREFIX/${passwordHistoryMode.toModeString()}" +
            "?$PASSWORD_HISTORY_ITEM_ID=${passwordHistoryMode.toIdOrNull()}",
        navOptions = navOptions,
    )
}

private fun GeneratorPasswordHistoryMode.toModeString(): String =
    when (this) {
        is GeneratorPasswordHistoryMode.Default -> DEFAULT_MODE
        is GeneratorPasswordHistoryMode.Item -> ITEM_MODE
    }

private fun GeneratorPasswordHistoryMode.toIdOrNull(): String? =
    when (this) {
        is GeneratorPasswordHistoryMode.Default -> null
        is GeneratorPasswordHistoryMode.Item -> itemId
    }
