package com.bitwarden.network.model

import java.time.ZonedDateTime

/**
 * Create a mock [SyncResponseJson.Folder] with a given [number].
 */
fun createMockFolder(
    number: Int,
    id: String = "mockId-$number",
    name: String? = "mockName-$number",
    revisionDate: ZonedDateTime = ZonedDateTime.parse("2023-10-27T12:00:00Z"),
): SyncResponseJson.Folder =
    SyncResponseJson.Folder(
        id = id,
        name = name,
        revisionDate = revisionDate,
    )
