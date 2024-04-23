package com.x8bit.bitwarden.ui.util

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
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
        this.onNode(
            hasTextExactly(
                *listOfNotNull(
                    accountSummary.email,
                    accountSummary.environmentLabel,
                    "locked".takeUnless { accountSummary.isVaultUnlocked },
                )
                    .toTypedArray(),
            ),
        )
            .assertIsDisplayed()
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
 * Asserts the "lock or logout" dialog is currently displayed with information from the given
 * [accountSummary].
 */
fun ComposeContentTestRule.assertLockOrLogoutDialogIsDisplayed(
    accountSummary: AccountSummary,
) {
    this
        .onNode(isDialog())
        .assertIsDisplayed()
    this
        .onAllNodesWithText("Lock")
        .filterToOne(hasAnyAncestor(isDialog()))
        .assertIsDisplayed()
    this
        .onAllNodesWithText("Log out")
        .filterToOne(hasAnyAncestor(isDialog()))
        .assertIsDisplayed()
    this
        .onAllNodesWithText(accountSummary.email, substring = true)
        .filterToOne(hasAnyAncestor(isDialog()))
        .assertIsDisplayed()
    this
        .onAllNodesWithText(accountSummary.environmentLabel, substring = true)
        .filterToOne(hasAnyAncestor(isDialog()))
        .assertIsDisplayed()
}

/**
 * Asserts the logout confirmation dialog is currently displayed with information from the given
 * [accountSummary].
 */
fun ComposeContentTestRule.assertLogoutConfirmationDialogIsDisplayed(
    accountSummary: AccountSummary,
) {
    this
        .onNode(isDialog())
        .assertIsDisplayed()
    this
        .onAllNodesWithText("Log out")
        .filterToOne(hasAnyAncestor(isDialog()))
        .assertIsDisplayed()
    this
        .onAllNodesWithText("Are you sure you want to log out?", substring = true)
        .filterToOne(hasAnyAncestor(isDialog()))
        .assertIsDisplayed()
    this
        .onAllNodesWithText(accountSummary.email, substring = true)
        .filterToOne(hasAnyAncestor(isDialog()))
        .assertIsDisplayed()
    this
        .onAllNodesWithText(accountSummary.environmentLabel, substring = true)
        .filterToOne(hasAnyAncestor(isDialog()))
        .assertIsDisplayed()
}

/**
 * Asserts the account removal confirmation dialog is currently displayed with information from
 * the given [accountSummary].
 */
fun ComposeContentTestRule.assertRemovalConfirmationDialogIsDisplayed(
    accountSummary: AccountSummary,
) {
    this
        .onNode(isDialog())
        .assertIsDisplayed()
    this
        .onAllNodesWithText("Remove account")
        .filterToOne(hasAnyAncestor(isDialog()))
        .assertIsDisplayed()
    this
        .onAllNodesWithText("Are you sure you want to remove this account?", substring = true)
        .filterToOne(hasAnyAncestor(isDialog()))
        .assertIsDisplayed()
    this
        .onAllNodesWithText(accountSummary.email, substring = true)
        .filterToOne(hasAnyAncestor(isDialog()))
        .assertIsDisplayed()
    this
        .onAllNodesWithText(accountSummary.environmentLabel, substring = true)
        .filterToOne(hasAnyAncestor(isDialog()))
        .assertIsDisplayed()
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
 * Long clicks on the given [accountSummary] in the account switcher.
 */
fun ComposeContentTestRule.performAccountLongClick(
    accountSummary: AccountSummary,
) {
    this.onNodeWithText(accountSummary.email).performTouchInput {
        this.longClick()
    }
}

/**
 * Clicks the "Lock" button in the "lock or logout" dialog.
 */
fun ComposeContentTestRule.performLockAccountClick() {
    this
        .onAllNodesWithText("Lock")
        .filterToOne(hasAnyAncestor(isDialog()))
        .performClick()
}

/**
 * Clicks the "log out" button in the "lock or logout" dialog.
 */
fun ComposeContentTestRule.performLogoutAccountClick() {
    this
        .onAllNodesWithText("Log out")
        .filterToOne(hasAnyAncestor(isDialog()))
        .performClick()
}

/**
 * Clicks the "Remove account" button in the "lock or logout" dialog.
 */
fun ComposeContentTestRule.performRemoveAccountClick() {
    this
        .onAllNodesWithText("Remove account")
        .filterToOne(hasAnyAncestor(isDialog()))
        .performClick()
}

/**
 * Clicks the "Yes" button in the account confirmation dialog to confirm the action.
 */
fun ComposeContentTestRule.performYesDialogButtonClick() {
    this
        .onAllNodesWithText("Yes")
        .filterToOne(hasAnyAncestor(isDialog()))
        .performClick()
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
