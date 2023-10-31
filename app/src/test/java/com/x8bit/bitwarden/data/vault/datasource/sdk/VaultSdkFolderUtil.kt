package com.x8bit.bitwarden.data.vault.datasource.sdk

import com.bitwarden.core.Folder
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Create a mock [Folder] with a given [number].
 */
fun createMockSdkFolder(number: Int): Folder =
    Folder(
        id = "mockId-$number",
        name = "mockName-$number",
        revisionDate = LocalDateTime
            .parse("2023-10-27T12:00:00")
            .toInstant(ZoneOffset.UTC),
    )
