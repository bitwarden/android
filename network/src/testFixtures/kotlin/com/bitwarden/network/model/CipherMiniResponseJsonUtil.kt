package com.bitwarden.network.model

import java.time.Instant

/**
 * Create a mock [CipherMiniResponseJson.CipherMiniResponse] for testing.
 */
fun createMockCipherMiniResponse(
    number: Int,
): CipherMiniResponseJson.CipherMiniResponse = CipherMiniResponseJson.CipherMiniResponse(
    id = "mockId-$number",
    organizationId = "mockOrgId-$number",
    type = CipherTypeJson.LOGIN,
    data = "mockData-$number",
    attachments = null,
    shouldOrganizationUseTotp = false,
    revisionDate = Instant.parse("2023-10-27T12:00:00.000Z"),
    creationDate = Instant.parse("2023-10-27T12:00:00.000Z"),
    deletedDate = null,
    reprompt = CipherRepromptTypeJson.NONE,
    key = "mockKey-$number",
    archivedDate = null,
    name = "mockName-$number",
    notes = "mockNotes-$number",
    login = createMockLogin(number = number),
    card = createMockCard(number = number),
    identity = createMockIdentity(number = number),
    secureNote = createMockSecureNote(),
    sshKey = createMockSshKey(number = number),
    bankAccount = createMockBankAccount(number = number),
    driversLicense = createMockDriversLicense(number = number),
    passport = createMockPassport(number = number),
    fields = listOf(createMockField(number = number)),
    passwordHistory = listOf(createMockPasswordHistory(number = number)),
)

/**
 * Create a mock [CipherMiniResponseJson] wrapper for testing.
 */
fun createMockCipherMiniResponseJson(
    vararg numbers: Int,
): CipherMiniResponseJson = CipherMiniResponseJson(
    cipherMiniResponse = numbers.map { createMockCipherMiniResponse(it) },
)
