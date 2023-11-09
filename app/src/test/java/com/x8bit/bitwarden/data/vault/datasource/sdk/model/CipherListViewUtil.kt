package com.x8bit.bitwarden.data.vault.datasource.sdk.model

import com.bitwarden.core.CipherListView
import com.bitwarden.core.CipherRepromptType
import com.bitwarden.core.CipherType
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Create a mock [CipherListView] with a given [number].
 */
fun createMockCipherListView(number: Int): CipherListView =
    CipherListView(
        id = "mockId-$number",
        organizationId = "mockOrganizationId-$number",
        folderId = "mockFolderId-$number",
        collectionIds = listOf("mockCollectionId-$number"),
        name = "mockName-$number",
        type = CipherType.LOGIN,
        creationDate = LocalDateTime
            .parse("2023-10-27T12:00:00")
            .toInstant(ZoneOffset.UTC),
        deletedDate = LocalDateTime
            .parse("2023-10-27T12:00:00")
            .toInstant(ZoneOffset.UTC),
        revisionDate = LocalDateTime
            .parse("2023-10-27T12:00:00")
            .toInstant(ZoneOffset.UTC),
        attachments = 1U,
        favorite = false,
        reprompt = CipherRepromptType.NONE,
        edit = false,
        viewPassword = false,
        subTitle = "",
    )
