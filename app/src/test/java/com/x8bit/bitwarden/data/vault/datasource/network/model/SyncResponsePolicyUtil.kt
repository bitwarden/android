package com.x8bit.bitwarden.data.vault.datasource.network.model

import kotlinx.serialization.json.JsonObject

/**
 * Create a mock [SyncResponseJson.Policy] with the given [number], [type], and [data].
 */
fun createMockPolicy(
    number: Int = 1,
    organizationId: String = "mockOrganizationId-$number",
    type: PolicyTypeJson = PolicyTypeJson.MASTER_PASSWORD,
    isEnabled: Boolean = false,
    data: JsonObject? = null,
): SyncResponseJson.Policy =
    SyncResponseJson.Policy(
        organizationId = organizationId,
        id = "mockId-$number",
        type = type,
        isEnabled = isEnabled,
        data = data,
    )
