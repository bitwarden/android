package com.bitwarden.authenticator.ui.authenticator.feature.manualcodeentry

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemEntity
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemType
import com.bitwarden.authenticator.data.authenticator.repository.AuthenticatorRepository
import com.bitwarden.authenticator.data.authenticator.repository.model.CreateItemResult
import com.bitwarden.authenticator.ui.platform.base.BaseViewModelTest
import com.bitwarden.authenticator.ui.platform.base.util.asText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class ManualCodeEntryViewModelTest : BaseViewModelTest() {

    private val mockAuthenticatorRepository = mockk<AuthenticatorRepository>()

    @BeforeEach
    fun setUp() {
        mockkStatic(UUID::class)
        every { UUID.randomUUID().toString() } returns "mockUUID"
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(UUID::class)
    }

    @Test
    fun `initial state should be correct when saved state is not null`() {
        val initialState = ManualCodeEntryState(
            code = "ABCD",
            issuer = "mockIssuer",
            dialog = null,
        )
        val viewModel = createViewModel(initialState = initialState)
        assertEquals(initialState, viewModel.stateFlow.value)
    }

    @Test
    fun `initial state should be correct when saved state is null`() {
        val viewModel = createViewModel(initialState = null)
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    @Test
    fun `CloseClick should navigate back`() = runTest {
        val viewModel = createViewModel()
        viewModel.trySendAction(ManualCodeEntryAction.CloseClick)
        viewModel.eventFlow.test {
            assertEquals(
                ManualCodeEntryEvent.NavigateBack,
                awaitItem(),
            )
        }
    }

    @Test
    fun `CodeTextChange should update state`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(ManualCodeEntryAction.CodeTextChange("newCode"))
        assertEquals(
            DEFAULT_STATE.copy(code = "newCode"),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `IssuerTextChange should update state`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(ManualCodeEntryAction.IssuerTextChange("newIssuer"))
        assertEquals(
            DEFAULT_STATE.copy(issuer = "newIssuer"),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `CodeSubmit should createItem, show toast, and navigate back on success when code is valid`() =
        runTest {
            coEvery {
                mockAuthenticatorRepository.createItem(
                    item = AuthenticatorItemEntity(
                        id = "mockUUID",
                        key = "ABCD",
                        issuer = "mockIssuer",
                        accountName = "",
                        userId = null,
                        favorite = false,
                        type = AuthenticatorItemType.TOTP,
                    ),
                )
            } returns CreateItemResult.Success

            val viewModel = createViewModel(
                initialState = DEFAULT_STATE
                    .copy(code = "ABCD", issuer = "mockIssuer"),
            )

            viewModel.trySendAction(ManualCodeEntryAction.CodeSubmit)

            coVerify {
                mockAuthenticatorRepository.createItem(
                    item = AuthenticatorItemEntity(
                        id = "mockUUID",
                        key = "ABCD",
                        issuer = "mockIssuer",
                        accountName = "",
                        userId = null,
                        favorite = false,
                        type = AuthenticatorItemType.TOTP,
                    ),
                )
            }
            viewModel.eventFlow.test {
                assertEquals(
                    ManualCodeEntryEvent.ShowToast(R.string.verification_code_added.asText()),
                    awaitItem(),
                )
                assertEquals(
                    ManualCodeEntryEvent.NavigateBack,
                    awaitItem(),
                )
            }
        }

    @Test
    fun `CodeSubmit should replace whitespace from code`() = runTest {
        coEvery {
            mockAuthenticatorRepository.createItem(
                item = AuthenticatorItemEntity(
                    id = "mockUUID",
                    key = "ABCD",
                    issuer = "mockIssuer",
                    accountName = "",
                    userId = null,
                    favorite = false,
                    type = AuthenticatorItemType.TOTP,
                ),
            )
        } returns CreateItemResult.Success

        val viewModel = createViewModel(
            initialState = DEFAULT_STATE.copy(
                code = "A B C D",
                issuer = "mockIssuer",
            ),
        )

        viewModel.trySendAction(ManualCodeEntryAction.CodeSubmit)

        coVerify {
            mockAuthenticatorRepository.createItem(
                item = AuthenticatorItemEntity(
                    id = "mockUUID",
                    key = "ABCD",
                    issuer = "mockIssuer",
                    accountName = "",
                    userId = null,
                    favorite = false,
                    type = AuthenticatorItemType.TOTP,
                ),
            )
        }
    }

    @Test
    fun `CodeSubmit should show error dialog when code is empty`() = runTest {
        val viewModel = createViewModel(
            initialState = DEFAULT_STATE.copy(
                code = "    ",
            ),
        )

        viewModel.trySendAction(ManualCodeEntryAction.CodeSubmit)

        assertEquals(
            ManualCodeEntryState.DialogState.Error(
                message = R.string.key_is_required.asText(),
            ),
            viewModel.stateFlow.value.dialog,
        )
    }

    @Test
    fun `CodeSubmit should show error dialog when code is not base32`() {
        val viewModel = createViewModel(
            initialState = DEFAULT_STATE.copy(
                code = "ABCD12345",
            ),
        )

        viewModel.trySendAction(ManualCodeEntryAction.CodeSubmit)

        assertEquals(
            ManualCodeEntryState.DialogState.Error(
                message = R.string.key_is_invalid.asText(),
            ),
            viewModel.stateFlow.value.dialog,
        )
    }

    @Test
    fun `CodeSubmit should show error dialog when issuer is empty`() {
        val viewModel = createViewModel(
            initialState = DEFAULT_STATE.copy(
                code = "ABCD",
                issuer = "",
            ),
        )

        viewModel.trySendAction(ManualCodeEntryAction.CodeSubmit)

        assertEquals(
            ManualCodeEntryState.DialogState.Error(
                message = R.string.name_is_required.asText(),
            ),
            viewModel.stateFlow.value.dialog,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `CodeSubmit should set AuthenticatorItemType to STEAM when code starts with steam protocol`() {
        coEvery {
            mockAuthenticatorRepository.createItem(
                item = AuthenticatorItemEntity(
                    id = "mockUUID",
                    key = "ABCD",
                    issuer = "mockIssuer",
                    accountName = "",
                    userId = null,
                    favorite = false,
                    type = AuthenticatorItemType.STEAM,
                ),
            )
        } returns CreateItemResult.Success

        val viewModel = createViewModel(
            initialState = DEFAULT_STATE.copy(
                code = "steam://ABCD",
                issuer = "mockIssuer",
            ),
        )

        viewModel.trySendAction(ManualCodeEntryAction.CodeSubmit)

        coVerify {
            mockAuthenticatorRepository.createItem(
                item = AuthenticatorItemEntity(
                    id = "mockUUID",
                    key = "ABCD",
                    issuer = "mockIssuer",
                    accountName = "",
                    userId = null,
                    favorite = false,
                    type = AuthenticatorItemType.STEAM,
                ),
            )
        }
    }

    @Test
    fun `ScanQrCodeTextClick should navigate to QR code screen`() = runTest {
        val viewModel = createViewModel()
        viewModel.trySendAction(ManualCodeEntryAction.ScanQrCodeTextClick)
        viewModel.eventFlow.test {
            assertEquals(
                ManualCodeEntryEvent.NavigateToQrCodeScreen,
                awaitItem(),
            )
        }
    }

    @Test
    fun `SettingsClick should navigate to app settings`() = runTest {
        val viewModel = createViewModel()
        viewModel.trySendAction(ManualCodeEntryAction.SettingsClick)
        viewModel.eventFlow.test {
            assertEquals(
                ManualCodeEntryEvent.NavigateToAppSettings,
                awaitItem(),
            )
        }
    }

    @Test
    fun `DismissDialog should clear dialog state`() {
        val viewModel = createViewModel(
            initialState = DEFAULT_STATE.copy(
                dialog = ManualCodeEntryState.DialogState.Error(
                    message = R.string.key_is_required.asText(),
                ),
            ),
        )
        viewModel.trySendAction(ManualCodeEntryAction.DismissDialog)
        assertEquals(
            DEFAULT_STATE,
            viewModel.stateFlow.value,
        )
    }

    private fun createViewModel(
        initialState: ManualCodeEntryState? = DEFAULT_STATE,
    ): ManualCodeEntryViewModel =
        ManualCodeEntryViewModel(
            savedStateHandle = SavedStateHandle().apply { set("state", initialState) },
            authenticatorRepository = mockAuthenticatorRepository,
        )
}

private val DEFAULT_STATE: ManualCodeEntryState =
    ManualCodeEntryState(
        code = "",
        issuer = "",
        dialog = null,
    )
