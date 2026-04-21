package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.managedevices

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.core.data.util.advanceTimeByAndRunCurrent
import com.bitwarden.core.util.isBuildVersionAtLeast
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import com.x8bit.bitwarden.ui.platform.manager.permissions.FakePermissionManager
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ManageDevicesScreenTest : BitwardenComposeTest() {

    private var onNavigateBackCalled = false
    private var navigateToLoginApprovalFingerprint: String? = null

    private val mutableEventFlow = bufferedMutableSharedFlow<ManageDevicesEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<ManageDevicesViewModel> {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
        every { trySendAction(any()) } just runs
    }
    private val permissionsManager = FakePermissionManager().apply {
        checkPermissionResult = false
        shouldShowRequestRationale = false
    }

    @Before
    fun setUp() {
        mockkStatic(::isBuildVersionAtLeast)
        every { isBuildVersionAtLeast(any()) } returns true
        setContent(
            permissionsManager = permissionsManager,
        ) {
            ManageDevicesScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                onNavigateToLoginApproval = { fingerprint ->
                    navigateToLoginApprovalFingerprint = fingerprint
                },
                viewModel = viewModel,
            )
        }
    }

    @After
    fun tearDown() {
        unmockkStatic(::isBuildVersionAtLeast)
    }

    @Test
    fun `on NavigateBack event should call onNavigateBack`() {
        mutableEventFlow.tryEmit(ManageDevicesEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `on NavigateToLoginApproval should call onNavigateToLoginApproval with fingerprint`() {
        val fingerprint = "mock-fingerprint"
        mutableEventFlow.tryEmit(ManageDevicesEvent.NavigateToLoginApproval(fingerprint))
        assertEquals(fingerprint, navigateToLoginApprovalFingerprint)
    }

    @Test
    fun `on ShowSnackbar event should display snackbar message`() {
        val message = "Test snackbar message"
        val data = BitwardenSnackbarData(message = message.asText())
        composeTestRule.onNodeWithText(message).assertDoesNotExist()
        mutableEventFlow.tryEmit(ManageDevicesEvent.ShowSnackbar(data))
        composeTestRule.onNodeWithText(message).assertIsDisplayed()
    }

    @Test
    fun `on close button click should send CloseClick action`() {
        // Hide bottom sheet so only the AppBar close button exists
        mutableStateFlow.value = DEFAULT_STATE.copy(internalHideBottomSheet = true)
        composeTestRule
            .onNodeWithContentDescription("Close")
            .performClick()
        verify { viewModel.trySendAction(ManageDevicesAction.CloseClick) }
    }

    @Test
    fun `loading state should display loading content`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            viewState = ManageDevicesState.ViewState.Loading,
        )
        // Loading spinner is shown – no device cells
        composeTestRule.onNodeWithTag("LoginRequestCell").assertDoesNotExist()
    }

    @Test
    fun `error state should display error message`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            viewState = ManageDevicesState.ViewState.Error,
            internalHideBottomSheet = true,
        )
        composeTestRule
            .onNodeWithText(
                "We were unable to process your request. " +
                    "Please try again or contact us.",
            )
            .assertIsDisplayed()
    }

    @Test
    fun `content state with current session device should display current session indicator`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            viewState = ManageDevicesState.ViewState.Content(
                items = listOf(
                    DEFAULT_DEVICE_ITEM.copy(status = DeviceSessionStatus.Current),
                ),
            ),
        )
        composeTestRule.onNodeWithText("Current session").assertIsDisplayed()
    }

    @Test
    fun `content state with trusted device should display trusted label`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            viewState = ManageDevicesState.ViewState.Content(
                items = listOf(
                    DEFAULT_DEVICE_ITEM.copy(
                        status = DeviceSessionStatus.None,
                        isTrusted = true,
                    ),
                ),
            ),
        )
        composeTestRule.onNodeWithText("Trusted").assertIsDisplayed()
    }

    @Test
    fun `content state with pending device should display pending request indicator`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            viewState = ManageDevicesState.ViewState.Content(
                items = listOf(
                    DEFAULT_DEVICE_ITEM.copy(
                        status = DeviceSessionStatus.Pending,
                        fingerprintPhrase = "mock-fingerprint",
                    ),
                ),
            ),
        )
        composeTestRule.onNodeWithText("Pending request").assertIsDisplayed()
    }

    @Test
    fun `clicking pending request row should send PendingRequestRowClick action`() {
        val fingerprint = "test-fingerprint"
        mutableStateFlow.value = DEFAULT_STATE.copy(
            viewState = ManageDevicesState.ViewState.Content(
                items = listOf(
                    DEFAULT_DEVICE_ITEM.copy(
                        status = DeviceSessionStatus.Pending,
                        fingerprintPhrase = fingerprint,
                    ),
                ),
            ),
        )
        composeTestRule
            .onNodeWithTag("LoginRequestCell")
            .performClick()
        verify { viewModel.trySendAction(ManageDevicesAction.PendingRequestRowClick(fingerprint)) }
    }

    @Test
    fun `on skip for now click should send HideBottomSheet action`() {
        composeTestRule
            .onNodeWithText("Skip for now")
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.OnClick)
        dispatcher.advanceTimeByAndRunCurrent(delayTimeMillis = 1000L)
        verify { viewModel.trySendAction(ManageDevicesAction.HideBottomSheet) }
    }

    @Test
    fun `bottom sheet should not show when permission already granted`() {
        permissionsManager.checkPermissionResult = true
        mutableStateFlow.value = DEFAULT_STATE.copy(devicesLoaded = true)
        composeTestRule.onNodeWithText("Skip for now").assertDoesNotExist()
    }

    @Test
    fun `content state should display device type name`() {
        val typeName = "Mobile - Android"
        mutableStateFlow.value = DEFAULT_STATE.copy(
            viewState = ManageDevicesState.ViewState.Content(
                items = listOf(
                    DEFAULT_DEVICE_ITEM.copy(typeName = typeName.asText()),
                ),
            ),
        )
        composeTestRule.onNodeWithText(typeName).assertIsDisplayed()
    }

    @Test
    fun `on lifecycle resume should send LifecycleResume action`() = runTest {
        // The LifecycleEventEffect fires ON_RESUME - verify it was called on setup
        verify { viewModel.trySendAction(ManageDevicesAction.LifecycleResume) }
    }
}

private val DEFAULT_DEVICE_ITEM = ManageDevicesState.ViewState.Content.DeviceItem(
    id = "device-1",
    name = "Test Device",
    typeName = "Mobile - Android".asText(),
    isTrusted = false,
    firstLoginDate = "Oct 27, 2023, 12:00:00 PM",
    lastActivityLabel = "Active today".asText(),
    status = DeviceSessionStatus.None,
    fingerprintPhrase = null,
)

private val DEFAULT_STATE = ManageDevicesState(
    authRequests = persistentListOf(),
    devices = persistentListOf(),
    viewState = ManageDevicesState.ViewState.Loading,
    isPullToRefreshSettingEnabled = false,
    isRefreshing = false,
    internalHideBottomSheet = false,
    isFdroid = false,
    devicesLoaded = false,
    authRequestsLoaded = false,
)
