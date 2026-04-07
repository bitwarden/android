package com.x8bit.bitwarden.ui.auth.feature.environment

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.data.datasource.disk.model.EnvironmentUrlDataJson
import com.bitwarden.data.manager.file.FileManager
import com.bitwarden.data.repository.model.Environment
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import com.bitwarden.ui.platform.model.FileData
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.platform.datasource.disk.model.MutualTlsKeyHost
import com.x8bit.bitwarden.data.platform.manager.CertificateManager
import com.x8bit.bitwarden.data.platform.manager.model.ImportPrivateKeyResult
import com.x8bit.bitwarden.data.platform.repository.util.FakeEnvironmentRepository
import com.x8bit.bitwarden.ui.platform.manager.keychain.model.PrivateKeyAliasSelectionResult
import com.x8bit.bitwarden.ui.platform.model.SnackbarRelay
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@Suppress("LargeClass")
class EnvironmentViewModelTest : BaseViewModelTest() {

    private val fakeEnvironmentRepository = FakeEnvironmentRepository()
    private val mockCertificateManager = mockk<CertificateManager> {
        every { getMutualTlsKeyAliases() } returns emptyList()
    }
    private val mockFileManager = mockk<FileManager>()
    private val snackbarRelayManager = mockk<SnackbarRelayManager<SnackbarRelay>> {
        every { sendSnackbarData(data = any(), relay = any()) } just runs
    }

