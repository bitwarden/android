@file:Suppress("TooManyFunctions")

package com.bitwarden.authenticator.ui.platform.feature.settings

import android.content.Intent
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.ui.platform.base.util.EventsEffect
import com.bitwarden.authenticator.ui.platform.base.util.Text
import com.bitwarden.authenticator.ui.platform.base.util.asText
import com.bitwarden.authenticator.ui.platform.base.util.mirrorIfRtl
import com.bitwarden.authenticator.ui.platform.components.appbar.BitwardenMediumTopAppBar
import com.bitwarden.authenticator.ui.platform.components.dialog.BitwardenSelectionDialog
import com.bitwarden.authenticator.ui.platform.components.dialog.BitwardenSelectionRow
import com.bitwarden.authenticator.ui.platform.components.header.BitwardenListHeaderText
import com.bitwarden.authenticator.ui.platform.components.row.BitwardenExternalLinkRow
import com.bitwarden.authenticator.ui.platform.components.row.BitwardenTextRow
import com.bitwarden.authenticator.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.authenticator.ui.platform.components.toggle.BitwardenWideSwitch
import com.bitwarden.authenticator.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppTheme
import com.bitwarden.authenticator.ui.platform.feature.settings.data.model.DefaultSaveOption
import com.bitwarden.authenticator.ui.platform.manager.biometrics.BiometricsManager
import com.bitwarden.authenticator.ui.platform.manager.intent.IntentManager
import com.bitwarden.authenticator.ui.platform.theme.AuthenticatorTheme
import com.bitwarden.authenticator.ui.platform.theme.LocalBiometricsManager
import com.bitwarden.authenticator.ui.platform.theme.LocalIntentManager
import com.bitwarden.authenticator.ui.platform.util.displayLabel

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
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

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
                title = stringResource(id = R.string.settings),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
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
                modifier = Modifier
                    .padding(horizontal = 16.dp),
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
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
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

    BitwardenListHeaderText(
        modifier = Modifier.padding(horizontal = 16.dp),
        label = stringResource(id = R.string.security),
    )
    Spacer(modifier = Modifier.height(8.dp))
    UnlockWithBiometricsRow(
        modifier = Modifier
            .testTag("UnlockWithBiometricsSwitch")
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        isChecked = state.isUnlockWithBiometricsEnabled,
        onBiometricToggle = { onBiometricToggle(it) },
        biometricsManager = biometricsManager,
    )
}

//endregion

//region Data settings

