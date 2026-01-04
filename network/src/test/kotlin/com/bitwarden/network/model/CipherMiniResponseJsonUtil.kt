package com.bitwarden.network.model

import java.time.ZonedDateTime

/**
 * Create a mock [CipherMiniResponseJson] for testing.
 */
fun createMockCipherMiniResponse(
    number: Int,
): CipherMiniResponseJson = CipherMiniResponseJson(
    id = "mockId-$number",
    organizationId = "mockOrgId-$number",
    type = CipherTypeJson.LOGIN,
    data = "mockData-$number",
    attachments = null,
    shouldOrganizationUseTotp = false,
    revisionDate = ZonedDateTime.parse("2023-10-27T12:00:00.000Z"),
    creationDate = ZonedDateTime.parse("2023-10-27T12:00:00.000Z"),
    deletedDate = null,
    reprompt = CipherRepromptTypeJson.NONE,
    key = "mockKey-$number",
    archivedDate = null,
)