    @Suppress("MaxLineLength")
    @Test
    fun `initial state should be correct when there is no saved state and the current environment is not self-hosted`() {
        val viewModel = createViewModel()
        assertEquals(
            DEFAULT_STATE,
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `initial state should be correct when there is no saved state and the current environment is self-hosted`() {
        val selfHostedEnvironmentUrlData = EnvironmentUrlDataJson(
            base = "self-hosted-base",
            api = "self-hosted-api",
            identity = "self-hosted-identity",
            icon = "self-hosted-icons",
            webVault = "self-hosted-web-vault",
        )
        fakeEnvironmentRepository.environment = Environment.SelfHosted(
            environmentUrlData = selfHostedEnvironmentUrlData,
        )
        val viewModel = createViewModel()
        assertEquals(
            DEFAULT_STATE.copy(
                serverUrl = "self-hosted-base",
                webVaultServerUrl = "self-hosted-web-vault",
                apiServerUrl = "self-hosted-api",
                identityServerUrl = "self-hosted-identity",
                iconsServerUrl = "self-hosted-icons",
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `initial state should be correct when restoring from the save state handle`() {
        val savedState = DEFAULT_STATE.copy(
            serverUrl = "saved-server",
            webVaultServerUrl = "saved-web-vault",
            apiServerUrl = "saved-api",
            identityServerUrl = "saved-identity",
            iconsServerUrl = "saved-icons",
            keyHost = MutualTlsKeyHost.ANDROID_KEY_STORE,
            keyAlias = "saved-key-alias",
        )
        val viewModel = createViewModel(
            savedStateHandle = SavedStateHandle(
                initialState = mapOf(
                    "state" to savedState,
                ),
            ),
        )
        assertEquals(
            DEFAULT_STATE.copy(
                serverUrl = "saved-server",
                webVaultServerUrl = "saved-web-vault",
                apiServerUrl = "saved-api",
                identityServerUrl = "saved-identity",
                iconsServerUrl = "saved-icons",
                keyHost = MutualTlsKeyHost.ANDROID_KEY_STORE,
                keyAlias = "saved-key-alias",
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `CloseClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(EnvironmentAction.CloseClick)
            assertEquals(EnvironmentEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `SaveClick should show the error dialog when any URLs are invalid`() = runTest {
        assertEquals(
            Environment.Us,
            fakeEnvironmentRepository.environment,
        )

        val viewModel = createViewModel()
        // Update to valid absolute URL
        listOf(
            EnvironmentAction.WebVaultServerUrlChange(
                webVaultServerUrl = "web vault",
            ),
        )
            .forEach { viewModel.trySendAction(it) }

        val initialState = DEFAULT_STATE.copy(webVaultServerUrl = "web vault")
        assertEquals(
            initialState,
            viewModel.stateFlow.value,
        )

        viewModel.trySendAction(EnvironmentAction.SaveClick)

        assertEquals(
            initialState.copy(
                dialog = EnvironmentState.DialogState.Error(
                    message = BitwardenString.environment_page_urls_error.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )

        // The Environment has not been updated
        assertEquals(
            Environment.Us,
            fakeEnvironmentRepository.environment,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `SaveClick should emit NavigateBack, send a snackbar to the ENVIRONMENT SAVED relay, and update the environment when all URLs are valid`() =
        runTest {
            assertEquals(
                Environment.Us,
                fakeEnvironmentRepository.environment,
            )

            val viewModel = createViewModel()
            // Update to valid absolute or relative URLs
            listOf(
                EnvironmentAction.ServerUrlChange(
                    serverUrl = "https://server-url",
                ),
                EnvironmentAction.WebVaultServerUrlChange(
                    webVaultServerUrl = "http://web-vault-url",
                ),
                EnvironmentAction.ApiServerUrlChange(
                    apiServerUrl = "api-url",
                ),
                EnvironmentAction.IdentityServerUrlChange(
                    identityServerUrl = "identity-url",
                ),
                EnvironmentAction.IconsServerUrlChange(
                    iconsServerUrl = "icons-url",
                ),
                EnvironmentAction.SystemCertificateSelectionResultReceive(
                    privateKeyAliasSelectionResult = PrivateKeyAliasSelectionResult.Success(
                        alias = "mockAlias",
                    ),
                ),
            )
                .forEach { viewModel.trySendAction(it) }

            viewModel.eventFlow.test {
                viewModel.trySendAction(EnvironmentAction.SaveClick)
                assertEquals(
                    EnvironmentEvent.NavigateBack,
                    awaitItem(),
                )
                // All the updated URLs should be prefixed with "https://" or "http://"
                assertEquals(
                    Environment.SelfHosted(
                        environmentUrlData = EnvironmentUrlDataJson(
                            base = "https://server-url",
                            api = "https://api-url",
                            identity = "https://identity-url",
                            icon = "https://icons-url",
                            notifications = null,
                            webVault = "http://web-vault-url",
                            events = null,
                            keyUri = "cert://KEY_CHAIN/mockAlias",
                        ),
                    ),
                    fakeEnvironmentRepository.environment,
                )
            }
            verify(exactly = 1) {
                snackbarRelayManager.sendSnackbarData(
                    data = BitwardenSnackbarData(message = BitwardenString.environment_saved.asText()),
                    relay = SnackbarRelay.ENVIRONMENT_SAVED,
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `SaveClick should emit NavigateBack, send a snackbar to the ENVIRONMENT SAVED relay, and update the environment when some URLs are valid and others are null`() =
        runTest {
            assertEquals(
                Environment.Us,
                fakeEnvironmentRepository.environment,
            )

            val viewModel = createViewModel()
            // Update to valid absolute URL
            listOf(
                EnvironmentAction.WebVaultServerUrlChange(
                    webVaultServerUrl = "http://web-vault-url",
                ),
            )
                .forEach { viewModel.trySendAction(it) }

            viewModel.eventFlow.test {
                viewModel.trySendAction(EnvironmentAction.SaveClick)
                assertEquals(
                    EnvironmentEvent.NavigateBack,
                    awaitItem(),
                )
                // All the updated URLs should be prefixed with "https://" or "http://"
                assertEquals(
                    Environment.SelfHosted(
                        environmentUrlData = EnvironmentUrlDataJson(
                            base = "",
                            api = null,
                            identity = null,
                            icon = null,
                            notifications = null,
                            webVault = "http://web-vault-url",
                            events = null,
                            keyUri = null,
                        ),
                    ),
                    fakeEnvironmentRepository.environment,
                )
            }
            verify(exactly = 1) {
                snackbarRelayManager.sendSnackbarData(
                    data = BitwardenSnackbarData(message = BitwardenString.environment_saved.asText()),
                    relay = SnackbarRelay.ENVIRONMENT_SAVED,
                )
            }
        }

    @Test
    fun `ServerUrlChange should update the server URL`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(
            EnvironmentAction.ServerUrlChange(serverUrl = "updated-server-url"),
        )
        assertEquals(
            DEFAULT_STATE.copy(serverUrl = "updated-server-url"),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `WebVaultServerUrlChange should update the web vault server URL`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(
            EnvironmentAction.WebVaultServerUrlChange(webVaultServerUrl = "updated-web-vault-url"),
        )
        assertEquals(
            DEFAULT_STATE.copy(webVaultServerUrl = "updated-web-vault-url"),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `ApiServerUrlChange should update the API server URL`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(
            EnvironmentAction.ApiServerUrlChange(apiServerUrl = "updated-api-url"),
        )
        assertEquals(
            DEFAULT_STATE.copy(apiServerUrl = "updated-api-url"),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `IdentityServerUrlChange should update the identity server URL`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(
            EnvironmentAction.IdentityServerUrlChange(identityServerUrl = "updated-identity-url"),
        )
        assertEquals(
            DEFAULT_STATE.copy(identityServerUrl = "updated-identity-url"),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `IconsServerUrlChange should update the icons server URL`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(
            EnvironmentAction.IconsServerUrlChange(iconsServerUrl = "updated-icons-url"),
        )
        assertEquals(
            DEFAULT_STATE.copy(iconsServerUrl = "updated-icons-url"),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `SystemCertificateSelectionResultReceive should update key alias and key host when successful`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(
            EnvironmentAction.SystemCertificateSelectionResultReceive(
                privateKeyAliasSelectionResult = PrivateKeyAliasSelectionResult.Success(
                    alias = "mockAlias",
                ),
            ),
        )
        assertEquals(
            DEFAULT_STATE.copy(
                keyAlias = "mockAlias",
                keyHost = MutualTlsKeyHost.KEY_CHAIN,
            ),
            viewModel.stateFlow.value,
        )

        viewModel.trySendAction(
            EnvironmentAction.SystemCertificateSelectionResultReceive(
                privateKeyAliasSelectionResult = PrivateKeyAliasSelectionResult.Success(
                    alias = null,
                ),
            ),
        )
        assertEquals(
            DEFAULT_STATE.copy(
                keyAlias = "",
                keyHost = null,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `SystemCertificateSelectionResultReceive should show snackbar when error`() = runTest {
        val viewModel = createViewModel()
        viewModel.trySendAction(
            EnvironmentAction.SystemCertificateSelectionResultReceive(
                privateKeyAliasSelectionResult = PrivateKeyAliasSelectionResult.Error,
            ),
        )
        viewModel.eventFlow.test {
            assertEquals(
                EnvironmentEvent.ShowSnackbar(BitwardenString.error_loading_certificate.asText()),
                awaitItem(),
            )
        }
    }

    @Test
    fun `ChooseSystemCertificate should show system certificate warning dialog`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.trySendAction(EnvironmentAction.ChooseSystemCertificateClick)

            assertEquals(
                DEFAULT_STATE.copy(
                    dialog = EnvironmentState.DialogState.SystemCertificateWarningDialog,
                ),
                viewModel.stateFlow.value,
            )
        }

    @Test
    fun `ErrorDialogDismiss should clear the dialog`() = runTest {
        val viewModel = createViewModel()
        viewModel.trySendAction(EnvironmentAction.DialogDismiss)
        assertEquals(
            DEFAULT_STATE.copy(dialog = null),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `ImportCertificateClick should emit ShowCertificateImportFileChooser`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(EnvironmentAction.ImportCertificateClick)
            assertEquals(EnvironmentEvent.ShowCertificateImportFileChooser, awaitItem())
        }
    }

    @Test
    fun `ImportCertificateFilePickerResultReceive should show SetCertificateData dialog`() =
        runTest {
            val viewModel = createViewModel()
            val mockFileData = mockk<FileData>()
            viewModel.trySendAction(
                EnvironmentAction.ImportCertificateFilePickerResultReceive(
                    certificateFileData = mockFileData,
                ),
            )
            assertEquals(
                DEFAULT_STATE.copy(
                    dialog = EnvironmentState.DialogState.SetCertificateData(
                        certificateBytes = mockFileData,
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Test
    fun `SetCertificatePasswordDialogDismiss should clear the dialog`() = runTest {
        val viewModel = createViewModel()
        viewModel.trySendAction(EnvironmentAction.SetCertificatePasswordDialogDismiss)
        assertEquals(
            DEFAULT_STATE.copy(dialog = null),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `CertificateInstallationResultReceive should show snackbar based on result`() = runTest {
        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(
                EnvironmentAction.CertificateInstallationResultReceive(
                    success = true,
                ),
            )
            assertEquals(
                EnvironmentEvent.ShowSnackbar(BitwardenString.certificate_installed.asText()),
                awaitItem(),
            )

            viewModel.trySendAction(
                EnvironmentAction.CertificateInstallationResultReceive(
                    success = false,
                ),
            )
            assertEquals(
                EnvironmentEvent.ShowSnackbar(
                    BitwardenString.certificate_installation_failed.asText(),
                ),
                awaitItem(),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `ConfirmChooseSystemCertificateClick should clear the dialog and emit ShowSystemCertificateSelectionDialog`() =
        runTest {
            val viewModel = createViewModel(
                savedStateHandle = SavedStateHandle(
                    initialState = mapOf(
                        "state" to DEFAULT_STATE.copy(serverUrl = "https://mockServerUrl"),
                    ),
                ),
            )
            viewModel.trySendAction(EnvironmentAction.ConfirmChooseSystemCertificateClick)
            assertEquals(
                DEFAULT_STATE.copy(dialog = null, serverUrl = "https://mockServerUrl"),
                viewModel.stateFlow.value,
            )
            viewModel.eventFlow.test {
                assertEquals(
                    EnvironmentEvent.ShowSystemCertificateSelectionDialog(
                        serverUrl = "https://mockServerUrl",
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `ImportKeyResultReceive should update key alias and key host on success`() = runTest {
        val viewModel = createViewModel()
        viewModel.trySendAction(
            EnvironmentAction.Internal.ImportKeyResultReceive(
                result = ImportPrivateKeyResult.Success(alias = "mockAlias"),
            ),
        )
        assertEquals(
            DEFAULT_STATE.copy(
                keyAlias = "mockAlias",
                keyHost = MutualTlsKeyHost.ANDROID_KEY_STORE,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `ImportKeyResultReceive should show snackbar with correct message on error`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(
                EnvironmentAction.Internal.ImportKeyResultReceive(
                    result = ImportPrivateKeyResult.Error.UnsupportedKey(
                        throwable = Throwable("Fail!"),
                    ),
                ),
            )
            assertEquals(
                EnvironmentEvent.ShowSnackbar(
                    BitwardenString.unsupported_certificate_type.asText(),
                ),
                awaitItem(),
            )

            viewModel.trySendAction(
                EnvironmentAction.Internal.ImportKeyResultReceive(
                    result = ImportPrivateKeyResult.Error.KeyStoreOperationFailed(
                        throwable = Throwable("Fail!"),
                    ),
                ),
            )
            assertEquals(
                EnvironmentEvent.ShowSnackbar(
                    BitwardenString.certificate_installation_failed.asText(),
                ),
                awaitItem(),
            )

            viewModel.trySendAction(
                EnvironmentAction.Internal.ImportKeyResultReceive(
                    result = ImportPrivateKeyResult.Error.UnrecoverableKey(
                        throwable = Throwable("Fail!"),
                    ),
                ),
            )
            assertEquals(
                EnvironmentEvent.ShowSnackbar(
                    BitwardenString.certificate_password_incorrect.asText(),
                ),
                awaitItem(),
            )

            viewModel.trySendAction(
                EnvironmentAction.Internal.ImportKeyResultReceive(
                    result = ImportPrivateKeyResult.Error.InvalidCertificateChain(
                        throwable = Throwable("Fail!"),
                    ),
                ),
            )
            assertEquals(
                EnvironmentEvent.ShowSnackbar(BitwardenString.invalid_certificate_chain.asText()),
                awaitItem(),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `SetCertificateInfoResultReceive should clear the dialog update key alias and key host after successful import`() =
        runTest {
            val viewModel = createViewModel()
            val mockUri = mockk<Uri>()
            val mockFileData = FileData(
                fileName = "mockFileName",
                uri = mockUri,
                sizeBytes = 0,
            )
            val keyBytes = byteArrayOf()
            coEvery {
                mockFileManager.uriToByteArray(mockFileData.uri)
            } returns keyBytes.asSuccess()
            coEvery {
                mockCertificateManager.importMutualTlsCertificate(
                    key = keyBytes,
                    alias = "mockAlias",
                    password = "mockPassword",
                )
            } returns ImportPrivateKeyResult.Success(alias = "mockAlias")

            viewModel.trySendAction(
                EnvironmentAction.SetCertificateInfoResultReceive(
                    certificateFileData = mockFileData,
                    alias = "mockAlias",
                    password = "mockPassword",
                ),
            )
            assertEquals(
                DEFAULT_STATE.copy(
                    dialog = null,
                    keyAlias = "mockAlias",
                    keyHost = MutualTlsKeyHost.ANDROID_KEY_STORE,
                ),
                viewModel.stateFlow.value,
            )

            coVerify {
                mockFileManager.uriToByteArray(mockFileData.uri)
                mockCertificateManager.importMutualTlsCertificate(
                    key = byteArrayOf(),
                    alias = "mockAlias",
                    password = "mockPassword",
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `SetCertificateInfoResultReceive should show ConfirmOverwriteAlias dialog if alias already exists`() =
        runTest {
            every { mockCertificateManager.getMutualTlsKeyAliases() } returns listOf("mockAlias")

            val action = EnvironmentAction.SetCertificateInfoResultReceive(
                certificateFileData = mockk(),
                alias = "mockAlias",
                password = "mockPassword",
            )
            val viewModel = createViewModel()

            viewModel.trySendAction(action)

            assertEquals(
                DEFAULT_STATE.copy(
                    dialog = EnvironmentState.DialogState.ConfirmOverwriteAlias(
                        title = BitwardenString.replace_existing_certificate.asText(),
                        message = BitwardenString.a_certificate_with_the_alias_x_already_exists_do_you_want_to_replace_it
                            .asText("mockAlias"),
                        triggeringAction = action,
                    ),
                ),
                viewModel.stateFlow.value,
            )

            verify(exactly = 0) {
                mockCertificateManager.importMutualTlsCertificate(any(), any(), any())
            }
        }

    @Test
    fun `SetCertificateInfoResultReceive should show error dialog if input is invalid`() = runTest {
        val viewModel = createViewModel()

        viewModel.trySendAction(
            EnvironmentAction.SetCertificateInfoResultReceive(
                certificateFileData = mockk(),
                alias = "mockAlias",
                password = "",
            ),
        )
        assertEquals(
            DEFAULT_STATE.copy(
                dialog = EnvironmentState.DialogState.Error(
                    BitwardenString.validation_field_required.asText(
                        BitwardenString.password.asText(),
                    ),
                ),
            ),
            viewModel.stateFlow.value,
        )

        viewModel.trySendAction(
            EnvironmentAction.SetCertificateInfoResultReceive(
                certificateFileData = mockk(),
                alias = "",
                password = "mockPassword",
            ),
        )
        assertEquals(
            DEFAULT_STATE.copy(
                dialog = EnvironmentState.DialogState.Error(
                    BitwardenString.validation_field_required.asText(
                        BitwardenString.alias.asText(),
                    ),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `SetCertificateInfoResultReceive should send ShowErrorDialog if uriToByteArray fails`() =
        runTest {
            val viewModel = createViewModel()
            val mockUri = mockk<Uri>()
            val mockFileData = FileData(
                fileName = "mockFileName",
                uri = mockUri,
                sizeBytes = 0,
            )
            val exception = Exception()

            coEvery { mockFileManager.uriToByteArray(any()) } returns Result.failure(exception)

            viewModel.trySendAction(
                EnvironmentAction.SetCertificateInfoResultReceive(
                    certificateFileData = mockFileData,
                    alias = "mockAlias",
                    password = "password",
                ),
            )

            assertEquals(
                DEFAULT_STATE.copy(
                    dialog = EnvironmentState.DialogState.Error(
                        message = BitwardenString.unable_to_read_certificate.asText(),
                        throwable = exception,
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Test
    fun `ConfirmOverwriteCertificateClick should clear dialog and import certificate`() = runTest {
        val viewModel = createViewModel()
        val mockUri = mockk<Uri>()
        val mockFileData = FileData(
            fileName = "mockFileName",
            uri = mockUri,
            sizeBytes = 0,
        )
        val keyBytes = byteArrayOf()
        coEvery {
            mockFileManager.uriToByteArray(mockFileData.uri)
        } returns keyBytes.asSuccess()
        coEvery {
            mockCertificateManager.importMutualTlsCertificate(
                key = keyBytes,
                alias = "mockAlias",
                password = "mockPassword",
            )
        } returns ImportPrivateKeyResult.Success(alias = "mockAlias")

        viewModel.trySendAction(
            EnvironmentAction.ConfirmOverwriteCertificateClick(
                triggeringAction = EnvironmentAction.SetCertificateInfoResultReceive(
                    certificateFileData = mockFileData,
                    alias = "mockAlias",
                    password = "mockPassword",
                ),
            ),
        )
        assertEquals(
            DEFAULT_STATE.copy(
                dialog = null,
                keyAlias = "mockAlias",
                keyHost = MutualTlsKeyHost.ANDROID_KEY_STORE,
            ),
            viewModel.stateFlow.value,
        )

        coVerify {
            mockFileManager.uriToByteArray(mockFileData.uri)
            mockCertificateManager.importMutualTlsCertificate(
                key = byteArrayOf(),
                alias = "mockAlias",
                password = "mockPassword",
            )
        }
    }

    @Test
    fun `ShowErrorDialog should show error dialog`() = runTest {
        val viewModel = createViewModel()
        val exception = RuntimeException()
        viewModel.trySendAction(
            EnvironmentAction.Internal.ShowErrorDialog(
                message = BitwardenString.generic_error_message.asText(),
                throwable = exception,
            ),
        )

        assertEquals(
            DEFAULT_STATE.copy(
                dialog = EnvironmentState.DialogState.Error(
                    message = BitwardenString.generic_error_message.asText(),
                    throwable = exception,
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    //region Helper methods

    private fun createViewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): EnvironmentViewModel =
        EnvironmentViewModel(
            environmentRepository = fakeEnvironmentRepository,
            certificateManager = mockCertificateManager,
            fileManager = mockFileManager,
            snackbarRelayManager = snackbarRelayManager,
            savedStateHandle = savedStateHandle,
        )

    //endregion Helper methods

    companion object {
        private val DEFAULT_STATE = EnvironmentState(
            serverUrl = "",
            keyAlias = "",
            webVaultServerUrl = "",
            apiServerUrl = "",
            identityServerUrl = "",
            iconsServerUrl = "",
            keyHost = null,
            dialog = null,
        )
    }
}
