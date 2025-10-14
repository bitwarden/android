@file:Suppress("TooManyFunctions")

package com.bitwarden.authenticator.ui.platform.feature.settings

import android.content.Intent
import android.content.res.Resources
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.authenticator.ui.platform.composition.LocalBiometricsManager
import com.bitwarden.authenticator.ui.platform.feature.settings.data.model.DefaultSaveOption
import com.bitwarden.authenticator.ui.platform.manager.biometrics.BiometricsManager
import com.bitwarden.authenticator.ui.platform.util.displayLabel
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.annotatedStringResource
import com.bitwarden.ui.platform.base.util.cardStyle
import com.bitwarden.ui.platform.base.util.mirrorIfRtl
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.appbar.BitwardenMediumTopAppBar
import com.bitwarden.ui.platform.components.dropdown.BitwardenMultiSelectButton
import com.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.row.BitwardenExternalLinkRow
import com.bitwarden.ui.platform.components.row.BitwardenTextRow
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.toggle.BitwardenSwitch
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.composition.LocalIntentManager
import com.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.bitwarden.ui.platform.util.displayLabel
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import kotlinx.collections.immutable.toImmutableList

/**
 * Display the settings screen.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    biometricsManager: BiometricsManager = LocalBiometricsManager.current,
    intentManager: IntentManager = LocalIntentManager.current,
    onNavigateToTutorial: () -> Unit,
    onNavigateToExport: () -> Unit,
    onNavigateToImport: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            SettingsEvent.NavigateToTutorial -> onNavigateToTutorial()
            SettingsEvent.NavigateToExport -> onNavigateToExport()
            SettingsEvent.NavigateToImport -> onNavigateToImport()
            SettingsEvent.NavigateToBackup -> {
                intentManager.launchUri(
                    uri = "https://support.google.com/android/answer/2819582".toUri(),
                )
            }

            SettingsEvent.NavigateToHelpCenter -> {
                intentManager.launchUri("https://bitwarden.com/help".toUri())
            }

            SettingsEvent.NavigateToPrivacyPolicy -> {
                intentManager.launchUri("https://bitwarden.com/privacy".toUri())
            }

            SettingsEvent.NavigateToSyncInformation -> {
                intentManager.launchUri("https://bitwarden.com/help/totp-sync".toUri())
            }

            SettingsEvent.NavigateToBitwardenApp -> {

                intentManager.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        "bitwarden://settings/account_security".toUri(),
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                )
            }

            SettingsEvent.NavigateToBitwardenPlayStoreListing -> {
                intentManager.launchUri(
                    "https://play.google.com/store/apps/details?id=com.x8bit.bitwarden".toUri(),
                )
            }
        }
    }

    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenMediumTopAppBar(
                title = stringResource(id = BitwardenString.settings),
                scrollBehavior = scrollBehavior,
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(state = rememberScrollState()),
        ) {
            SecuritySettings(
                state = state,
                biometricsManager = biometricsManager,
                onBiometricToggle = remember(viewModel) {
                    {
                        viewModel.trySendAction(
                            SettingsAction.SecurityClick.UnlockWithBiometricToggle(it),
                        )
                    }
                },
            )
            Spacer(modifier = Modifier.height(16.dp))
            VaultSettings(
                onExportClick = remember(viewModel) {
                    {
                        viewModel.trySendAction(SettingsAction.DataClick.ExportClick)
                    }
                },
                onImportClick = remember(viewModel) {
                    {
                        viewModel.trySendAction(SettingsAction.DataClick.ImportClick)
                    }
                },
                onBackupClick = remember(viewModel) {
                    {
                        viewModel.trySendAction(SettingsAction.DataClick.BackupClick)
                    }
                },
                onSyncWithBitwardenClick = remember(viewModel) {
                    {
                        viewModel.trySendAction(SettingsAction.DataClick.SyncWithBitwardenClick)
                    }
                },
                onSyncLearnMoreClick = remember(viewModel) {
                    { viewModel.trySendAction(SettingsAction.DataClick.SyncLearnMoreClick) }
                },
                onDefaultSaveOptionUpdated = remember(viewModel) {
                    {
                        viewModel.trySendAction(
                            SettingsAction.DataClick.DefaultSaveOptionUpdated(it),
                        )
                    }
                },
                defaultSaveOption = state.defaultSaveOption,
                shouldShowDefaultSaveOptions = state.showDefaultSaveOptionRow,
                shouldShowSyncWithBitwardenApp = state.showSyncWithBitwarden,
            )
            Spacer(modifier = Modifier.height(16.dp))
            AppearanceSettings(
                state = state,
                onThemeSelection = remember(viewModel) {
                    {
                        viewModel.trySendAction(SettingsAction.AppearanceChange.ThemeChange(it))
                    }
                },
            )
            Spacer(Modifier.height(16.dp))
            HelpSettings(
                onTutorialClick = remember(viewModel) {
                    {
                        viewModel.trySendAction(SettingsAction.HelpClick.ShowTutorialClick)
                    }
                },
                onHelpCenterClick = remember(viewModel) {
                    {
                        viewModel.trySendAction(SettingsAction.HelpClick.HelpCenterClick)
                    }
                },
            )
            Spacer(modifier = Modifier.height(16.dp))
            AboutSettings(
                state = state,
                onSubmitCrashLogsCheckedChange = remember(viewModel) {
                    { viewModel.trySendAction(SettingsAction.AboutClick.SubmitCrashLogsClick(it)) }
                },
                onPrivacyPolicyClick = remember(viewModel) {
                    { viewModel.trySendAction(SettingsAction.AboutClick.PrivacyPolicyClick) }
                },
                onVersionClick = remember(viewModel) {
                    { viewModel.trySendAction(SettingsAction.AboutClick.VersionClick) }
                },
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
            Spacer(modifier = Modifier.height(height = 12.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

//region Security settings

@Composable
private fun SecuritySettings(
    state: SettingsState,
    biometricsManager: BiometricsManager = LocalBiometricsManager.current,
    onBiometricToggle: (Boolean) -> Unit,
) {
    if (!biometricsManager.isBiometricsSupported) return
    Spacer(modifier = Modifier.height(height = 12.dp))
    BitwardenListHeaderText(
        modifier = Modifier
            .standardHorizontalMargin()
            .padding(horizontal = 16.dp),
        label = stringResource(id = BitwardenString.security),
    )
    Spacer(modifier = Modifier.height(8.dp))
    UnlockWithBiometricsRow(
        modifier = Modifier
            .testTag("UnlockWithBiometricsSwitch")
            .fillMaxWidth()
            .standardHorizontalMargin(),
        isChecked = state.isUnlockWithBiometricsEnabled,
        onBiometricToggle = { onBiometricToggle(it) },
        biometricsManager = biometricsManager,
    )
}

//endregion

//region Data settings

@Composable
@Suppress("LongMethod")
private fun ColumnScope.VaultSettings(
    defaultSaveOption: DefaultSaveOption,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onBackupClick: () -> Unit,
    onSyncWithBitwardenClick: () -> Unit,
    onSyncLearnMoreClick: () -> Unit,
    onDefaultSaveOptionUpdated: (DefaultSaveOption) -> Unit,
    shouldShowSyncWithBitwardenApp: Boolean,
    shouldShowDefaultSaveOptions: Boolean,
) {
    BitwardenListHeaderText(
        modifier = Modifier
            .standardHorizontalMargin()
            .padding(horizontal = 16.dp),
        label = stringResource(id = BitwardenString.data),
    )
    Spacer(modifier = Modifier.height(height = 8.dp))
    BitwardenTextRow(
        text = stringResource(id = BitwardenString.import_vault),
        onClick = onImportClick,
        modifier = Modifier
            .standardHorizontalMargin()
            .testTag("Import"),
        cardStyle = CardStyle.Top(),
        content = {
            Icon(
                modifier = Modifier.mirrorIfRtl(),
                painter = painterResource(id = BitwardenDrawable.ic_chevron_right),
                contentDescription = null,
                tint = BitwardenTheme.colorScheme.icon.primary,
            )
        },
    )
    BitwardenTextRow(
        text = stringResource(id = BitwardenString.export),
        onClick = onExportClick,
        modifier = Modifier
            .standardHorizontalMargin()
            .testTag("Export"),
        cardStyle = CardStyle.Middle(),
        content = {
            Icon(
                modifier = Modifier.mirrorIfRtl(),
                painter = painterResource(id = BitwardenDrawable.ic_chevron_right),
                contentDescription = null,
                tint = BitwardenTheme.colorScheme.icon.primary,
            )
        },
    )
    BitwardenExternalLinkRow(
        text = stringResource(BitwardenString.backup),
        onConfirmClick = onBackupClick,
        modifier = Modifier
            .standardHorizontalMargin()
            .testTag("Backup"),
        withDivider = false,
        dialogTitle = stringResource(BitwardenString.data_backup_title),
        dialogMessage = stringResource(BitwardenString.data_backup_message),
        dialogConfirmButtonText = stringResource(BitwardenString.learn_more),
        dialogDismissButtonText = stringResource(BitwardenString.okay),
        cardStyle = if (shouldShowSyncWithBitwardenApp || shouldShowDefaultSaveOptions) {
            CardStyle.Middle()
        } else {
            CardStyle.Bottom
        },
    )
    if (shouldShowSyncWithBitwardenApp) {
        BitwardenTextRow(
            text = stringResource(id = BitwardenString.sync_with_bitwarden_app),
            description = annotatedStringResource(
                id = BitwardenString.learn_more_link,
                onAnnotationClick = {
                    when (it) {
                        "learnMore" -> onSyncLearnMoreClick()
                    }
                },
            ),
            onClick = onSyncWithBitwardenClick,
            modifier = Modifier.standardHorizontalMargin(),
            cardStyle = if (shouldShowDefaultSaveOptions) {
                CardStyle.Middle()
            } else {
                CardStyle.Bottom
            },
            content = {
                Icon(
                    modifier = Modifier.mirrorIfRtl(),
                    painter = painterResource(id = BitwardenDrawable.ic_external_link),
                    contentDescription = null,
                    tint = BitwardenTheme.colorScheme.icon.primary,
                )
            },
        )
    }
    if (shouldShowDefaultSaveOptions) {
        DefaultSaveOptionSelectionRow(
            currentSelection = defaultSaveOption,
            onSaveOptionUpdated = onDefaultSaveOptionUpdated,
            modifier = Modifier.standardHorizontalMargin(),
        )
    }
}

@Composable
private fun DefaultSaveOptionSelectionRow(
    currentSelection: DefaultSaveOption,
    onSaveOptionUpdated: (DefaultSaveOption) -> Unit,
    modifier: Modifier = Modifier,
    resources: Resources = LocalResources.current,
) {
    BitwardenMultiSelectButton(
        label = stringResource(id = BitwardenString.default_save_option),
        dialogSubtitle = stringResource(id = BitwardenString.default_save_options_subtitle),
        options = DefaultSaveOption.entries.map { it.displayLabel() }.toImmutableList(),
        selectedOption = currentSelection.displayLabel(),
        onOptionSelected = { selectedOptionLabel ->
            val selectedOption = DefaultSaveOption
                .entries
                .first { it.displayLabel(resources) == selectedOptionLabel }
            onSaveOptionUpdated(selectedOption)
        },
        cardStyle = CardStyle.Bottom,
        modifier = modifier,
    )
}

@Composable
private fun UnlockWithBiometricsRow(
    isChecked: Boolean,
    onBiometricToggle: (Boolean) -> Unit,
    biometricsManager: BiometricsManager,
    modifier: Modifier = Modifier,
) {
    if (!biometricsManager.isBiometricsSupported) return
    var showBiometricsPrompt by rememberSaveable { mutableStateOf(false) }
    BitwardenSwitch(
        modifier = modifier,
        cardStyle = CardStyle.Full,
        label = stringResource(BitwardenString.unlock_with_biometrics),
        isChecked = isChecked || showBiometricsPrompt,
        onCheckedChange = { toggled ->
            if (toggled) {
                showBiometricsPrompt = true
                biometricsManager.promptBiometrics(
                    onSuccess = {
                        onBiometricToggle(true)
                        showBiometricsPrompt = false
                    },
                    onCancel = { showBiometricsPrompt = false },
                    onLockOut = { showBiometricsPrompt = false },
                    onError = { showBiometricsPrompt = false },
                )
            } else {
                onBiometricToggle(false)
            }
        },
    )
}

//endregion Data settings

//region Appearance settings

@Composable
private fun ColumnScope.AppearanceSettings(
    state: SettingsState,
    onThemeSelection: (theme: AppTheme) -> Unit,
) {
    BitwardenListHeaderText(
        modifier = Modifier
            .standardHorizontalMargin()
            .padding(horizontal = 16.dp),
        label = stringResource(id = BitwardenString.appearance),
    )
    Spacer(modifier = Modifier.height(height = 8.dp))
    ThemeSelectionRow(
        currentSelection = state.appearance.theme,
        onThemeSelection = onThemeSelection,
        modifier = Modifier
            .testTag("ThemeChooser")
            .standardHorizontalMargin()
            .fillMaxWidth(),
    )
}

@Composable
private fun ThemeSelectionRow(
    currentSelection: AppTheme,
    onThemeSelection: (AppTheme) -> Unit,
    modifier: Modifier = Modifier,
    resources: Resources = LocalResources.current,
) {
    BitwardenMultiSelectButton(
        label = stringResource(id = BitwardenString.theme),
        options = AppTheme.entries.map { it.displayLabel() }.toImmutableList(),
        selectedOption = currentSelection.displayLabel(),
        onOptionSelected = { selectedOptionLabel ->
            val selectedOption = AppTheme
                .entries
                .first { it.displayLabel(resources) == selectedOptionLabel }
            onThemeSelection(selectedOption)
        },
        cardStyle = CardStyle.Full,
        modifier = modifier,
    )
}

//endregion Appearance settings

//region Help settings

@Composable
private fun ColumnScope.HelpSettings(
    onTutorialClick: () -> Unit,
    onHelpCenterClick: () -> Unit,
) {
    BitwardenListHeaderText(
        modifier = Modifier
            .standardHorizontalMargin()
            .padding(horizontal = 16.dp),
        label = stringResource(id = BitwardenString.help),
    )
    Spacer(modifier = Modifier.height(height = 8.dp))
    BitwardenTextRow(
        text = stringResource(id = BitwardenString.launch_tutorial),
        onClick = onTutorialClick,
        modifier = Modifier
            .testTag("LaunchTutorial")
            .standardHorizontalMargin(),
        cardStyle = CardStyle.Top(),
    )
    BitwardenExternalLinkRow(
        text = stringResource(id = BitwardenString.bitwarden_help_center),
        onConfirmClick = onHelpCenterClick,
        modifier = Modifier
            .standardHorizontalMargin()
            .testTag("BitwardenHelpCenter"),
        withDivider = false,
        dialogTitle = stringResource(id = BitwardenString.continue_to_help_center),
        dialogMessage = stringResource(
            BitwardenString.learn_more_about_how_to_use_bitwarden_authenticator_on_the_help_center,
        ),
        cardStyle = CardStyle.Bottom,
    )
}

//endregion Help settings

//region About settings
@Composable
private fun ColumnScope.AboutSettings(
    state: SettingsState,
    onSubmitCrashLogsCheckedChange: (Boolean) -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onVersionClick: () -> Unit,
) {
    BitwardenListHeaderText(
        modifier = Modifier
            .standardHorizontalMargin()
            .padding(horizontal = 16.dp),
        label = stringResource(id = BitwardenString.about),
    )
    Spacer(modifier = Modifier.height(height = 8.dp))
    BitwardenSwitch(
        modifier = Modifier
            .standardHorizontalMargin()
            .testTag("SubmitCrashLogs"),
        label = stringResource(id = BitwardenString.submit_crash_logs),
        isChecked = state.isSubmitCrashLogsEnabled,
        onCheckedChange = onSubmitCrashLogsCheckedChange,
        cardStyle = CardStyle.Top(),
    )
    BitwardenExternalLinkRow(
        text = stringResource(id = BitwardenString.privacy_policy),
        modifier = Modifier
            .standardHorizontalMargin()
            .testTag("PrivacyPolicy"),
        withDivider = false,
        onConfirmClick = onPrivacyPolicyClick,
        dialogTitle = stringResource(id = BitwardenString.continue_to_privacy_policy),
        dialogMessage = stringResource(
            id = BitwardenString.privacy_policy_description_long,
        ),
        cardStyle = CardStyle.Middle(),
    )
    CopyRow(
        text = state.version,
        onClick = onVersionClick,
        modifier = Modifier.standardHorizontalMargin(),
    )
}

@Composable
private fun CopyRow(
    text: Text,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    resources: Resources = LocalResources.current,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .defaultMinSize(minHeight = 60.dp)
            .cardStyle(cardStyle = CardStyle.Bottom, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = text.toString(resources)
            },
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .weight(1f),
                text = text(),
                style = BitwardenTheme.typography.bodyLarge,
                color = BitwardenTheme.colorScheme.text.primary,
            )
            Icon(
                painter = rememberVectorPainter(id = BitwardenDrawable.ic_copy),
                contentDescription = null,
                tint = BitwardenTheme.colorScheme.icon.primary,
            )
        }
    }
}

//endregion About settings

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
