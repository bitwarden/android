package com.x8bit.bitwarden.data.vault.datasource.sdk.model

import com.bitwarden.vault.FolderView
import java.time.ZonedDateTime

/**
 * Create a mock [FolderView] with a given [number].
 */
fun createMockFolderView(number: Int): FolderView =
    FolderView(
        id = "mockId-$number",
        name = "mockName-$number",
        revisionDate = ZonedDateTime
            .parse("2023-10-27T12:00:00Z")
            .toInstant(),
    )
