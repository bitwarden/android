package e2e.pageObjects.login

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.x8bit.bitwarden.e2e.pages.LoginPage
import e2e.pageObjects.Page
import e2e.pageObjects.vault.VaultPage

class MainPage(composeTestRule: ComposeTestRule) : Page(composeTestRule) {

    // UI Elements
    private val loginButton by lazy { getElement("ChooseLoginButton") }
    private val createAccountButton by lazy { getElement("ChooseAccountCreationButton") }

    fun startLogin(): LoginPage {
        loginButton.performClick()
        return LoginPage(composeTestRule)
    }
}
