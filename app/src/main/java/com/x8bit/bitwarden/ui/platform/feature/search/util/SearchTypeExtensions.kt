package com.x8bit.bitwarden.ui.platform.feature.search.util

import com.x8bit.bitwarden.ui.platform.feature.search.SearchTypeData
import com.x8bit.bitwarden.ui.platform.feature.search.model.SearchType

/**
 * Transforms a [SearchType] into a [SearchTypeData].
 */
fun SearchType.toSearchTypeData(): SearchTypeData =
    when (this) {
        SearchType.Sends.All -> SearchTypeData.Sends.All
        SearchType.Sends.Files -> SearchTypeData.Sends.Files
        SearchType.Sends.Texts -> SearchTypeData.Sends.Texts
        SearchType.Vault.All -> SearchTypeData.Vault.All
        SearchType.Vault.Cards -> SearchTypeData.Vault.Cards
        is SearchType.Vault.Collection -> SearchTypeData.Vault.Collection(collectionId)
        is SearchType.Vault.Folder -> SearchTypeData.Vault.Folder(folderId)
        SearchType.Vault.Identities -> SearchTypeData.Vault.Identities
        SearchType.Vault.Logins -> SearchTypeData.Vault.Logins
        SearchType.Vault.NoFolder -> SearchTypeData.Vault.NoFolder
        SearchType.Vault.SecureNotes -> SearchTypeData.Vault.SecureNotes
        SearchType.Vault.Trash -> SearchTypeData.Vault.Trash
        SearchType.Vault.VerificationCodes -> SearchTypeData.Vault.VerificationCodes
        SearchType.Vault.SshKeys -> SearchTypeData.Vault.SshKeys
    }
