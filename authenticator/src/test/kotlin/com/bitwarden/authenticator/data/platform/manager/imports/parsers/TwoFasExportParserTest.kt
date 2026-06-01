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

class TwoFasExportParserTest {

    private val mockUuidManager = mockk<UuidManager>()
    private val parser = TwoFasExportParser(mockUuidManager)

    @BeforeEach
    fun setup() {
        every { mockUuidManager.generateUuid() } returns "00000000-0000-0000-0000-000000000001"
    }

    @Test
    fun `parseForResult with servicesEncrypted not null should return Error`() {
        val json = ENCRYPTED_SERVICES_JSON

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Error(
            message = BitwardenString.import_2fas_password_protected_not_supported.asText(),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult with servicesEncrypted empty string should succeed`() {
        val json = ENCRYPTED_SERVICES_EMPTY_JSON

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Success(items = emptyList())
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult with servicesEncrypted null should succeed`() {
        val json = VALID_SINGLE_SERVICE_JSON

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
                    issuer = "GitHub",
                    accountName = "user@example.com",
                    favorite = false,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult with HOTP service should return Error`() {
        val json = HOTP_SERVICE_JSON

        val result = parser.parseForResult(json.toByteArray())
        val expected = ExportParseResult.Error(
            message = "Unsupported OTP type: HOTP.".asText(),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult with mixed TOTP and HOTP should return Error due to HOTP`() {
        val json = MIXED_TOTP_HOTP_JSON

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Error(
            message = "Unsupported OTP type: HOTP.".asText(),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult with valid TOTP should succeed`() {
        val json = VALID_SINGLE_SERVICE_JSON

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
                    issuer = "GitHub",
                    accountName = "user@example.com",
                    favorite = false,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult with unsupported tokenType should return Error`() {
        val json = UNSUPPORTED_TOKEN_TYPE_JSON

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Error(
            message = "Unsupported OTP type: UNKNOWN.".asText(),
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
                    key = "SECRET",
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
                    key = "SECRET",
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
                    key = "SECRET",
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
    fun `parseForResult with case insensitive algorithm should succeed`() {
        val json = CASE_INSENSITIVE_ALGORITHM_JSON

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Success(
            items = listOf(
                AuthenticatorItemEntity(
                    id = "00000000-0000-0000-0000-000000000001",
                    key = "SECRET",
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
    fun `parseForResult with null algorithm should default to SHA1`() {
        val json = NULL_ALGORITHM_JSON

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Success(
            items = listOf(
                AuthenticatorItemEntity(
                    id = "00000000-0000-0000-0000-000000000001",
                    key = "SECRET",
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
    fun `parseForResult with null period should default to 30`() {
        val json = NULL_PERIOD_JSON

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Success(
            items = listOf(
                AuthenticatorItemEntity(
                    id = "00000000-0000-0000-0000-000000000001",
                    key = "SECRET",
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
    fun `parseForResult with custom period should use provided value`() {
        val json = CUSTOM_PERIOD_JSON

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Success(
            items = listOf(
                AuthenticatorItemEntity(
                    id = "00000000-0000-0000-0000-000000000001",
                    key = "SECRET",
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
    fun `parseForResult with null digits should default to 6`() {
        val json = NULL_DIGITS_JSON

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Success(
            items = listOf(
                AuthenticatorItemEntity(
                    id = "00000000-0000-0000-0000-000000000001",
                    key = "SECRET",
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
    fun `parseForResult with custom digits should use provided value`() {
        val json = CUSTOM_DIGITS_JSON

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Success(
            items = listOf(
                AuthenticatorItemEntity(
                    id = "00000000-0000-0000-0000-000000000001",
                    key = "SECRET",
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
    fun `parseForResult with issuer should use issuer field`() {
        val json = VALID_WITH_ISSUER_JSON

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Success(
            items = listOf(
                AuthenticatorItemEntity(
                    id = "00000000-0000-0000-0000-000000000001",
                    key = "SECRET",
                    type = AuthenticatorItemType.TOTP,
                    algorithm = AuthenticatorItemAlgorithm.SHA1,
                    period = 30,
                    digits = 6,
                    issuer = "Microsoft",
                    accountName = "user",
                    favorite = false,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult with null issuer should fallback to name`() {
        val json = NULL_ISSUER_JSON

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Success(
            items = listOf(
                AuthenticatorItemEntity(
                    id = "00000000-0000-0000-0000-000000000001",
                    key = "SECRET",
                    type = AuthenticatorItemType.TOTP,
                    algorithm = AuthenticatorItemAlgorithm.SHA1,
                    period = 30,
                    digits = 6,
                    issuer = "ServiceName",
                    accountName = "user",
                    favorite = false,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult with empty issuer should fallback to name`() {
        val json = EMPTY_ISSUER_JSON

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Success(
            items = listOf(
                AuthenticatorItemEntity(
                    id = "00000000-0000-0000-0000-000000000001",
                    key = "SECRET",
                    type = AuthenticatorItemType.TOTP,
                    algorithm = AuthenticatorItemAlgorithm.SHA1,
                    period = 30,
                    digits = 6,
                    issuer = "ServiceName",
                    accountName = "user",
                    favorite = false,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult should always set favorite to false`() {
        val json = VALID_SINGLE_SERVICE_JSON

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
                    issuer = "GitHub",
                    accountName = "user@example.com",
                    favorite = false,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult with multiple services should return all TOTP items`() {
        every { mockUuidManager.generateUuid() } returnsMany listOf(
            "00000000-0000-0000-0000-000000000001",
            "00000000-0000-0000-0000-000000000002",
            "00000000-0000-0000-0000-000000000003",
        )
        val json = MULTIPLE_SERVICES_JSON

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
                    issuer = "Service 1",
                    accountName = "user1",
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
                    issuer = "Service 2",
                    accountName = "user2",
                    favorite = false,
                    userId = null,
                ),
                AuthenticatorItemEntity(
                    id = "00000000-0000-0000-0000-000000000003",
                    key = "SECRET3",
                    type = AuthenticatorItemType.TOTP,
                    algorithm = AuthenticatorItemAlgorithm.SHA512,
                    period = 60,
                    digits = 8,
                    issuer = "Service 3",
                    accountName = "user3",
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
    fun `parseForResult with missing services field should return Error`() {
        val json = MISSING_SERVICES_FIELD_JSON

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Error(
            title = BitwardenString.required_information_missing.asText(),
            message = BitwardenString.required_information_missing_message.asText(),
        )

        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult with empty services should return empty list`() {
        val json = EMPTY_SERVICES_JSON

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
        val json = MULTIPLE_SERVICES_JSON

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
                    issuer = "Service 1",
                    accountName = "user1",
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
                    issuer = "Service 2",
                    accountName = "user2",
                    favorite = false,
                    userId = null,
                ),
                AuthenticatorItemEntity(
                    id = "00000000-0000-0000-0000-000000000003",
                    key = "SECRET3",
                    type = AuthenticatorItemType.TOTP,
                    algorithm = AuthenticatorItemAlgorithm.SHA512,
                    period = 60,
                    digits = 8,
                    issuer = "Service 3",
                    accountName = "user3",
                    favorite = false,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }
}

private const val ENCRYPTED_SERVICES_JSON = """
{
  "schemaVersion": 2,
  "appVersionCode": 4020303,
  "appOrigin": "android",
  "services": [],
  "servicesEncrypted": "encryptedData123",
  "groups": []
}
"""

private const val ENCRYPTED_SERVICES_EMPTY_JSON = """
{
  "schemaVersion": 2,
  "appVersionCode": 4020303,
  "appOrigin": "android",
  "services": [],
  "servicesEncrypted": "",
  "groups": []
}
"""

private const val VALID_SINGLE_SERVICE_JSON = """
{
  "schemaVersion": 2,
  "appVersionCode": 4020303,
  "appOrigin": "android",
  "services": [
    {
      "otp": {
        "counter": null,
        "period": 30,
        "digits": 6,
        "account": "user@example.com",
        "source": "manual",
        "tokenType": "TOTP",
        "algorithm": "SHA1",
        "link": null,
        "issuer": "GitHub"
      },
      "order": {
        "position": 0
      },
      "updatedAt": 1234567890,
      "name": "GitHub Service",
      "icon": null,
      "secret": "JBSWY3DPEHPK3PXP",
      "badge": null,
      "serviceTypeId": "github"
    }
  ],
  "servicesEncrypted": null,
  "groups": []
}
"""

private const val HOTP_SERVICE_JSON = """
{
  "schemaVersion": 2,
  "appVersionCode": 4020303,
  "appOrigin": "android",
  "services": [
    {
      "otp": {
        "counter": 0,
        "period": null,
        "digits": 6,
        "account": "user@example.com",
        "source": "manual",
        "tokenType": "HOTP",
        "algorithm": "SHA1",
        "link": null,
        "issuer": "TestIssuer"
      },
      "order": null,
      "updatedAt": 1234567890,
      "name": "HOTP Service",
      "icon": null,
      "secret": "HOPTSECRET",
      "badge": null,
      "serviceTypeId": null
    }
  ],
  "servicesEncrypted": null,
  "groups": []
}
"""

private const val MIXED_TOTP_HOTP_JSON = """
{
  "schemaVersion": 2,
  "appVersionCode": 4020303,
  "appOrigin": "android",
  "services": [
    {
      "otp": {
        "counter": null,
        "period": 30,
        "digits": 6,
        "account": "user1",
        "source": "manual",
        "tokenType": "TOTP",
        "algorithm": "SHA1",
        "link": null,
        "issuer": "Service1"
      },
      "order": null,
      "updatedAt": 1234567890,
      "name": "Service 1",
      "icon": null,
      "secret": "SECRET1",
      "badge": null,
      "serviceTypeId": null
    },
    {
      "otp": {
        "counter": 0,
        "period": null,
        "digits": 6,
        "account": "user2",
        "source": "manual",
        "tokenType": "HOTP",
        "algorithm": "SHA1",
        "link": null,
        "issuer": "Service2"
      },
      "order": null,
      "updatedAt": 1234567891,
      "name": "Service 2",
      "icon": null,
      "secret": "SECRET2",
      "badge": null,
      "serviceTypeId": null
    },
    {
      "otp": {
        "counter": null,
        "period": 30,
        "digits": 6,
        "account": "user3",
        "source": "manual",
        "tokenType": "TOTP",
        "algorithm": "SHA1",
        "link": null,
        "issuer": "Service3"
      },
      "order": null,
      "updatedAt": 1234567892,
      "name": "Service 3",
      "icon": null,
      "secret": "SECRET3",
      "badge": null,
      "serviceTypeId": null
    }
  ],
  "servicesEncrypted": null,
  "groups": []
}
"""

private const val UNSUPPORTED_TOKEN_TYPE_JSON = """
{
  "schemaVersion": 2,
  "appVersionCode": 4020303,
  "appOrigin": "android",
  "services": [
    {
      "otp": {
        "counter": null,
        "period": 30,
        "digits": 6,
        "account": "user",
        "source": "manual",
        "tokenType": "UNKNOWN",
        "algorithm": "SHA1",
        "link": null,
        "issuer": "Test"
      },
      "order": null,
      "updatedAt": 1234567890,
      "name": "Test",
      "icon": null,
      "secret": "SECRET",
      "badge": null,
      "serviceTypeId": null
    }
  ],
  "servicesEncrypted": null,
  "groups": []
}
"""

private const val VALID_SHA1_ALGORITHM_JSON = """
{
  "schemaVersion": 2,
  "appVersionCode": 4020303,
  "appOrigin": "android",
  "services": [
    {
      "otp": {
        "counter": null,
        "period": 30,
        "digits": 6,
        "account": "user",
        "source": "manual",
        "tokenType": "TOTP",
        "algorithm": "SHA1",
        "link": null,
        "issuer": "Test"
      },
      "order": null,
      "updatedAt": 1234567890,
      "name": "Test",
      "icon": null,
      "secret": "SECRET",
      "badge": null,
      "serviceTypeId": null
    }
  ],
  "servicesEncrypted": null,
  "groups": []
}
"""

private const val VALID_SHA256_ALGORITHM_JSON = """
{
  "schemaVersion": 2,
  "appVersionCode": 4020303,
  "appOrigin": "android",
  "services": [
    {
      "otp": {
        "counter": null,
        "period": 30,
        "digits": 6,
        "account": "user",
        "source": "manual",
        "tokenType": "TOTP",
        "algorithm": "SHA256",
        "link": null,
        "issuer": "Test"
      },
      "order": null,
      "updatedAt": 1234567890,
      "name": "Test",
      "icon": null,
      "secret": "SECRET",
      "badge": null,
      "serviceTypeId": null
    }
  ],
  "servicesEncrypted": null,
  "groups": []
}
"""

private const val VALID_SHA512_ALGORITHM_JSON = """
{
  "schemaVersion": 2,
  "appVersionCode": 4020303,
  "appOrigin": "android",
  "services": [
    {
      "otp": {
        "counter": null,
        "period": 30,
        "digits": 6,
        "account": "user",
        "source": "manual",
        "tokenType": "TOTP",
        "algorithm": "SHA512",
        "link": null,
        "issuer": "Test"
      },
      "order": null,
      "updatedAt": 1234567890,
      "name": "Test",
      "icon": null,
      "secret": "SECRET",
      "badge": null,
      "serviceTypeId": null
    }
  ],
  "servicesEncrypted": null,
  "groups": []
}
"""

private const val CASE_INSENSITIVE_ALGORITHM_JSON = """
{
  "schemaVersion": 2,
  "appVersionCode": 4020303,
  "appOrigin": "android",
  "services": [
    {
      "otp": {
        "counter": null,
        "period": 30,
        "digits": 6,
        "account": "user",
        "source": "manual",
        "tokenType": "TOTP",
        "algorithm": "sha256",
        "link": null,
        "issuer": "Test"
      },
      "order": null,
      "updatedAt": 1234567890,
      "name": "Test",
      "icon": null,
      "secret": "SECRET",
      "badge": null,
      "serviceTypeId": null
    }
  ],
  "servicesEncrypted": null,
  "groups": []
}
"""

private const val NULL_ALGORITHM_JSON = """
{
  "schemaVersion": 2,
  "appVersionCode": 4020303,
  "appOrigin": "android",
  "services": [
    {
      "otp": {
        "counter": null,
        "period": 30,
        "digits": 6,
        "account": "user",
        "source": "manual",
        "tokenType": "TOTP",
        "algorithm": null,
        "link": null,
        "issuer": "Test"
      },
      "order": null,
      "updatedAt": 1234567890,
      "name": "Test",
      "icon": null,
      "secret": "SECRET",
      "badge": null,
      "serviceTypeId": null
    }
  ],
  "servicesEncrypted": null,
  "groups": []
}
"""

private const val NULL_PERIOD_JSON = """
{
  "schemaVersion": 2,
  "appVersionCode": 4020303,
  "appOrigin": "android",
  "services": [
    {
      "otp": {
        "counter": null,
        "period": null,
        "digits": 6,
        "account": "user",
        "source": "manual",
        "tokenType": "TOTP",
        "algorithm": "SHA1",
        "link": null,
        "issuer": "Test"
      },
      "order": null,
      "updatedAt": 1234567890,
      "name": "Test",
      "icon": null,
      "secret": "SECRET",
      "badge": null,
      "serviceTypeId": null
    }
  ],
  "servicesEncrypted": null,
  "groups": []
}
"""

private const val CUSTOM_PERIOD_JSON = """
{
  "schemaVersion": 2,
  "appVersionCode": 4020303,
  "appOrigin": "android",
  "services": [
    {
      "otp": {
        "counter": null,
        "period": 60,
        "digits": 6,
        "account": "user",
        "source": "manual",
        "tokenType": "TOTP",
        "algorithm": "SHA1",
        "link": null,
        "issuer": "Test"
      },
      "order": null,
      "updatedAt": 1234567890,
      "name": "Test",
      "icon": null,
      "secret": "SECRET",
      "badge": null,
      "serviceTypeId": null
    }
  ],
  "servicesEncrypted": null,
  "groups": []
}
"""

private const val NULL_DIGITS_JSON = """
{
  "schemaVersion": 2,
  "appVersionCode": 4020303,
  "appOrigin": "android",
  "services": [
    {
      "otp": {
        "counter": null,
        "period": 30,
        "digits": null,
        "account": "user",
        "source": "manual",
        "tokenType": "TOTP",
        "algorithm": "SHA1",
        "link": null,
        "issuer": "Test"
      },
      "order": null,
      "updatedAt": 1234567890,
      "name": "Test",
      "icon": null,
      "secret": "SECRET",
      "badge": null,
      "serviceTypeId": null
    }
  ],
  "servicesEncrypted": null,
  "groups": []
}
"""

private const val CUSTOM_DIGITS_JSON = """
{
  "schemaVersion": 2,
  "appVersionCode": 4020303,
  "appOrigin": "android",
  "services": [
    {
      "otp": {
        "counter": null,
        "period": 30,
        "digits": 8,
        "account": "user",
        "source": "manual",
        "tokenType": "TOTP",
        "algorithm": "SHA1",
        "link": null,
        "issuer": "Test"
      },
      "order": null,
      "updatedAt": 1234567890,
      "name": "Test",
      "icon": null,
      "secret": "SECRET",
      "badge": null,
      "serviceTypeId": null
    }
  ],
  "servicesEncrypted": null,
  "groups": []
}
"""

private const val VALID_WITH_ISSUER_JSON = """
{
  "schemaVersion": 2,
  "appVersionCode": 4020303,
  "appOrigin": "android",
  "services": [
    {
      "otp": {
        "counter": null,
        "period": 30,
        "digits": 6,
        "account": "user",
        "source": "manual",
        "tokenType": "TOTP",
        "algorithm": "SHA1",
        "link": null,
        "issuer": "Microsoft"
      },
      "order": null,
      "updatedAt": 1234567890,
      "name": "Microsoft Service",
      "icon": null,
      "secret": "SECRET",
      "badge": null,
      "serviceTypeId": null
    }
  ],
  "servicesEncrypted": null,
  "groups": []
}
"""

private const val NULL_ISSUER_JSON = """
{
  "schemaVersion": 2,
  "appVersionCode": 4020303,
  "appOrigin": "android",
  "services": [
    {
      "otp": {
        "counter": null,
        "period": 30,
        "digits": 6,
        "account": "user",
        "source": "manual",
        "tokenType": "TOTP",
        "algorithm": "SHA1",
        "link": null,
        "issuer": null
      },
      "order": null,
      "updatedAt": 1234567890,
      "name": "ServiceName",
      "icon": null,
      "secret": "SECRET",
      "badge": null,
      "serviceTypeId": null
    }
  ],
  "servicesEncrypted": null,
  "groups": []
}
"""

private const val EMPTY_ISSUER_JSON = """
{
  "schemaVersion": 2,
  "appVersionCode": 4020303,
  "appOrigin": "android",
  "services": [
    {
      "otp": {
        "counter": null,
        "period": 30,
        "digits": 6,
        "account": "user",
        "source": "manual",
        "tokenType": "TOTP",
        "algorithm": "SHA1",
        "link": null,
        "issuer": ""
      },
      "order": null,
      "updatedAt": 1234567890,
      "name": "ServiceName",
      "icon": null,
      "secret": "SECRET",
      "badge": null,
      "serviceTypeId": null
    }
  ],
  "servicesEncrypted": null,
  "groups": []
}
"""

private const val MULTIPLE_SERVICES_JSON = """
{
  "schemaVersion": 2,
  "appVersionCode": 4020303,
  "appOrigin": "android",
  "services": [
    {
      "otp": {
        "counter": null,
        "period": 30,
        "digits": 6,
        "account": "user1",
        "source": "manual",
        "tokenType": "TOTP",
        "algorithm": "SHA1",
        "link": null,
        "issuer": "Service 1"
      },
      "order": null,
      "updatedAt": 1234567890,
      "name": "Service 1",
      "icon": null,
      "secret": "SECRET1",
      "badge": null,
      "serviceTypeId": null
    },
    {
      "otp": {
        "counter": null,
        "period": 30,
        "digits": 6,
        "account": "user2",
        "source": "manual",
        "tokenType": "TOTP",
        "algorithm": "SHA256",
        "link": null,
        "issuer": "Service 2"
      },
      "order": null,
      "updatedAt": 1234567891,
      "name": "Service 2",
      "icon": null,
      "secret": "SECRET2",
      "badge": null,
      "serviceTypeId": null
    },
    {
      "otp": {
        "counter": null,
        "period": 60,
        "digits": 8,
        "account": "user3",
        "source": "manual",
        "tokenType": "TOTP",
        "algorithm": "SHA512",
        "link": null,
        "issuer": "Service 3"
      },
      "order": null,
      "updatedAt": 1234567892,
      "name": "Service 3",
      "icon": null,
      "secret": "SECRET3",
      "badge": null,
      "serviceTypeId": null
    }
  ],
  "servicesEncrypted": null,
  "groups": []
}
"""

private const val MALFORMED_JSON = """
{
  "schemaVersion": 2,
  "appVersionCode": 4020303
  "appOrigin": "android"
"""

private const val MISSING_SERVICES_FIELD_JSON = """
{
  "schemaVersion": 2,
  "appVersionCode": 4020303,
  "appOrigin": "android",
  "servicesEncrypted": null,
  "groups": []
}
"""

private const val EMPTY_SERVICES_JSON = """
{
  "schemaVersion": 2,
  "appVersionCode": 4020303,
  "appOrigin": "android",
  "services": [],
  "servicesEncrypted": null,
  "groups": []
}
"""
