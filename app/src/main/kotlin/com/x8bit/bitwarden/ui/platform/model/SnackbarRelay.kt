package com.x8bit.bitwarden.ui.platform.model

import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import kotlinx.serialization.Serializable

/**
 * Models a relay key to be mapped to an instance of [BitwardenSnackbarData] being sent
 * between producers and consumers of the data.
 */
@Serializable
enum class SnackbarRelay {
    CIPHER_CREATED,
    CIPHER_DELETED,
    CIPHER_DELETED_SOFT,
    CIPHER_MOVED_TO_ORGANIZATION,
    CIPHER_RESTORED,
    CIPHER_UPDATED,
    ENVIRONMENT_SAVED,
    FOLDER_CREATED,
    FOLDER_DELETED,
    FOLDER_UPDATED,
    LOGIN_APPROVAL,
    LOGIN_SUCCESS,
    LOGINS_IMPORTED,
    SEND_DELETED,
    SEND_UPDATED,
    LEFT_ORGANIZATION,
}
