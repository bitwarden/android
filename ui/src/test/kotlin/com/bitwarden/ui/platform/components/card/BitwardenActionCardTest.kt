package com.bitwarden.ui.platform.components.card

import androidx.compose.ui.test.assertIsDisplayed
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
}
