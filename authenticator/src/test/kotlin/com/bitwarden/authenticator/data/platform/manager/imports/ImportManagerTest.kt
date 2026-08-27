package com.bitwarden.authenticator.data.platform.manager.imports

import com.bitwarden.authenticator.data.authenticator.datasource.disk.AuthenticatorDiskSource
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemEntity
import com.bitwarden.authenticator.data.platform.manager.crypto.ExportEncryptionManager
import com.bitwarden.authenticator.data.platform.manager.crypto.model.DecryptExportResult
import com.bitwarden.authenticator.data.platform.manager.imports.model.ExportParseResult
import com.bitwarden.authenticator.data.platform.manager.imports.model.ImportDataResult
import com.bitwarden.authenticator.data.platform.manager.imports.model.ImportFileFormat
import com.bitwarden.authenticator.data.platform.manager.imports.parsers.BitwardenExportParser
import com.bitwarden.core.data.manager.UuidManager
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.runs
import io.mockk.unmockkConstructor
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ImportManagerTest {
    private val mockAuthenticatorDiskSource = mockk<AuthenticatorDiskSource>()
    private val mockUuidManager = mockk<UuidManager>()
    private val mockExportEncryptionManager = mockk<ExportEncryptionManager>()

    private val manager = ImportManagerImpl(
        authenticatorDiskSource = mockAuthenticatorDiskSource,
        exportEncryptionManager = mockExportEncryptionManager,
        uuidManager = mockUuidManager,
    )

    @BeforeEach
    fun setup() {
        mockkConstructor(BitwardenExportParser::class)
        every { mockUuidManager.generateUuid() } returns "test-uuid-1"
        every { mockExportEncryptionManager.isPasswordProtected(any()) } returns false
    }

    @AfterEach
    fun tearDown() {
        unmockkConstructor(BitwardenExportParser::class)
    }

    @Test
    fun `ImportManager returns success result from ExportParser and saves items to disk`() =
        runTest {
            val listOfItems = emptyList<AuthenticatorItemEntity>()

            coEvery {
                mockAuthenticatorDiskSource.saveItem(*listOfItems.toTypedArray())
            } just runs

            every {
                anyConstructed<BitwardenExportParser>().parseForResult(any())
            } returns ExportParseResult.Success(listOfItems)

            val result = manager.import(ImportFileFormat.BITWARDEN_JSON, DEFAULT_BYTE_ARRAY)
            assertEquals(ImportDataResult.Success, result)
            coVerify(exactly = 1) {
                mockAuthenticatorDiskSource.saveItem(*listOfItems.toTypedArray())
            }
        }

    @Test
    fun `ImportManager returns correct error result from ExportParser`() = runTest {
        val errorMessage = "borked".asText()

        every {
            anyConstructed<BitwardenExportParser>().parseForResult(any())
        } returns ExportParseResult.Error(message = errorMessage)

        val result = manager.import(ImportFileFormat.BITWARDEN_JSON, DEFAULT_BYTE_ARRAY)
        assertEquals(ImportDataResult.Error(message = errorMessage), result)
    }

    @Test
    fun `ImportManager returns PasswordRequired for an encrypted file with no password`() =
        runTest {
            every { mockExportEncryptionManager.isPasswordProtected(any()) } returns true

            val result = manager.import(
                importFileFormat = ImportFileFormat.BITWARDEN_JSON,
                byteArray = DEFAULT_BYTE_ARRAY,
                password = null,
            )

            assertEquals(ImportDataResult.PasswordRequired, result)
        }

    @Test
    fun `ImportManager returns IncorrectPassword when decryption rejects the password`() = runTest {
        every { mockExportEncryptionManager.isPasswordProtected(any()) } returns true
        coEvery {
            mockExportEncryptionManager.decrypt(any(), any())
        } returns DecryptExportResult.IncorrectPassword

        val result = manager.import(
            importFileFormat = ImportFileFormat.BITWARDEN_JSON,
            byteArray = DEFAULT_BYTE_ARRAY,
            password = "wrong",
        )

        assertEquals(ImportDataResult.IncorrectPassword, result)
    }

    @Test
    fun `ImportManager reports an unsupported kdf as an error`() = runTest {
        every { mockExportEncryptionManager.isPasswordProtected(any()) } returns true
        coEvery {
            mockExportEncryptionManager.decrypt(any(), any())
        } returns DecryptExportResult.UnsupportedKdf

        val result = manager.import(
            importFileFormat = ImportFileFormat.BITWARDEN_JSON,
            byteArray = DEFAULT_BYTE_ARRAY,
            password = "pass",
        )

        assertEquals(
            ImportDataResult.Error(
                message = BitwardenString.the_file_uses_an_unsupported_encryption_setting.asText(),
            ),
            result,
        )
    }

    @Test
    fun `ImportManager parses the decrypted document when the password is correct`() = runTest {
        val listOfItems = emptyList<AuthenticatorItemEntity>()
        every { mockExportEncryptionManager.isPasswordProtected(any()) } returns true
        coEvery {
            mockExportEncryptionManager.decrypt(any(), any())
        } returns DecryptExportResult.Success(json = DECRYPTED_JSON)
        coEvery { mockAuthenticatorDiskSource.saveItem(*listOfItems.toTypedArray()) } just runs
        every {
            anyConstructed<BitwardenExportParser>().parseForResult(any())
        } returns ExportParseResult.Success(listOfItems)

        val result = manager.import(
            importFileFormat = ImportFileFormat.BITWARDEN_JSON,
            byteArray = DEFAULT_BYTE_ARRAY,
            password = "pass",
        )

        assertEquals(ImportDataResult.Success, result)
        verify {
            anyConstructed<BitwardenExportParser>().parseForResult(
                match { it.decodeToString() == DECRYPTED_JSON },
            )
        }
    }

    @Test
    fun `ImportManager never checks non-Bitwarden formats for password protection`() = runTest {
        manager.import(ImportFileFormat.AEGIS, DEFAULT_BYTE_ARRAY)

        verify(exactly = 0) { mockExportEncryptionManager.isPasswordProtected(any()) }
    }
}

private val DEFAULT_BYTE_ARRAY = "".toByteArray()
private const val DECRYPTED_JSON: String = """{"encrypted":false,"items":[]}"""
