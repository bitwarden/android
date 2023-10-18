package com.x8bit.bitwarden.ui.platform.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.BitwardenMediumTopAppBar

/**
 * Displays the settings screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToAccountSecurity: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            SettingsEvent.NavigateAccountSecurity -> onNavigateToAccountSecurity.invoke()
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.surface),
    ) {
        BitwardenMediumTopAppBar(
            title = stringResource(id = R.string.settings),
            scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
        )
        SettingsRow(
            text = R.string.account.asText(),
            onClick = remember(viewModel) {
                { viewModel.trySendAction(SettingsAction.AccountSecurityClick) }
            },
        )
    }
}

@Composable
private fun SettingsRow(
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
