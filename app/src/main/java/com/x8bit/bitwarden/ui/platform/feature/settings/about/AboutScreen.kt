package com.x8bit.bitwarden.ui.platform.feature.settings.about

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.mirrorIfRtl
import com.x8bit.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.model.CardStyle
import com.x8bit.bitwarden.ui.platform.components.model.TooltipData
import com.x8bit.bitwarden.ui.platform.components.row.BitwardenExternalLinkRow
import com.x8bit.bitwarden.ui.platform.components.row.BitwardenPushRow
import com.x8bit.bitwarden.ui.platform.components.row.BitwardenTextRow
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.toggle.BitwardenSwitch
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.composition.LocalIntentManager
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Displays the about screen.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    onNavigateToFlightRecorder: () -> Unit,
    onNavigateToRecordedLogs: () -> Unit,
    viewModel: AboutViewModel = hiltViewModel(),
    intentManager: IntentManager = LocalIntentManager.current,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is AboutEvent.NavigateToWebVault -> intentManager.launchUri(event.vaultUrl.toUri())
            AboutEvent.NavigateBack -> onNavigateBack.invoke()
            AboutEvent.NavigateToFlightRecorder -> onNavigateToFlightRecorder()
            AboutEvent.NavigateToRecordedLogs -> onNavigateToRecordedLogs()

            AboutEvent.NavigateToFlightRecorderHelp -> {
                intentManager.launchUri("https://bitwarden.com/help/flight-recorder".toUri())
            }

            AboutEvent.NavigateToHelpCenter -> {
                intentManager.launchUri("https://bitwarden.com/help".toUri())
            }

            AboutEvent.NavigateToPrivacyPolicy -> {
                intentManager.launchUri("https://bitwarden.com/privacy".toUri())
            }

            AboutEvent.NavigateToLearnAboutOrganizations -> {
                intentManager.launchUri("https://bitwarden.com/help/about-organizations".toUri())
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.about),
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = R.drawable.ic_back),
                navigationIconContentDescription = stringResource(id = R.string.back),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(AboutAction.BackClick) }
                },
            )
        },
    ) {
        AboutScreenContent(
            state = state,
            modifier = Modifier.fillMaxSize(),
            onHelpCenterClick = remember(viewModel) {
                { viewModel.trySendAction(AboutAction.HelpCenterClick) }
            },
            onPrivacyPolicyClick = remember(viewModel) {
                { viewModel.trySendAction(AboutAction.PrivacyPolicyClick) }
            },
            onLearnAboutOrgsClick = remember(viewModel) {
                { viewModel.trySendAction(AboutAction.LearnAboutOrganizationsClick) }
            },
            onSubmitCrashLogsCheckedChange = remember(viewModel) {
                { viewModel.trySendAction(AboutAction.SubmitCrashLogsClick(it)) }
            },
            onFlightRecorderCheckedChange = remember(viewModel) {
                { viewModel.trySendAction(AboutAction.FlightRecorderCheckedChange(it)) }
            },
            onFlightRecorderTooltipClick = remember(viewModel) {
                { viewModel.trySendAction(AboutAction.FlightRecorderTooltipClick) }
            },
            onViewRecordedLogsClick = remember(viewModel) {
                { viewModel.trySendAction(AboutAction.ViewRecordedLogsClick) }
            },
            onVersionClick = remember(viewModel) {
                { viewModel.trySendAction(AboutAction.VersionClick) }
            },
            onWebVaultClick = remember(viewModel) {
                { viewModel.trySendAction(AboutAction.WebVaultClick) }
            },
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun AboutScreenContent(
    state: AboutState,
    onHelpCenterClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onLearnAboutOrgsClick: () -> Unit,
    onSubmitCrashLogsCheckedChange: (Boolean) -> Unit,
    onFlightRecorderCheckedChange: (Boolean) -> Unit,
    onFlightRecorderTooltipClick: () -> Unit,
    onViewRecordedLogsClick: () -> Unit,
    onVersionClick: () -> Unit,
    onWebVaultClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.height(height = 12.dp))
        CrashLogsCard(
            isVisible = state.shouldShowCrashLogsButton,
            isEnabled = state.isSubmitCrashLogsEnabled,
            onSubmitCrashLogsCheckedChange = onSubmitCrashLogsCheckedChange,
        )
        FlightRecorderCard(
            isVisible = state.shouldShowFlightRecorder,
            isFlightRecorderEnabled = state.isFlightRecorderEnabled,
            logExpiration = state.flightRecorderSubtext,
            onFlightRecorderCheckedChange = onFlightRecorderCheckedChange,
            onFlightRecorderTooltipClick = onFlightRecorderTooltipClick,
            onViewRecordedLogsClick = onViewRecordedLogsClick,
        )
        BitwardenExternalLinkRow(
            text = stringResource(id = R.string.bitwarden_help_center),
            onConfirmClick = onHelpCenterClick,
            dialogTitle = stringResource(id = R.string.continue_to_help_center),
            dialogMessage = stringResource(
                id = R.string.learn_more_about_how_to_use_bitwarden_on_the_help_center,
            ),
            withDivider = false,
            cardStyle = CardStyle.Top(),
            modifier = Modifier
                .standardHorizontalMargin()
                .fillMaxWidth()
                .testTag(tag = "BitwardenHelpCenterRow"),
        )
        BitwardenExternalLinkRow(
            text = stringResource(id = R.string.privacy_policy),
            onConfirmClick = onPrivacyPolicyClick,
            dialogTitle = stringResource(id = R.string.continue_to_privacy_policy),
            dialogMessage = stringResource(
                id = R.string.privacy_policy_description_long,
            ),
            withDivider = false,
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .standardHorizontalMargin()
                .fillMaxWidth()
                .testTag(tag = "PrivacyPolicyRow"),
        )
        BitwardenExternalLinkRow(
            text = stringResource(id = R.string.web_vault),
            onConfirmClick = onWebVaultClick,
            dialogTitle = stringResource(id = R.string.continue_to_web_app),
            dialogMessage = stringResource(
                id = R.string.explore_more_features_of_your_bitwarden_account_on_the_web_app,
            ),
            withDivider = false,
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .standardHorizontalMargin()
                .fillMaxWidth()
                .testTag(tag = "BitwardenWebVaultRow"),
        )
        BitwardenExternalLinkRow(
            text = stringResource(id = R.string.learn_org),
            onConfirmClick = onLearnAboutOrgsClick,
            dialogTitle = stringResource(id = R.string.continue_to_x, "bitwarden.com"),
            dialogMessage = stringResource(
                id = R.string.learn_about_organizations_description_long,
            ),
            withDivider = false,
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .standardHorizontalMargin()
                .fillMaxWidth()
                .testTag(tag = "LearnAboutOrganizationsRow"),
        )
        CopyRow(
            text = state.version,
            onClick = onVersionClick,
            modifier = Modifier
                .standardHorizontalMargin()
                .fillMaxWidth()
                .testTag("CopyAboutInfoRow"),
        )
        Box(
            modifier = Modifier
                .defaultMinSize(minHeight = 60.dp)
                .standardHorizontalMargin()
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                modifier = Modifier.padding(end = 16.dp),
                text = state.copyrightInfo.invoke(),
                style = BitwardenTheme.typography.bodySmall,
                color = BitwardenTheme.colorScheme.text.primary,
            )
        }
        Spacer(modifier = Modifier.height(height = 16.dp))
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun ColumnScope.CrashLogsCard(
    isVisible: Boolean,
    isEnabled: Boolean,
    onSubmitCrashLogsCheckedChange: (Boolean) -> Unit,
) {
    if (!isVisible) return
    BitwardenSwitch(
        label = stringResource(id = R.string.submit_crash_logs),
        contentDescription = stringResource(id = R.string.submit_crash_logs),
        isChecked = isEnabled,
        onCheckedChange = onSubmitCrashLogsCheckedChange,
        cardStyle = CardStyle.Full,
        modifier = Modifier
            .testTag(tag = "SubmitCrashLogsSwitch")
            .fillMaxWidth()
            .standardHorizontalMargin(),
    )
    Spacer(modifier = Modifier.height(height = 8.dp))
}

@Composable
private fun ColumnScope.FlightRecorderCard(
    isVisible: Boolean,
    isFlightRecorderEnabled: Boolean,
    logExpiration: Text?,
    onFlightRecorderCheckedChange: (Boolean) -> Unit,
    onFlightRecorderTooltipClick: () -> Unit,
    onViewRecordedLogsClick: () -> Unit,
) {
    if (!isVisible) return
    BitwardenSwitch(
        label = stringResource(id = R.string.flight_recorder),
        isChecked = isFlightRecorderEnabled,
        onCheckedChange = onFlightRecorderCheckedChange,
        tooltip = TooltipData(
            contentDescription = stringResource(id = R.string.flight_recorder_help),
            onClick = onFlightRecorderTooltipClick,
        ),
        subtext = logExpiration?.invoke(),
        cardStyle = CardStyle.Top(),
        modifier = Modifier
            .testTag(tag = "FlightRecorderSwitch")
            .fillMaxWidth()
            .standardHorizontalMargin(),
    )
    BitwardenPushRow(
        text = stringResource(id = R.string.view_recorded_logs),
        onClick = onViewRecordedLogsClick,
        cardStyle = CardStyle.Bottom,
        modifier = Modifier
            .testTag(tag = "ViewRecordedLogs")
            .fillMaxWidth()
            .standardHorizontalMargin(),
    )
    Spacer(modifier = Modifier.height(height = 8.dp))
}

@Composable
private fun CopyRow(
    text: Text,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BitwardenTextRow(
        text = text(),
        onClick = onClick,
        withDivider = false,
        cardStyle = CardStyle.Bottom,
        modifier = modifier,
    ) {
        Icon(
            painter = rememberVectorPainter(id = R.drawable.ic_copy),
            contentDescription = null,
            tint = BitwardenTheme.colorScheme.icon.primary,
            modifier = Modifier.mirrorIfRtl(),
        )
    }
}

@Preview
@Composable
private fun AboutScreenContent_preview() {
    BitwardenTheme {
        AboutScreenContent(
            state = AboutState(
                version = "Version: 1.0.0 (1)".asText(),
                deviceData = "device_data".asText(),
                ciData = "ci_data".asText(),
                isSubmitCrashLogsEnabled = false,
                copyrightInfo = "".asText(),
                shouldShowCrashLogsButton = true,
                isFlightRecorderEnabled = true,
                shouldShowFlightRecorder = true,
                flightRecorderSubtext = "Expires 3/21/25 at 3:20 PM".asText(),
            ),
            onHelpCenterClick = {},
            onPrivacyPolicyClick = {},
            onLearnAboutOrgsClick = {},
            onSubmitCrashLogsCheckedChange = { },
            onFlightRecorderCheckedChange = { },
            onFlightRecorderTooltipClick = {},
            onViewRecordedLogsClick = {},
            onVersionClick = {},
            onWebVaultClick = {},
        )
    }
}
