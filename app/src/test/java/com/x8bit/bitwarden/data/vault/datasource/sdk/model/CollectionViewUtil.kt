package com.x8bit.bitwarden.data.vault.datasource.sdk.model

import com.bitwarden.core.CollectionView

/**
 * Create a mock [CollectionView] with a given [number].
 */
fun createMockCollectionView(number: Int, name: String? = null): CollectionView =
    CollectionView(
        id = "mockId-$number",
        organizationId = "mockOrganizationId-$number",
        hidePasswords = false,
        name = name ?: "mockName-$number",
        externalId = "mockExternalId-$number",
        readOnly = false,
    )
