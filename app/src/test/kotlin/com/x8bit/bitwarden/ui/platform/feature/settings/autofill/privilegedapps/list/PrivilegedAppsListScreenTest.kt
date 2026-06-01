package com.x8bit.bitwarden.ui.platform.feature.settings.autofill.privilegedapps.list

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.assertNoDialogExists
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.privilegedapps.list.model.PrivilegedAppListItem
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Test

class PrivilegedAppsListScreenTest : BitwardenComposeTest() {

    private var onNavigateBackCalled = false
    private val mutableEventFlow = bufferedMutableSharedFlow<PrivilegedAppsListEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<PrivilegedAppsListViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setUp() {
        setContent {
            PrivilegedAppsListScreen(
                onNavigateBack = {
                    onNavigateBackCalled = true
                },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `on back click sends BackClick`() {
        composeTestRule
            .onNodeWithContentDescription("Back")
            .performClick()
        verify { viewModel.trySendAction(PrivilegedAppsListAction.BackClick) }
    }

    @Test
    fun `BackClick event should navigate back`() {
        mutableEventFlow.tryEmit(PrivilegedAppsListEvent.NavigateBack)
        assert(onNavigateBackCalled)
    }

    @Test
    fun `dialog is shown based on state`() {
        // Verify loading dialog is shown.
        mutableStateFlow.value = DEFAULT_STATE.copy(
            dialogState = PrivilegedAppsListState.DialogState.Loading,
        )
        composeTestRule
            .onNodeWithText("Loading")
            .assert(hasAnyAncestor(isDialog()))
            .assertExists()

        // Verify general dialog is shown.
        mutableStateFlow.value = DEFAULT_STATE.copy(
            dialogState = PrivilegedAppsListState.DialogState.General(message = "error".asText()),
        )
        composeTestRule
            .onNodeWithText("error")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        // Verify confirm delete trusted app dialog is shown.
        mutableStateFlow.value = DEFAULT_STATE.copy(
            dialogState = PrivilegedAppsListState.DialogState.ConfirmDeleteTrustedApp(
                createMockPrivilegedAppListItem(number = 1),
            ),
        )
        composeTestRule
            .onNodeWithText("Are you sure you want to stop trusting mockPackageName-1?")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        // Verify no dialog is shown.
        mutableStateFlow.value = DEFAULT_STATE.copy(
            dialogState = null,
        )
        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `General dialog Okay click sends DismissDialogClick`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            dialogState = PrivilegedAppsListState.DialogState.General(message = "error".asText()),
        )

        composeTestRule
            .onNodeWithText("Okay")
            .performClick()

        verify { viewModel.trySendAction(PrivilegedAppsListAction.DismissDialogClick) }
    }

    @Test
    fun `onConfirmDeleteTrustedAppClick sends UserTrustedAppDeleteConfirmClick`() {
        val app = createMockPrivilegedAppListItem(number = 1)
        mutableStateFlow.value = DEFAULT_STATE.copy(
            dialogState = PrivilegedAppsListState.DialogState.ConfirmDeleteTrustedApp(
                app = app,
            ),
        )
        composeTestRule
            .onNodeWithText("Okay")
            .performClick()

        verify {
            viewModel.trySendAction(
                PrivilegedAppsListAction.UserTrustedAppDeleteConfirmClick(app),
            )
        }
    }

    @Test
    fun `ConfirmDeleteTrustedApp dialog Cancel click sends DismissDialogClick`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            dialogState = PrivilegedAppsListState.DialogState.ConfirmDeleteTrustedApp(
                app = createMockPrivilegedAppListItem(number = 1),
            ),
        )

        composeTestRule
            .onNodeWithText("Cancel")
            .performClick()

        verify { viewModel.trySendAction(PrivilegedAppsListAction.DismissDialogClick) }
    }

    @Test
    fun `privileged app name displays correctly based on state`() {
        val installedApps = persistentListOf(
            createMockPrivilegedAppListItem(number = 1),
            createMockPrivilegedAppListItem(number = 2).copy(appName = null),
        )

        mutableStateFlow.value = DEFAULT_STATE.copy(
            installedApps = installedApps,
        )

        // Verify app name is shown if present.
        composeTestRule
            .onNodeWithText("mockAppName-1 (mockPackageName-1)")
            .assertIsDisplayed()

        // Verify package name is shown when app name is null.
        composeTestRule
            .onNodeWithText("mockPackageName-2")
            .assertIsDisplayed()
    }

