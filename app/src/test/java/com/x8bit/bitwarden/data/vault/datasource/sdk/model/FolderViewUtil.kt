package com.x8bit.bitwarden.data.vault.datasource.sdk.model

import com.bitwarden.core.FolderView
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Create a mock [FolderView] with a given [number].
 */
fun createMockFolderView(number: Int): FolderView =
    FolderView(
        id = "mockId-$number",
        name = "mockName-$number",
        revisionDate = LocalDateTime
            .parse("2023-10-27T12:00:00")
            .toInstant(ZoneOffset.UTC),
    )
