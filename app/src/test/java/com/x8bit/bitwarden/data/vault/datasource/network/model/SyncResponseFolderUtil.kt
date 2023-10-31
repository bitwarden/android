package com.x8bit.bitwarden.data.vault.datasource.network.model

import java.time.LocalDateTime

/**
 * Create a mock [SyncResponseJson.Folder] with a given [number].
 */
fun createMockFolder(number: Int): SyncResponseJson.Folder =
    SyncResponseJson.Folder(
        id = "mockId-$number",
        name = "mockName-$number",
        revisionDate = LocalDateTime.parse("2023-10-27T12:00:00"),
    )