    @Test
    fun `privileged app trust authority displays correctly based on state`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            installedApps = persistentListOf(
                createMockPrivilegedAppListItem(number = 1),
            ),
        )

        composeTestRule
            .onNodeWithText("Trusted by You")
            .assertIsDisplayed()

        mutableStateFlow.value = DEFAULT_STATE.copy(
            installedApps = persistentListOf(
                createMockPrivilegedAppListItem(number = 1).copy(
                    trustAuthority = PrivilegedAppListItem.PrivilegedAppTrustAuthority.COMMUNITY,
                ),
            ),
        )

        composeTestRule
            .onNodeWithText("Trusted by the Community")
            .assertIsDisplayed()

        mutableStateFlow.value = DEFAULT_STATE.copy(
            installedApps = persistentListOf(
                createMockPrivilegedAppListItem(number = 1).copy(
                    trustAuthority = PrivilegedAppListItem.PrivilegedAppTrustAuthority.GOOGLE,
                ),
            ),
        )
        composeTestRule
            .onNodeWithText("Trusted by Google")
            .assertIsDisplayed()
    }

    @Test
    fun `Installed apps header is shown based on state`() {

        composeTestRule
            .onNodeWithText("INSTALLED APPS", substring = true)
            .assertDoesNotExist()

        mutableStateFlow.value = DEFAULT_STATE.copy(
            installedApps = persistentListOf(
                createMockPrivilegedAppListItem(number = 1),
            ),
        )

        composeTestRule
            .onNodeWithText("INSTALLED APPS (1)")
            .assertIsDisplayed()
    }

    @Test
    fun `All trusted apps header is shown based on state`() {
        composeTestRule
            .onNodeWithText("All trusted apps")
            .assertDoesNotExist()

        mutableStateFlow.value = DEFAULT_STATE.copy(
            notInstalledApps = persistentListOf(
                createMockPrivilegedAppListItem(number = 1),
            ),
        )

        composeTestRule
            .onNodeWithText("All trusted apps")
            .assertIsDisplayed()
    }

    @Test
    fun `user trusted apps display correctly`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            notInstalledApps = persistentListOf(
                createMockPrivilegedAppListItem(number = 1),
            ),
        )

        composeTestRule
            .onNodeWithText("All trusted apps")
            .performClick()

        composeTestRule
            .onNodeWithText("TRUSTED BY YOU (1)")
            .performScrollTo()
            .assertIsDisplayed()

        mutableStateFlow.value = DEFAULT_STATE.copy(
            notInstalledApps = persistentListOf(
                createMockPrivilegedAppListItem(number = 1).copy(
                    trustAuthority = PrivilegedAppListItem.PrivilegedAppTrustAuthority.COMMUNITY,
                ),
            ),
        )

        composeTestRule
            .onNodeWithText("TRUSTED BY YOU (1)")
            .assertDoesNotExist()
    }

    @Test
    fun `community trusted apps display correctly`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            notInstalledApps = persistentListOf(
                createMockPrivilegedAppListItem(
                    number = 1,
                    trustAuthority = PrivilegedAppListItem.PrivilegedAppTrustAuthority.COMMUNITY,
                ),
            ),
        )

        composeTestRule
            .onNodeWithText("All trusted apps")
            .performClick()

        composeTestRule
            .onNodeWithText("TRUSTED BY THE COMMUNITY (1)")
            .assertIsDisplayed()

        mutableStateFlow.value = DEFAULT_STATE.copy(
            notInstalledApps = persistentListOf(
                createMockPrivilegedAppListItem(
                    number = 1,
                ),
            ),
        )

        composeTestRule
            .onNodeWithText("TRUSTED BY THE COMMUNITY (1)")
            .assertDoesNotExist()
    }

    @Test
    fun `google trusted apps display correctly`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            notInstalledApps = persistentListOf(
                createMockPrivilegedAppListItem(
                    number = 1,
                    trustAuthority = PrivilegedAppListItem.PrivilegedAppTrustAuthority.GOOGLE,
                ),
            ),
        )

        composeTestRule
            .onNodeWithText("All trusted apps")
            .performClick()

        composeTestRule
            .onNodeWithText("TRUSTED BY GOOGLE (1)")
            .assertIsDisplayed()

        mutableStateFlow.value = DEFAULT_STATE.copy(
            notInstalledApps = persistentListOf(
                createMockPrivilegedAppListItem(
                    number = 1,
                ),
            ),
        )

        composeTestRule
            .onNodeWithText("TRUSTED BY GOOGLE (1)")
            .assertDoesNotExist()
    }

    @Test
    fun `Delete icon displays based on state`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            installedApps = persistentListOf(
                createMockPrivilegedAppListItem(number = 1),
            ),
        )

        composeTestRule
            .onNodeWithContentDescription("Delete", substring = true)
            .assertIsDisplayed()

        mutableStateFlow.value = DEFAULT_STATE.copy(
            installedApps = persistentListOf(
                createMockPrivilegedAppListItem(
                    number = 1,
                    trustAuthority = PrivilegedAppListItem.PrivilegedAppTrustAuthority.COMMUNITY,
                ),
            ),
        )

        composeTestRule
            .onNodeWithContentDescription("Delete", substring = true)
            .assertDoesNotExist()
    }

    @Test
    fun `onDeleteClick sends UserTrustedAppDeleteClick`() {
        val app = createMockPrivilegedAppListItem(number = 1)

        mutableStateFlow.value = DEFAULT_STATE.copy(
            installedApps = persistentListOf(app),
        )

        composeTestRule
            .onNodeWithContentDescription("Delete", substring = true)
            .performClick()

        verify { viewModel.trySendAction(PrivilegedAppsListAction.UserTrustedAppDeleteClick(app)) }
    }
}

private fun createMockPrivilegedAppListItem(
    number: Int,
    appName: String? = "mockAppName-$number",
    packageName: String = "mockPackageName-$number",
    signature: String = "mockSignature-$number",
    trustAuthority: PrivilegedAppListItem.PrivilegedAppTrustAuthority =
        PrivilegedAppListItem.PrivilegedAppTrustAuthority.USER,
): PrivilegedAppListItem = PrivilegedAppListItem(
    appName = appName,
    packageName = packageName,
    signature = signature,
    trustAuthority = trustAuthority,
)

private val DEFAULT_STATE = PrivilegedAppsListState(
    installedApps = persistentListOf(),
    notInstalledApps = persistentListOf(),
    dialogState = null,
)
