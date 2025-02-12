package com.bitwarden.authenticator.data.platform.manager.imports

import com.bitwarden.authenticator.data.authenticator.datasource.disk.AuthenticatorDiskSource
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemEntity
import com.bitwarden.authenticator.data.platform.manager.imports.model.ExportParseResult
import com.bitwarden.authenticator.data.platform.manager.imports.model.ImportDataResult
import com.bitwarden.authenticator.data.platform.manager.imports.model.ImportFileFormat
import com.bitwarden.authenticator.data.platform.manager.imports.parsers.BitwardenExportParser
import com.bitwarden.authenticator.ui.platform.base.util.asText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.runs
import io.mockk.unmockkConstructor
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ImportManagerTest {
    private val mockAuthenticatorDiskSource = mockk<AuthenticatorDiskSource>()

    private val manager = ImportManagerImpl(
        authenticatorDiskSource = mockAuthenticatorDiskSource,
    )

    @BeforeEach
    fun setup() {
        mockkConstructor(BitwardenExportParser::class)
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
}

private val DEFAULT_BYTE_ARRAY = "".toByteArray()
