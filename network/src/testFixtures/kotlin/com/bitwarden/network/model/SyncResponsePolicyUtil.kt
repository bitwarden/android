package com.bitwarden.network.model

import kotlinx.serialization.json.JsonObject
import java.time.ZonedDateTime

/**
 * Create a mock [SyncResponseJson.Policy] with the given [number], [type], and [data].
 */
@Suppress("LongParameterList")
fun createMockPolicy(
    number: Int = 1,
    organizationId: String = "mockOrganizationId-$number",
    id: String = "mockId-$number",
    type: PolicyTypeJson = PolicyTypeJson.MASTER_PASSWORD,
    isEnabled: Boolean = false,
    data: JsonObject? = null,
    revisionDate: ZonedDateTime? = null,
): SyncResponseJson.Policy =
    SyncResponseJson.Policy(
        organizationId = organizationId,
        id = id,
        type = type,
        isEnabled = isEnabled,
        data = data,
        revisionDate = revisionDate,
    )
