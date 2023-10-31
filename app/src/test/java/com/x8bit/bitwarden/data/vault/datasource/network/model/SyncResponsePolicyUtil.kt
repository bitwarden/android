package com.x8bit.bitwarden.data.vault.datasource.network.model

/**
 * Create a mock [SyncResponseJson.Policy] with a given [number].
 */
fun createMockPolicy(number: Int): SyncResponseJson.Policy =
    SyncResponseJson.Policy(
        organizationId = "mockOrganizationId-$number",
        id = "mockId-$number",
        type = PolicyTypeJson.MASTER_PASSWORD,
        isEnabled = false,
    )
