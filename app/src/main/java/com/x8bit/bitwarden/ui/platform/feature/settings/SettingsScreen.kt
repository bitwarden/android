package com.x8bit.bitwarden.ui.platform.feature.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.cardStyle
import com.x8bit.bitwarden.ui.platform.base.util.mirrorIfRtl
import com.x8bit.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.x8bit.bitwarden.ui.platform.base.util.toListItemCardStyle
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenMediumTopAppBar
import com.x8bit.bitwarden.ui.platform.components.badge.NotificationBadge
import com.x8bit.bitwarden.ui.platform.components.model.CardStyle
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Displays the settings screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToAbout: () -> Unit,
    onNavigateToAccountSecurity: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToAutoFill: () -> Unit,
    onNavigateToOther: () -> Unit,
    onNavigateToVault: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            SettingsEvent.NavigateAbout -> onNavigateToAbout()
            SettingsEvent.NavigateAccountSecurity -> onNavigateToAccountSecurity.invoke()
            SettingsEvent.NavigateAppearance -> onNavigateToAppearance()
            SettingsEvent.NavigateAutoFill -> onNavigateToAutoFill()
            SettingsEvent.NavigateOther -> onNavigateToOther()
            SettingsEvent.NavigateVault -> onNavigateToVault()
            SettingsEvent.NavigateAccountSecurityShortcut -> onNavigateToAccountSecurity()
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        topBar = {
            BitwardenMediumTopAppBar(
                title = stringResource(id = R.string.settings),
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(state = rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(height = 12.dp))
            Settings.entries.forEachIndexed { index, settingEntry ->
                SettingsRow(
                    text = settingEntry.text,
                    onClick = remember(viewModel) {
                        { viewModel.trySendAction(SettingsAction.SettingsClick(settingEntry)) }
                    },
                    notificationCount = state.notificationBadgeCountMap.getOrDefault(
                        key = settingEntry,
                        defaultValue = 0,
                    ),
                    cardStyle = Settings.entries.toListItemCardStyle(
                        index = index,
                        // Start padding, plus icon, plus spacing between text.
                        dividerPadding = 48.dp,
                    ),
                    iconVectorResource = settingEntry.vectorIconRes,
                    modifier = Modifier
                        .testTag(tag = settingEntry.testTag)
                        .standardHorizontalMargin()
                        .fillMaxWidth(),
                )
            }
            Spacer(modifier = Modifier.height(height = 16.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun SettingsRow(
    text: Text,
    onClick: () -> Unit,
    notificationCount: Int,
    cardStyle: CardStyle?,
    @DrawableRes iconVectorResource: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 60.dp)
            .cardStyle(
                cardStyle = cardStyle,
                onClick = onClick,
                paddingStart = 12.dp,
                paddingEnd = 12.dp,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = rememberVectorPainter(iconVectorResource),
                contentDescription = null,
                tint = BitwardenTheme.colorScheme.icon.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                modifier = Modifier
                    .padding(end = 16.dp),
                text = text(),
                style = BitwardenTheme.typography.bodyLarge,
                color = BitwardenTheme.colorScheme.text.primary,
            )
        }
        TrailingContent(notificationCount = notificationCount)
    }
}

@Composable
private fun TrailingContent(
    notificationCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val notificationBadgeVisible = notificationCount > 0
        NotificationBadge(
            notificationCount = notificationCount,
            isVisible = notificationBadgeVisible,
        )
        if (notificationBadgeVisible) {
            Spacer(modifier = Modifier.width(12.dp))
        }
        Icon(
            painter = rememberVectorPainter(id = R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = BitwardenTheme.colorScheme.icon.primary,
            modifier = Modifier
                .mirrorIfRtl()
                .size(size = 16.dp),
        )
    }
}

@Preview
@Preview(name = "Right-To-Left", locale = "ar")
@Composable
private fun SettingsRows_preview() {
    BitwardenTheme {
        Column(
            modifier = Modifier
                .background(BitwardenTheme.colorScheme.background.primary)
                .padding(16.dp)
                .fillMaxSize(),
        ) {
            Settings.entries.forEachIndexed { index, it ->
                SettingsRow(
                    text = it.text,
                    onClick = { },
                    notificationCount = index % 3,
                    iconVectorResource = it.vectorIconRes,
                    cardStyle = Settings.entries.toListItemCardStyle(
                        index = index,
                        dividerPadding = 48.dp,
                    ),
                )
            }
        }
    }
}
