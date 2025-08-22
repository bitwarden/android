package com.x8bit.bitwarden.data.vault.repository.util

import com.bitwarden.collections.Collection
import com.bitwarden.collections.CollectionType
import com.bitwarden.collections.CollectionView
import com.bitwarden.core.data.repository.util.SpecialCharWithPrecedenceComparator
import com.bitwarden.network.model.CollectionTypeJson
import com.bitwarden.network.model.SyncResponseJson

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
        manage = this.canManage ?: !this.isReadOnly,
        defaultUserCollectionEmail = this.defaultUserCollectionEmail,
        type = this.type.toCollectionType(),
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
fun List<CollectionView>.sortAlphabetically(): List<CollectionView> {
    return this.sortedWith(
        comparator = { collection1, collection2 ->
            SpecialCharWithPrecedenceComparator.compare(collection1.name, collection2.name)
        },
    )
}

/**
 * Converts a [CollectionType] object to a corresponding
 * Bitwarden SDK [CollectionTypeJson] object.
 */
fun CollectionTypeJson.toCollectionType(): CollectionType =
    when (this) {
        CollectionTypeJson.SHARED_COLLECTION -> CollectionType.SHARED_COLLECTION
        CollectionTypeJson.DEFAULT_USER_COLLECTION -> CollectionType.DEFAULT_USER_COLLECTION
    }
