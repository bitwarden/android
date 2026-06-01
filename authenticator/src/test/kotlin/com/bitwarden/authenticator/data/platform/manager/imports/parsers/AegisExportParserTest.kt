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

class AegisExportParserTest {

    private val mockUuidManager = mockk<UuidManager>()
    private val parser = AegisExportParser(mockUuidManager)

    @BeforeEach
    fun setup() {
        every { mockUuidManager.generateUuid() } returns "00000000-0000-0000-0000-000000000001"
    }

    @Test
    fun `parseForResult with valid JSON should return Success`() {
        val json = VALID_SINGLE_ENTRY_JSON

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
    fun `parseForResult with multiple entries should return all items`() {
        every { mockUuidManager.generateUuid() } returnsMany listOf(
            "00000000-0000-0000-0000-000000000001",
            "00000000-0000-0000-0000-000000000002",
            "00000000-0000-0000-0000-000000000003",
        )
        val json = VALID_MULTIPLE_ENTRIES_JSON

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
                    issuer = "Test Issuer 1",
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
                    issuer = "Test Issuer 2",
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
                    issuer = "Test Issuer 3",
                    accountName = "user3@example.com",
                    favorite = false,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult with TOTP type should succeed`() {
        val json = VALID_TOTP_TYPE_JSON

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
                    issuer = "TestIssuer",
                    accountName = "TestName",
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
                    issuer = "TestIssuer",
                    accountName = "TestName",
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
                    issuer = "TestIssuer",
                    accountName = "TestName",
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
                    issuer = "TestIssuer",
                    accountName = "TestName",
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
    fun `parseForResult with issuer should use issuer field`() {
        val json = VALID_WITH_ISSUER_JSON

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
                    accountName = "user@example.com",
                    favorite = false,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult with empty issuer should derive from name`() {
        val json = VALID_EMPTY_ISSUER_JSON

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
                    accountName = "user@example.com",
                    favorite = false,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult with name containing colon should split issuer and account`() {
        val json = VALID_NAME_WITH_COLON_JSON

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
                    issuer = "Microsoft",
                    accountName = "user@outlook.com",
                    favorite = false,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult with redundant accountName should set to null`() {
        val json = VALID_REDUNDANT_ACCOUNT_NAME_JSON

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
                    issuer = "SingleName",
                    accountName = null,
                    favorite = false,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult with favorite true should preserve flag`() {
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
                    issuer = "TestIssuer",
                    accountName = "TestName",
                    favorite = true,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult with favorite false should preserve flag`() {
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
                    issuer = "TestIssuer",
                    accountName = "TestName",
                    favorite = false,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult with custom period should use provided value`() {
        val json = VALID_CUSTOM_PERIOD_JSON

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
                    issuer = "TestIssuer",
                    accountName = "TestName",
                    favorite = false,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult with custom digits should use provided value`() {
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
                    issuer = "TestIssuer",
                    accountName = "TestName",
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
    fun `parseForResult with missing version field should return Error`() {
        val json = MISSING_VERSION_FIELD_JSON

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Error(
            title = BitwardenString.required_information_missing.asText(),
            message = BitwardenString.required_information_missing_message.asText(),
        )

        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult with empty entries should return empty list`() {
        val json = VALID_EMPTY_ENTRIES_JSON

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Success(items = emptyList())
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult with unsupported OTP type should return Error`() {
        val json = UNSUPPORTED_OTP_TYPE_JSON

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Error(
            message = "Unsupported OTP type".asText(),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult should generate unique UUID for each item`() {
        every { mockUuidManager.generateUuid() } returnsMany listOf(
            "00000000-0000-0000-0000-000000000001",
            "00000000-0000-0000-0000-000000000002",
            "00000000-0000-0000-0000-000000000003",
        )
        val json = VALID_MULTIPLE_ENTRIES_JSON

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
                    issuer = "Test Issuer 1",
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
                    issuer = "Test Issuer 2",
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
                    issuer = "Test Issuer 3",
                    accountName = "user3@example.com",
                    favorite = false,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }
}

private const val VALID_SINGLE_ENTRY_JSON = """
{
  "version": 1,
  "db": {
    "version": 2,
    "entries": [
      {
        "type": "totp",
        "uuid": "test-uuid-1",
        "name": "Test Issuer:test@example.com",
        "issuer": "Test Issuer",
        "note": "",
        "favorite": false,
        "info": {
          "secret": "JBSWY3DPEHPK3PXP",
          "algo": "SHA1",
          "digits": 6,
          "period": 30
        },
        "groups": []
      }
    ],
    "groups": []
  }
}
"""

private const val VALID_MULTIPLE_ENTRIES_JSON = """
{
  "version": 1,
  "db": {
    "version": 2,
    "entries": [
      {
        "type": "totp",
        "uuid": "uuid-1",
        "name": "Test Issuer 1:user1@example.com",
        "issuer": "Test Issuer 1",
        "note": "",
        "favorite": false,
        "info": {
          "secret": "SECRET1",
          "algo": "SHA1",
          "digits": 6,
          "period": 30
        },
        "groups": []
      },
      {
        "type": "totp",
        "uuid": "uuid-2",
        "name": "Test Issuer 2:user2@example.com",
        "issuer": "Test Issuer 2",
        "note": "",
        "favorite": true,
        "info": {
          "secret": "SECRET2",
          "algo": "SHA256",
          "digits": 6,
          "period": 30
        },
        "groups": []
      },
      {
        "type": "totp",
        "uuid": "uuid-3",
        "name": "Test Issuer 3:user3@example.com",
        "issuer": "Test Issuer 3",
        "note": "",
        "favorite": false,
        "info": {
          "secret": "SECRET3",
          "algo": "SHA512",
          "digits": 8,
          "period": 60
        },
        "groups": []
      }
    ],
    "groups": []
  }
}
"""

private const val VALID_TOTP_TYPE_JSON = """
{
  "version": 1,
  "db": {
    "version": 2,
    "entries": [
      {
        "type": "totp",
        "uuid": "test-uuid",
        "name": "TestName",
        "issuer": "TestIssuer",
        "note": "",
        "favorite": false,
        "info": {
          "secret": "TESTSECRET",
          "algo": "SHA1",
          "digits": 6,
          "period": 30
        },
        "groups": []
      }
    ],
    "groups": []
  }
}
"""

private const val VALID_SHA1_ALGORITHM_JSON = """
{
  "version": 1,
  "db": {
    "version": 2,
    "entries": [
      {
        "type": "totp",
        "uuid": "test-uuid",
        "name": "TestName",
        "issuer": "TestIssuer",
        "note": "",
        "favorite": false,
        "info": {
          "secret": "TESTSECRET",
          "algo": "SHA1",
          "digits": 6,
          "period": 30
        },
        "groups": []
      }
    ],
    "groups": []
  }
}
"""

private const val VALID_SHA256_ALGORITHM_JSON = """
{
  "version": 1,
  "db": {
    "version": 2,
    "entries": [
      {
        "type": "totp",
        "uuid": "test-uuid",
        "name": "TestName",
        "issuer": "TestIssuer",
        "note": "",
        "favorite": false,
        "info": {
          "secret": "TESTSECRET",
          "algo": "SHA256",
          "digits": 6,
          "period": 30
        },
        "groups": []
      }
    ],
    "groups": []
  }
}
"""

private const val VALID_SHA512_ALGORITHM_JSON = """
{
  "version": 1,
  "db": {
    "version": 2,
    "entries": [
      {
        "type": "totp",
        "uuid": "test-uuid",
        "name": "TestName",
        "issuer": "TestIssuer",
        "note": "",
        "favorite": false,
        "info": {
          "secret": "TESTSECRET",
          "algo": "SHA512",
          "digits": 6,
          "period": 30
        },
        "groups": []
      }
    ],
    "groups": []
  }
}
"""

private const val INVALID_ALGORITHM_JSON = """
{
  "version": 1,
  "db": {
    "version": 2,
    "entries": [
      {
        "type": "totp",
        "uuid": "test-uuid",
        "name": "TestName",
        "issuer": "TestIssuer",
        "note": "",
        "favorite": false,
        "info": {
          "secret": "TESTSECRET",
          "algo": "MD5",
          "digits": 6,
          "period": 30
        },
        "groups": []
      }
    ],
    "groups": []
  }
}
"""

private const val VALID_WITH_ISSUER_JSON = """
{
  "version": 1,
  "db": {
    "version": 2,
    "entries": [
      {
        "type": "totp",
        "uuid": "test-uuid",
        "name": "GitHub:user@example.com",
        "issuer": "GitHub",
        "note": "",
        "favorite": false,
        "info": {
          "secret": "TESTSECRET",
          "algo": "SHA1",
          "digits": 6,
          "period": 30
        },
        "groups": []
      }
    ],
    "groups": []
  }
}
"""

private const val VALID_EMPTY_ISSUER_JSON = """
{
  "version": 1,
  "db": {
    "version": 2,
    "entries": [
      {
        "type": "totp",
        "uuid": "test-uuid",
        "name": "GitHub:user@example.com",
        "issuer": "",
        "note": "",
        "favorite": false,
        "info": {
          "secret": "TESTSECRET",
          "algo": "SHA1",
          "digits": 6,
          "period": 30
        },
        "groups": []
      }
    ],
    "groups": []
  }
}
"""

private const val VALID_NAME_WITH_COLON_JSON = """
{
  "version": 1,
  "db": {
    "version": 2,
    "entries": [
      {
        "type": "totp",
        "uuid": "test-uuid",
        "name": "Microsoft:user@outlook.com",
        "issuer": "Microsoft",
        "note": "",
        "favorite": false,
        "info": {
          "secret": "TESTSECRET",
          "algo": "SHA1",
          "digits": 6,
          "period": 30
        },
        "groups": []
      }
    ],
    "groups": []
  }
}
"""

private const val VALID_REDUNDANT_ACCOUNT_NAME_JSON = """
{
  "version": 1,
  "db": {
    "version": 2,
    "entries": [
      {
        "type": "totp",
        "uuid": "test-uuid",
        "name": "SingleName",
        "issuer": "",
        "note": "",
        "favorite": false,
        "info": {
          "secret": "TESTSECRET",
          "algo": "SHA1",
          "digits": 6,
          "period": 30
        },
        "groups": []
      }
    ],
    "groups": []
  }
}
"""

private const val VALID_FAVORITE_TRUE_JSON = """
{
  "version": 1,
  "db": {
    "version": 2,
    "entries": [
      {
        "type": "totp",
        "uuid": "test-uuid",
        "name": "TestName",
        "issuer": "TestIssuer",
        "note": "",
        "favorite": true,
        "info": {
          "secret": "TESTSECRET",
          "algo": "SHA1",
          "digits": 6,
          "period": 30
        },
        "groups": []
      }
    ],
    "groups": []
  }
}
"""

private const val VALID_FAVORITE_FALSE_JSON = """
{
  "version": 1,
  "db": {
    "version": 2,
    "entries": [
      {
        "type": "totp",
        "uuid": "test-uuid",
        "name": "TestName",
        "issuer": "TestIssuer",
        "note": "",
        "favorite": false,
        "info": {
          "secret": "TESTSECRET",
          "algo": "SHA1",
          "digits": 6,
          "period": 30
        },
        "groups": []
      }
    ],
    "groups": []
  }
}
"""

private const val VALID_CUSTOM_PERIOD_JSON = """
{
  "version": 1,
  "db": {
    "version": 2,
    "entries": [
      {
        "type": "totp",
        "uuid": "test-uuid",
        "name": "TestName",
        "issuer": "TestIssuer",
        "note": "",
        "favorite": false,
        "info": {
          "secret": "TESTSECRET",
          "algo": "SHA1",
          "digits": 6,
          "period": 60
        },
        "groups": []
      }
    ],
    "groups": []
  }
}
"""

private const val VALID_CUSTOM_DIGITS_JSON = """
{
  "version": 1,
  "db": {
    "version": 2,
    "entries": [
      {
        "type": "totp",
        "uuid": "test-uuid",
        "name": "TestName",
        "issuer": "TestIssuer",
        "note": "",
        "favorite": false,
        "info": {
          "secret": "TESTSECRET",
          "algo": "SHA1",
          "digits": 8,
          "period": 30
        },
        "groups": []
      }
    ],
    "groups": []
  }
}
"""

private const val MALFORMED_JSON = """
{
  "version": 1,
  "db": {
    "version": 2
    "entries": [
"""

private const val MISSING_VERSION_FIELD_JSON = """
{
  "db": {
    "version": 2,
    "entries": [],
    "groups": []
  }
}
"""

private const val VALID_EMPTY_ENTRIES_JSON = """
{
  "version": 1,
  "db": {
    "version": 2,
    "entries": [],
    "groups": []
  }
}
"""

private const val UNSUPPORTED_OTP_TYPE_JSON = """
{
  "version": 1,
  "db": {
    "version": 2,
    "entries": [
      {
        "type": "hotp",
        "uuid": "test-uuid",
        "name": "TestName",
        "issuer": "TestIssuer",
        "note": "",
        "favorite": false,
        "info": {
          "secret": "TESTSECRET",
          "algo": "SHA1",
          "digits": 6,
          "period": 30
        },
        "groups": []
      }
    ],
    "groups": []
  }
}
"""
