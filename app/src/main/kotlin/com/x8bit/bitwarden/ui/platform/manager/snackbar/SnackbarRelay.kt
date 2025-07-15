package com.x8bit.bitwarden.ui.platform.manager.snackbar

import com.x8bit.bitwarden.ui.platform.components.snackbar.BitwardenSnackbarData
import kotlinx.serialization.Serializable

/**
 * Models a relay key to be mapped to an instance of [BitwardenSnackbarData] being sent
 * between producers and consumers of the data.
 */
@Serializable
enum class SnackbarRelay {
    CIPHER_DELETED,
    CIPHER_MOVED_TO_ORGANIZATION,
    CIPHER_RESTORED,
    ENVIRONMENT_SAVED,
    LOGIN_APPROVAL,
    LOGIN_SUCCESS,
    LOGINS_IMPORTED,
    SEND_DELETED,
    SEND_UPDATED,
}
