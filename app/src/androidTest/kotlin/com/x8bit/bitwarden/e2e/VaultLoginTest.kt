package com.x8bit.bitwarden.e2e

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.x8bit.bitwarden.e2e.flows.goToLoginScreen
import com.x8bit.bitwarden.e2e.flows.logInWithMasterPassword
import com.x8bit.bitwarden.e2e.robots.login
import com.x8bit.bitwarden.e2e.robots.navBar
import com.x8bit.bitwarden.e2e.robots.vault
import com.x8bit.bitwarden.e2e.robots.vaultItem
import com.x8bit.bitwarden.e2e.seeder.LoginItem
import com.x8bit.bitwarden.e2e.seeder.addLoginItem
import com.x8bit.bitwarden.e2e.seeder.createFreeUser
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Native ports of the Appium/C# login and vault smoke tests, written with robots (`robots/`).
 * Test names carry the Testmo case ID (backtick names with spaces can't be dexed below API 30, and
 * minSdk is 29).
 *
 * Robots don't return the next screen; each test opens the robot for the screen it expects, so the
 * same actions can lead to different screens without a method per outcome.
 */
@RunWith(AndroidJUnit4::class)
class VaultLoginTest : BaseE2eTest() {
    /**
     * Testmo 22270: a seeded user logs in and reaches the vault.
     */
    @Test
    fun test_22270_logIn() {
        val account = seeder.createFreeUser()

        with(composeRule) {
            goToLoginScreen(serverUrl = config.serverUrl, email = account.email)

            login {
                enterMasterPassword(password = account.password)
                tapLogInWithMasterPassword()

                vault {
                    assertVaultIsDisplayed()
                }
                navBar {
                    assertSettingsTabIsDisplayed()
                }
            }
        }
    }

    /**
     * Testmo 22279: a seeded login item shows its username, password and URI.
     */
    @Test
    fun test_22279_readUsernameAndPassword() {
        val account = seeder.createFreeUser()
        val item = LoginItem(
            name = "Cipher-22279",
            username = "Cipher-22279-User",
            password = "Cipher-22279-Password",
            uri = "www.test.com",
        )
        seeder.addLoginItem(account, item)

        with(composeRule) {
            logInWithMasterPassword(serverUrl = config.serverUrl, account = account)

            vault {
                openItem(name = item.name)

                vaultItem {
                    assertUsername(expected = item.username)
                    assertPasswordIsHidden(password = item.password)

                    tapShowPassword()
                    assertPasswordIsShown(expected = item.password)
                    assertUri(expected = item.uri)
                }
            }
        }
    }
}
