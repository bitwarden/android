package com.x8bit.bitwarden.data.vault.repository.util

import com.bitwarden.vault.Collection
import com.bitwarden.vault.CollectionView
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import java.util.Locale

/**
 * Converts a [SyncResponseJson.Collection] object to a corresponding Bitwarden SDK [Collection]
 * object.
 */
fun SyncResponseJson.Collection.toEncryptedSdkCollection(): Collection =
    Collection(
        id = this.id,
        organizationId = this.organizationId,
        name = this.name,
        externalId = this.externalId,
        hidePasswords = this.shouldHidePasswords,
        readOnly = this.isReadOnly,
    )

/**
 * Converts a list of [SyncResponseJson.Collection] objects to a list of corresponding
 * Bitwarden SDK [Collection] objects.
 */
fun List<SyncResponseJson.Collection>.toEncryptedSdkCollectionList(): List<Collection> =
    map { it.toEncryptedSdkCollection() }

/**
 * Sorts the data in alphabetical order by name.
 */
@JvmName("toAlphabeticallySortedCollectionList")
fun List<CollectionView>.sortAlphabetically(): List<CollectionView> =
    this.sortedBy { it.name.uppercase(Locale.getDefault()) }
