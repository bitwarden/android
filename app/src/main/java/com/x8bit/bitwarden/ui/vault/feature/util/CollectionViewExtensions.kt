package com.x8bit.bitwarden.ui.vault.feature.util

import com.bitwarden.vault.CollectionView
import com.x8bit.bitwarden.ui.vault.feature.util.model.CollectionPermission

private const val COLLECTION_DIVIDER: String = "/"

/**
 * Retrieves the nested collections of a given [collectionId] and updates their names to proper
 * display names. This function is necessary if we want to show the nested collections for a
 * specific collection.
 */
fun List<CollectionView>.getCollections(collectionId: String): List<CollectionView> {
    val currentCollection = this.find { it.id == collectionId } ?: return emptyList()

    // If two collections have the same name the second collection should have no nested collections
    val firstCollectionWithName = this.first { it.name == currentCollection.name }
    if (firstCollectionWithName.id != collectionId) return emptyList()

    val collectionList = this
        .getFilteredCollections(currentCollection.name)
        .map {
            it.copy(name = it.name.substringAfter(currentCollection.name + COLLECTION_DIVIDER))
        }

    return collectionList
}

/**
 * Filters out the nest collections of the nest collections from the given list. If a
 * [collectionName] is provided, collections that are not nested of the specified [collectionName]
 * will be filtered out.
 */
fun List<CollectionView>.getFilteredCollections(
    collectionName: String? = null,
): List<CollectionView> =
    this.filter { collectionView ->
        // If the collection name is not null we filter out collections that are not nested
        // collections.
        if (collectionName != null &&
            !collectionView.name.startsWith(collectionName + COLLECTION_DIVIDER)
        ) {
            return@filter false
        }

        this.forEach {
            val firstCollection = collectionName
                ?.let { name -> collectionView.name.substringAfter(name + COLLECTION_DIVIDER) }
                ?: collectionView.name

            val secondCollection = collectionName
                ?.let { name -> it.name.substringAfter(name + COLLECTION_DIVIDER) }
                ?: it.name

            // We don't want to compare the collection to itself or itself plus a slash.
            if (firstCollection == secondCollection) {
                return@forEach
            }

            // If the first collection name is blank or the first collection is a nested collection
            // of the second collection, we want to filter it out.
            if (firstCollection.isEmpty() ||
                firstCollection.startsWith(secondCollection + COLLECTION_DIVIDER)
            ) {
                return@filter false
            }
        }

        true
    }

/**
 * Converts a collection name to a user-friendly display name. This function is necessary because
 * the collection name we receive is often nested, and we want to extract just the relevant name for
 * display to the user.
 */
fun String.toCollectionDisplayName(list: List<CollectionView>): String {
    var collectionName = this

    // cycle through the list and determine the correct display name of the collection.
    list.forEach { collection ->
        if (this.startsWith(collection.name + COLLECTION_DIVIDER)) {
            val newName = this.substringAfter(collection.name + COLLECTION_DIVIDER)
            if (newName.length < collectionName.length) {
                collectionName = newName
            }
        }
    }

    return collectionName
}

/**
 * Checks if the user has delete permission in at least one collection.
 *
 * Deletion is allowed when the item is in any collection that the user has "manage" permission for.
 */
fun List<CollectionView>?.hasDeletePermissionInAtLeastOneCollection(
    collectionIds: List<String>?,
): Boolean {
    if (this.isNullOrEmpty() || collectionIds.isNullOrEmpty()) return true
    return this
        .any { collectionView ->
            collectionIds
                .contains(collectionView.id)
                .let { isInCollection -> isInCollection && collectionView.manage }
        }
}

/**
 * Checks if the user has permission to assign an item to a collection.
 *
 * Assigning to a collection is not allowed when the item is in a collection that the user does not
 * have "manage" permission for and is also in a collection they cannot view the passwords in.
 *
 * E.g., If an item is in A collection with "view except passwords" or "edit except passwords"
 * permission and in another with "manage" permission, the user **cannot** assign the item to other
 * collections. Conversely, if an item is in a collection with "manage" permission and another with
 * "view" or "edit" permission, the user **can** assign the item to other collections.
 */
fun List<CollectionView>?.canAssignToCollections(currentCollectionIds: List<String>?): Boolean {
    if (this.isNullOrEmpty()) return true
    if (currentCollectionIds.isNullOrEmpty()) return true

    // Verify user can MANAGE at least one collection the item is in.
    return this
        .any {
            currentCollectionIds.contains(it.id) &&
                it.permission == CollectionPermission.MANAGE
        } &&

        // Verify user does not have "edit except password" or "view except passwords"
        // permission in any collection the item is not in.
        this
            .none {
                currentCollectionIds.contains(it.id) &&
                    (it.permission == CollectionPermission.EDIT_EXCEPT_PASSWORD ||
                        it.permission == CollectionPermission.VIEW_EXCEPT_PASSWORDS)
            }
}

/**
 * Determines the user's permission level for a given [CollectionView].
 */
val CollectionView.permission: CollectionPermission
    get() = when {
        manage -> CollectionPermission.MANAGE
        readOnly && hidePasswords -> CollectionPermission.VIEW_EXCEPT_PASSWORDS
        readOnly -> CollectionPermission.VIEW
        !readOnly && hidePasswords -> CollectionPermission.EDIT_EXCEPT_PASSWORD
        // !readOnly is the only other possible condition, which resolves to EDIT permission
        else -> CollectionPermission.EDIT
    }
