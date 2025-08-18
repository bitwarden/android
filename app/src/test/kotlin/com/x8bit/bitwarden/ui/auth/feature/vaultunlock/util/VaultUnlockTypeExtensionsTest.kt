package com.x8bit.bitwarden.ui.auth.feature.vaultunlock.util

import androidx.compose.ui.text.input.KeyboardType
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.auth.repository.model.VaultUnlockType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VaultUnlockTypeExtensionsTest {
    @Test
    fun `unlockScreenTitle should return the correct title for each type`() {
        mapOf(
            VaultUnlockType.MASTER_PASSWORD to BitwardenString.verify_master_password.asText(),
            VaultUnlockType.PIN to BitwardenString.verify_pin.asText(),
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
            VaultUnlockType.MASTER_PASSWORD to
                BitwardenString.vault_locked_master_password.asText(),
            VaultUnlockType.PIN to BitwardenString.vault_locked_pin.asText(),
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
            VaultUnlockType.MASTER_PASSWORD to BitwardenString.master_password.asText(),
            VaultUnlockType.PIN to BitwardenString.pin.asText(),
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
            VaultUnlockType.MASTER_PASSWORD to BitwardenString.invalid_master_password.asText(),
            VaultUnlockType.PIN to BitwardenString.invalid_pin.asText(),
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
            VaultUnlockType.MASTER_PASSWORD to BitwardenString.validation_field_required.asText(
                BitwardenString.master_password.asText(),
            ),
            VaultUnlockType.PIN to
                BitwardenString.validation_field_required.asText(BitwardenString.pin.asText()),
        )
            .forEach { (type, expected) ->
                assertEquals(
                    expected,
                    type.emptyInputDialogMessage,
                )
            }
    }
}
