package com.x8bit.bitwarden.data.vault.datasource.network.model

import kotlinx.serialization.json.JsonObject

/**
 * Create a mock [SyncResponseJson.Policy] with the given [number], [type], and [data].
 */
fun createMockPolicy(
    number: Int = 1,
    type: PolicyTypeJson = PolicyTypeJson.MASTER_PASSWORD,
    data: JsonObject? = null,
): SyncResponseJson.Policy =
    SyncResponseJson.Policy(
        organizationId = "mockOrganizationId-$number",
        id = "mockId-$number",
        type = type,
        isEnabled = false,
        data = data,
    )
