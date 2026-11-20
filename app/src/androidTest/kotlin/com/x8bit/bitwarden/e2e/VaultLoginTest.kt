package com.x8bit.bitwarden.e2e

import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.x8bit.bitwarden.e2e.seeder.LoginItem
import com.x8bit.bitwarden.e2e.seeder.addLoginItem
import com.x8bit.bitwarden.e2e.seeder.createFreeUser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Native ports of the Appium/C# login and vault smoke tests. Test names carry the Testmo case ID
 * (backtick names with spaces can't be dexed below API 30, and minSdk is 29).
 */
@RunWith(AndroidJUnit4::class)
class VaultLoginTest : BaseE2eTest() {
    /**
     * Testmo 22270: a seeded user logs in and reaches the vault.
     */
    @Test
    fun test_22270_logIn() {
        val account = seeder.createFreeUser()

        val vault = logIn(account)

        vault.settingsTab.assertIsDisplayed()
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

        val itemScreen = logIn(account).openItem(item.name)

        assertEquals(item.username, itemScreen.username())
        assertNotEquals(item.password, itemScreen.displayedPassword())
        assertEquals(item.password, itemScreen.revealPassword().displayedPassword())
        assertEquals(item.uri, itemScreen.uri())
    }
}
