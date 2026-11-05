package com.x8bit.bitwarden.e2e

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.x8bit.bitwarden.e2e.robots.goToLoginScreen
import com.x8bit.bitwarden.e2e.robots.logInWithMasterPassword
import com.x8bit.bitwarden.e2e.robots.login
import com.x8bit.bitwarden.e2e.robots.vault
import com.x8bit.bitwarden.e2e.robots.vaultItem
import com.x8bit.bitwarden.e2e.seeder.LoginItem
import com.x8bit.bitwarden.e2e.seeder.addLoginItem
import com.x8bit.bitwarden.e2e.seeder.createFreeUser
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The same smoke tests as [VaultLoginTest], written with robots (`robots/`) instead of chained
 * screen objects (`screens/`), for comparison. Shares [BaseE2eTest]'s rule, config and seeding.
 *
 * Robots don't return the next screen; each test opens the robot for the screen it expects. That
 * lets the same login actions lead to the vault or to an error without a method per outcome
 * (see [logIn_withWrongMasterPassword_showsErrorDialog]).
 */
@RunWith(AndroidJUnit4::class)
class VaultLoginRobotTest : BaseE2eTest() {
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
            }
        }
    }

    /**
     * Not yet a Testmo case: a wrong master password keeps the user on the Login screen with an
     * error. Same login actions as [test_22270_logIn]; only the expected outcome differs.
     */
    @Test
    fun logIn_withWrongMasterPassword_showsErrorDialog() {
        val account = seeder.createFreeUser()

        with(composeRule) {
            goToLoginScreen(serverUrl = config.serverUrl, email = account.email)

            login {
                enterMasterPassword(password = "${account.password}-wrong")
                tapLogInWithMasterPassword()

                assertErrorDialogIsDisplayed()
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
