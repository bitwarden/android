package com.x8bit.bitwarden.data.vault.datasource.network.model

import java.time.ZonedDateTime

/**
 * Create a mock [CipherJsonRequest] with a given [number].
 */
fun createMockCipherJsonRequest(number: Int, hasNullUri: Boolean = false): CipherJsonRequest =
    CipherJsonRequest(
        organizationId = "mockOrganizationId-$number",
        folderId = "mockFolderId-$number",
        name = "mockName-$number",
        notes = "mockNotes-$number",
        type = CipherTypeJson.LOGIN,
        login = createMockLogin(number = number, hasNullUri = hasNullUri),
        card = createMockCard(number = number),
        fields = listOf(createMockField(number = number)),
        identity = createMockIdentity(number = number),
        isFavorite = false,
        passwordHistory = listOf(createMockPasswordHistory(number = number)),
        reprompt = CipherRepromptTypeJson.NONE,
        secureNote = createMockSecureNote(),
        lastKnownRevisionDate = ZonedDateTime.parse("2023-10-27T12:00:00Z"),
    )
