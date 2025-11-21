package com.bitwarden.authenticator.ui.platform.model

import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import kotlinx.serialization.Serializable

/**
 * Models a relay key to be mapped to an instance of [BitwardenSnackbarData] being sent
 * between producers and consumers of the data.
 */
@Serializable
enum class SnackbarRelay {
    IMPORT_SUCCESS,
    ITEM_ADDED,
    ITEM_SAVED,
}
