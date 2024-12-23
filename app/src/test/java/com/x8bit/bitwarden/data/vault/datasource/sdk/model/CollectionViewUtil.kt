package com.x8bit.bitwarden.data.vault.datasource.sdk.model

import com.bitwarden.vault.CollectionView

/**
 * Create a mock [CollectionView] with a given [number].
 */
fun createMockCollectionView(
    number: Int,
    name: String? = null,
    readOnly: Boolean = false,
    manage: Boolean = true,
    hidePasswords: Boolean = false,
): CollectionView =
    CollectionView(
        id = "mockId-$number",
        organizationId = "mockOrganizationId-$number",
        hidePasswords = hidePasswords,
        name = name ?: "mockName-$number",
        externalId = "mockExternalId-$number",
        readOnly = readOnly,
        manage = manage,
    )

/**
 * Create a [CollectionView] configured to reflect MANAGE permission.
 */
fun createManageCollectionView(number: Int) = createMockCollectionView(
    number = number,
    manage = true,
    readOnly = false,
    hidePasswords = false,
)

/**
 * Create a [CollectionView] configured to reflect EDIT permission.
 */
fun createEditCollectionView(number: Int) = createMockCollectionView(
    number = number,
    manage = false,
    readOnly = false,
    hidePasswords = false,
)

/**
 * Create a [CollectionView] configured to reflect EDIT_EXCEPT_PASSWORDS permission.
 */
fun createEditExceptPasswordsCollectionView(number: Int) = createMockCollectionView(
    number = number,
    manage = false,
    readOnly = false,
    hidePasswords = true,
)

/**
 * Create a [CollectionView] configured to reflect VIEW permission.
 */
fun createViewCollectionView(number: Int) = createMockCollectionView(
    number = number,
    manage = false,
    readOnly = true,
    hidePasswords = false,
)

/**
 * Create a [CollectionView] configured to reflect VIEW_EXCEPT_PASSWORDS permission.
 */
fun createViewExceptPasswordsCollectionView(number: Int) = createMockCollectionView(
    number = number,
    manage = false,
    readOnly = true,
    hidePasswords = true,
)
