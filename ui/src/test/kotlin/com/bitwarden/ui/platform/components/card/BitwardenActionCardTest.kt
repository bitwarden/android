package com.bitwarden.ui.platform.components.card

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.bitwarden.ui.platform.base.BaseComposeTest
import com.bitwarden.ui.platform.components.button.model.BitwardenButtonData
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.bitwarden.ui.util.asText
import org.junit.Test

class BitwardenActionCardTest : BaseComposeTest() {

    @Test
    fun `action button content description defaults to the action button label`() {
        setTestContent {
            BitwardenTheme {
                BitwardenActionCard(
                    cardTitle = "Title",
                    actionButton = BitwardenButtonData(
                        label = "Learn more".asText(),
                        onClick = {},
                    ),
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
                    actionButton = BitwardenButtonData(
                        label = "Learn more".asText(),
                        onClick = {},
                        isExternalLink = true,
                    ),
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

    @Test
    fun `no buttons are displayed when neither button is provided`() {
        setTestContent {
            BitwardenTheme {
                BitwardenActionCard(
                    cardTitle = "Title",
                    cardSubtitle = "Subtitle",
                )
            }
        }
        composeTestRule
            .onNodeWithText(text = "Subtitle")
            .assertIsDisplayed()
        composeTestRule
            .onNode(matcher = hasClickAction())
            .assertDoesNotExist()
    }

    @Test
    fun `secondary button is displayed when the action button is omitted`() {
        setTestContent {
            BitwardenTheme {
                BitwardenActionCard(
                    cardTitle = "Title",
                    secondaryButton = BitwardenButtonData(
                        label = "Learn more".asText(),
                        onClick = {},
                    ),
                )
            }
        }
        composeTestRule
            .onNodeWithText(text = "Learn more")
            .assertIsDisplayed()
    }
}
