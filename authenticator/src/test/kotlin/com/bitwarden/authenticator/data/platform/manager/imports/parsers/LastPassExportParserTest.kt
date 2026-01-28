package com.bitwarden.authenticator.data.platform.manager.imports.parsers

import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemAlgorithm
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemType
import com.bitwarden.authenticator.data.platform.manager.UuidManager
import com.bitwarden.authenticator.data.platform.manager.imports.model.ExportParseResult
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LastPassExportParserTest {

    private val mockUuidManager = mockk<UuidManager>()
    private val parser = LastPassExportParser(mockUuidManager)

    @BeforeEach
    fun setup() {
        every { mockUuidManager.generateUuid() } returns "00000000-0000-0000-0000-000000000001"
    }

    @Test
    fun `parseForResult with valid JSON should return Success`() {
        val json = VALID_SINGLE_ACCOUNT_JSON

        val result = parser.parseForResult(json.toByteArray())

        assertTrue(result is ExportParseResult.Success)
        val items = (result as ExportParseResult.Success).items
        assertEquals(1, items.size)

        val item = items.first()
        assertEquals("JBSWY3DPEHPK3PXP", item.key)
        assertEquals(AuthenticatorItemType.TOTP, item.type)
        assertEquals(AuthenticatorItemAlgorithm.SHA1, item.algorithm)
        assertEquals(30, item.period)
        assertEquals(6, item.digits)
        assertEquals("Test Issuer", item.issuer)
        assertEquals("test@example.com", item.accountName)
        assertFalse(item.favorite)
        assertNull(item.userId)
        assertNotNull(item.id)
    }

    @Test
    fun `parseForResult with multiple accounts should return all items`() {
        val json = VALID_MULTIPLE_ACCOUNTS_JSON

        val result = parser.parseForResult(json.toByteArray())

        assertTrue(result is ExportParseResult.Success)
        val items = (result as ExportParseResult.Success).items
        assertEquals(3, items.size)

        assertEquals("Issuer 1", items[0].issuer)
        assertEquals("Issuer 2", items[1].issuer)
        assertEquals("Issuer 3", items[2].issuer)
    }

    @Test
    fun `parseForResult should always create TOTP type`() {
        val json = VALID_SINGLE_ACCOUNT_JSON

        val result = parser.parseForResult(json.toByteArray())

        assertTrue(result is ExportParseResult.Success)
        val item = (result as ExportParseResult.Success).items.first()
        assertEquals(AuthenticatorItemType.TOTP, item.type)
    }

    @Test
    fun `parseForResult with SHA1 algorithm should succeed`() {
        val json = VALID_SHA1_ALGORITHM_JSON

        val result = parser.parseForResult(json.toByteArray())

        assertTrue(result is ExportParseResult.Success)
        val item = (result as ExportParseResult.Success).items.first()
        assertEquals(AuthenticatorItemAlgorithm.SHA1, item.algorithm)
    }

    @Test
    fun `parseForResult with SHA256 algorithm should succeed`() {
        val json = VALID_SHA256_ALGORITHM_JSON

        val result = parser.parseForResult(json.toByteArray())

        assertTrue(result is ExportParseResult.Success)
        val item = (result as ExportParseResult.Success).items.first()
        assertEquals(AuthenticatorItemAlgorithm.SHA256, item.algorithm)
    }

    @Test
    fun `parseForResult with SHA512 algorithm should succeed`() {
        val json = VALID_SHA512_ALGORITHM_JSON

        val result = parser.parseForResult(json.toByteArray())

        assertTrue(result is ExportParseResult.Success)
        val item = (result as ExportParseResult.Success).items.first()
        assertEquals(AuthenticatorItemAlgorithm.SHA512, item.algorithm)
    }

    @Test
    fun `parseForResult with unsupported algorithm should return Error`() {
        val json = INVALID_ALGORITHM_JSON

        val result = parser.parseForResult(json.toByteArray())

        assertTrue(result is ExportParseResult.Error)
        val error = result as ExportParseResult.Error
        assertNotNull(error.message)
    }

    @Test
    fun `parseForResult should map originalIssuerName to issuer`() {
        val json = VALID_ORIGINAL_ISSUER_NAME_JSON

        val result = parser.parseForResult(json.toByteArray())

        assertTrue(result is ExportParseResult.Success)
        val item = (result as ExportParseResult.Success).items.first()
        assertEquals("GitHub", item.issuer)
    }

    @Test
    fun `parseForResult should map originalUserName to accountName`() {
        val json = VALID_ORIGINAL_USER_NAME_JSON

        val result = parser.parseForResult(json.toByteArray())

        assertTrue(result is ExportParseResult.Success)
        val item = (result as ExportParseResult.Success).items.first()
        assertEquals("user@github.com", item.accountName)
    }

    @Test
    fun `parseForResult should map timeStep to period`() {
        val json = VALID_CUSTOM_TIME_STEP_JSON

        val result = parser.parseForResult(json.toByteArray())

        assertTrue(result is ExportParseResult.Success)
        val item = (result as ExportParseResult.Success).items.first()
        assertEquals(60, item.period)
    }

    @Test
    fun `parseForResult should map digits to digits`() {
        val json = VALID_CUSTOM_DIGITS_JSON

        val result = parser.parseForResult(json.toByteArray())

        assertTrue(result is ExportParseResult.Success)
        val item = (result as ExportParseResult.Success).items.first()
        assertEquals(8, item.digits)
    }

    @Test
    fun `parseForResult with isFavorite true should preserve flag`() {
        val json = VALID_FAVORITE_TRUE_JSON

        val result = parser.parseForResult(json.toByteArray())

        assertTrue(result is ExportParseResult.Success)
        val item = (result as ExportParseResult.Success).items.first()
        assertTrue(item.favorite)
    }

    @Test
    fun `parseForResult with isFavorite false should preserve flag`() {
        val json = VALID_FAVORITE_FALSE_JSON

        val result = parser.parseForResult(json.toByteArray())

        assertTrue(result is ExportParseResult.Success)
        val item = (result as ExportParseResult.Success).items.first()
        assertFalse(item.favorite)
    }

    @Test
    fun `parseForResult with malformed JSON should return Error`() {
        val json = MALFORMED_JSON

        val result = parser.parseForResult(json.toByteArray())

        assertTrue(result is ExportParseResult.Error)
    }

    @Test
    fun `parseForResult with missing required field should return Error`() {
        val json = MISSING_ACCOUNTS_FIELD_JSON

        val result = parser.parseForResult(json.toByteArray())

        assertTrue(result is ExportParseResult.Error)
    }

    @Test
    fun `parseForResult with empty accounts should return empty list`() {
        val json = VALID_EMPTY_ACCOUNTS_JSON

        val result = parser.parseForResult(json.toByteArray())

        assertTrue(result is ExportParseResult.Success)
        val items = (result as ExportParseResult.Success).items
        assertTrue(items.isEmpty())
    }

    @Test
    fun `parseForResult should generate unique UUID for each item`() {
        every { mockUuidManager.generateUuid() } returnsMany listOf(
            "00000000-0000-0000-0000-000000000001",
            "00000000-0000-0000-0000-000000000002",
            "00000000-0000-0000-0000-000000000003",
        )
        val json = VALID_MULTIPLE_ACCOUNTS_JSON

        val result = parser.parseForResult(json.toByteArray())

        assertTrue(result is ExportParseResult.Success)
        val items = (result as ExportParseResult.Success).items
        val ids = items.map { it.id }.toSet()
        assertEquals(items.size, ids.size)
    }

    @Test
    fun `parseForResult with null folderData should succeed`() {
        val json = VALID_NULL_FOLDER_DATA_JSON

        val result = parser.parseForResult(json.toByteArray())

        assertTrue(result is ExportParseResult.Success)
        val items = (result as ExportParseResult.Success).items
        assertEquals(1, items.size)
    }

    @Test
    fun `parseForResult with null backupInfo should succeed`() {
        val json = VALID_NULL_BACKUP_INFO_JSON

        val result = parser.parseForResult(json.toByteArray())

        assertTrue(result is ExportParseResult.Success)
        val items = (result as ExportParseResult.Success).items
        assertEquals(1, items.size)
    }

    @Test
    fun `parseForResult should use secret field for key`() {
        val json = VALID_SECRET_FIELD_JSON

        val result = parser.parseForResult(json.toByteArray())

        assertTrue(result is ExportParseResult.Success)
        val item = (result as ExportParseResult.Success).items.first()
        assertEquals("MYSECRETKEY123", item.key)
    }

    companion object {
        private const val VALID_SINGLE_ACCOUNT_JSON = """
{
  "deviceId": "device-123",
  "deviceSecret": "secret-456",
  "localDeviceId": "local-789",
  "deviceName": "My Device",
  "version": 1,
  "accounts": [
    {
      "accountID": "account-1",
      "issuerName": "Test Issuer",
      "originalIssuerName": "Test Issuer",
      "userName": "test@example.com",
      "originalUserName": "test@example.com",
      "pushNotification": false,
      "secret": "JBSWY3DPEHPK3PXP",
      "timeStep": 30,
      "digits": 6,
      "creationTimestamp": 1234567890,
      "isFavorite": false,
      "algorithm": "SHA1",
      "folderData": null,
      "backupInfo": null
    }
  ],
  "folders": []
}
"""

        private const val VALID_MULTIPLE_ACCOUNTS_JSON = """
{
  "deviceId": "device-123",
  "deviceSecret": "secret-456",
  "localDeviceId": "local-789",
  "deviceName": "My Device",
  "version": 1,
  "accounts": [
    {
      "accountID": "account-1",
      "issuerName": "Issuer 1",
      "originalIssuerName": "Issuer 1",
      "userName": "user1@example.com",
      "originalUserName": "user1@example.com",
      "pushNotification": false,
      "secret": "SECRET1",
      "timeStep": 30,
      "digits": 6,
      "creationTimestamp": 1234567890,
      "isFavorite": false,
      "algorithm": "SHA1",
      "folderData": null,
      "backupInfo": null
    },
    {
      "accountID": "account-2",
      "issuerName": "Issuer 2",
      "originalIssuerName": "Issuer 2",
      "userName": "user2@example.com",
      "originalUserName": "user2@example.com",
      "pushNotification": false,
      "secret": "SECRET2",
      "timeStep": 30,
      "digits": 6,
      "creationTimestamp": 1234567891,
      "isFavorite": true,
      "algorithm": "SHA256",
      "folderData": null,
      "backupInfo": null
    },
    {
      "accountID": "account-3",
      "issuerName": "Issuer 3",
      "originalIssuerName": "Issuer 3",
      "userName": "user3@example.com",
      "originalUserName": "user3@example.com",
      "pushNotification": false,
      "secret": "SECRET3",
      "timeStep": 60,
      "digits": 8,
      "creationTimestamp": 1234567892,
      "isFavorite": false,
      "algorithm": "SHA512",
      "folderData": null,
      "backupInfo": null
    }
  ],
  "folders": []
}
"""

        private const val VALID_SHA1_ALGORITHM_JSON = """
{
  "deviceId": "device-123",
  "deviceSecret": "secret-456",
  "localDeviceId": "local-789",
  "deviceName": "My Device",
  "version": 1,
  "accounts": [
    {
      "accountID": "account-1",
      "issuerName": "Test",
      "originalIssuerName": "Test",
      "userName": "user",
      "originalUserName": "user",
      "pushNotification": false,
      "secret": "TESTSECRET",
      "timeStep": 30,
      "digits": 6,
      "creationTimestamp": 1234567890,
      "isFavorite": false,
      "algorithm": "SHA1",
      "folderData": null,
      "backupInfo": null
    }
  ],
  "folders": []
}
"""

        private const val VALID_SHA256_ALGORITHM_JSON = """
{
  "deviceId": "device-123",
  "deviceSecret": "secret-456",
  "localDeviceId": "local-789",
  "deviceName": "My Device",
  "version": 1,
  "accounts": [
    {
      "accountID": "account-1",
      "issuerName": "Test",
      "originalIssuerName": "Test",
      "userName": "user",
      "originalUserName": "user",
      "pushNotification": false,
      "secret": "TESTSECRET",
      "timeStep": 30,
      "digits": 6,
      "creationTimestamp": 1234567890,
      "isFavorite": false,
      "algorithm": "SHA256",
      "folderData": null,
      "backupInfo": null
    }
  ],
  "folders": []
}
"""

        private const val VALID_SHA512_ALGORITHM_JSON = """
{
  "deviceId": "device-123",
  "deviceSecret": "secret-456",
  "localDeviceId": "local-789",
  "deviceName": "My Device",
  "version": 1,
  "accounts": [
    {
      "accountID": "account-1",
      "issuerName": "Test",
      "originalIssuerName": "Test",
      "userName": "user",
      "originalUserName": "user",
      "pushNotification": false,
      "secret": "TESTSECRET",
      "timeStep": 30,
      "digits": 6,
      "creationTimestamp": 1234567890,
      "isFavorite": false,
      "algorithm": "SHA512",
      "folderData": null,
      "backupInfo": null
    }
  ],
  "folders": []
}
"""

        private const val INVALID_ALGORITHM_JSON = """
{
  "deviceId": "device-123",
  "deviceSecret": "secret-456",
  "localDeviceId": "local-789",
  "deviceName": "My Device",
  "version": 1,
  "accounts": [
    {
      "accountID": "account-1",
      "issuerName": "Test",
      "originalIssuerName": "Test",
      "userName": "user",
      "originalUserName": "user",
      "pushNotification": false,
      "secret": "TESTSECRET",
      "timeStep": 30,
      "digits": 6,
      "creationTimestamp": 1234567890,
      "isFavorite": false,
      "algorithm": "MD5",
      "folderData": null,
      "backupInfo": null
    }
  ],
  "folders": []
}
"""

        private const val VALID_ORIGINAL_ISSUER_NAME_JSON = """
{
  "deviceId": "device-123",
  "deviceSecret": "secret-456",
  "localDeviceId": "local-789",
  "deviceName": "My Device",
  "version": 1,
  "accounts": [
    {
      "accountID": "account-1",
      "issuerName": "ModifiedGitHub",
      "originalIssuerName": "GitHub",
      "userName": "user",
      "originalUserName": "user",
      "pushNotification": false,
      "secret": "TESTSECRET",
      "timeStep": 30,
      "digits": 6,
      "creationTimestamp": 1234567890,
      "isFavorite": false,
      "algorithm": "SHA1",
      "folderData": null,
      "backupInfo": null
    }
  ],
  "folders": []
}
"""

        private const val VALID_ORIGINAL_USER_NAME_JSON = """
{
  "deviceId": "device-123",
  "deviceSecret": "secret-456",
  "localDeviceId": "local-789",
  "deviceName": "My Device",
  "version": 1,
  "accounts": [
    {
      "accountID": "account-1",
      "issuerName": "GitHub",
      "originalIssuerName": "GitHub",
      "userName": "modified@github.com",
      "originalUserName": "user@github.com",
      "pushNotification": false,
      "secret": "TESTSECRET",
      "timeStep": 30,
      "digits": 6,
      "creationTimestamp": 1234567890,
      "isFavorite": false,
      "algorithm": "SHA1",
      "folderData": null,
      "backupInfo": null
    }
  ],
  "folders": []
}
"""

        private const val VALID_CUSTOM_TIME_STEP_JSON = """
{
  "deviceId": "device-123",
  "deviceSecret": "secret-456",
  "localDeviceId": "local-789",
  "deviceName": "My Device",
  "version": 1,
  "accounts": [
    {
      "accountID": "account-1",
      "issuerName": "Test",
      "originalIssuerName": "Test",
      "userName": "user",
      "originalUserName": "user",
      "pushNotification": false,
      "secret": "TESTSECRET",
      "timeStep": 60,
      "digits": 6,
      "creationTimestamp": 1234567890,
      "isFavorite": false,
      "algorithm": "SHA1",
      "folderData": null,
      "backupInfo": null
    }
  ],
  "folders": []
}
"""

        private const val VALID_CUSTOM_DIGITS_JSON = """
{
  "deviceId": "device-123",
  "deviceSecret": "secret-456",
  "localDeviceId": "local-789",
  "deviceName": "My Device",
  "version": 1,
  "accounts": [
    {
      "accountID": "account-1",
      "issuerName": "Test",
      "originalIssuerName": "Test",
      "userName": "user",
      "originalUserName": "user",
      "pushNotification": false,
      "secret": "TESTSECRET",
      "timeStep": 30,
      "digits": 8,
      "creationTimestamp": 1234567890,
      "isFavorite": false,
      "algorithm": "SHA1",
      "folderData": null,
      "backupInfo": null
    }
  ],
  "folders": []
}
"""

        private const val VALID_FAVORITE_TRUE_JSON = """
{
  "deviceId": "device-123",
  "deviceSecret": "secret-456",
  "localDeviceId": "local-789",
  "deviceName": "My Device",
  "version": 1,
  "accounts": [
    {
      "accountID": "account-1",
      "issuerName": "Test",
      "originalIssuerName": "Test",
      "userName": "user",
      "originalUserName": "user",
      "pushNotification": false,
      "secret": "TESTSECRET",
      "timeStep": 30,
      "digits": 6,
      "creationTimestamp": 1234567890,
      "isFavorite": true,
      "algorithm": "SHA1",
      "folderData": null,
      "backupInfo": null
    }
  ],
  "folders": []
}
"""

        private const val VALID_FAVORITE_FALSE_JSON = """
{
  "deviceId": "device-123",
  "deviceSecret": "secret-456",
  "localDeviceId": "local-789",
  "deviceName": "My Device",
  "version": 1,
  "accounts": [
    {
      "accountID": "account-1",
      "issuerName": "Test",
      "originalIssuerName": "Test",
      "userName": "user",
      "originalUserName": "user",
      "pushNotification": false,
      "secret": "TESTSECRET",
      "timeStep": 30,
      "digits": 6,
      "creationTimestamp": 1234567890,
      "isFavorite": false,
      "algorithm": "SHA1",
      "folderData": null,
      "backupInfo": null
    }
  ],
  "folders": []
}
"""

        private const val MALFORMED_JSON = """
{
  "deviceId": "device-123",
  "deviceSecret": "secret-456"
  "localDeviceId": "local-789"
"""

        private const val MISSING_ACCOUNTS_FIELD_JSON = """
{
  "deviceId": "device-123",
  "deviceSecret": "secret-456",
  "localDeviceId": "local-789",
  "deviceName": "My Device",
  "version": 1,
  "folders": []
}
"""

        private const val VALID_EMPTY_ACCOUNTS_JSON = """
{
  "deviceId": "device-123",
  "deviceSecret": "secret-456",
  "localDeviceId": "local-789",
  "deviceName": "My Device",
  "version": 1,
  "accounts": [],
  "folders": []
}
"""

        private const val VALID_NULL_FOLDER_DATA_JSON = """
{
  "deviceId": "device-123",
  "deviceSecret": "secret-456",
  "localDeviceId": "local-789",
  "deviceName": "My Device",
  "version": 1,
  "accounts": [
    {
      "accountID": "account-1",
      "issuerName": "Test",
      "originalIssuerName": "Test",
      "userName": "user",
      "originalUserName": "user",
      "pushNotification": false,
      "secret": "TESTSECRET",
      "timeStep": 30,
      "digits": 6,
      "creationTimestamp": 1234567890,
      "isFavorite": false,
      "algorithm": "SHA1",
      "folderData": null,
      "backupInfo": null
    }
  ],
  "folders": []
}
"""

        private const val VALID_NULL_BACKUP_INFO_JSON = """
{
  "deviceId": "device-123",
  "deviceSecret": "secret-456",
  "localDeviceId": "local-789",
  "deviceName": "My Device",
  "version": 1,
  "accounts": [
    {
      "accountID": "account-1",
      "issuerName": "Test",
      "originalIssuerName": "Test",
      "userName": "user",
      "originalUserName": "user",
      "pushNotification": false,
      "secret": "TESTSECRET",
      "timeStep": 30,
      "digits": 6,
      "creationTimestamp": 1234567890,
      "isFavorite": false,
      "algorithm": "SHA1",
      "folderData": null,
      "backupInfo": null
    }
  ],
  "folders": []
}
"""

        private const val VALID_SECRET_FIELD_JSON = """
{
  "deviceId": "device-123",
  "deviceSecret": "secret-456",
  "localDeviceId": "local-789",
  "deviceName": "My Device",
  "version": 1,
  "accounts": [
    {
      "accountID": "account-1",
      "issuerName": "Test",
      "originalIssuerName": "Test",
      "userName": "user",
      "originalUserName": "user",
      "pushNotification": false,
      "secret": "MYSECRETKEY123",
      "timeStep": 30,
      "digits": 6,
      "creationTimestamp": 1234567890,
      "isFavorite": false,
      "algorithm": "SHA1",
      "folderData": null,
      "backupInfo": null
    }
  ],
  "folders": []
}
"""
    }
}
