package com.x8bit.bitwarden.ui.platform.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.BitwardenOverflowActionItem
import com.x8bit.bitwarden.ui.platform.components.BitwardenTopAppBar

/**
 * Displays the account security screen.
 */
@Composable
fun AccountSecurityScreen(
    onNavigateBack: () -> Unit,
    viewModel: AccountSecurityViewModel = hiltViewModel(),
) {
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            AccountSecurityEvent.NavigateBack -> onNavigateBack.invoke()
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.surface),
    ) {
        BitwardenTopAppBar(
            title = stringResource(id = R.string.account),
            navigationIcon = painterResource(id = R.drawable.ic_back),
            navigationIconContentDescription = stringResource(id = R.string.back),
            onNavigationIconClick = remember(viewModel) {
                { viewModel.trySendAction(AccountSecurityAction.BackClick) }
            },
            actions = {
                BitwardenOverflowActionItem()
            },
        )
        Spacer(Modifier.height(8.dp))
        AccountSecurityRow(
            text = R.string.log_out.asText(),
            onClick = remember(viewModel) {
                { viewModel.trySendAction(AccountSecurityAction.LogoutClick) }
            },
        )
    }
}

@Composable
private fun AccountSecurityRow(
    text: Text,
    onClick: () -> Unit,
) {
    Text(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(color = MaterialTheme.colorScheme.primary),
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .fillMaxWidth(),
        text = text(),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
}
