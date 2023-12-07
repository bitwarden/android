package com.x8bit.bitwarden.ui.vault.model

/**
 * Represents different types of listing that can be viewed.
 */
sealed class VaultItemListingType {

    /**
     * A Login listing.
     */
    data object Login : VaultItemListingType()

    /**
     * An Identity listing.
     */
    data object Identity : VaultItemListingType()

    /**
     * A Secure Note listing.
     */
    data object SecureNote : VaultItemListingType()

    /**
     * A Card  listing.
     */
    data object Card : VaultItemListingType()

    /**
     * A Trash listing.
     */
    data object Trash : VaultItemListingType()

    /**
     * A Folder listing.
     *
     * @param folderId the id of the folder, a null value indicates a, "no folder" grouping.
     */
    data class Folder(val folderId: String?) : VaultItemListingType()
}
