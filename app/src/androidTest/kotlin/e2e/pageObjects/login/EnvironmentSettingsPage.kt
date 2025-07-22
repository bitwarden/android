package e2e.pageObjects.login

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.x8bit.bitwarden.e2e.pages.LoginPage
import e2e.pageObjects.Page

class EnvironmentSettingsPage(composeTestRule: ComposeTestRule) : Page(composeTestRule) {

    private val serverUrlField by lazy { getElement("ServerUrlEntry") }
    private val saveButton by lazy { getElement("SaveButton") }

    fun setupEnvironment(url: String): LoginPage {
        serverUrlField
            .performClick()
            .performTextInput(url)
        saveButton.performClick()
        return LoginPage(composeTestRule)
    }
}
