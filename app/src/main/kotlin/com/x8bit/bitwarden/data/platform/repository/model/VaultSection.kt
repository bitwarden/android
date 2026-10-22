package com.x8bit.bitwarden.data.platform.repository.model

/**
 * Represents a collapsible section on the vault screen.
 *
 * The [value] is used for consistent storage purposes.
 */
enum class VaultSection(
    val value: String,
) {
    /**
     * The section containing the verification codes group.
     */
    TOTP(value = "totp"),

    /**
     * The section containing the favorite items.
     */
    FAVORITES(value = "favorites"),

    /**
     * The section containing the item type groups.
     */
    TYPES(value = "types"),

    /**
     * The section containing the folders.
     */
    FOLDERS(value = "folders"),

    /**
     * The section containing the items that are not in a folder.
     */
    NO_FOLDER(value = "noFolder"),

    /**
     * The section containing the collections.
     */
    COLLECTIONS(value = "collections"),

    /**
     * The section containing the archive and trash groups.
     */
    HIDDEN_ITEMS(value = "hiddenItems"),
}
