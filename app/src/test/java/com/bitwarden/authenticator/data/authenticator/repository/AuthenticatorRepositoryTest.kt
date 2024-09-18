package com.bitwarden.authenticator.data.authenticator.repository

import app.cash.turbine.test
import com.bitwarden.authenticator.data.authenticator.datasource.disk.util.FakeAuthenticatorDiskSource
import com.bitwarden.authenticator.data.authenticator.datasource.entity.createMockAuthenticatorItemEntity
import com.bitwarden.authenticator.data.authenticator.manager.FileManager
import com.bitwarden.authenticator.data.authenticator.manager.TotpCodeManager
import com.bitwarden.authenticator.data.authenticator.manager.model.VerificationCodeItem
import com.bitwarden.authenticator.data.platform.base.FakeDispatcherManager
import com.bitwarden.authenticator.data.platform.manager.imports.ImportManager
import com.bitwarden.authenticator.data.platform.repository.model.DataState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AuthenticatorRepositoryTest {

    private val fakeAuthenticatorDiskSource = FakeAuthenticatorDiskSource()
    private val mutableTotpCodesStateFlow =
        MutableStateFlow<DataState<List<VerificationCodeItem>>>(DataState.Loading)
    private val mockTotpCodeManager = mockk<TotpCodeManager> {
        every { getTotpCodesStateFlow(any()) } returns mutableTotpCodesStateFlow
    }
    private val mockFileManager = mockk<FileManager>()
    private val mockImportManager = mockk<ImportManager>()
    private val mockDispatcherManager = FakeDispatcherManager()

    private val authenticatorRepository = AuthenticatorRepositoryImpl(
        authenticatorDiskSource = fakeAuthenticatorDiskSource,
        totpCodeManager = mockTotpCodeManager,
        fileManager = mockFileManager,
        importManager = mockImportManager,
        dispatcherManager = mockDispatcherManager,
    )

    @Test
    fun `initial state should be correct`() = runTest {
        authenticatorRepository.ciphersStateFlow.test {
            assertEquals(
                DataState.Loading,
                awaitItem(),
            )
        }
    }

    @Test
    fun `ciphersStateFlow should emit sorted authenticator items when disk source changes`() =
        runTest {
            val mockItem = createMockAuthenticatorItemEntity(1)
            fakeAuthenticatorDiskSource.saveItem(mockItem)
            assertEquals(
                DataState.Loaded(listOf(mockItem)),
                authenticatorRepository.ciphersStateFlow.value,
            )
        }
}
