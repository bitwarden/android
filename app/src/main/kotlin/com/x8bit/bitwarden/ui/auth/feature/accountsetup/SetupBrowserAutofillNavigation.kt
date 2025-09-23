package com.x8bit.bitwarden.ui.auth.feature.accountsetup

import android.os.Parcelable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.ui.platform.base.util.composableWithPushTransitions
import com.bitwarden.ui.platform.util.ParcelableRouteSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the setup browser autofill screen.
 */
@Parcelize
@Serializable(with = SetupBrowserAutofillRoute.Serializer::class)
data object SetupBrowserAutofillRoute : Parcelable {
    /**
     * Custom serializer for this route.
     */
    class Serializer : ParcelableRouteSerializer<SetupBrowserAutofillRoute>(
        kClass = SetupBrowserAutofillRoute::class,
    )
}

/**
 * Navigate to the setup browser autofill screen.
 */
fun NavController.navigateToSetupBrowserAutofillScreen(navOptions: NavOptions? = null) {
    this.navigate(route = SetupBrowserAutofillRoute, navOptions = navOptions)
}

/**
 * Add the setup browser autofill screen to the nav graph.
 */
fun NavGraphBuilder.setupBrowserAutofillDestination() {
    composableWithPushTransitions<SetupBrowserAutofillRoute> {
        SetupBrowserAutofillScreen()
    }
}
