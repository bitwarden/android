package com.x8bit.bitwarden.ui.platform.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.lowercaseWithCurrentLocal
import com.x8bit.bitwarden.ui.platform.base.util.toSafeOverlayColor
import com.x8bit.bitwarden.ui.platform.base.util.toUnscaledTextUnit
import com.x8bit.bitwarden.ui.platform.components.model.AccountSummary
import com.x8bit.bitwarden.ui.vault.feature.vault.util.iconRes
import com.x8bit.bitwarden.ui.vault.feature.vault.util.initials
import com.x8bit.bitwarden.ui.vault.feature.vault.util.supportingTextResOrNull
import kotlinx.collections.immutable.ImmutableList

/**
 * The maximum number of accounts before the "Add account" button will be hidden to prevent the user
 * from adding any more.
 */
private const val MAXIMUM_ACCOUNT_LIMIT = 5

/**
 * An account switcher that will slide down inside whatever parent is it placed in and add a
 * a scrim via a [BitwardenAnimatedScrim] to all content below it (but not above it). Additional
 * [BitwardenAnimatedScrim] may be manually placed over other components that might not be covered
 * by the internal one.
 *
 * Note that this is intended to be used in conjunction with screens containing a top app bar but
 * should be placed with the screen's content and not with the bar itself.
 *
 * @param isVisible Whether or not this component is visible. Changing this value will animate the
 * component in or out of view.
 * @param accountSummaries The accounts to display in the switcher.
 * @param onSwitchAccountClick A callback when an account is clicked indicating that the account
 * should be switched to.
 * @param onLockAccountClick A callback when an account is clicked indicating that the account
 * should be locked.
 * @param onLogoutAccountClick A callback when an account is clicked indicating that the account
 * should be logged out.
 * @param onAddAccountClick A callback when the Add Account row is clicked.
 * @param onDismissRequest A callback when the component requests to be dismissed. This is triggered
 * whenever the user clicks on the scrim or any of the switcher items.
 * @param isAddAccountAvailable Whether or not the "Add account" button is available. Note that even
 * when `true`, this button may be hidden when there are more than [MAXIMUM_ACCOUNT_LIMIT] accounts
 * present.
 * @param modifier A [Modifier] for the composable.
 * @param topAppBarScrollBehavior Used to derive the background color of the content and keep it in
 * sync with the associated app bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BitwardenAccountSwitcher(
    isVisible: Boolean,
    accountSummaries: ImmutableList<AccountSummary>,
    onSwitchAccountClick: (AccountSummary) -> Unit,
    onLockAccountClick: (AccountSummary) -> Unit,
    onLogoutAccountClick: (AccountSummary) -> Unit,
    onAddAccountClick: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    isAddAccountAvailable: Boolean = true,
    topAppBarScrollBehavior: TopAppBarScrollBehavior,
) {
    // Track the actual visibility (according to the internal transitions) so that we know when we
    // can safely show dialogs.
    var isVisibleActual by remember { mutableStateOf(isVisible) }

    var lockOrLogoutAccount by remember { mutableStateOf<AccountSummary?>(null) }
    if (lockOrLogoutAccount != null && !isVisibleActual) {
        LockOrLogoutDialog(
            accountSummary = requireNotNull(lockOrLogoutAccount),
            onDismissRequest = { lockOrLogoutAccount = null },
            onLockAccountClick = onLockAccountClick,
            onLogoutAccountClick = onLogoutAccountClick,
        )
    }

    Box(modifier = modifier) {
        BitwardenAnimatedScrim(
            isVisible = isVisible,
            onClick = onDismissRequest,
            modifier = Modifier
                .fillMaxSize(),
        )
        AnimatedAccountSwitcher(
            isVisible = isVisible,
            accountSummaries = accountSummaries,
            onSwitchAccountClick = {
                onDismissRequest()
                onSwitchAccountClick(it)
            },
            onSwitchAccountLongClick = {
                onDismissRequest()
                lockOrLogoutAccount = it
            },
            onAddAccountClick = {
                onDismissRequest()
                onAddAccountClick()
            },
            isAddAccountAvailable = isAddAccountAvailable,
            topAppBarScrollBehavior = topAppBarScrollBehavior,
            currentAnimationState = { isVisibleActual = it },
            modifier = Modifier
                .fillMaxWidth(),
        )
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalAnimationApi::class,
)
@Composable
private fun AnimatedAccountSwitcher(
    isVisible: Boolean,
    accountSummaries: ImmutableList<AccountSummary>,
    onSwitchAccountClick: (AccountSummary) -> Unit,
    onSwitchAccountLongClick: (AccountSummary) -> Unit,
    onAddAccountClick: () -> Unit,
    isAddAccountAvailable: Boolean,
    modifier: Modifier = Modifier,
    topAppBarScrollBehavior: TopAppBarScrollBehavior,
    currentAnimationState: (isVisible: Boolean) -> Unit,
) {
    val expandedColor = MaterialTheme.colorScheme.surface
    val collapsedColor = MaterialTheme.colorScheme.surfaceContainer
    val transition = updateTransition(
        targetState = isVisible,
        label = "AnimatedAccountSwitcher",
    )
        .also { currentAnimationState(it.currentState) }
    transition.AnimatedVisibility(
        visible = { it },
        enter = slideInVertically { -it },
        exit = slideOutVertically { -it },
    ) {
        LazyColumn(
            modifier = modifier
                // To prevent going all the way up to the bottom of the screen, we'll add some small
                // bottom padding.
                .padding(bottom = 24.dp)
                // Match the color of the switcher the different states of the app bar.
                .drawBehind {
                    val progressFraction = if (topAppBarScrollBehavior.isPinned) {
                        topAppBarScrollBehavior.state.overlappedFraction
                    } else {
                        topAppBarScrollBehavior.state.collapsedFraction
                    }
                    val contentBackgroundColor =
                        lerp(
                            start = expandedColor,
                            stop = collapsedColor,
                            fraction = progressFraction,
                        )
                    drawRect(contentBackgroundColor)
                },
        ) {
            items(accountSummaries) { accountSummary ->
                AccountSummaryItem(
                    accountSummary = accountSummary,
                    onSwitchAccountClick = onSwitchAccountClick,
                    onSwitchAccountLongClick = onSwitchAccountLongClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
            if (accountSummaries.size < MAXIMUM_ACCOUNT_LIMIT && isAddAccountAvailable) {
                item {
                    AddAccountItem(
                        onClick = onAddAccountClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AccountSummaryItem(
    accountSummary: AccountSummary,
    onSwitchAccountClick: (AccountSummary) -> Unit,
    onSwitchAccountLongClick: (AccountSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(color = MaterialTheme.colorScheme.primary),
                onClick = { onSwitchAccountClick(accountSummary) },
                onLongClick = { onSwitchAccountLongClick(accountSummary) },
            )
            .padding(vertical = 8.dp)
            .then(modifier),
    ) {
        Box(
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_account_initials_container),
                contentDescription = null,
                tint = accountSummary.avatarColor,
                modifier = Modifier.size(40.dp),
            )

            Text(
                text = accountSummary.initials,
                style = MaterialTheme.typography.titleMedium
                    // Do not allow scaling
                    .copy(fontSize = 16.dp.toUnscaledTextUnit()),
                color = accountSummary.avatarColor.toSafeOverlayColor(),
                modifier = Modifier.clearAndSetSemantics { },
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = accountSummary.email,
                style = MaterialTheme.typography.bodyLarge,
            )

            accountSummary.supportingTextResOrNull?.let { supportingTextResId ->
                Text(
                    text = stringResource(id = supportingTextResId).lowercaseWithCurrentLocal(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Icon(
            painter = painterResource(id = accountSummary.iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .size(24.dp),
        )

        Spacer(modifier = Modifier.width(8.dp))
    }
}

@Composable
private fun LockOrLogoutDialog(
    accountSummary: AccountSummary,
    onDismissRequest: () -> Unit,
    onLockAccountClick: (AccountSummary) -> Unit,
    onLogoutAccountClick: (AccountSummary) -> Unit,
) {
    BitwardenSelectionDialog(
        title = "${accountSummary.email}\n${accountSummary.environmentLabel}",
        onDismissRequest = onDismissRequest,
        selectionItems = {
            BitwardenBasicDialogRow(
                text = stringResource(id = R.string.lock),
                onClick = {
                    onLockAccountClick(accountSummary)
                },
            )
            BitwardenBasicDialogRow(
                text = stringResource(id = R.string.log_out),
                onClick = {
                    onLogoutAccountClick(accountSummary)
                },
            )
        },
    )
}

@Composable
private fun AddAccountItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(color = MaterialTheme.colorScheme.primary),
                onClick = onClick,
            )
            .padding(vertical = 8.dp)
            .then(modifier),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_plus),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .size(24.dp),
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = stringResource(id = R.string.add_account),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
