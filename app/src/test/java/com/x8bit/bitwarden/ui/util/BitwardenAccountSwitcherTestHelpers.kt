package com.x8bit.bitwarden.ui.util

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.x8bit.bitwarden.ui.platform.components.model.AccountSummary

private const val ACCOUNT = "Account"
private const val ADD_ACCOUNT = "Add account"

/**
 * Asserts that the account switcher is visible and displaying information for the given
 * [accountSummaries]. The existence of the "Add account" button can be asserted with
 * [isAddAccountButtonVisible].
 */
fun ComposeContentTestRule.assertSwitcherIsDisplayed(
    accountSummaries: List<AccountSummary>,
    isAddAccountButtonVisible: Boolean = true,
) {
    accountSummaries.forEach { accountSummary ->
        this.onNodeWithText(accountSummary.email).assertIsDisplayed()
    }

    if (isAddAccountButtonVisible) {
        this.onNodeWithText(ADD_ACCOUNT).assertIsDisplayed()
    } else {
        this.onNodeWithText(ADD_ACCOUNT).assertDoesNotExist()
    }
}

/**
 * Asserts that the account switcher is not visible by looking for information from the given
 * [accountSummaries] and by checking for the "Add account" item.
 */
fun ComposeContentTestRule.assertSwitcherIsNotDisplayed(
    accountSummaries: List<AccountSummary>,
) {
    accountSummaries.forEach { accountSummary ->
        this.onNodeWithText(accountSummary.email).assertDoesNotExist()
    }
    this.onNodeWithText(ADD_ACCOUNT).assertDoesNotExist()
}

/**
 * Clicks on the given [accountSummary] in the account switcher.
 */
fun ComposeContentTestRule.performAccountClick(
    accountSummary: AccountSummary,
) {
    this.onNodeWithText(accountSummary.email).performClick()
}

/**
 * Opens the account switcher.
 *
 * This is controlled by clicking on an element with the given [targetContentDescription].
 */
fun ComposeContentTestRule.performAccountIconClick(
    targetContentDescription: String = ACCOUNT,
) {
    this.onNodeWithContentDescription(targetContentDescription).performClick()
}

/**
 * Clicks on the "Add account" item. Note that this assumes the Account Switcher is currently
 * visible.
 */
fun ComposeContentTestRule.performAddAccountClick() {
    this.onNodeWithText(ADD_ACCOUNT).performClick()
}