@Composable
@Suppress("LongMethod")
private fun VaultSettings(
    modifier: Modifier = Modifier,
    defaultSaveOption: DefaultSaveOption,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onBackupClick: () -> Unit,
    onSyncWithBitwardenClick: () -> Unit,
    onDefaultSaveOptionUpdated: (DefaultSaveOption) -> Unit,
    shouldShowSyncWithBitwardenApp: Boolean,
    shouldShowDefaultSaveOptions: Boolean,
) {
    BitwardenListHeaderText(
        modifier = Modifier.padding(horizontal = 16.dp),
        label = stringResource(id = R.string.data),
    )
    Spacer(modifier = Modifier.height(8.dp))
    BitwardenTextRow(
        text = stringResource(id = R.string.import_vault),
        onClick = onImportClick,
        modifier = modifier
            .semantics { testTag = "Import" },
        withDivider = true,
        content = {
            Icon(
                modifier = Modifier
                    .mirrorIfRtl()
                    .size(24.dp),
                painter = painterResource(id = R.drawable.ic_navigate_next),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        },
    )
    Spacer(modifier = Modifier.height(8.dp))
    BitwardenTextRow(
        text = stringResource(id = R.string.export),
        onClick = onExportClick,
        modifier = modifier
            .semantics { testTag = "Export" },
        withDivider = true,
        content = {
            Icon(
                modifier = Modifier
                    .mirrorIfRtl()
                    .size(24.dp),
                painter = painterResource(id = R.drawable.ic_navigate_next),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        },
    )
    Spacer(modifier = Modifier.height(8.dp))
    BitwardenExternalLinkRow(
        text = stringResource(R.string.backup),
        onConfirmClick = onBackupClick,
        modifier = modifier
            .semantics { testTag = "Backup" },
        dialogTitle = stringResource(R.string.data_backup_title),
        dialogMessage = stringResource(R.string.data_backup_message),
        dialogConfirmButtonText = stringResource(R.string.learn_more),
        dialogDismissButtonText = stringResource(R.string.ok),
    )
    if (shouldShowSyncWithBitwardenApp) {
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenTextRow(
            text = stringResource(id = R.string.sync_with_bitwarden_app),
            onClick = onSyncWithBitwardenClick,
            modifier = modifier,
            withDivider = true,
            content = {
                Icon(
                    modifier = Modifier
                        .mirrorIfRtl()
                        .size(24.dp),
                    painter = painterResource(id = R.drawable.ic_external_link),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            },
        )
    }
    if (shouldShowDefaultSaveOptions) {
        DefaultSaveOptionSelectionRow(
            currentSelection = defaultSaveOption,
            onSaveOptionUpdated = onDefaultSaveOptionUpdated,
        )
    }
}

@Composable
private fun DefaultSaveOptionSelectionRow(
    currentSelection: DefaultSaveOption,
    onSaveOptionUpdated: (DefaultSaveOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    var shouldShowDefaultSaveOptionDialog by remember { mutableStateOf(false) }

    BitwardenTextRow(
        text = stringResource(id = R.string.default_save_option),
        onClick = { shouldShowDefaultSaveOptionDialog = true },
        modifier = modifier,
        withDivider = true,
    ) {
        Text(
            modifier = Modifier.padding(vertical = 20.dp),
            text = currentSelection.displayLabel(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    var dialogSelection by remember { mutableStateOf(currentSelection) }
    if (shouldShowDefaultSaveOptionDialog) {
        BitwardenSelectionDialog(
            title = stringResource(id = R.string.default_save_option),
            subtitle = stringResource(id = R.string.default_save_options_subtitle),
            dismissLabel = stringResource(id = R.string.confirm),
            onDismissRequest = { shouldShowDefaultSaveOptionDialog = false },
            onDismissActionClick = {
                onSaveOptionUpdated(dialogSelection)
                shouldShowDefaultSaveOptionDialog = false
            },
        ) {
            DefaultSaveOption.entries.forEach { option ->
                BitwardenSelectionRow(
                    text = option.displayLabel,
                    isSelected = option == dialogSelection,
                    onClick = {
                        dialogSelection = DefaultSaveOption.entries.first { it == option }
                    },
                )
            }
        }
    }
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
    BitwardenWideSwitch(
        modifier = modifier,
        label = stringResource(
            id = R.string.unlock_with,
            stringResource(id = R.string.biometrics),
        ),
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
private fun AppearanceSettings(
    state: SettingsState,
    onThemeSelection: (theme: AppTheme) -> Unit,
) {
    BitwardenListHeaderText(
        modifier = Modifier.padding(horizontal = 16.dp),
        label = stringResource(id = R.string.appearance),
    )
    ThemeSelectionRow(
        currentSelection = state.appearance.theme,
        onThemeSelection = onThemeSelection,
        modifier = Modifier
            .semantics { testTag = "ThemeChooser" }
            .fillMaxWidth(),
    )
}

@Composable
private fun ThemeSelectionRow(
    currentSelection: AppTheme,
    onThemeSelection: (AppTheme) -> Unit,
    modifier: Modifier = Modifier,
) {
    var shouldShowThemeSelectionDialog by remember { mutableStateOf(false) }

    BitwardenTextRow(
        text = stringResource(id = R.string.theme),
        onClick = { shouldShowThemeSelectionDialog = true },
        modifier = modifier,
        withDivider = true,
    ) {
        Icon(
            modifier = Modifier
                .mirrorIfRtl()
                .size(24.dp),
            painter = painterResource(
                id = R.drawable.ic_navigate_next,
            ),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }

    if (shouldShowThemeSelectionDialog) {
        BitwardenSelectionDialog(
            title = stringResource(id = R.string.theme),
            onDismissRequest = { shouldShowThemeSelectionDialog = false },
        ) {
            AppTheme.entries.forEach { option ->
                BitwardenSelectionRow(
                    text = option.displayLabel,
                    isSelected = option == currentSelection,
                    onClick = {
                        shouldShowThemeSelectionDialog = false
                        onThemeSelection(
                            AppTheme.entries.first { it == option },
                        )
                    },
                )
            }
        }
    }
}

//endregion Appearance settings

//region Help settings

@Composable
private fun HelpSettings(
    modifier: Modifier = Modifier,
    onTutorialClick: () -> Unit,
    onHelpCenterClick: () -> Unit,
) {
    BitwardenListHeaderText(
        modifier = Modifier.padding(horizontal = 16.dp),
        label = stringResource(id = R.string.help),
    )
    BitwardenTextRow(
        text = stringResource(id = R.string.launch_tutorial),
        onClick = onTutorialClick,
        modifier = modifier
            .semantics { testTag = "LaunchTutorial" },
        withDivider = true,
    )
    Spacer(modifier = Modifier.height(8.dp))
    BitwardenExternalLinkRow(
        text = stringResource(id = R.string.bitwarden_help_center),
        onConfirmClick = onHelpCenterClick,
        modifier = modifier
            .semantics { testTag = "BitwardenHelpCenter" },
        dialogTitle = stringResource(id = R.string.continue_to_help_center),
        dialogMessage = stringResource(
            id = R.string.learn_more_about_how_to_use_bitwarden_on_the_help_center,
        ),
    )
}

//endregion Help settings

//region About settings
@Composable
private fun AboutSettings(
    modifier: Modifier = Modifier,
    state: SettingsState,
    onSubmitCrashLogsCheckedChange: (Boolean) -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onVersionClick: () -> Unit,
) {
    BitwardenListHeaderText(
        modifier = modifier,
        label = stringResource(id = R.string.about),
    )
    BitwardenWideSwitch(
        modifier = modifier
            .semantics { testTag = "SubmitCrashLogs" },
        label = stringResource(id = R.string.submit_crash_logs),
        isChecked = state.isSubmitCrashLogsEnabled,
        onCheckedChange = onSubmitCrashLogsCheckedChange,
    )
    BitwardenExternalLinkRow(
        text = stringResource(id = R.string.privacy_policy),
        modifier = modifier
            .semantics { testTag = "PrivacyPolicy" },
        onConfirmClick = onPrivacyPolicyClick,
        dialogTitle = stringResource(id = R.string.continue_to_privacy_policy),
        dialogMessage = stringResource(
            id = R.string.privacy_policy_description_long,
        ),
    )
    CopyRow(
        text = state.version,
        onClick = onVersionClick,
    )
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
                indication = ripple(color = MaterialTheme.colorScheme.primary),
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
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Icon(
                painter = rememberVectorPainter(id = R.drawable.ic_copy),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 16.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

//endregion About settings

@Preview
@Composable
private fun CopyRow_preview() {
    AuthenticatorTheme {
        CopyRow(
            text = "Copyable Text".asText(),
            onClick = { },
        )
    }
}
