package com.bitwarden.ui.platform.components.card

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.bitwarden.ui.platform.base.BaseComposeTest
import com.bitwarden.ui.platform.theme.BitwardenTheme
import org.junit.Test

class BitwardenActionCardTest : BaseComposeTest() {

    @Test
    fun `action button content description defaults to actionText`() {
        setTestContent {
            BitwardenTheme {
                BitwardenActionCard(
                    cardTitle = "Title",
                    actionText = "Learn more",
                    onActionClick = {},
                )
            }
        }
        composeTestRule
            .onNodeWithContentDescription(label = "Learn more")
            .assertIsDisplayed()
    }

    @Test
    fun `action button announces external-link affordance when isExternalLink is true`() {
        setTestContent {
            BitwardenTheme {
                BitwardenActionCard(
                    cardTitle = "Title",
                    actionText = "Learn more",
                    isExternalLink = true,
                    onActionClick = {},
                )
            }
        }
        composeTestRule
            .onNodeWithContentDescription(label = "Learn more, External link")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = "Learn more")
            .assertIsDisplayed()
    }
}
