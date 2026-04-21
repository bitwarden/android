package com.bitwarden.ui.platform.components.badge

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import com.bitwarden.ui.platform.base.BaseComposeTest
import com.bitwarden.ui.platform.theme.BitwardenTheme
import org.junit.Test

class BitwardenStatusBadgeTest : BaseComposeTest() {

    @Test
    fun `success variant renders with label`() {
        setTestContent {
            BitwardenTheme {
                BitwardenStatusBadge(
                    label = "Active",
                    colors = BitwardenTheme.colorScheme.statusBadge.success,
                )
            }
        }
        composeTestRule.onNodeWithText("Active").assertIsDisplayed()
    }

    @Test
    fun `error variant renders with label`() {
        setTestContent {
            BitwardenTheme {
                BitwardenStatusBadge(
                    label = "Canceled",
                    colors = BitwardenTheme.colorScheme.statusBadge.error,
                )
            }
        }
        composeTestRule.onNodeWithText("Canceled").assertIsDisplayed()
    }

    @Test
    fun `warning variant renders with label`() {
        setTestContent {
            BitwardenTheme {
                BitwardenStatusBadge(
                    label = "Overdue payment",
                    colors = BitwardenTheme.colorScheme.statusBadge.warning,
                )
            }
        }
        composeTestRule.onNodeWithText("Overdue payment").assertIsDisplayed()
    }
}
