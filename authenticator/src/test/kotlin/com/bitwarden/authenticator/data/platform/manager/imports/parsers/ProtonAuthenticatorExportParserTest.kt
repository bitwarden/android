package com.bitwarden.authenticator.data.platform.manager.imports.parsers

import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemAlgorithm
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemEntity
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemType
import com.bitwarden.authenticator.data.platform.manager.BitwardenEncodingManagerImpl
import com.bitwarden.authenticator.data.platform.manager.imports.model.ExportParseResult
import com.bitwarden.core.data.manager.UuidManager
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ProtonAuthenticatorExportParserTest {

    private val mockUuidManager = mockk<UuidManager>()
    private val parser = ProtonAuthenticatorExportParser(
        uuidManager = mockUuidManager,
        encodingManager = BitwardenEncodingManagerImpl(),
    )

    @BeforeEach
    fun setup() {
        every { mockUuidManager.generateUuid() } returns TEST_ID
    }

    @Test
    fun `parseForResult with issuer should use issuer as title and label as account`() {
        val result = parser.parseForResult(
            exportJson(totpEntry(uri = "otpauth://totp/Account?secret=$SECRET&issuer=Issuer"))
                .toByteArray(),
        )

        assertEquals(
            ExportParseResult.Success(items = listOf(totpItem(accountName = "Account"))),
            result,
        )
    }

    @Test
    fun `parseForResult should decode the label and remove an issuer prefix`() {
        val result = parser.parseForResult(
            exportJson(
                totpEntry(
                    uri = "otpauth://totp/Issuer%3Ajohn.doe%40example.com" +
                        "?secret=$SECRET&issuer=Issuer",
                ),
            )
                .toByteArray(),
        )

        assertEquals(
            ExportParseResult.Success(
                items = listOf(totpItem(accountName = "john.doe@example.com")),
            ),
            result,
        )
    }

    @Test
    fun `parseForResult without issuer should use name as title and drop a duplicate account`() {
        val result = parser.parseForResult(
            exportJson(totpEntry(uri = "otpauth://totp/Account?secret=$SECRET", name = "Account"))
                .toByteArray(),
        )

        assertEquals(
            ExportParseResult.Success(
                items = listOf(totpItem(issuer = "Account", accountName = null)),
            ),
            result,
        )
    }

    @Test
    fun `parseForResult without issuer or name should use label as title`() {
        val result = parser.parseForResult(
            exportJson(totpEntry(uri = "otpauth://totp/Account?secret=$SECRET", name = null))
                .toByteArray(),
        )

        assertEquals(
            ExportParseResult.Success(
                items = listOf(totpItem(issuer = "Account", accountName = null)),
            ),
            result,
        )
    }

    @Test
    fun `parseForResult with empty label should fall back to name as account`() {
        val result = parser.parseForResult(
            exportJson(totpEntry(uri = "otpauth://totp/?secret=$SECRET&issuer=Issuer"))
                .toByteArray(),
        )

        assertEquals(
            ExportParseResult.Success(items = listOf(totpItem(accountName = "Name"))),
            result,
        )
    }

    @Test
    fun `parseForResult should read algorithm, digits and period`() {
        val result = parser.parseForResult(
            exportJson(
                totpEntry(
                    uri = "otpauth://totp/Account?secret=$SECRET&issuer=Issuer" +
                        "&algorithm=SHA256&digits=8&period=60",
                ),
            )
                .toByteArray(),
        )

        assertEquals(
            ExportParseResult.Success(
                items = listOf(
                    totpItem(
                        accountName = "Account",
                        algorithm = AuthenticatorItemAlgorithm.SHA256,
                        digits = 8,
                        period = 60,
                    ),
                ),
            ),
            result,
        )
    }

    @Test
    fun `parseForResult should uppercase the secret and remove spaces`() {
        val result = parser.parseForResult(
            exportJson(
                totpEntry(
                    uri = "otpauth://totp/Account?secret=jbsw%20y3dp%20ehpk3pxp&issuer=Issuer",
                ),
            )
                .toByteArray(),
        )

        assertEquals(
            ExportParseResult.Success(items = listOf(totpItem(accountName = "Account"))),
            result,
        )
    }

    @Test
    fun `parseForResult should read a URI in the format Proton Authenticator writes`() {
        val result = parser.parseForResult(
            exportJson(
                totpEntry(
                    uri = "otpauth://totp/john.doe%40example.com?secret=$SECRET" +
                        "&issuer=My+Company&algorithm=sha256&digits=8&period=60",
                ),
            )
                .toByteArray(),
        )

        assertEquals(
            ExportParseResult.Success(
                items = listOf(
                    totpItem(
                        issuer = "My Company",
                        accountName = "john.doe@example.com",
                        algorithm = AuthenticatorItemAlgorithm.SHA256,
                        digits = 8,
                        period = 60,
                    ),
                ),
            ),
            result,
        )
    }

    @Test
    fun `parseForResult should remove dashes, underscores and padding from the secret`() {
        val result = parser.parseForResult(
            exportJson(
                totpEntry(
                    uri = "otpauth://totp/Account?secret=jbsw-y3dp_ehpk3pxp%3D%3D&issuer=Issuer",
                ),
            )
                .toByteArray(),
        )

        assertEquals(
            ExportParseResult.Success(items = listOf(totpItem(accountName = "Account"))),
            result,
        )
    }

    @Test
    fun `parseForResult should base32 encode a non-base32 TOTP secret as raw bytes`() {
        val result = parser.parseForResult(
            exportJson(totpEntry(uri = "otpauth://totp/Account?secret=12345678&issuer=Issuer"))
                .toByteArray(),
        )

        assertEquals(
            ExportParseResult.Success(
                items = listOf(totpItem(accountName = "Account", key = "GEZDGNBVGY3TQ")),
            ),
            result,
        )
    }

    @Test
    fun `parseForResult should only uppercase ASCII letters before the raw bytes fallback`() {
        val result = parser.parseForResult(
            exportJson(totpEntry(uri = "otpauth://totp/Account?secret=%C3%A91&issuer=Issuer"))
                .toByteArray(),
        )

        assertEquals(
            ExportParseResult.Success(
                items = listOf(totpItem(accountName = "Account", key = "YOUTC")),
            ),
            result,
        )
    }

    @Test
    fun `parseForResult should import every entry when ids are duplicated`() {
        val entry = totpEntry(uri = "otpauth://totp/Account?secret=$SECRET&issuer=Issuer")
        val result = parser.parseForResult(exportJson(entry, entry).toByteArray())

        assertEquals(
            ExportParseResult.Success(
                items = List(size = 2) { totpItem(accountName = "Account") },
            ),
            result,
        )
    }

    @Test
    fun `parseForResult should ignore a note of an unexpected type`() {
        val result = parser.parseForResult(
            exportJson(
                totpEntry(
                    uri = "otpauth://totp/Account?secret=$SECRET&issuer=Issuer",
                    note = """{"text":"Recovery codes"}""",
                ),
            )
                .toByteArray(),
        )

        assertEquals(
            ExportParseResult.Success(items = listOf(totpItem(accountName = "Account"))),
            result,
        )
    }

    @Test
    fun `parseForResult with CRLF line endings should succeed`() {
        val result = parser.parseForResult(
            exportJson(totpEntry(uri = "otpauth://totp/Account?secret=$SECRET&issuer=Issuer"))
                .replace("\n", "\r\n")
                .toByteArray(),
        )

        assertEquals(
            ExportParseResult.Success(items = listOf(totpItem(accountName = "Account"))),
            result,
        )
    }

    @Test
    fun `parseForResult with a byte order mark should return Error`() {
        val result = parser.parseForResult(
            byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) +
                exportJson(totpEntry(uri = "otpauth://totp/Account?secret=$SECRET&issuer=Issuer"))
                    .toByteArray(),
        )

        assertEquals(FILE_COULD_NOT_BE_PROCESSED_ERROR, result)
    }

    @Test
    fun `parseForResult with a binary file should return Error`() {
        val result = parser.parseForResult(
            byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0xFF.toByte()),
        )

        assertEquals(FILE_COULD_NOT_BE_PROCESSED_ERROR, result)
    }

    @Test
    fun `parseForResult with an empty file should return Error`() {
        val result = parser.parseForResult(ByteArray(0))

        assertEquals(FILE_COULD_NOT_BE_PROCESSED_ERROR, result)
    }

    @Test
    fun `parseForResult with a JSON array should return Error`() {
        val result = parser.parseForResult("[]".toByteArray())

        assertEquals(FILE_COULD_NOT_BE_PROCESSED_ERROR, result)
    }

    @Test
    fun `parseForResult with a null entry should return Error`() {
        val result = parser.parseForResult("""{"version":1,"entries":[null]}""".toByteArray())

        assertEquals(FILE_COULD_NOT_BE_PROCESSED_ERROR, result)
    }

    @Test
    fun `parseForResult with an invalid URI should return Error`() {
        val result = parser.parseForResult(
            exportJson(totpEntry(uri = "otpauth://totp/Acc ount?secret=%ZZ")).toByteArray(),
        )

        assertEquals(FILE_COULD_NOT_BE_PROCESSED_ERROR, result)
    }

    @Test
    fun `parseForResult with Steam entry should use Steam as title and name as account`() {
        val result = parser.parseForResult(
            exportJson(steamEntry(uri = "steam://$SECRET", name = "steam-user")).toByteArray(),
        )

        assertEquals(
            ExportParseResult.Success(items = listOf(steamItem(accountName = "steam-user"))),
            result,
        )
    }

    @Test
    fun `parseForResult should convert a base64 Steam secret to base32`() {
        val result = parser.parseForResult(
            exportJson(steamEntry(uri = "steam://SGVsbG8h3q2+7w", name = "steam-user"))
                .toByteArray(),
        )

        assertEquals(
            ExportParseResult.Success(items = listOf(steamItem(accountName = "steam-user"))),
            result,
        )
    }

    @Test
    fun `parseForResult with unreadable Steam secret should return Error`() {
        val result = parser.parseForResult(
            exportJson(steamEntry(uri = "steam://!!!", name = "steam-user")).toByteArray(),
        )

        assertEquals(FILE_COULD_NOT_BE_PROCESSED_ERROR, result)
    }

    @Test
    fun `parseForResult should skip unknown entry types`() {
        val result = parser.parseForResult(
            exportJson(
                """{"content":{"uri":"otpauth://hotp/A?secret=$SECRET","entry_type":"Hotp"}}""",
                totpEntry(uri = "otpauth://totp/Account?secret=$SECRET&issuer=Issuer"),
            )
                .toByteArray(),
        )

        assertEquals(
            ExportParseResult.Success(items = listOf(totpItem(accountName = "Account"))),
            result,
        )
    }

    @Test
    fun `parseForResult should ignore note and id`() {
        val result = parser.parseForResult(
            exportJson(
                totpEntry(
                    uri = "otpauth://totp/Account?secret=$SECRET&issuer=Issuer",
                    note = "\"Recovery codes in the safe\"",
                ),
            )
                .toByteArray(),
        )

        assertEquals(
            ExportParseResult.Success(items = listOf(totpItem(accountName = "Account"))),
            result,
        )
    }

    @Test
    fun `parseForResult with empty entries should succeed with no items`() {
        val result = parser.parseForResult(exportJson().toByteArray())

        assertEquals(ExportParseResult.Success(items = emptyList()), result)
    }

    @Test
    fun `parseForResult with password protected file should return Error`() {
        val result = parser.parseForResult(
            """{"version":1,"salt":"c2FsdHNhbHRzYWx0c2FsdA==","content":"Y2lwaGVydGV4dA=="}"""
                .toByteArray(),
        )

        assertEquals(
            ExportParseResult.Error(
                message = BitwardenString
                    .import_proton_authenticator_password_protected_not_supported
                    .asText(),
            ),
            result,
        )
    }

    @Test
    fun `parseForResult with unsupported version should return Error`() {
        val result = parser.parseForResult(
            exportJson(
                totpEntry(uri = "otpauth://totp/Account?secret=$SECRET&issuer=Issuer"),
                version = 2,
            )
                .toByteArray(),
        )

        assertEquals(FILE_COULD_NOT_BE_PROCESSED_ERROR, result)
    }

    @Test
    fun `parseForResult without entries or salt should return Error`() {
        val result = parser.parseForResult("""{"version":1}""".toByteArray())

        assertEquals(
            ExportParseResult.Error(
                title = BitwardenString.required_information_missing.asText(),
                message = BitwardenString.required_information_missing_message.asText(),
            ),
            result,
        )
    }

    @Test
    fun `parseForResult with TOTP entry missing a secret should return Error`() {
        val result = parser.parseForResult(
            exportJson(totpEntry(uri = "otpauth://totp/Account?issuer=Issuer")).toByteArray(),
        )

        assertEquals(FILE_COULD_NOT_BE_PROCESSED_ERROR, result)
    }

    @Test
    fun `parseForResult with TOTP entry with an unknown algorithm should return Error`() {
        val result = parser.parseForResult(
            exportJson(
                totpEntry(uri = "otpauth://totp/Account?secret=$SECRET&algorithm=MD5"),
            )
                .toByteArray(),
        )

        assertEquals(FILE_COULD_NOT_BE_PROCESSED_ERROR, result)
    }

    @Test
    fun `parseForResult with TOTP entry with digits out of range should return Error`() {
        val result = parser.parseForResult(
            exportJson(totpEntry(uri = "otpauth://totp/Account?secret=$SECRET&digits=4"))
                .toByteArray(),
        )

        assertEquals(FILE_COULD_NOT_BE_PROCESSED_ERROR, result)
    }

    @Test
    fun `parseForResult with TOTP entry with a zero period should return Error`() {
        val result = parser.parseForResult(
            exportJson(totpEntry(uri = "otpauth://totp/Account?secret=$SECRET&period=0"))
                .toByteArray(),
        )

        assertEquals(FILE_COULD_NOT_BE_PROCESSED_ERROR, result)
    }

    @Test
    fun `parseForResult with malformed JSON should return Error`() {
        val result = parser.parseForResult("""{"version":1,"entries":[""".toByteArray())

        assertEquals(FILE_COULD_NOT_BE_PROCESSED_ERROR, result)
    }

    @Test
    fun `parseForResult with entry missing content should return Error`() {
        val result = parser.parseForResult(
            """{"version":1,"entries":[{"id":"x","note":null}]}""".toByteArray(),
        )

        assertEquals(
            ExportParseResult.Error(
                title = BitwardenString.required_information_missing.asText(),
                message = BitwardenString.required_information_missing_message.asText(),
            ),
            result,
        )
    }
}

