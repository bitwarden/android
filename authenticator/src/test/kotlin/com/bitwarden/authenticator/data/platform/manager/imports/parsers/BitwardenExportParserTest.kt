package com.bitwarden.authenticator.data.platform.manager.imports.parsers

import android.net.Uri
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemAlgorithm
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemType
import com.bitwarden.authenticator.data.platform.manager.imports.model.ExportParseResult
import com.bitwarden.authenticator.data.platform.manager.imports.model.ImportFileFormat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BitwardenExportParserTest {

    @BeforeEach
    fun setup() {
        mockkStatic(Uri::class)
        mockkStatic("androidx.core.net.UriKt")
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Uri::class)
        unmockkStatic("androidx.core.net.UriKt")
    }

    @Test
    fun `parseForResult with unsupported format should return Error`() {
        val parser = BitwardenExportParser(ImportFileFormat.AEGIS)
        val json = VALID_BITWARDEN_JSON

        val result = parser.parseForResult(json.toByteArray())

        assertTrue(result is ExportParseResult.Error)
        val error = result as ExportParseResult.Error
        assertNotNull(error.title)
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

        assertTrue(result is ExportParseResult.Success)
        val items = (result as ExportParseResult.Success).items
        assertEquals(1, items.size)
    }

    @Test
    fun `parseForResult should filter items without totp`() {
        val parser = BitwardenExportParser(ImportFileFormat.BITWARDEN_JSON)
        val json = ITEMS_WITHOUT_TOTP_JSON

        val result = parser.parseForResult(json.toByteArray())

        assertTrue(result is ExportParseResult.Success)
        val items = (result as ExportParseResult.Success).items
        assertTrue(items.isEmpty())
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

        assertTrue(result is ExportParseResult.Success)
        val item = (result as ExportParseResult.Success).items.first()
        assertEquals(AuthenticatorItemType.TOTP, item.type)
        assertEquals("JBSWY3DPEHPK3PXP", item.key)
        assertEquals(AuthenticatorItemAlgorithm.SHA1, item.algorithm)
        assertEquals(30, item.period)
        assertEquals(6, item.digits)
        assertEquals("GitHub", item.issuer)
        assertEquals("user", item.accountName)
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

        assertTrue(result is ExportParseResult.Success)
        val item = (result as ExportParseResult.Success).items.first()
        assertEquals(AuthenticatorItemType.STEAM, item.type)
        assertEquals("STEAMSECRETKEY", item.key)
        assertEquals(5, item.digits)
        assertNull(item.accountName)
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

        assertTrue(result is ExportParseResult.Success)
        val item = (result as ExportParseResult.Success).items.first()
        assertEquals(AuthenticatorItemType.TOTP, item.type)
        assertEquals("PLAINSECRET", item.key)
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

        assertTrue(result is ExportParseResult.Error)
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

        assertTrue(result is ExportParseResult.Success)
        val item = (result as ExportParseResult.Success).items.first()
        assertEquals("EXTRACTEDSECRET", item.key)
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

        assertTrue(result is ExportParseResult.Success)
        val item = (result as ExportParseResult.Success).items.first()
        assertEquals(AuthenticatorItemAlgorithm.SHA256, item.algorithm)
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

        assertTrue(result is ExportParseResult.Success)
        val item = (result as ExportParseResult.Success).items.first()
        assertEquals(60, item.period)
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

        assertTrue(result is ExportParseResult.Success)
        val item = (result as ExportParseResult.Success).items.first()
        assertEquals(8, item.digits)
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

        assertTrue(result is ExportParseResult.Success)
        val item = (result as ExportParseResult.Success).items.first()
        assertEquals("Microsoft", item.issuer)
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

        assertTrue(result is ExportParseResult.Success)
        val item = (result as ExportParseResult.Success).items.first()
        assertEquals(AuthenticatorItemAlgorithm.SHA1, item.algorithm)
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

        assertTrue(result is ExportParseResult.Success)
        val item = (result as ExportParseResult.Success).items.first()
        assertEquals(30, item.period)
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

        assertTrue(result is ExportParseResult.Success)
        val item = (result as ExportParseResult.Success).items.first()
        assertEquals(6, item.digits)
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

        assertTrue(result is ExportParseResult.Success)
        val item = (result as ExportParseResult.Success).items.first()
        assertEquals(5, item.digits)
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

        assertTrue(result is ExportParseResult.Success)
        val item = (result as ExportParseResult.Success).items.first()
        assertEquals("username", item.accountName)
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

        assertTrue(result is ExportParseResult.Success)
        val item = (result as ExportParseResult.Success).items.first()
        assertNull(item.accountName)
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

        assertTrue(result is ExportParseResult.Success)
        val item = (result as ExportParseResult.Success).items.first()
        assertEquals("test-item-id-123", item.id)
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

        assertTrue(result is ExportParseResult.Success)
        val item = (result as ExportParseResult.Success).items.first()
        assertTrue(item.favorite)
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

        assertTrue(result is ExportParseResult.Success)
        val item = (result as ExportParseResult.Success).items.first()
        assertFalse(item.favorite)
    }

    @Test
    fun `parseForResult with malformed JSON should return Error`() {
        val parser = BitwardenExportParser(ImportFileFormat.BITWARDEN_JSON)
        val json = MALFORMED_JSON

        val result = parser.parseForResult(json.toByteArray())

        assertTrue(result is ExportParseResult.Error)
    }

    @Test
    fun `parseForResult with missing encrypted field should return Error`() {
        val parser = BitwardenExportParser(ImportFileFormat.BITWARDEN_JSON)
        val json = MISSING_ENCRYPTED_FIELD_JSON

        val result = parser.parseForResult(json.toByteArray())

        assertTrue(result is ExportParseResult.Error)
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

        assertTrue(result is ExportParseResult.Success)
        val item = (result as ExportParseResult.Success).items.first()
        assertEquals("TestName", item.issuer)
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

    companion object {
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
    }
}
