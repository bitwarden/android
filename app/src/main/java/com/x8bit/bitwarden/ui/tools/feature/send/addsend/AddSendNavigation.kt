package com.x8bit.bitwarden.ui.tools.feature.send.addsend

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import com.x8bit.bitwarden.ui.tools.feature.send.addsend.model.AddSendType

private const val ADD_TYPE: String = "add"
private const val EDIT_TYPE: String = "edit"
private const val EDIT_ITEM_ID: String = "edit_send_id"

private const val ADD_SEND_ITEM_PREFIX: String = "add_send_item"
private const val ADD_SEND_ITEM_TYPE: String = "add_send_item_type"

const val ADD_SEND_ROUTE: String =
    "$ADD_SEND_ITEM_PREFIX/{$ADD_SEND_ITEM_TYPE}?$EDIT_ITEM_ID={$EDIT_ITEM_ID}"

/**
 * Class to retrieve send add & edit arguments from the [SavedStateHandle].
 */
@OmitFromCoverage
data class AddSendArgs(
    val sendAddType: AddSendType,
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        sendAddType = when (requireNotNull(savedStateHandle.get<String>(ADD_SEND_ITEM_TYPE))) {
            ADD_TYPE -> AddSendType.AddItem
            EDIT_TYPE -> AddSendType.EditItem(requireNotNull(savedStateHandle[EDIT_ITEM_ID]))
            else -> throw IllegalStateException("Unknown VaultAddEditType.")
        },
    )
}

/**
 * Add the new send screen to the nav graph.
 */
fun NavGraphBuilder.addSendDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions(
        route = ADD_SEND_ROUTE,
        arguments = listOf(
            navArgument(ADD_SEND_ITEM_TYPE) {
                type = NavType.StringType
            },
        ),
    ) {
        AddSendScreen(onNavigateBack = onNavigateBack)
    }
}

/**
 * Navigate to the new send screen.
 */
fun NavController.navigateToAddSend(
    sendAddType: AddSendType,
    navOptions: NavOptions? = null,
) {
    navigate(
        route = "$ADD_SEND_ITEM_PREFIX/${sendAddType.toTypeString()}" +
            "?${EDIT_ITEM_ID}=${sendAddType.toIdOrNull()}",
        navOptions = navOptions,
    )
}

private fun AddSendType.toTypeString(): String =
    when (this) {
        is AddSendType.AddItem -> ADD_TYPE
        is AddSendType.EditItem -> EDIT_TYPE
    }

private fun AddSendType.toIdOrNull(): String? =
    (this as? AddSendType.EditItem)?.sendItemId
