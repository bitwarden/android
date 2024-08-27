package com.x8bit.bitwarden.ui.auth.feature.vaultunlock.util

import androidx.compose.ui.text.input.KeyboardType
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.model.VaultUnlockType
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText

/**
 * A title to use on the Vault Unlock screen.
 */
val VaultUnlockType.unlockScreenTitle: Text
    get() = when (this) {
        VaultUnlockType.MASTER_PASSWORD -> R.string.verify_master_password
        VaultUnlockType.PIN -> R.string.verify_pin
    }
        .asText()

/**
 * A descriptive message to use on the Vault Unlock screen.
 */
val VaultUnlockType.unlockScreenMessage: Text
    get() = when (this) {
        VaultUnlockType.MASTER_PASSWORD -> R.string.vault_locked_master_password
        VaultUnlockType.PIN -> R.string.vault_locked_pin
    }
        .asText()

/**
 * The semantic test tag to use for the main input field on the Vault Unlock screen.
 */
val VaultUnlockType.unlockScreenInputTestTag: String
    get() = when (this) {
        VaultUnlockType.MASTER_PASSWORD -> "MasterPasswordEntry"
        VaultUnlockType.PIN -> "PinEntry"
    }

/**
 * The label for the main text input to use on the Vault Unlock screen.
 */
val VaultUnlockType.unlockScreenInputLabel: Text
    get() = when (this) {
        VaultUnlockType.MASTER_PASSWORD -> R.string.master_password
        VaultUnlockType.PIN -> R.string.pin
    }
        .asText()

/**
 * The semantic test tag to use for the hide button in the input field on the Vault Unlock screen.
 */
val VaultUnlockType.inputFieldVisibilityToggleTestTag: String
    get() = when (this) {
        VaultUnlockType.MASTER_PASSWORD -> "PasswordVisibilityToggle"
        VaultUnlockType.PIN -> "PinVisibilityToggle"
    }

/**
 * The error message to use for a failed unlock on the Vault Unlock screen.
 */
val VaultUnlockType.unlockScreenErrorMessage: Text
    get() = when (this) {
        VaultUnlockType.MASTER_PASSWORD -> R.string.invalid_master_password
        VaultUnlockType.PIN -> R.string.invalid_pin
    }
        .asText()

/**
 * The [KeyboardType] to use on the input on the Vault Unlock screen.
 */
val VaultUnlockType.unlockScreenKeyboardType: KeyboardType
    get() = when (this) {
        VaultUnlockType.MASTER_PASSWORD -> KeyboardType.Password
        VaultUnlockType.PIN -> KeyboardType.Number
    }

/**
 * The message to show when user try to unlock vault with empty or blank input.
 */
val VaultUnlockType.emptyInputDialogMessage: Text
    get() = R.string.validation_field_required.asText(unlockScreenInputLabel)
