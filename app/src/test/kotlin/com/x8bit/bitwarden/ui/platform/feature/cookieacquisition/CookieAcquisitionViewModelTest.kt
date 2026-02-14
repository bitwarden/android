package com.x8bit.bitwarden.ui.platform.feature.cookieacquisition

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.util.CookieCallbackResult
import com.x8bit.bitwarden.data.platform.manager.CookieAcquisitionRequestManager
import com.x8bit.bitwarden.data.platform.manager.model.CookieAcquisitionRequest
import com.x8bit.bitwarden.data.platform.repository.util.FakeEnvironmentRepository
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CookieAcquisitionViewModelTest : BaseViewModelTest() {

    private val mutableCookieCallbackResultFlow =
        MutableSharedFlow<CookieCallbackResult>()

    private val mutableCookieAcquisitionRequestFlow =
        MutableStateFlow<CookieAcquisitionRequest?>(DEFAULT_COOKIE_ACQUISITION_REQUEST)

    private val mockAuthRepository: AuthRepository = mockk {
        every { cookieCallbackResultFlow } returns mutableCookieCallbackResultFlow
    }

    private val mockCookieAcquisitionRequestManager: CookieAcquisitionRequestManager =
        mockk {
            every {
                cookieAcquisitionRequestFlow
            } returns mutableCookieAcquisitionRequestFlow
            every { setPendingCookieAcquisition(data = any()) } just runs
        }

    private val fakeEnvironmentRepository = FakeEnvironmentRepository()

    @Test
    fun `initial state should be correct`() {
        val viewModel = createViewModel()
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    @Test
    fun `LaunchBrowserClick should emit LaunchBrowser event with hostname`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(CookieAcquisitionAction.LaunchBrowserClick)
                assertEquals(
                    CookieAcquisitionEvent.LaunchBrowser(
                        uri = DEFAULT_HOSTNAME,
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `LaunchBrowserClick should do nothing when no pending request`() =
        runTest {
            mutableCookieAcquisitionRequestFlow.value = null
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(CookieAcquisitionAction.LaunchBrowserClick)
                expectNoEvents()
            }
        }

    @Test
    fun `ContinueWithoutSyncingClick should clear pending acquisition`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.trySendAction(
                CookieAcquisitionAction.ContinueWithoutSyncingClick,
            )
            verify {
                mockCookieAcquisitionRequestManager
                    .setPendingCookieAcquisition(data = null)
            }
        }

    @Test
    fun `WhyAmISeeingThisClick should emit NavigateToHelp event`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(CookieAcquisitionAction.WhyAmISeeingThisClick)
            assertEquals(
                CookieAcquisitionEvent.NavigateToHelp(
                    uri = "https://bitwarden.com/help",
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `CookieCallbackResult Success should clear pending request`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                mutableCookieCallbackResultFlow.emit(
                    CookieCallbackResult.Success(cookies = mapOf("cookie" to "value")),
                )
                expectNoEvents()
            }
            verify {
                mockCookieAcquisitionRequestManager.setPendingCookieAcquisition(data = null)
            }
        }

    @Test
    fun `CookieCallbackResult MissingCookie should update state with error dialog`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.stateFlow.test {
                assertEquals(DEFAULT_STATE, awaitItem())
                mutableCookieCallbackResultFlow.emit(
                    CookieCallbackResult.MissingCookie,
                )
                assertEquals(
                    DEFAULT_STATE.copy(
                        dialogState = CookieAcquisitionDialogState.Error(
                            title = BitwardenString
                                .an_error_has_occurred
                                .asText(),
                            message = BitwardenString
                                .generic_error_message
                                .asText(),
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `DismissDialogClick should clear dialog state`() = runTest {
        val viewModel = createViewModel()
        // First trigger an error dialog
        mutableCookieCallbackResultFlow.emit(CookieCallbackResult.MissingCookie)

        viewModel.stateFlow.test {
            // Current state should have error dialog
            assert(awaitItem().dialogState != null)

            viewModel.trySendAction(CookieAcquisitionAction.DismissDialogClick)
            assertNull(awaitItem().dialogState)
        }
    }

    @Test
    fun `state should be restored from SavedStateHandle`() {
        val savedState = CookieAcquisitionState(
            environmentUrl = "https://custom.vault.com",
            dialogState = null,
        )
        val viewModel = createViewModel(state = savedState)
        assertEquals(savedState, viewModel.stateFlow.value)
    }

    @Test
    fun `initial state should use environment URL when no saved state`() {
        val viewModel = createViewModel(state = null)
        assertEquals(
            CookieAcquisitionState(
                environmentUrl = "https://vault.bitwarden.com",
                dialogState = null,
            ),
            viewModel.stateFlow.value,
        )
    }

    private fun createViewModel(
        state: CookieAcquisitionState? = DEFAULT_STATE,
    ): CookieAcquisitionViewModel =
        CookieAcquisitionViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf("state" to state),
            ),
            cookieAcquisitionRequestManager = mockCookieAcquisitionRequestManager,
            authRepository = mockAuthRepository,
            environmentRepository = fakeEnvironmentRepository,
        )
}

private const val DEFAULT_HOSTNAME = "https://example.com"

private val DEFAULT_COOKIE_ACQUISITION_REQUEST = CookieAcquisitionRequest(
    hostname = DEFAULT_HOSTNAME,
)

private val DEFAULT_STATE = CookieAcquisitionState(
    environmentUrl = "https://vault.bitwarden.us",
    dialogState = null,
)
