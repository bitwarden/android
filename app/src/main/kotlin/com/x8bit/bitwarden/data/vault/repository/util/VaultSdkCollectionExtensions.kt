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
        type = this.type.toSdkCollectionType(),
    )

/**
 * Converts a list of [SyncResponseJson.Collection] objects to a list of corresponding
 * Bitwarden SDK [Collection] objects.
 */
fun List<SyncResponseJson.Collection>.toEncryptedSdkCollectionList(): List<Collection> =
    map { it.toEncryptedSdkCollection() }

/**
 * Sorts the collections, grouping them by type, with `DEFAULT_USER_COLLECTION` types displayed
 * first. Within each group, collections are sorted alphabetically by name.
 */
@JvmName("toAlphabeticallySortedCollectionList")
fun List<CollectionView>.sortAlphabeticallyByType(): List<CollectionView> {
    return this.sortedWith(
        // DEFAULT_USER_COLLECTION come first
        comparator = compareBy<CollectionView> { it.type != CollectionType.DEFAULT_USER_COLLECTION }
            // Then sort by other CollectionType ordinals
            .thenBy { it.type }
            // Finally, sort by name within each group
            .thenComparing(
                CollectionView::name,
                SpecialCharWithPrecedenceComparator,
            ),
    )
}

/**
 * Converts a [CollectionType] object to a corresponding
 * Bitwarden SDK [CollectionTypeJson] object.
 */
fun CollectionTypeJson.toSdkCollectionType(): CollectionType =
    when (this) {
        CollectionTypeJson.SHARED_COLLECTION -> CollectionType.SHARED_COLLECTION
        CollectionTypeJson.DEFAULT_USER_COLLECTION -> CollectionType.DEFAULT_USER_COLLECTION
    }
