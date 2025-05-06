@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.platform.feature.settings.other

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.core.annotation.OmitFromCoverage
import com.x8bit.bitwarden.ui.platform.base.util.composableWithPushTransitions
import com.x8bit.bitwarden.ui.platform.util.toObjectRoute
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the settings other screen.
 */
@Serializable
sealed class SettingsOtherRoute {
    /**
     * Indicates that the settings other screen should be shown as a pre-authentication.
     */
    abstract val isPreAuth: Boolean

    /**
     * The type-safe route for the settings other screen.
     */
    @Serializable
    data object Standard : SettingsOtherRoute() {
        override val isPreAuth: Boolean get() = false
    }

    /**
     * The type-safe route for the pre-auth settings other screen.
     */
    @Serializable
    data object PreAuth : SettingsOtherRoute() {
        override val isPreAuth: Boolean get() = true
    }
}

/**
 * Class to retrieve other settings arguments from the [SavedStateHandle].
 */
data class OtherArgs(val isPreAuth: Boolean)

/**
 * Constructs a [OtherArgs] from the [SavedStateHandle] and internal route data.
 */
fun SavedStateHandle.toOtherArgs(): OtherArgs {
    val route = this.toObjectRoute<SettingsOtherRoute.PreAuth>()
        ?: this.toObjectRoute<SettingsOtherRoute.Standard>()
    return route
        ?.let { OtherArgs(isPreAuth = it.isPreAuth) }
        ?: throw IllegalStateException("Missing correct route for SettingsOtherScreen")
}

/**
 * Add settings destinations to the nav graph.
 */
fun NavGraphBuilder.otherDestination(
    isPreAuth: Boolean,
    onNavigateBack: () -> Unit,
) {
    if (isPreAuth) {
        composableWithPushTransitions<SettingsOtherRoute.PreAuth> {
            OtherScreen(onNavigateBack = onNavigateBack)
        }
    } else {
        composableWithPushTransitions<SettingsOtherRoute.Standard> {
            OtherScreen(onNavigateBack = onNavigateBack)
        }
    }
}

/**
 * Navigate to the about screen.
 */
fun NavController.navigateToOther(
    isPreAuth: Boolean,
    navOptions: NavOptions? = null,
) {
    this.navigate(
        route = if (isPreAuth) SettingsOtherRoute.PreAuth else SettingsOtherRoute.Standard,
        navOptions = navOptions,
    )
}
