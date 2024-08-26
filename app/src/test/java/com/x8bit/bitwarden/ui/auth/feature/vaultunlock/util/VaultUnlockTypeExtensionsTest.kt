package com.x8bit.bitwarden.ui.auth.feature.vaultunlock.util

import androidx.compose.ui.text.input.KeyboardType
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.model.VaultUnlockType
import com.x8bit.bitwarden.ui.platform.base.util.asText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VaultUnlockTypeExtensionsTest {
    @Test
    fun `unlockScreenTitle should return the correct title for each type`() {
        mapOf(
            VaultUnlockType.MASTER_PASSWORD to R.string.verify_master_password.asText(),
            VaultUnlockType.PIN to R.string.verify_pin.asText(),
        )
            .forEach { (type, expected) ->
                assertEquals(
                    expected,
                    type.unlockScreenTitle,
                )
            }
    }

    @Test
    fun `unlockScreenMessage should return the correct title for each type`() {
        mapOf(
            VaultUnlockType.MASTER_PASSWORD to R.string.vault_locked_master_password.asText(),
            VaultUnlockType.PIN to R.string.vault_locked_pin.asText(),
        )
            .forEach { (type, expected) ->
                assertEquals(
                    expected,
                    type.unlockScreenMessage,
                )
            }
    }

    @Test
    fun `unlockScreenInputLabel should return the correct title for each type`() {
        mapOf(
            VaultUnlockType.MASTER_PASSWORD to R.string.master_password.asText(),
            VaultUnlockType.PIN to R.string.pin.asText(),
        )
            .forEach { (type, expected) ->
                assertEquals(
                    expected,
                    type.unlockScreenInputLabel,
                )
            }
    }

    @Test
    fun `unlockScreenErrorMessage should return the correct title for each type`() {
        mapOf(
            VaultUnlockType.MASTER_PASSWORD to R.string.invalid_master_password.asText(),
            VaultUnlockType.PIN to R.string.invalid_pin.asText(),
        )
            .forEach { (type, expected) ->
                assertEquals(
                    expected,
                    type.unlockScreenErrorMessage,
                )
            }
    }

    @Test
    fun `unlockScreenKeyboardType should return the correct title for each type`() {
        mapOf(
            VaultUnlockType.MASTER_PASSWORD to KeyboardType.Password,
            VaultUnlockType.PIN to KeyboardType.Number,
        )
            .forEach { (type, expected) ->
                assertEquals(
                    expected,
                    type.unlockScreenKeyboardType,
                )
            }
    }

    @Test
    fun `emptyInputDialogMessage should return the correct title for each type`() {
        mapOf(
            VaultUnlockType.MASTER_PASSWORD to R.string.validation_field_required.asText(
                R.string.master_password.asText(),
            ),
            VaultUnlockType.PIN to R.string.validation_field_required.asText(R.string.pin.asText()),
        )
            .forEach { (type, expected) ->
                assertEquals(
                    expected,
                    type.emptyInputDialogMessage,
                )
            }
    }
}
