package com.x8bit.bitwarden.ui.platform.manager.snackbar

import com.x8bit.bitwarden.ui.platform.components.snackbar.BitwardenSnackbarData

/**
 * Models a relay key to be mapped to an instance of [BitwardenSnackbarData] being sent
 * between producers and consumers of the data.
 */
enum class SnackbarRelay {
    VAULT_SETTINGS_RELAY,
    MY_VAULT_RELAY,
}
