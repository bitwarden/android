package com.x8bit.bitwarden.ui.vault.feature.addedit.util

import com.bitwarden.ui.platform.base.util.toAndroidAppUriString
import com.x8bit.bitwarden.data.credentials.model.CreateCredentialRequest
import com.x8bit.bitwarden.data.credentials.model.PasskeyAttestationOptions
import com.x8bit.bitwarden.data.platform.util.toUriOrNull
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditState
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.UriItem
import java.util.UUID

/**
 * Returns pre-filled content that may be used for an "add" type
 * [VaultAddEditState.ViewState.Content] during FIDO 2 or Password credential creation.
 */
fun CreateCredentialRequest.toDefaultAddTypeContent(
    attestationOptions: PasskeyAttestationOptions?,
    isIndividualVaultDisabled: Boolean,
): VaultAddEditState.ViewState.Content {

    val rpUri = providerRequest.callingRequest.origin
        ?.toUriOrNull()
        ?.toString()
        ?: callingAppInfo.packageName
            .toAndroidAppUriString()

    val rpName = attestationOptions
        ?.relyingParty
        ?.name
        ?: callingAppInfo.packageName

    val username = attestationOptions
        ?.user
        ?.name
        ?: createPasswordCredentialRequest
            ?.id
            .orEmpty()

    val password = createPasswordCredentialRequest
        ?.password
        .orEmpty()

    return VaultAddEditState.ViewState.Content(
        common = VaultAddEditState.ViewState.Content.Common(
            name = rpName,
        ),
        isIndividualVaultDisabled = isIndividualVaultDisabled,
        type = VaultAddEditState.ViewState.Content.ItemType.Login(
            username = username,
            password = password,
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
