package com.x8bit.bitwarden.ui.vault.feature.pamnotificationprompt

import android.content.Intent
import android.provider.Settings
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.manager.IntentManager
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import com.x8bit.bitwarden.ui.platform.manager.permissions.FakePermissionManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.annotation.Config

class PamNotificationsSettingRowTest : BitwardenComposeTest() {

    private val permissionsManager = FakePermissionManager()
    private val intentManager = mockk<IntentManager> {
        every { packageName } returns "com.x8bit.bitwarden"
        every { startActivity(any()) } returns true
    }

    @Config(sdk = [33])
    @Test
    fun `state should be Off when the permission is not granted`() {
        permissionsManager.checkPermissionResult = false
        setRow()

        composeTestRule.onNodeWithText("Access request notifications").assertIsDisplayed()
        composeTestRule.onNodeWithText("Off").assertIsDisplayed()
    }

    @Config(sdk = [33])
    @Test
    fun `state should be On when the permission is granted`() {
        permissionsManager.checkPermissionResult = true
        setRow()

        composeTestRule.onNodeWithText("On").assertIsDisplayed()
    }

    @Config(sdk = [32])
    @Test
    fun `state should be On before Android 13 without checking the permission`() {
        permissionsManager.checkPermissionResult = false
        setRow()

        composeTestRule.onNodeWithText("On").assertIsDisplayed()
    }

    @Config(sdk = [33])
    @Test
    fun `click should request the permission when it is not granted and a prompt can be shown`() {
        permissionsManager.checkPermissionResult = false
        permissionsManager.getPermissionsResult = false
        permissionsManager.shouldShowRequestRationale = true
        setRow()

        composeTestRule.onNodeWithText("Access request notifications").performClick()

        assertTrue(permissionsManager.hasGetLauncherBeenCalled)
        verify(exactly = 0) { intentManager.startActivity(any()) }
    }

    @Config(sdk = [33])
    @Test
    fun `click should open the notification settings when the OS will not ask again`() {
        permissionsManager.checkPermissionResult = false
        permissionsManager.getPermissionsResult = false
        permissionsManager.shouldShowRequestRationale = false
        val intent = slot<Intent>()
        every { intentManager.startActivity(capture(intent)) } returns true
        setRow()

        composeTestRule.onNodeWithText("Access request notifications").performClick()

        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, intent.captured.action)
        assertEquals(
            "com.x8bit.bitwarden",
            intent.captured.getStringExtra(Settings.EXTRA_APP_PACKAGE),
        )
    }

    @Config(sdk = [33])
    @Test
    fun `click should open the notification settings when the permission is already granted`() {
        permissionsManager.checkPermissionResult = true
        val intent = slot<Intent>()
        every { intentManager.startActivity(capture(intent)) } returns true
        setRow()

        composeTestRule.onNodeWithText("Access request notifications").performClick()

        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, intent.captured.action)
    }

    @Test
    fun `isPamNotificationsEnabled should require the permission only when it is a runtime one`() {
        assertTrue(isPamNotificationsEnabled(true, true, true))
        assertFalse(isPamNotificationsEnabled(true, false, true))
        assertFalse(isPamNotificationsEnabled(true, true, false))
        assertTrue(isPamNotificationsEnabled(false, false, true))
        assertFalse(isPamNotificationsEnabled(false, false, false))
    }

    private fun setRow() {
        setContent(permissionsManager = permissionsManager, intentManager = intentManager) {
            PamNotificationsSettingRow(cardStyle = CardStyle.Full)
        }
    }
}
