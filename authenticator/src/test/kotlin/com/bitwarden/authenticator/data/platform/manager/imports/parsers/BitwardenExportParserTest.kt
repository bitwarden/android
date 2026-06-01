package com.bitwarden.authenticator.data.platform.manager.imports.parsers

import android.net.Uri
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemAlgorithm
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemEntity
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemType
import com.bitwarden.authenticator.data.platform.manager.imports.model.ExportParseResult
import com.bitwarden.authenticator.data.platform.manager.imports.model.ImportFileFormat
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@Suppress("LargeClass")
class BitwardenExportParserTest {

    @BeforeEach
    fun setup() {
        mockkStatic(Uri::class)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Uri::class)
    }

    @Test
    fun `parseForResult with unsupported format should return Error`() {
        val parser = BitwardenExportParser(ImportFileFormat.AEGIS)
        val json = VALID_BITWARDEN_JSON

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Error(
            title = BitwardenString.import_bitwarden_unsupported_format.asText(),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult with BITWARDEN_JSON format should succeed`() {
        val parser = BitwardenExportParser(ImportFileFormat.BITWARDEN_JSON)
        val json = VALID_BITWARDEN_JSON
        mockOtpauthUri(
            uriString = "otpauth://totp/GitHub:user?" +
                "secret=JBSWY3DPEHPK3PXP&algorithm=SHA1&digits=6&period=30&issuer=GitHub",
            scheme = "otpauth",
            authority = "totp",
            secretParam = "JBSWY3DPEHPK3PXP",
            algorithmParam = "SHA1",
            digitsParam = "6",
            periodParam = "30",
            issuerParam = "GitHub",
            pathSegments = listOf("GitHub:user"),
        )

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Success(
            items = listOf(
                AuthenticatorItemEntity(
                    id = "test-item-id-123",
                    key = "JBSWY3DPEHPK3PXP",
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
    fun `parseForResult should filter items without totp`() {
        val parser = BitwardenExportParser(ImportFileFormat.BITWARDEN_JSON)
        val json = ITEMS_WITHOUT_TOTP_JSON

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Success(items = emptyList())
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult with otpauth TOTP URI should parse correctly`() {
        val parser = BitwardenExportParser(ImportFileFormat.BITWARDEN_JSON)
        val json = OTPAUTH_TOTP_URI_JSON
        mockOtpauthUri(
            uriString = "otpauth://totp/GitHub:user?" +
                "secret=JBSWY3DPEHPK3PXP&algorithm=SHA1&digits=6&period=30&issuer=GitHub",
            scheme = "otpauth",
            authority = "totp",
            secretParam = "JBSWY3DPEHPK3PXP",
            algorithmParam = "SHA1",
            digitsParam = "6",
            periodParam = "30",
            issuerParam = "GitHub",
            pathSegments = listOf("GitHub:user"),
        )

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Success(
            items = listOf(
                AuthenticatorItemEntity(
                    id = "test-id",
                    key = "JBSWY3DPEHPK3PXP",
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
    fun `parseForResult with steam URI should parse correctly`() {
        val parser = BitwardenExportParser(ImportFileFormat.BITWARDEN_JSON)
        val json = STEAM_URI_JSON
        mockSteamUri(
            uriString = "steam://STEAMSECRETKEY",
            scheme = "steam",
            authority = "STEAMSECRETKEY",
        )

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Success(
            items = listOf(
                AuthenticatorItemEntity(
                    id = "steam-id",
                    key = "STEAMSECRETKEY",
                    type = AuthenticatorItemType.STEAM,
                    algorithm = AuthenticatorItemAlgorithm.SHA1,
                    period = 30,
                    digits = 5,
                    issuer = "Steam",
                    accountName = null,
                    favorite = false,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult with plain secret should wrap in URI format`() {
        val parser = BitwardenExportParser(ImportFileFormat.BITWARDEN_JSON)
        val json = PLAIN_SECRET_JSON
        mockOtpauthUri(
            uriString = "otpauth://totp/TestName?secret=PLAINSECRET",
            scheme = "otpauth",
            authority = "totp",
            secretParam = "PLAINSECRET",
            algorithmParam = null,
            digitsParam = null,
            periodParam = null,
            issuerParam = null,
            pathSegments = listOf("TestName"),
        )

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Success(
            items = listOf(
                AuthenticatorItemEntity(
                    id = "plain-id",
                    key = "PLAINSECRET",
                    type = AuthenticatorItemType.TOTP,
                    algorithm = AuthenticatorItemAlgorithm.SHA1,
                    period = 30,
                    digits = 6,
                    issuer = "TestName",
                    accountName = "TestName",
                    favorite = false,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult with unsupported URI scheme should return Error`() {
        val parser = BitwardenExportParser(ImportFileFormat.BITWARDEN_JSON)
        val json = OTPAUTH_TOTP_URI_JSON
        val mockUri = mockk<Uri>()
        every {
            Uri.parse(
                "otpauth://totp/GitHub:user?" +
                    "secret=JBSWY3DPEHPK3PXP&algorithm=SHA1&digits=6&period=30&issuer=GitHub",
            )
        } returns mockUri
        every { mockUri.scheme } returns "otpauth"
        every { mockUri.authority } returns "hotp"

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Error(
            message = "Unsupported OTP type.".asText(),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult should extract secret query parameter`() {
        val parser = BitwardenExportParser(ImportFileFormat.BITWARDEN_JSON)
        val json = SECRET_EXTRACTEDSECRET_JSON
        mockOtpauthUri(
            uriString = "otpauth://totp/TestName?secret=EXTRACTEDSECRET",
            scheme = "otpauth",
            authority = "totp",
            secretParam = "EXTRACTEDSECRET",
            algorithmParam = null,
            digitsParam = null,
            periodParam = null,
            issuerParam = null,
            pathSegments = listOf("TestName"),
        )

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Success(
            items = listOf(
                AuthenticatorItemEntity(
                    id = "test-id",
                    key = "EXTRACTEDSECRET",
                    type = AuthenticatorItemType.TOTP,
                    algorithm = AuthenticatorItemAlgorithm.SHA1,
                    period = 30,
                    digits = 6,
                    issuer = "TestName",
                    accountName = "TestName",
                    favorite = false,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult should extract algorithm query parameter`() {
        val parser = BitwardenExportParser(ImportFileFormat.BITWARDEN_JSON)
        val json = ALGORITHM_SHA256_JSON
        mockOtpauthUri(
            uriString = "otpauth://totp/TestName?secret=SECRET&algorithm=SHA256",
            scheme = "otpauth",
            authority = "totp",
            secretParam = "SECRET",
            algorithmParam = "SHA256",
            digitsParam = null,
            periodParam = null,
            issuerParam = null,
            pathSegments = listOf("TestName"),
        )

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Success(
            items = listOf(
                AuthenticatorItemEntity(
                    id = "test-id",
                    key = "SECRET",
                    type = AuthenticatorItemType.TOTP,
                    algorithm = AuthenticatorItemAlgorithm.SHA256,
                    period = 30,
                    digits = 6,
                    issuer = "TestName",
                    accountName = "TestName",
                    favorite = false,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult should extract period query parameter`() {
        val parser = BitwardenExportParser(ImportFileFormat.BITWARDEN_JSON)
        val json = PERIOD_60_JSON
        mockOtpauthUri(
            uriString = "otpauth://totp/TestName?secret=SECRET&period=60",
            scheme = "otpauth",
            authority = "totp",
            secretParam = "SECRET",
            algorithmParam = null,
            digitsParam = null,
            periodParam = "60",
            issuerParam = null,
            pathSegments = listOf("TestName"),
        )

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Success(
            items = listOf(
                AuthenticatorItemEntity(
                    id = "test-id",
                    key = "SECRET",
                    type = AuthenticatorItemType.TOTP,
                    algorithm = AuthenticatorItemAlgorithm.SHA1,
                    period = 60,
                    digits = 6,
                    issuer = "TestName",
                    accountName = "TestName",
                    favorite = false,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult should extract digits query parameter`() {
        val parser = BitwardenExportParser(ImportFileFormat.BITWARDEN_JSON)
        val json = DIGITS_8_JSON
        mockOtpauthUri(
            uriString = "otpauth://totp/TestName?secret=SECRET&digits=8",
            scheme = "otpauth",
            authority = "totp",
            secretParam = "SECRET",
            algorithmParam = null,
            digitsParam = "8",
            periodParam = null,
            issuerParam = null,
            pathSegments = listOf("TestName"),
        )

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Success(
            items = listOf(
                AuthenticatorItemEntity(
                    id = "test-id",
                    key = "SECRET",
                    type = AuthenticatorItemType.TOTP,
                    algorithm = AuthenticatorItemAlgorithm.SHA1,
                    period = 30,
                    digits = 8,
                    issuer = "TestName",
                    accountName = "TestName",
                    favorite = false,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult should extract issuer query parameter`() {
        val parser = BitwardenExportParser(ImportFileFormat.BITWARDEN_JSON)
        val json = ISSUER_MICROSOFT_JSON
        mockOtpauthUri(
            uriString = "otpauth://totp/TestName?secret=SECRET&issuer=Microsoft",
            scheme = "otpauth",
            authority = "totp",
            secretParam = "SECRET",
            algorithmParam = null,
            digitsParam = null,
            periodParam = null,
            issuerParam = "Microsoft",
            pathSegments = listOf("TestName"),
        )

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Success(
            items = listOf(
                AuthenticatorItemEntity(
                    id = "test-id",
                    key = "SECRET",
                    type = AuthenticatorItemType.TOTP,
                    algorithm = AuthenticatorItemAlgorithm.SHA1,
                    period = 30,
                    digits = 6,
                    issuer = "Microsoft",
                    accountName = "TestName",
                    favorite = false,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult should default to SHA1 algorithm when not specified`() {
        val parser = BitwardenExportParser(ImportFileFormat.BITWARDEN_JSON)
        val json = NO_ALGORITHM_PARAM_JSON
        mockOtpauthUri(
            uriString = "otpauth://totp/TestName?secret=SECRET",
            scheme = "otpauth",
            authority = "totp",
            secretParam = "SECRET",
            algorithmParam = null,
            digitsParam = null,
            periodParam = null,
            issuerParam = null,
            pathSegments = listOf("TestName"),
        )

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Success(
            items = listOf(
                AuthenticatorItemEntity(
                    id = "test-id",
                    key = "SECRET",
                    type = AuthenticatorItemType.TOTP,
                    algorithm = AuthenticatorItemAlgorithm.SHA1,
                    period = 30,
                    digits = 6,
                    issuer = "TestName",
                    accountName = "TestName",
                    favorite = false,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult should default to 30 period when not specified for TOTP`() {
        val parser = BitwardenExportParser(ImportFileFormat.BITWARDEN_JSON)
        val json = NO_PERIOD_PARAM_JSON
        mockOtpauthUri(
            uriString = "otpauth://totp/TestName?secret=SECRET",
            scheme = "otpauth",
            authority = "totp",
            secretParam = "SECRET",
            algorithmParam = null,
            digitsParam = null,
            periodParam = null,
            issuerParam = null,
            pathSegments = listOf("TestName"),
        )

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Success(
            items = listOf(
                AuthenticatorItemEntity(
                    id = "test-id",
                    key = "SECRET",
                    type = AuthenticatorItemType.TOTP,
                    algorithm = AuthenticatorItemAlgorithm.SHA1,
                    period = 30,
                    digits = 6,
                    issuer = "TestName",
                    accountName = "TestName",
                    favorite = false,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult should default to 6 digits when not specified for TOTP`() {
        val parser = BitwardenExportParser(ImportFileFormat.BITWARDEN_JSON)
        val json = NO_DIGITS_PARAM_JSON
        mockOtpauthUri(
            uriString = "otpauth://totp/TestName?secret=SECRET",
            scheme = "otpauth",
            authority = "totp",
            secretParam = "SECRET",
            algorithmParam = null,
            digitsParam = null,
            periodParam = null,
            issuerParam = null,
            pathSegments = listOf("TestName"),
        )

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Success(
            items = listOf(
                AuthenticatorItemEntity(
                    id = "test-id",
                    key = "SECRET",
                    type = AuthenticatorItemType.TOTP,
                    algorithm = AuthenticatorItemAlgorithm.SHA1,
                    period = 30,
                    digits = 6,
                    issuer = "TestName",
                    accountName = "TestName",
                    favorite = false,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult should default to 5 digits for STEAM`() {
        val parser = BitwardenExportParser(ImportFileFormat.BITWARDEN_JSON)
        val json = STEAM_URI_JSON
        mockSteamUri(
            uriString = "steam://STEAMSECRETKEY",
            scheme = "steam",
            authority = "STEAMSECRETKEY",
        )

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Success(
            items = listOf(
                AuthenticatorItemEntity(
                    id = "steam-id",
                    key = "STEAMSECRETKEY",
                    type = AuthenticatorItemType.STEAM,
                    algorithm = AuthenticatorItemAlgorithm.SHA1,
                    period = 30,
                    digits = 5,
                    issuer = "Steam",
                    accountName = null,
                    favorite = false,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult should extract label from path segments`() {
        val parser = BitwardenExportParser(ImportFileFormat.BITWARDEN_JSON)
        val json = LABEL_IN_PATH_JSON
        mockOtpauthUri(
            uriString = "otpauth://totp/GitHub:username?secret=SECRET&issuer=GitHub",
            scheme = "otpauth",
            authority = "totp",
            secretParam = "SECRET",
            algorithmParam = null,
            digitsParam = null,
            periodParam = null,
            issuerParam = "GitHub",
            pathSegments = listOf("GitHub:username"),
        )

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Success(
            items = listOf(
                AuthenticatorItemEntity(
                    id = "test-id",
                    key = "SECRET",
                    type = AuthenticatorItemType.TOTP,
                    algorithm = AuthenticatorItemAlgorithm.SHA1,
                    period = 30,
                    digits = 6,
                    issuer = "GitHub",
                    accountName = "username",
                    favorite = false,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult should set STEAM label to null`() {
        val parser = BitwardenExportParser(ImportFileFormat.BITWARDEN_JSON)
        val json = STEAM_URI_JSON
        mockSteamUri(
            uriString = "steam://STEAMSECRETKEY",
            scheme = "steam",
            authority = "STEAMSECRETKEY",
        )

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Success(
            items = listOf(
                AuthenticatorItemEntity(
                    id = "steam-id",
                    key = "STEAMSECRETKEY",
                    type = AuthenticatorItemType.STEAM,
                    algorithm = AuthenticatorItemAlgorithm.SHA1,
                    period = 30,
                    digits = 5,
                    issuer = "Steam",
                    accountName = null,
                    favorite = false,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult should preserve item ID from export`() {
        val parser = BitwardenExportParser(ImportFileFormat.BITWARDEN_JSON)
        val json = VALID_BITWARDEN_JSON
        mockOtpauthUri(
            uriString = "otpauth://totp/GitHub:user?" +
                "secret=JBSWY3DPEHPK3PXP&algorithm=SHA1&digits=6&period=30&issuer=GitHub",
            scheme = "otpauth",
            authority = "totp",
            secretParam = "JBSWY3DPEHPK3PXP",
            algorithmParam = "SHA1",
            digitsParam = "6",
            periodParam = "30",
            issuerParam = "GitHub",
            pathSegments = listOf("GitHub:user"),
        )

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Success(
            items = listOf(
                AuthenticatorItemEntity(
                    id = "test-item-id-123",
                    key = "JBSWY3DPEHPK3PXP",
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
    fun `parseForResult should preserve favorite flag`() {
        val parser = BitwardenExportParser(ImportFileFormat.BITWARDEN_JSON)
        val json = FAVORITE_TRUE_JSON
        mockOtpauthUri(
            uriString = "otpauth://totp/Test?secret=SECRET",
            scheme = "otpauth",
            authority = "totp",
            secretParam = "SECRET",
            algorithmParam = null,
            digitsParam = null,
            periodParam = null,
            issuerParam = null,
            pathSegments = listOf("Test"),
        )

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Success(
            items = listOf(
                AuthenticatorItemEntity(
                    id = "fav-id",
                    key = "SECRET",
                    type = AuthenticatorItemType.TOTP,
                    algorithm = AuthenticatorItemAlgorithm.SHA1,
                    period = 30,
                    digits = 6,
                    issuer = "Test",
                    accountName = "Test",
                    favorite = true,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult with favorite false should preserve flag`() {
        val parser = BitwardenExportParser(ImportFileFormat.BITWARDEN_JSON)
        val json = FAVORITE_FALSE_JSON
        mockOtpauthUri(
            uriString = "otpauth://totp/Test?secret=SECRET",
            scheme = "otpauth",
            authority = "totp",
            secretParam = "SECRET",
            algorithmParam = null,
            digitsParam = null,
            periodParam = null,
            issuerParam = null,
            pathSegments = listOf("Test"),
        )

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Success(
            items = listOf(
                AuthenticatorItemEntity(
                    id = "not-fav-id",
                    key = "SECRET",
                    type = AuthenticatorItemType.TOTP,
                    algorithm = AuthenticatorItemAlgorithm.SHA1,
                    period = 30,
                    digits = 6,
                    issuer = "Test",
                    accountName = "Test",
                    favorite = false,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult with malformed JSON should return Error`() {
        val parser = BitwardenExportParser(ImportFileFormat.BITWARDEN_JSON)
        val json = MALFORMED_JSON

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Error(
            title = BitwardenString.file_could_not_be_processed.asText(),
            message = BitwardenString.file_could_not_be_processed_message.asText(),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult with missing encrypted field should return Error`() {
        val parser = BitwardenExportParser(ImportFileFormat.BITWARDEN_JSON)
        val json = MISSING_ENCRYPTED_FIELD_JSON

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Error(
            title = BitwardenString.required_information_missing.asText(),
            message = BitwardenString.required_information_missing_message.asText(),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `parseForResult should use item name as issuer when issuer param missing`() {
        val parser = BitwardenExportParser(ImportFileFormat.BITWARDEN_JSON)
        val json = NO_ISSUER_PARAM_JSON
        mockOtpauthUri(
            uriString = "otpauth://totp/TestName?secret=SECRET",
            scheme = "otpauth",
            authority = "totp",
            secretParam = "SECRET",
            algorithmParam = null,
            digitsParam = null,
            periodParam = null,
            issuerParam = null,
            pathSegments = listOf("TestName"),
        )

        val result = parser.parseForResult(json.toByteArray())

        val expected = ExportParseResult.Success(
            items = listOf(
                AuthenticatorItemEntity(
                    id = "test-id",
                    key = "SECRET",
                    type = AuthenticatorItemType.TOTP,
                    algorithm = AuthenticatorItemAlgorithm.SHA1,
                    period = 30,
                    digits = 6,
                    issuer = "TestName",
                    accountName = "TestName",
                    favorite = false,
                    userId = null,
                ),
            ),
        )
        assertEquals(expected, result)
    }

    @Suppress("LongParameterList")
    private fun mockOtpauthUri(
        uriString: String,
        scheme: String,
        authority: String,
        secretParam: String?,
        algorithmParam: String?,
        digitsParam: String?,
        periodParam: String?,
        issuerParam: String?,
        pathSegments: List<String>,
    ) {
        val mockUri = mockk<Uri>()
        every { Uri.parse(uriString) } returns mockUri
        every { mockUri.scheme } returns scheme
        every { mockUri.authority } returns authority
        every { mockUri.getQueryParameter("secret") } returns secretParam
        every { mockUri.getQueryParameter("algorithm") } returns algorithmParam
        every { mockUri.getQueryParameter("digits") } returns digitsParam
        every { mockUri.getQueryParameter("period") } returns periodParam
        every { mockUri.getQueryParameter("issuer") } returns issuerParam
        every { mockUri.pathSegments } returns pathSegments
    }

    private fun mockSteamUri(
        uriString: String,
        scheme: String,
        authority: String?,
    ) {
        val mockUri = mockk<Uri>()
        every { Uri.parse(uriString) } returns mockUri
        every { mockUri.scheme } returns scheme
        every { mockUri.authority } returns authority
        // STEAM URIs don't have query parameters, return null for all
        every { mockUri.getQueryParameter(any()) } returns null
        every { mockUri.pathSegments } returns emptyList()
    }
}

private const val VALID_BITWARDEN_JSON = """
{
  "encrypted": false,
  "items": [
    {
      "id": "test-item-id-123",
      "name": "GitHub",
      "folderId": null,
      "organizationId": null,
      "collectionIds": null,
      "notes": null,
      "type": 1,
      "login": {
        "totp": "otpauth://totp/GitHub:user?secret=JBSWY3DPEHPK3PXP&algorithm=SHA1&digits=6&period=30&issuer=GitHub"
      },
      "favorite": false
    }
  ]
}
"""

private const val ITEMS_WITHOUT_TOTP_JSON = """
{
  "encrypted": false,
  "items": [
    {
      "id": "item-1",
      "name": "No TOTP Item",
      "folderId": null,
      "organizationId": null,
      "collectionIds": null,
      "notes": null,
      "type": 1,
      "login": {
        "totp": null
      },
      "favorite": false
    },
    {
      "id": "item-2",
      "name": "Another Item",
      "folderId": null,
      "organizationId": null,
      "collectionIds": null,
      "notes": null,
      "type": 1,
      "login": null,
      "favorite": false
    }
  ]
}
"""

private const val OTPAUTH_TOTP_URI_JSON = """
{
  "encrypted": false,
  "items": [
    {
      "id": "test-id",
      "name": "TestName",
      "folderId": null,
      "organizationId": null,
      "collectionIds": null,
      "notes": null,
      "type": 1,
      "login": {
        "totp": "otpauth://totp/GitHub:user?secret=JBSWY3DPEHPK3PXP&algorithm=SHA1&digits=6&period=30&issuer=GitHub"
      },
      "favorite": false
    }
  ]
}
"""

private const val STEAM_URI_JSON = """
{
  "encrypted": false,
  "items": [
    {
      "id": "steam-id",
      "name": "Steam",
      "folderId": null,
      "organizationId": null,
      "collectionIds": null,
      "notes": null,
      "type": 1,
      "login": {
        "totp": "steam://STEAMSECRETKEY"
      },
      "favorite": false
    }
  ]
}
"""

private const val PLAIN_SECRET_JSON = """
{
  "encrypted": false,
  "items": [
    {
      "id": "plain-id",
      "name": "TestName",
      "folderId": null,
      "organizationId": null,
      "collectionIds": null,
      "notes": null,
      "type": 1,
      "login": {
        "totp": "PLAINSECRET"
      },
      "favorite": false
    }
  ]
}
"""

private const val FAVORITE_TRUE_JSON = """
{
  "encrypted": false,
  "items": [
    {
      "id": "fav-id",
      "name": "Test",
      "folderId": null,
      "organizationId": null,
      "collectionIds": null,
      "notes": null,
      "type": 1,
      "login": {
        "totp": "otpauth://totp/Test?secret=SECRET"
      },
      "favorite": true
    }
  ]
}
"""

private const val FAVORITE_FALSE_JSON = """
{
  "encrypted": false,
  "items": [
    {
      "id": "not-fav-id",
      "name": "Test",
      "folderId": null,
      "organizationId": null,
      "collectionIds": null,
      "notes": null,
      "type": 1,
      "login": {
        "totp": "otpauth://totp/Test?secret=SECRET"
      },
      "favorite": false
    }
  ]
}
"""

private const val MALFORMED_JSON = """
{
  "encrypted": false
  "items": [
"""

private const val MISSING_ENCRYPTED_FIELD_JSON = """
{
  "items": []
}
"""

private const val NO_ALGORITHM_PARAM_JSON = """
{
  "encrypted": false,
  "items": [
    {
      "id": "test-id",
      "name": "TestName",
      "folderId": null,
      "organizationId": null,
      "collectionIds": null,
      "notes": null,
      "type": 1,
      "login": {
        "totp": "otpauth://totp/TestName?secret=SECRET"
      },
      "favorite": false
    }
  ]
}
"""

private const val NO_PERIOD_PARAM_JSON = """
{
  "encrypted": false,
  "items": [
    {
      "id": "test-id",
      "name": "TestName",
      "folderId": null,
      "organizationId": null,
      "collectionIds": null,
      "notes": null,
      "type": 1,
      "login": {
        "totp": "otpauth://totp/TestName?secret=SECRET"
      },
      "favorite": false
    }
  ]
}
"""

private const val NO_DIGITS_PARAM_JSON = """
{
  "encrypted": false,
  "items": [
    {
      "id": "test-id",
      "name": "TestName",
      "folderId": null,
      "organizationId": null,
      "collectionIds": null,
      "notes": null,
      "type": 1,
      "login": {
        "totp": "otpauth://totp/TestName?secret=SECRET"
      },
      "favorite": false
    }
  ]
}
"""

private const val NO_ISSUER_PARAM_JSON = """
{
  "encrypted": false,
  "items": [
    {
      "id": "test-id",
      "name": "TestName",
      "folderId": null,
      "organizationId": null,
      "collectionIds": null,
      "notes": null,
      "type": 1,
      "login": {
        "totp": "otpauth://totp/TestName?secret=SECRET"
      },
      "favorite": false
    }
  ]
}
"""

private const val ALGORITHM_SHA256_JSON = """
{
  "encrypted": false,
  "items": [
    {
      "id": "test-id",
      "name": "TestName",
      "folderId": null,
      "organizationId": null,
      "collectionIds": null,
      "notes": null,
      "type": 1,
      "login": {
        "totp": "otpauth://totp/TestName?secret=SECRET&algorithm=SHA256"
      },
      "favorite": false
    }
  ]
}
"""

private const val PERIOD_60_JSON = """
{
  "encrypted": false,
  "items": [
    {
      "id": "test-id",
      "name": "TestName",
      "folderId": null,
      "organizationId": null,
      "collectionIds": null,
      "notes": null,
      "type": 1,
      "login": {
        "totp": "otpauth://totp/TestName?secret=SECRET&period=60"
      },
      "favorite": false
    }
  ]
}
"""

private const val DIGITS_8_JSON = """
{
  "encrypted": false,
  "items": [
    {
      "id": "test-id",
      "name": "TestName",
      "folderId": null,
      "organizationId": null,
      "collectionIds": null,
      "notes": null,
      "type": 1,
      "login": {
        "totp": "otpauth://totp/TestName?secret=SECRET&digits=8"
      },
      "favorite": false
    }
  ]
}
"""

private const val ISSUER_MICROSOFT_JSON = """
{
  "encrypted": false,
  "items": [
    {
      "id": "test-id",
      "name": "TestName",
      "folderId": null,
      "organizationId": null,
      "collectionIds": null,
      "notes": null,
      "type": 1,
      "login": {
        "totp": "otpauth://totp/TestName?secret=SECRET&issuer=Microsoft"
      },
      "favorite": false
    }
  ]
}
"""

private const val LABEL_IN_PATH_JSON = """
{
  "encrypted": false,
  "items": [
    {
      "id": "test-id",
      "name": "TestName",
      "folderId": null,
      "organizationId": null,
      "collectionIds": null,
      "notes": null,
      "type": 1,
      "login": {
        "totp": "otpauth://totp/GitHub:username?secret=SECRET&issuer=GitHub"
      },
      "favorite": false
    }
  ]
}
"""

private const val SECRET_EXTRACTEDSECRET_JSON = """
{
  "encrypted": false,
  "items": [
    {
      "id": "test-id",
      "name": "TestName",
      "folderId": null,
      "organizationId": null,
      "collectionIds": null,
      "notes": null,
      "type": 1,
      "login": {
        "totp": "otpauth://totp/TestName?secret=EXTRACTEDSECRET"
      },
      "favorite": false
    }
  ]
}
"""
