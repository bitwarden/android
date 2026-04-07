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
 * Sorts a list of [CollectionView] objects based on a multi-level sorting logic.
 *
 * The sorting criteria are as follows, in order of precedence:
 * 1. Collections of type `DEFAULT_USER_COLLECTION` are placed first.
 * 2. All other collections are grouped by their `CollectionType`.
 * 3. Within each group, collections are sorted alphabetically by name. For collections of
 *    type `DEFAULT_USER_COLLECTION`, the corresponding organization's name is used for sorting
 *    instead of the collection's own name.
 *
 * This function uses a [SpecialCharWithPrecedenceComparator] for the alphabetical sort.
 *
 * @param userOrganizations A list of the user's organizations, used to find the name for
 * `DEFAULT_USER_COLLECTION` types.
 * @return A new list containing the sorted [CollectionView] objects.
 */
fun List<CollectionView>.sortAlphabeticallyByTypeAndOrganization(
    userOrganizations: List<SyncResponseJson.Profile.Organization>,
): List<CollectionView> {
    return this.sortedWith(
        // DEFAULT_USER_COLLECTION come first
        comparator = compareBy<CollectionView> { it.type != CollectionType.DEFAULT_USER_COLLECTION }
            // Then sort by other CollectionType ordinals
            .thenBy { it.type }
            // Finally, sort within each group. For default collections, use the
            // organization's name; for others, use the collection's name.
            .thenBy(SpecialCharWithPrecedenceComparator) {
                if (it.type == CollectionType.DEFAULT_USER_COLLECTION) {
                    // For default collections, sort by the organization's name
                    userOrganizations
                        .find { org -> org.id == it.organizationId }
                        ?.name
                        ?: it.name
                } else {
                    // For other collections, sort by the collection's name
                    it.name
                }
            }
            // As a final fallback if names are identical, sort by ID to ensure a stable order
            .thenBy { it.id },
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
