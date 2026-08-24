package com.bitwarden.ui.platform.components.field

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithText
import com.bitwarden.ui.platform.base.BaseComposeTest
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.theme.BitwardenTheme
import org.junit.Test

class BitwardenPasswordFieldTest : BaseComposeTest() {

    @Test
    fun `read only hidden password uses fixed length mask`() {
        val password = "correct horse battery staple"

        setTestContent {
            BitwardenTheme {
                BitwardenPasswordField(
                    label = "Password",
                    value = password,
                    showPassword = false,
                    showPasswordChange = { },
                    onValueChange = { },
                    readOnly = true,
                    useFixedLengthMask = true,
                    cardStyle = CardStyle.Full,
                )
            }
        }

        composeTestRule
            .onNodeWithText("Password")
            .assertTextEquals("Password", "••••••••")
    }

    @Test
    fun `read only visible password shows actual value`() {
        val password = "correct horse battery staple"

        setTestContent {
            BitwardenTheme {
                BitwardenPasswordField(
                    label = "Password",
                    value = password,
                    showPassword = true,
                    showPasswordChange = { },
                    onValueChange = { },
                    readOnly = true,
                    useFixedLengthMask = true,
                    cardStyle = CardStyle.Full,
                )
            }
        }

        composeTestRule
            .onNodeWithText("Password")
            .assertTextEquals("Password", password)
    }

    @Test
    fun `read only hidden password preserves value length by default`() {
        setTestContent {
            BitwardenTheme {
                BitwardenPasswordField(
                    label = "Password",
                    value = "12345",
                    showPassword = false,
                    showPasswordChange = { },
                    onValueChange = { },
                    readOnly = true,
                    cardStyle = CardStyle.Full,
                )
            }
        }

        composeTestRule
            .onNodeWithText("Password")
            .assertTextEquals("Password", "•••••")
    }

    @Test
    fun `non interactable hidden password uses fixed length mask`() {
        setTestContent {
            BitwardenTheme {
                BitwardenHiddenPasswordField(
                    label = "Password",
                    value = "correct horse battery staple",
                    cardStyle = CardStyle.Full,
                )
            }
        }

        composeTestRule
            .onNodeWithText("Password")
            .assertTextEquals("Password", "••••••••")
    }
}
