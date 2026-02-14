@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.platform.feature.cookieacquisition

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the cookie acquisition screen.
 */
@OmitFromCoverage
@Serializable
data object CookieAcquisitionRoute

/**
 * Add the cookie acquisition screen to the nav graph.
 */
fun NavGraphBuilder.cookieAcquisitionDestination() {
    composableWithSlideTransitions<CookieAcquisitionRoute> {
        CookieAcquisitionScreen()
    }
}

/**
 * Navigate to the cookie acquisition screen.
 */
fun NavController.navigateToCookieAcquisition(
    navOptions: NavOptions? = null,
) {
    this.navigate(
        route = CookieAcquisitionRoute,
        navOptions = navOptions,
    )
}
