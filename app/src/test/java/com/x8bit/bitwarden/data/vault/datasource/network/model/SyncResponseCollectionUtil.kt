package com.x8bit.bitwarden.data.vault.datasource.network.model

/**
 * Create a mock [SyncResponseJson.Collection] with a given [number].
 */
fun createMockCollection(number: Int): SyncResponseJson.Collection =
    SyncResponseJson.Collection(
        organizationId = "mockOrganizationId-$number",
        shouldHidePasswords = false,
        name = "mockName-$number",
        externalId = "mockExternalId-$number",
        isReadOnly = false,
        id = "mockId-$number",
        canManage = true,
    )
