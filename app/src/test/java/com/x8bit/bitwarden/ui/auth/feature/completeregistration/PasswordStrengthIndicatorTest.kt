package com.x8bit.bitwarden.ui.auth.feature.completeregistration

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import org.junit.Test

class PasswordStrengthIndicatorTest : BaseComposeTest() {

    @Suppress("MaxLineLength")
    @Test
    fun `PasswordStrengthIndicator with minimum character count met displays minimum character count`() {
        composeTestRule.setContent {
            PasswordStrengthIndicator(
                state = PasswordStrengthState.WEAK_3,
                currentCharacterCount = 12,
                minimumCharacterCount = 12,
            )
        }

        composeTestRule
            .onNodeWithText("characters", substring = true)
            .assertExists()
            .assertIsDisplayed()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `PasswordStrengthIndicator with no minimum character count met does not minimum character count`() {
        composeTestRule.setContent {
            PasswordStrengthIndicator(
                state = PasswordStrengthState.WEAK_3,
                currentCharacterCount = 12,
                minimumCharacterCount = null,
            )
        }

        composeTestRule
            .onNodeWithText("characters", substring = true)
            .assertDoesNotExist()
    }
}
