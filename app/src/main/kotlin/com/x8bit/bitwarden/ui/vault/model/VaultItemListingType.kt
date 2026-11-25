package com.x8bit.bitwarden.ui.vault.model

import kotlinx.serialization.Serializable

/**
 * Represents different types of listing that can be viewed.
 */
@Serializable
sealed class VaultItemListingType {

    /**
     * An Archive listing.
     */
    @Serializable
    data object Archive : VaultItemListingType()

    /**
     * A Login listing.
     */
    @Serializable
    data object Login : VaultItemListingType()

    /**
     * An Identity listing.
     */
    @Serializable
    data object Identity : VaultItemListingType()

    /**
     * A Secure Note listing.
     */
    @Serializable
    data object SecureNote : VaultItemListingType()

    /**
     * A Card listing.
     */
    @Serializable
    data object Card : VaultItemListingType()

    /**
     * A SSH key listing.
     */
    @Serializable
    data object SshKey : VaultItemListingType()

    /**
     * A Bank Account listing.
     */
    @Serializable
    data object BankAccount : VaultItemListingType()

    /**
     * A License listing.
     */
    @Serializable
    data object License : VaultItemListingType()

    /**
     * A Passport listing.
     */
    @Serializable
    data object Passport : VaultItemListingType()

    /**
     * A Trash listing.
     */
    @Serializable
    data object Trash : VaultItemListingType()

    /**
     * A Folder listing.
     *
     * @param folderId the id of the folder, a null value indicates a, "no folder" grouping.
     */
    @Serializable
    data class Folder(val folderId: String?) : VaultItemListingType()

    /**
     * A Collection listing.
     *
     * @param collectionId the ID of the collection.
     */
    @Serializable
    data class Collection(val collectionId: String) : VaultItemListingType()

    /**
     * A Send File listing.
     */
    @Serializable
    data object SendFile : VaultItemListingType()

    /**
     * A Send Text listing.
     */
    @Serializable
    data object SendText : VaultItemListingType()
}
