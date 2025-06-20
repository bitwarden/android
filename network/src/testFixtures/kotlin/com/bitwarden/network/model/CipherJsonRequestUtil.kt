package com.bitwarden.network.model

import java.time.ZonedDateTime

/**
 * Create a mock [CipherJsonRequest] with a given [number].
 */
@Suppress("LongParameterList")
fun createMockCipherJsonRequest(
    number: Int,
    attachments: Map<String, AttachmentJsonRequest>? = mapOf(
        "mockId-$number" to createMockAttachmentJsonRequest(number = 1),
    ),
    organizationId: String? = "mockOrganizationId-$number",
    folderId: String? = "mockFolderId-$number",
    name: String? = "mockName-$number",
    notes: String? = "mockNotes-$number",
    type: CipherTypeJson = CipherTypeJson.LOGIN,
    login: SyncResponseJson.Cipher.Login? = createMockLogin(number = number),
    card: SyncResponseJson.Cipher.Card? = createMockCard(number = number),
    sshKey: SyncResponseJson.Cipher.SshKey? = createMockSshKey(number = number),
    identity: SyncResponseJson.Cipher.Identity? = createMockIdentity(number = number),
    secureNote: SyncResponseJson.Cipher.SecureNote? = createMockSecureNote(),
    fields: List<SyncResponseJson.Cipher.Field>? = listOf(createMockField(number = number)),
    isFavorite: Boolean = false,
    passwordHistory: List<SyncResponseJson.Cipher.PasswordHistory>? = listOf(
        createMockPasswordHistory(number = number),
    ),
    reprompt: CipherRepromptTypeJson = CipherRepromptTypeJson.NONE,
    lastKnownRevisionDate: ZonedDateTime? = ZonedDateTime.parse("2023-10-27T12:00:00Z"),
    key: String? = "mockKey-$number",
    encryptedFor: String? = "mockEncryptedFor-$number",
): CipherJsonRequest =
    CipherJsonRequest(
        attachments = attachments,
        organizationId = organizationId,
        folderId = folderId,
        name = name,
        notes = notes,
        type = type,
        login = login,
        card = card,
        sshKey = sshKey,
        identity = identity,
        secureNote = secureNote,
        fields = fields,
        isFavorite = isFavorite,
        passwordHistory = passwordHistory,
        reprompt = reprompt,
        lastKnownRevisionDate = lastKnownRevisionDate,
        key = key,
        encryptedFor = encryptedFor,
    )
