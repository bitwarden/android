package com.x8bit.bitwarden.data.vault.repository.util

import com.bitwarden.vault.CipherRepromptType
import com.bitwarden.vault.CipherType
import com.bitwarden.vault.FieldType
import com.bitwarden.vault.UriMatchType
import com.x8bit.bitwarden.data.vault.datasource.network.model.CipherRepromptTypeJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.CipherTypeJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.FieldTypeJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.UriMatchTypeJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockAttachment
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockAttachmentJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockCard
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockCipher
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockCipherJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockField
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockIdentity
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockLogin
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockPasswordHistory
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockSecureNote
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockSshKey
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockUri
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkAttachment
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkCard
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkCipher
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkField
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkIdentity
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkLogin
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkPasswordHistory
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkSecureNote
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkSshKey
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkUri
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Default date time used for [ZonedDateTime] properties of mock objects.
 */
private const val DEFAULT_TIMESTAMP = "2023-10-27T12:00:00Z"
private val FIXED_CLOCK: Clock = Clock.fixed(
    Instant.parse(DEFAULT_TIMESTAMP),
    ZoneOffset.UTC,
)

class VaultSdkCipherExtensionsTest {

    @Test
    fun `toEncryptedNetworkCipherResponse should convert an Sdk Cipher to a cipher`() {
        val sdkCipher = createMockSdkCipher(number = 1, clock = FIXED_CLOCK)

        val result = sdkCipher.toEncryptedNetworkCipherResponse()

        assertEquals(
            createMockCipher(
                number = 1,
                hasNullUri = true,
            ),
            result,
        )
    }

