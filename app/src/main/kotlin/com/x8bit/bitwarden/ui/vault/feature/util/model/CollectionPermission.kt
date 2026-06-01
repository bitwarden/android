package com.x8bit.bitwarden.ui.vault.feature.util.model

/**
 * Represents the permission levels a user can be assigned to a collection.
 */
enum class CollectionPermission {
    VIEW,
    VIEW_EXCEPT_PASSWORDS,
    EDIT,
    EDIT_EXCEPT_PASSWORD,
    MANAGE,
}