private const val TEST_ID = "00000000-0000-0000-0000-000000000001"
private const val SECRET = "JBSWY3DPEHPK3PXP"

private val FILE_COULD_NOT_BE_PROCESSED_ERROR = ExportParseResult.Error(
    title = BitwardenString.file_could_not_be_processed.asText(),
    message = BitwardenString.file_could_not_be_processed_message.asText(),
)

private fun exportJson(vararg entries: String, version: Int = 1): String =
    """{"version":$version,"entries":[${entries.joinToString(",")}]}"""

private fun totpEntry(
    uri: String,
    name: String? = "Name",
    note: String = "null",
): String {
    val nameJson = name?.let { "\"$it\"" } ?: "null"
    return """
        {
          "id": "11111111-1111-1111-1111-111111111111",
          "content": {"uri": "$uri", "entry_type": "Totp", "name": $nameJson},
          "note": $note
        }
    """.trimIndent()
}

private fun steamEntry(uri: String, name: String): String =
    """{"id":"s","content":{"uri":"$uri","entry_type":"Steam","name":"$name"},"note":null}"""

@Suppress("LongParameterList")
private fun totpItem(
    issuer: String = "Issuer",
    accountName: String?,
    algorithm: AuthenticatorItemAlgorithm = AuthenticatorItemAlgorithm.SHA1,
    digits: Int = 6,
    period: Int = 30,
    key: String = SECRET,
): AuthenticatorItemEntity = AuthenticatorItemEntity(
    id = TEST_ID,
    key = key,
    type = AuthenticatorItemType.TOTP,
    algorithm = algorithm,
    period = period,
    digits = digits,
    issuer = issuer,
    userId = null,
    accountName = accountName,
    favorite = false,
)

private fun steamItem(accountName: String): AuthenticatorItemEntity = AuthenticatorItemEntity(
    id = TEST_ID,
    key = SECRET,
    type = AuthenticatorItemType.STEAM,
    algorithm = AuthenticatorItemAlgorithm.SHA1,
    period = 30,
    digits = 5,
    issuer = "Steam",
    userId = null,
    accountName = accountName,
    favorite = false,
)
