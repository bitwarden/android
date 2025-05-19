package com.x8bit.bitwarden.ui.tools.feature.send.addsend

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import com.x8bit.bitwarden.ui.tools.feature.send.addsend.model.AddSendType
import com.x8bit.bitwarden.ui.tools.feature.send.model.SendItemType
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the add send screen.
 */
@Serializable
data class AddEditSendRoute(
    val sendType: SendItemType,
    val modeType: ModeType,
    val sendId: String? = null,
)

/**
 * Indicates the mode of send to be displayed.
 */
@Serializable
enum class ModeType {
    ADD,
    EDIT,
}

/**
 * Class to retrieve send add & edit arguments from the [SavedStateHandle].
 */
data class AddSendArgs(
    val sendType: SendItemType,
    val sendAddType: AddSendType,
)

/**
 * Constructs a [AddSendArgs] from the [SavedStateHandle] and internal route data.
 */
fun SavedStateHandle.toAddSendArgs(): AddSendArgs {
    val route = this.toRoute<AddEditSendRoute>()
    return AddSendArgs(
        sendType = route.sendType,
        sendAddType = when (route.modeType) {
            ModeType.ADD -> AddSendType.AddItem
            ModeType.EDIT -> AddSendType.EditItem(sendItemId = requireNotNull(route.sendId))
        },
    )
}

/**
 * Add the new send screen to the nav graph.
 */
fun NavGraphBuilder.addSendDestination(
    onNavigateBack: () -> Unit,
    onNavigateUpToRoot: () -> Unit,
) {
    composableWithSlideTransitions<AddEditSendRoute> {
        AddSendScreen(
            onNavigateBack = onNavigateBack,
            onNavigateUpToRoot = onNavigateUpToRoot,
        )
    }
}

/**
 * Navigate to the new send screen.
 */
fun NavController.navigateToAddSend(
    route: AddEditSendRoute,
    navOptions: NavOptions? = null,
) {
    this.navigate(route = route, navOptions = navOptions)
}
