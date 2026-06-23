package com.x8bit.bitwarden.ui.platform.feature.localnetworkaccess

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.manager.util.startAppSettingsActivity
import com.bitwarden.ui.util.assertNoDialogExists
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import com.x8bit.bitwarden.ui.platform.manager.permissions.FakePermissionManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LocalNetworkAccessScreenTest : BitwardenComposeTest() {

    private var onDismissCalled: Boolean = false

    private val mutableStateFlow = MutableStateFlow(value = LocalNetworkAccessState)
    private val mutableEventFlow = bufferedMutableSharedFlow<LocalNetworkAccessEvent>()
    private val viewModel = mockk<LocalNetworkAccessViewModel>(relaxed = true) {
        every { stateFlow } returns mutableStateFlow
        every { eventFlow } returns mutableEventFlow
    }
    private val intentManager = mockk<IntentManager> {
        every { startActivity(intent = any()) } returns true
    }
    private val permissionsManager = FakePermissionManager()

    @Before
    fun setup() {
        setContent(
            intentManager = intentManager,
            permissionsManager = permissionsManager,
        ) {
            LocalNetworkAccessScreen(
                onDismiss = { onDismissCalled = true },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `screen displays title, description, and buttons`() {
        composeTestRule
            .onNodeWithText(text = "Access your local network")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(
                text = "Bitwarden needs local network access to sync with your server. Without " +
                    "this permission, the app won’t be able to connect.",
            )
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = "Enable local network access")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = "Ask again later")
            .assertIsDisplayed()
    }

    @Test
    fun `system back press sends CloseClick action`() {
        backDispatcher?.onBackPressed()
        verify { viewModel.trySendAction(LocalNetworkAccessAction.CloseClick) }
    }

    @Test
    fun `Ask again later click sends ContinueWithoutPermissionClick action`() {
        composeTestRule
            .onNodeWithText(text = "Ask again later")
            .performClick()
        verify { viewModel.trySendAction(LocalNetworkAccessAction.ContinueWithoutPermissionClick) }
    }

    @Test
    fun `Enable local network access click with permission granted sends CloseClick action`() {
        permissionsManager.getPermissionsResult = true
        composeTestRule
            .onNodeWithText(text = "Enable local network access")
            .performClick()
        verify { viewModel.trySendAction(LocalNetworkAccessAction.CloseClick) }
    }

    @Test
    fun `Enable local network access click with permission denied shows dialog`() {
        permissionsManager.getPermissionsResult = false
        composeTestRule.assertNoDialogExists()
        composeTestRule
            .onNodeWithText(text = "Enable local network access")
            .performClick()
        composeTestRule
            .onNodeWithText(
                text = "Without this permission, Bitwarden won’t be able to connect and sync " +
                    "with your server. You can enable this in your device settings.",
            )
            .assertIsDisplayed()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `Enable local network access click with permission denied and rationale shows does not show dialog`() {
        permissionsManager.getPermissionsResult = false
        permissionsManager.shouldShowRequestRationale = true
        composeTestRule.assertNoDialogExists()
        composeTestRule
            .onNodeWithText(text = "Enable local network access")
            .performClick()
        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `dialog Go to settings click sends SettingsClick and hides dialog`() {
        permissionsManager.getPermissionsResult = false
        composeTestRule
            .onNodeWithText(text = "Enable local network access")
            .performClick()
        composeTestRule
            .onAllNodesWithText(text = "Go to settings")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify { viewModel.trySendAction(LocalNetworkAccessAction.SettingsClick) }
        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `dialog No thanks click hides dialog`() {
        permissionsManager.getPermissionsResult = false
        composeTestRule
            .onNodeWithText(text = "Enable local network access")
            .performClick()
        composeTestRule
            .onAllNodesWithText(text = "No thanks")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `NavigateBack event calls onDismiss`() {
        mutableEventFlow.tryEmit(LocalNetworkAccessEvent.NavigateBack)
        assertTrue(onDismissCalled)
    }

    @Test
    fun `NavigateToSettings event calls startAppSettingsActivity`() {
        mockkStatic(IntentManager::startAppSettingsActivity) {
            every { intentManager.startAppSettingsActivity() } returns true
            mutableEventFlow.tryEmit(LocalNetworkAccessEvent.NavigateToSettings)
            verify(exactly = 1) { intentManager.startAppSettingsActivity() }
        }
    }
}
