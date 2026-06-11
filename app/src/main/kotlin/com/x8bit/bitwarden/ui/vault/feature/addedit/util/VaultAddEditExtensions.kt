package com.x8bit.bitwarden.ui.vault.feature.addedit.util

import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.concat
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditState
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType

/**
 * Default, "select" Text to show on multi select buttons in the VaultAddEdit package.
 */
val SELECT_TEXT: Text
    get() = "-- "
        .asText()
        .concat(BitwardenString.select.asText())
        .concat(" --".asText())

/**
 * Transforms a [VaultItemCipherType] into [VaultAddEditState.ViewState.Content.ItemType].
 */
fun VaultItemCipherType.toItemType(): VaultAddEditState.ViewState.Content.ItemType =
    when (this) {
        VaultItemCipherType.LOGIN -> VaultAddEditState.ViewState.Content.ItemType.Login()
        VaultItemCipherType.CARD -> VaultAddEditState.ViewState.Content.ItemType.Card()
        VaultItemCipherType.IDENTITY -> VaultAddEditState.ViewState.Content.ItemType.Identity()
        VaultItemCipherType.SECURE_NOTE -> VaultAddEditState.ViewState.Content.ItemType.SecureNotes
        VaultItemCipherType.SSH_KEY -> VaultAddEditState.ViewState.Content.ItemType.SshKey()
        VaultItemCipherType.BANK_ACCOUNT -> {
            VaultAddEditState.ViewState.Content.ItemType.BankAccount()
        }
        VaultItemCipherType.DRIVERS_LICENSE -> {
            VaultAddEditState.ViewState.Content.ItemType.License()
        }
        VaultItemCipherType.PASSPORT -> {
            VaultAddEditState.ViewState.Content.ItemType.Passport()
        }
    }

/**
 * Returns a copy of the [VaultAddEditState.ViewState] with the authenticator key Premium gate
 * applied to its Login content (if any). Used to seed the gate for Add mode, where the Login
 * state is constructed by factories that have no premium context. Edit and Clone modes already
 * set the gate via `CipherView.toViewState`, which additionally honors `organizationUseTotp`.
 */
fun VaultAddEditState.ViewState.withAuthenticatorKeyPremiumGate(
    isPremium: Boolean,
): VaultAddEditState.ViewState {
    val content = this as? VaultAddEditState.ViewState.Content ?: return this
    val login = content.type as? VaultAddEditState.ViewState.Content.ItemType.Login ?: return this
    return content.copy(
        type = login.copy(isAuthenticatorKeyPremiumGated = !isPremium),
    )
}