    @Test
    fun `toEncryptedNetworkCipher should convert an Sdk Cipher to a Network Cipher`() {
        val sdkCipher = createMockSdkCipher(number = 1, clock = FIXED_CLOCK)
        val syncCipher = sdkCipher.toEncryptedNetworkCipher()
        assertEquals(
            createMockCipherJsonRequest(
                number = 1,
                hasNullUri = true,
            ),
            syncCipher,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toNetworkAttachmentRequest should convert an Sdk Attachment to a Network Attachment Request`() {
        val sdkAttachment = createMockSdkAttachment(number = 1)
        val attachmentRequest = sdkAttachment.toNetworkAttachmentRequest()
        assertEquals(
            createMockAttachmentJsonRequest(number = 1),
            attachmentRequest,
        )
    }

    @Test
    fun `toEncryptedSdkCipherList should convert list of Network Cipher to List of Sdk Cipher`() {
        val syncCiphers = listOf(
            createMockCipher(number = 1),
            createMockCipher(number = 2),
        )
        val sdkCiphers = syncCiphers.toEncryptedSdkCipherList()
        assertEquals(
            listOf(
                createMockSdkCipher(number = 1, clock = FIXED_CLOCK),
                createMockSdkCipher(number = 2, clock = FIXED_CLOCK),
            ),
            sdkCiphers,
        )
    }

    @Test
    fun `toEncryptedSdkCipher should convert a SyncResponseJson Cipher to a Cipher`() {
        val syncCipher = createMockCipher(number = 1)
        val sdkCipher = syncCipher.toEncryptedSdkCipher()
        assertEquals(
            createMockSdkCipher(number = 1, clock = FIXED_CLOCK),
            sdkCipher,
        )
    }

    @Test
    fun `toSdkLogin should convert a SyncResponseJson Cipher Login to a Login`() {
        val syncLogin = createMockLogin(number = 1)
        val sdkLogin = syncLogin.toSdkLogin()
        assertEquals(
            createMockSdkLogin(number = 1, clock = FIXED_CLOCK),
            sdkLogin,
        )
    }

    @Test
    fun `toSdkIdentity should convert a SyncResponseJson Cipher Identity to a Identity`() {
        val syncIdentity = createMockIdentity(number = 1)
        val sdkIdentity = syncIdentity.toSdkIdentity()
        assertEquals(
            createMockSdkIdentity(number = 1),
            sdkIdentity,
        )
    }

    @Test
    fun `toSdkCard should convert a SyncResponseJson Cipher Card to a Card`() {
        val syncCard = createMockCard(number = 1)
        val sdkCard = syncCard.toSdkCard()
        assertEquals(
            createMockSdkCard(number = 1),
            sdkCard,
        )
    }

    @Test
    fun `toSdkSecureNote should convert a SyncResponseJson Cipher SecureNote to a SecureNote`() {
        val syncSecureNote = createMockSecureNote()
        val sdkSecureNote = syncSecureNote.toSdkSecureNote()
        assertEquals(
            createMockSdkSecureNote(),
            sdkSecureNote,
        )
    }

    @Test
    fun `toSdkSshKey should convert a SyncResponseJson Cipher SshKey to a SshKey`() {
        val syncSshKey = createMockSshKey(number = 1)
        val sdkSshKey = syncSshKey.toSdkSshKey()
        assertEquals(
            createMockSdkSshKey(number = 1),
            sdkSshKey,
        )
    }

    @Test
    fun `toSdkLoginUriList should convert list of LoginUri to List of Sdk LoginUri`() {
        val syncLoginUris = listOf(
            createMockUri(number = 1),
            createMockUri(number = 2),
        )
        val sdkLoginUris = syncLoginUris.toSdkLoginUriList()
        assertEquals(
            listOf(
                createMockSdkUri(number = 1),
                createMockSdkUri(number = 2),
            ),
            sdkLoginUris,
        )
    }

    @Test
    fun `toSdkLoginUri should convert Network Cipher LoginUri to Sdk LoginUri`() {
        val syncLoginUri = createMockUri(number = 1)
        val sdkLoginUri = syncLoginUri.toSdkLoginUri()
        assertEquals(
            createMockSdkUri(number = 1),
            sdkLoginUri,
        )
    }

    @Test
    fun `toSdkAttachmentList should convert list of Attachment to List of Sdk Attachment`() {
        val syncAttachments = listOf(
            createMockAttachment(number = 1),
            createMockAttachment(number = 2),
        )
        val sdkAttachments = syncAttachments.toSdkAttachmentList()
        assertEquals(
            listOf(
                createMockSdkAttachment(number = 1),
                createMockSdkAttachment(number = 2),
            ),
            sdkAttachments,
        )
    }

    @Test
    fun `toSdkAttachment should convert Network Cipher Attachment to Sdk Attachment`() {
        val syncAttachment = createMockAttachment(number = 1)
        val sdkAttachment = syncAttachment.toSdkAttachment()
        assertEquals(
            createMockSdkAttachment(number = 1),
            sdkAttachment,
        )
    }

    @Test
    fun `toSdkFieldList should convert list of Network Cipher Field to List of Sdk Field`() {
        val syncFields = listOf(
            createMockField(number = 1),
            createMockField(number = 2),
        )
        val sdkFields = syncFields.toSdkFieldList()
        assertEquals(
            listOf(
                createMockSdkField(number = 1),
                createMockSdkField(number = 2),
            ),
            sdkFields,
        )
    }

    @Test
    fun `toSdkField should convert Network Cipher Attachment to Sdk Attachment`() {
        val syncField = createMockField(number = 1)
        val sdkField = syncField.toSdkField()
        assertEquals(
            createMockSdkField(number = 1),
            sdkField,
        )
    }

    @Test
    @Suppress("MaxLineLength")
    fun `toSdkPasswordHistoryList should convert PasswordHistory list to Sdk PasswordHistory List`() {
        val syncPasswordHistories = listOf(
            createMockPasswordHistory(number = 1),
            createMockPasswordHistory(number = 2),
        )
        val sdkPasswordHistories = syncPasswordHistories.toSdkPasswordHistoryList()
        assertEquals(
            listOf(
                createMockSdkPasswordHistory(number = 1, FIXED_CLOCK),
                createMockSdkPasswordHistory(number = 2, FIXED_CLOCK),
            ),
            sdkPasswordHistories,
        )
    }

    @Test
    fun `toSdkPasswordHistory should convert PasswordHistory to Sdk PasswordHistory`() {
        val syncPasswordHistory = createMockPasswordHistory(number = 1)
        val sdkPasswordHistory = syncPasswordHistory.toSdkPasswordHistory()
        assertEquals(
            createMockSdkPasswordHistory(number = 1, clock = FIXED_CLOCK),
            sdkPasswordHistory,
        )
    }

    @Test
    fun `toSdkCipherType should convert CipherTypeJson to CipherType`() {
        val cipherType = CipherTypeJson.IDENTITY
        val sdkCipherType = cipherType.toSdkCipherType()
        assertEquals(
            CipherType.IDENTITY,
            sdkCipherType,
        )
    }

    @Test
    fun `toSdkMatchType should convert UriMatchTypeJson to UriMatchType`() {
        val uriMatchType = UriMatchTypeJson.DOMAIN
        val sdkUriMatchType = uriMatchType.toSdkMatchType()
        assertEquals(
            UriMatchType.DOMAIN,
            sdkUriMatchType,
        )
    }

    @Test
    fun `toSdkRepromptType should convert CipherRepromptTypeJson to CipherRepromptType`() {
        val repromptType = CipherRepromptTypeJson.NONE
        val sdkRepromptType = repromptType.toSdkRepromptType()
        assertEquals(
            CipherRepromptType.NONE,
            sdkRepromptType,
        )
    }

    @Test
    fun `toSdkFieldType should convert FieldTypeJson to FieldType`() {
        val fieldType = FieldTypeJson.HIDDEN
        val sdkFieldType = fieldType.toSdkFieldType()
        assertEquals(
            FieldType.HIDDEN,
            sdkFieldType,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toSortAlphabetically should sort ciphers by name`() {
        val list = listOf(
            createMockCipherView(1).copy(name = "c"),
            createMockCipherView(1).copy(name = "B"),
            createMockCipherView(1).copy(name = "z"),
            createMockCipherView(1).copy(name = "8"),
            createMockCipherView(1).copy(name = "7"),
            createMockCipherView(1).copy(name = "_"),
            createMockCipherView(1).copy(name = "A"),
            createMockCipherView(1).copy(name = "D"),
            createMockCipherView(1).copy(name = "AbA"),
            createMockCipherView(1).copy(name = "aAb"),
        )

        val expected = listOf(
            createMockCipherView(1).copy(name = "_"),
            createMockCipherView(1).copy(name = "7"),
            createMockCipherView(1).copy(name = "8"),
            createMockCipherView(1).copy(name = "aAb"),
            createMockCipherView(1).copy(name = "A"),
            createMockCipherView(1).copy(name = "AbA"),
            createMockCipherView(1).copy(name = "B"),
            createMockCipherView(1).copy(name = "c"),
            createMockCipherView(1).copy(name = "D"),
            createMockCipherView(1).copy(name = "z"),
        )

        assertEquals(
            expected,
            list.sortAlphabetically(),
        )
    }
}
