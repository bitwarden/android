package com.bitwarden.network.model

/**
 * Create a mock [SyncResponseJson.Collection] with a given [number].
 */
@Suppress("LongParameterList")
fun createMockCollection(
    number: Int,
    organizationId: String = "mockOrganizationId-$number",
    shouldHidePasswords: Boolean = false,
    name: String = "mockName-$number",
    externalId: String? = "mockExternalId-$number",
    isReadOnly: Boolean = false,
    id: String = "mockId-$number",
    canManage: Boolean? = true,
): SyncResponseJson.Collection =
    SyncResponseJson.Collection(
        organizationId = organizationId,
        shouldHidePasswords = shouldHidePasswords,
        name = name,
        externalId = externalId,
        isReadOnly = isReadOnly,
        id = id,
        canManage = canManage,
    )
