package com.bitwarden.authenticator.data.platform.manager.imports.parsers

import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemAlgorithm
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemEntity
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemType
import com.bitwarden.authenticator.data.platform.manager.imports.model.ExportParseResult
import com.bitwarden.core.data.manager.UuidManager
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
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

        val expected = ExportParseResult.Success(
            items = listOf(
                AuthenticatorItemEntity(
                    id = "00000000-0000-0000-0000-000000000001",
                    key = "JBSWY3DPEHPK3PXP",
                    type = AuthenticatorItemType.TOTP,
                    algorithm = AuthenticatorItemAlgorithm.SHA1,
                    period = 30,
                    digits = 6,
                    issuer = "Test Issuer",
                    accountName = "test@example.com",
                    favorite = false,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult with multiple accounts should return all items`() {
        every { mockUuidManager.generateUuid() } returnsMany listOf(
            "00000000-0000-0000-0000-000000000001",
            "00000000-0000-0000-0000-000000000002",
            "00000000-0000-0000-0000-000000000003",
        )
        val json = VALID_MULTIPLE_ACCOUNTS_JSON

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Success(
            items = listOf(
                AuthenticatorItemEntity(
                    id = "00000000-0000-0000-0000-000000000001",
                    key = "SECRET1",
                    type = AuthenticatorItemType.TOTP,
                    algorithm = AuthenticatorItemAlgorithm.SHA1,
                    period = 30,
                    digits = 6,
                    issuer = "Issuer 1",
                    accountName = "user1@example.com",
                    favorite = false,
                    userId = null,
                ),
                AuthenticatorItemEntity(
                    id = "00000000-0000-0000-0000-000000000002",
                    key = "SECRET2",
                    type = AuthenticatorItemType.TOTP,
                    algorithm = AuthenticatorItemAlgorithm.SHA256,
                    period = 30,
                    digits = 6,
                    issuer = "Issuer 2",
                    accountName = "user2@example.com",
                    favorite = true,
                    userId = null,
                ),
                AuthenticatorItemEntity(
                    id = "00000000-0000-0000-0000-000000000003",
                    key = "SECRET3",
                    type = AuthenticatorItemType.TOTP,
                    algorithm = AuthenticatorItemAlgorithm.SHA512,
                    period = 60,
                    digits = 8,
                    issuer = "Issuer 3",
                    accountName = "user3@example.com",
                    favorite = false,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult should always create TOTP type`() {
        val json = VALID_SINGLE_ACCOUNT_JSON

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Success(
            items = listOf(
                AuthenticatorItemEntity(
                    id = "00000000-0000-0000-0000-000000000001",
                    key = "JBSWY3DPEHPK3PXP",
                    type = AuthenticatorItemType.TOTP,
                    algorithm = AuthenticatorItemAlgorithm.SHA1,
                    period = 30,
                    digits = 6,
                    issuer = "Test Issuer",
                    accountName = "test@example.com",
                    favorite = false,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult with SHA1 algorithm should succeed`() {
        val json = VALID_SHA1_ALGORITHM_JSON

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Success(
            items = listOf(
                AuthenticatorItemEntity(
                    id = "00000000-0000-0000-0000-000000000001",
                    key = "TESTSECRET",
                    type = AuthenticatorItemType.TOTP,
                    algorithm = AuthenticatorItemAlgorithm.SHA1,
                    period = 30,
                    digits = 6,
                    issuer = "Test",
                    accountName = "user",
                    favorite = false,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult with SHA256 algorithm should succeed`() {
        val json = VALID_SHA256_ALGORITHM_JSON

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Success(
            items = listOf(
                AuthenticatorItemEntity(
                    id = "00000000-0000-0000-0000-000000000001",
                    key = "TESTSECRET",
                    type = AuthenticatorItemType.TOTP,
                    algorithm = AuthenticatorItemAlgorithm.SHA256,
                    period = 30,
                    digits = 6,
                    issuer = "Test",
                    accountName = "user",
                    favorite = false,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult with SHA512 algorithm should succeed`() {
        val json = VALID_SHA512_ALGORITHM_JSON

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Success(
            items = listOf(
                AuthenticatorItemEntity(
                    id = "00000000-0000-0000-0000-000000000001",
                    key = "TESTSECRET",
                    type = AuthenticatorItemType.TOTP,
                    algorithm = AuthenticatorItemAlgorithm.SHA512,
                    period = 30,
                    digits = 6,
                    issuer = "Test",
                    accountName = "user",
                    favorite = false,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult with unsupported algorithm should return Error`() {
        val json = INVALID_ALGORITHM_JSON

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Error(
            message = "Unsupported algorithm.".asText(),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult should map originalIssuerName to issuer`() {
        val json = VALID_ORIGINAL_ISSUER_NAME_JSON

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Success(
            items = listOf(
                AuthenticatorItemEntity(
                    id = "00000000-0000-0000-0000-000000000001",
                    key = "TESTSECRET",
                    type = AuthenticatorItemType.TOTP,
                    algorithm = AuthenticatorItemAlgorithm.SHA1,
                    period = 30,
                    digits = 6,
                    issuer = "GitHub",
                    accountName = "user",
                    favorite = false,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult should map originalUserName to accountName`() {
        val json = VALID_ORIGINAL_USER_NAME_JSON

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Success(
            items = listOf(
                AuthenticatorItemEntity(
                    id = "00000000-0000-0000-0000-000000000001",
                    key = "TESTSECRET",
                    type = AuthenticatorItemType.TOTP,
                    algorithm = AuthenticatorItemAlgorithm.SHA1,
                    period = 30,
                    digits = 6,
                    issuer = "GitHub",
                    accountName = "user@github.com",
                    favorite = false,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult should map timeStep to period`() {
        val json = VALID_CUSTOM_TIME_STEP_JSON

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Success(
            items = listOf(
                AuthenticatorItemEntity(
                    id = "00000000-0000-0000-0000-000000000001",
                    key = "TESTSECRET",
                    type = AuthenticatorItemType.TOTP,
                    algorithm = AuthenticatorItemAlgorithm.SHA1,
                    period = 60,
                    digits = 6,
                    issuer = "Test",
                    accountName = "user",
                    favorite = false,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult should map digits to digits`() {
        val json = VALID_CUSTOM_DIGITS_JSON

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Success(
            items = listOf(
                AuthenticatorItemEntity(
                    id = "00000000-0000-0000-0000-000000000001",
                    key = "TESTSECRET",
                    type = AuthenticatorItemType.TOTP,
                    algorithm = AuthenticatorItemAlgorithm.SHA1,
                    period = 30,
                    digits = 8,
                    issuer = "Test",
                    accountName = "user",
                    favorite = false,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult with isFavorite true should preserve flag`() {
        val json = VALID_FAVORITE_TRUE_JSON

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Success(
            items = listOf(
                AuthenticatorItemEntity(
                    id = "00000000-0000-0000-0000-000000000001",
                    key = "TESTSECRET",
                    type = AuthenticatorItemType.TOTP,
                    algorithm = AuthenticatorItemAlgorithm.SHA1,
                    period = 30,
                    digits = 6,
                    issuer = "Test",
                    accountName = "user",
                    favorite = true,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult with isFavorite false should preserve flag`() {
        val json = VALID_FAVORITE_FALSE_JSON

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Success(
            items = listOf(
                AuthenticatorItemEntity(
                    id = "00000000-0000-0000-0000-000000000001",
                    key = "TESTSECRET",
                    type = AuthenticatorItemType.TOTP,
                    algorithm = AuthenticatorItemAlgorithm.SHA1,
                    period = 30,
                    digits = 6,
                    issuer = "Test",
                    accountName = "user",
                    favorite = false,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult with malformed JSON should return Error`() {
        val json = MALFORMED_JSON

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Error(
            title = BitwardenString.file_could_not_be_processed.asText(),
            message = BitwardenString.file_could_not_be_processed_message.asText(),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult with missing required field should return Error`() {
        val json = MISSING_ACCOUNTS_FIELD_JSON

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Error(
            title = BitwardenString.required_information_missing.asText(),
            message = BitwardenString.required_information_missing_message.asText(),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult with empty accounts should return empty list`() {
        val json = VALID_EMPTY_ACCOUNTS_JSON

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Success(items = emptyList())
        assertEquals(expected, result)
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

        val expected = ExportParseResult.Success(
            items = listOf(
                AuthenticatorItemEntity(
                    id = "00000000-0000-0000-0000-000000000001",
                    key = "SECRET1",
                    type = AuthenticatorItemType.TOTP,
                    algorithm = AuthenticatorItemAlgorithm.SHA1,
                    period = 30,
                    digits = 6,
                    issuer = "Issuer 1",
                    accountName = "user1@example.com",
                    favorite = false,
                    userId = null,
                ),
                AuthenticatorItemEntity(
                    id = "00000000-0000-0000-0000-000000000002",
                    key = "SECRET2",
                    type = AuthenticatorItemType.TOTP,
                    algorithm = AuthenticatorItemAlgorithm.SHA256,
                    period = 30,
                    digits = 6,
                    issuer = "Issuer 2",
                    accountName = "user2@example.com",
                    favorite = true,
                    userId = null,
                ),
                AuthenticatorItemEntity(
                    id = "00000000-0000-0000-0000-000000000003",
                    key = "SECRET3",
                    type = AuthenticatorItemType.TOTP,
                    algorithm = AuthenticatorItemAlgorithm.SHA512,
                    period = 60,
                    digits = 8,
                    issuer = "Issuer 3",
                    accountName = "user3@example.com",
                    favorite = false,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult with null folderData should succeed`() {
        val json = VALID_NULL_FOLDER_DATA_JSON

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Success(
            items = listOf(
                AuthenticatorItemEntity(
                    id = "00000000-0000-0000-0000-000000000001",
                    key = "TESTSECRET",
                    type = AuthenticatorItemType.TOTP,
                    algorithm = AuthenticatorItemAlgorithm.SHA1,
                    period = 30,
                    digits = 6,
                    issuer = "Test",
                    accountName = "user",
                    favorite = false,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult with null backupInfo should succeed`() {
        val json = VALID_NULL_BACKUP_INFO_JSON

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Success(
            items = listOf(
                AuthenticatorItemEntity(
                    id = "00000000-0000-0000-0000-000000000001",
                    key = "TESTSECRET",
                    type = AuthenticatorItemType.TOTP,
                    algorithm = AuthenticatorItemAlgorithm.SHA1,
                    period = 30,
                    digits = 6,
                    issuer = "Test",
                    accountName = "user",
                    favorite = false,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult should use secret field for key`() {
        val json = VALID_SECRET_FIELD_JSON

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Success(
            items = listOf(
                AuthenticatorItemEntity(
                    id = "00000000-0000-0000-0000-000000000001",
                    key = "MYSECRETKEY123",
                    type = AuthenticatorItemType.TOTP,
                    algorithm = AuthenticatorItemAlgorithm.SHA1,
                    period = 30,
                    digits = 6,
                    issuer = "Test",
                    accountName = "user",
                    favorite = false,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }
}

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
