package com.x8bit.bitwarden.data.vault.datasource.sdk.model

import com.bitwarden.collections.Collection
import com.bitwarden.collections.CollectionType

/**
 * Create a mock [Collection] with a given [number].
 */
fun createMockSdkCollection(number: Int): Collection =
    Collection(
        id = "mockId-$number",
        organizationId = "mockOrganizationId-$number",
        hidePasswords = false,
        name = "mockName-$number",
        externalId = "mockExternalId-$number",
        readOnly = false,
        manage = true,
        defaultUserCollectionEmail = "mockOffboardedUserEmail-$number",
        type = CollectionType.SHARED_COLLECTION,
    )
