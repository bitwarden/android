package com.x8bit.bitwarden.ui.vault.feature.addedit.util

import com.x8bit.bitwarden.data.autofill.password.model.PasswordCredentialRequest
import com.x8bit.bitwarden.data.platform.util.toUriOrNull
import com.x8bit.bitwarden.ui.platform.base.util.toAndroidAppUriString
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditState
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.UriItem
import java.util.UUID

/**
 * Returns pre-filled content that may be used for an "add" type
 * [VaultAddEditState.ViewState.Content] during Password credential creation.
 */
fun PasswordCredentialRequest.toDefaultAddTypeContent(
    isIndividualVaultDisabled: Boolean,
): VaultAddEditState.ViewState.Content {

    val rpUri = origin
        ?.toUriOrNull()
        ?.toString()
        ?: packageName
            .toAndroidAppUriString()

    val rpName = origin
        ?.toUriOrNull()
        ?.toString()
        ?: packageName

    return VaultAddEditState.ViewState.Content(
        common = VaultAddEditState.ViewState.Content.Common(
            name = rpName,
        ),
        isIndividualVaultDisabled = isIndividualVaultDisabled,
        type = VaultAddEditState.ViewState.Content.ItemType.Login(
            username = this.userName,
            password = this.password,
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
