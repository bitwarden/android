package com.x8bit.bitwarden.ui.tools.feature.send.viewsend

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.AddEditSendRoute
import com.x8bit.bitwarden.ui.tools.feature.send.model.SendItemType
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the view send screen.
 */
@Serializable
data class ViewSendRoute(
    val sendId: String,
    val sendType: SendItemType,
)

/**
 * Class to retrieve vault item arguments from the [SavedStateHandle].
 */
data class ViewSendArgs(
    val sendId: String,
    val sendType: SendItemType,
)

/**
 * Constructs a [ViewSendArgs] from the [SavedStateHandle] and internal route data.
 */
fun SavedStateHandle.toViewSendArgs(): ViewSendArgs {
    val route = this.toRoute<ViewSendRoute>()
    return ViewSendArgs(sendId = route.sendId, sendType = route.sendType)
}

/**
 * Add the view send screen to the nav graph.
 */
fun NavGraphBuilder.viewSendDestination(
    onNavigateBack: () -> Unit,
    onNavigateToAddEditSend: (route: AddEditSendRoute) -> Unit,
) {
    composableWithSlideTransitions<ViewSendRoute> {
        ViewSendScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToAddEditSend = onNavigateToAddEditSend,
        )
    }
}

/**
 * Navigate to the view send screen.
 */
fun NavController.navigateToViewSend(
    route: ViewSendRoute,
    navOptions: NavOptions? = null,
) {
    this.navigate(route = route, navOptions = navOptions)
}
