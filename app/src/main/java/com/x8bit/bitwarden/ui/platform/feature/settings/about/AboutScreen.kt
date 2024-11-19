package com.x8bit.bitwarden.ui.platform.feature.settings.about

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.divider.BitwardenHorizontalDivider
import com.x8bit.bitwarden.ui.platform.components.row.BitwardenExternalLinkRow
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
    viewModel: AboutViewModel = hiltViewModel(),
    intentManager: IntentManager = LocalIntentManager.current,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is AboutEvent.NavigateToWebVault -> {
                intentManager.launchUri(event.vaultUrl.toUri())
            }

            AboutEvent.NavigateBack -> onNavigateBack.invoke()

            AboutEvent.NavigateToFeedbackForm -> {
                intentManager.launchUri("https://livefrontinc.typeform.com/to/irgrRu4a".toUri())
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

            AboutEvent.NavigateToRateApp -> {
                intentManager.launchUri(
                    uri =
                    "https://play.google.com/store/apps/details?id=com.x8bit.bitwarden".toUri(),
                )
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
        ContentColumn(
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
            onRateTheAppClick = remember(viewModel) {
                { viewModel.trySendAction(AboutAction.RateAppClick) }
            },
            onGiveFeedbackClick = remember(viewModel) {
                { viewModel.trySendAction(AboutAction.GiveFeedbackClick) }
            },
            onSubmitCrashLogsCheckedChange = remember(viewModel) {
                { viewModel.trySendAction(AboutAction.SubmitCrashLogsClick(it)) }
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
private fun ContentColumn(
    state: AboutState,
    onHelpCenterClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onLearnAboutOrgsClick: () -> Unit,
    onRateTheAppClick: () -> Unit,
    onGiveFeedbackClick: () -> Unit,
    onSubmitCrashLogsCheckedChange: (Boolean) -> Unit,
    onVersionClick: () -> Unit,
    onWebVaultClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        if (state.shouldShowCrashLogsButton) {
            BitwardenSwitch(
                label = stringResource(id = R.string.submit_crash_logs),
                isChecked = state.isSubmitCrashLogsEnabled,
                onCheckedChange = onSubmitCrashLogsCheckedChange,
                modifier = Modifier
                    .testTag("SubmitCrashLogsSwitch")
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentDescription = stringResource(id = R.string.submit_crash_logs),
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        BitwardenExternalLinkRow(
            text = stringResource(id = R.string.bitwarden_help_center),
            onConfirmClick = onHelpCenterClick,
            dialogTitle = stringResource(id = R.string.continue_to_help_center),
            modifier = Modifier.testTag("BitwardenHelpCenterRow"),
            dialogMessage = stringResource(
                id = R.string.learn_more_about_how_to_use_bitwarden_on_the_help_center,
            ),
        )
        BitwardenExternalLinkRow(
            text = stringResource(id = R.string.privacy_policy),
            onConfirmClick = onPrivacyPolicyClick,
            dialogTitle = stringResource(id = R.string.continue_to_privacy_policy),
            modifier = Modifier.testTag("PrivacyPolicyRow"),
            dialogMessage = stringResource(
                id = R.string.privacy_policy_description_long,
            ),
        )
        BitwardenExternalLinkRow(
            text = stringResource(id = R.string.web_vault),
            onConfirmClick = onWebVaultClick,
            modifier = Modifier.testTag("BitwardenWebVaultRow"),
            dialogTitle = stringResource(id = R.string.continue_to_web_app),
            dialogMessage = stringResource(
                id = R.string.explore_more_features_of_your_bitwarden_account_on_the_web_app,
            ),
        )
        BitwardenExternalLinkRow(
            text = stringResource(id = R.string.learn_org),
            onConfirmClick = onLearnAboutOrgsClick,
            dialogTitle = stringResource(id = R.string.continue_to_x, "bitwarden.com"),
            modifier = Modifier.testTag("LearnAboutOrganizationsRow"),
            dialogMessage = stringResource(
                id = R.string.learn_about_organizations_description_long,
            ),
        )
        BitwardenExternalLinkRow(
            text = stringResource(R.string.give_feedback),
            onConfirmClick = onGiveFeedbackClick,
            modifier = Modifier.testTag("GiveFeedbackRow"),
            dialogTitle = stringResource(R.string.continue_to_give_feedback),
            dialogMessage = stringResource(R.string.continue_to_provide_feedback),
        )
        CopyRow(
            text = state.version,
            onClick = onVersionClick,
            modifier = Modifier.testTag("CopyAboutInfoRow"),
        )
        Box(
            modifier = Modifier
                .defaultMinSize(minHeight = 56.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp)
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
    }
}

@Composable
private fun CopyRow(
    text: Text,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resources = LocalContext.current.resources
    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(
                    color = BitwardenTheme.colorScheme.background.pressed,
                ),
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) {
                contentDescription = text.toString(resources)
            },
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 56.dp)
                .padding(start = 16.dp, end = 24.dp, top = 8.dp, bottom = 8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .weight(1f),
                text = text(),
                style = BitwardenTheme.typography.bodySmall,
                color = BitwardenTheme.colorScheme.text.primary,
            )
            Icon(
                painter = rememberVectorPainter(id = R.drawable.ic_copy),
                contentDescription = null,
                tint = BitwardenTheme.colorScheme.icon.primary,
            )
        }
        BitwardenHorizontalDivider(modifier = Modifier.padding(start = 16.dp))
    }
}

@Preview
@Composable
private fun CopyRow_preview() {
    BitwardenTheme {
        CopyRow(
            text = "Copyable Text".asText(),
            onClick = { },
        )
    }
}
