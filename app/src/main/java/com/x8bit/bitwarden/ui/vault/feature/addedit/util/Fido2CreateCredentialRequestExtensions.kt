package com.x8bit.bitwarden.ui.vault.feature.addedit.util

import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CreateCredentialRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.PasskeyAttestationOptions
import com.x8bit.bitwarden.data.platform.util.toUriOrNull
import com.x8bit.bitwarden.ui.platform.base.util.toAndroidAppUriString
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditState
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.UriItem
import java.util.UUID

/**
 * Returns pre-filled content that may be used for an "add" type
 * [VaultAddEditState.ViewState.Content] during FIDO 2 credential creation.
 */
fun Fido2CreateCredentialRequest.toDefaultAddTypeContent(
    attestationOptions: PasskeyAttestationOptions?,
    isIndividualVaultDisabled: Boolean,
): VaultAddEditState.ViewState.Content {

    val rpUri = origin
        ?.toUriOrNull()
        ?.toString()
        ?: packageName
            .toAndroidAppUriString()

    val rpName = attestationOptions
        ?.relyingParty
        ?.name
        .orEmpty()

    val username = attestationOptions
        ?.user
        ?.name
        .orEmpty()

    return VaultAddEditState.ViewState.Content(
        common = VaultAddEditState.ViewState.Content.Common(
            name = rpName,
        ),
        isIndividualVaultDisabled = isIndividualVaultDisabled,
        type = VaultAddEditState.ViewState.Content.ItemType.Login(
            username = username,
            uriList = listOf(
                UriItem(
                    id = UUID.randomUUID().toString(),
                    uri = rpUri,
                    match = null,
                    checksum = null,
                ),
            ),
        ),
    )
}
