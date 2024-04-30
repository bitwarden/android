package com.bitwarden.authenticator.ui.platform.feature.settings

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
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
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
import com.bitwarden.authenticator.ui.platform.components.dialog.BasicDialogState
import com.bitwarden.authenticator.ui.platform.components.dialog.BitwardenBasicDialog
import com.bitwarden.authenticator.ui.platform.components.dialog.BitwardenSelectionDialog
import com.bitwarden.authenticator.ui.platform.components.dialog.BitwardenSelectionRow
import com.bitwarden.authenticator.ui.platform.components.header.BitwardenListHeaderText
import com.bitwarden.authenticator.ui.platform.components.row.BitwardenExternalLinkRow
import com.bitwarden.authenticator.ui.platform.components.row.BitwardenTextRow
import com.bitwarden.authenticator.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.authenticator.ui.platform.components.toggle.BitwardenWideSwitch
import com.bitwarden.authenticator.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppLanguage
import com.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppTheme
import com.bitwarden.authenticator.ui.platform.manager.biometrics.BiometricsManager
import com.bitwarden.authenticator.ui.platform.manager.intent.IntentManager
import com.bitwarden.authenticator.ui.platform.theme.AuthenticatorTheme
import com.bitwarden.authenticator.ui.platform.theme.LocalBiometricsManager
import com.bitwarden.authenticator.ui.platform.theme.LocalIntentManager
import com.bitwarden.authenticator.ui.platform.util.displayLabel

/**
 * Display the settings screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    biometricsManager: BiometricsManager = LocalBiometricsManager.current,
    intentManager: IntentManager = LocalIntentManager.current,
    onNavigateToTutorial: () -> Unit,
    onNavigateToExport: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            SettingsEvent.NavigateToTutorial -> onNavigateToTutorial()
            SettingsEvent.NavigateToExport -> onNavigateToExport()
            SettingsEvent.NavigateToHelpCenter -> {
                intentManager.launchUri("https://bitwarden.com/help".toUri())
            }

            SettingsEvent.NavigateToPrivacyPolicy -> {
                intentManager.launchUri("https://bitwarden.com/privacy".toUri())
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
                scrollBehavior = scrollBehavior
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(state = rememberScrollState())
        ) {
            SecuritySettings(
                state = state,
                biometricsManager = biometricsManager,
                onBiometricToggle = remember(viewModel) {
                    {
                        viewModel.trySendAction(
                            SettingsAction.SecurityClick.UnlockWithBiometricToggle(it)
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            VaultSettings(
                onExportClick = remember(viewModel) {
                    {
                        viewModel.trySendAction(SettingsAction.VaultClick.ExportClick)
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            AppearanceSettings(
                state = state,
                onLanguageSelection = remember(viewModel) {
                    {
                        viewModel.trySendAction(SettingsAction.AppearanceChange.LanguageChange(it))
                    }
                },
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
                }
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
fun SecuritySettings(
    state: SettingsState,
    biometricsManager: BiometricsManager = LocalBiometricsManager.current,
    onBiometricToggle: (Boolean) -> Unit,
) {
    if (!biometricsManager.isBiometricsSupported) return

    BitwardenListHeaderText(
        modifier = Modifier.padding(horizontal = 16.dp),
        label = stringResource(id = R.string.security)
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

//region Vault settings

@Composable
fun VaultSettings(
    modifier: Modifier = Modifier,
    onExportClick: () -> Unit,
) {
    BitwardenListHeaderText(
        modifier = Modifier.padding(horizontal = 16.dp),
        label = stringResource(id = R.string.vault)
    )
    Spacer(modifier = Modifier.height(8.dp))
    BitwardenTextRow(
        text = stringResource(id = R.string.export),
        onClick = onExportClick,
        modifier = modifier,
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
        }
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

//endregion Vault settings

//region Appearance settings

@Composable
private fun AppearanceSettings(
    state: SettingsState,
    onLanguageSelection: (language: AppLanguage) -> Unit,
    onThemeSelection: (theme: AppTheme) -> Unit,
) {
    BitwardenListHeaderText(
        modifier = Modifier.padding(horizontal = 16.dp),
        label = stringResource(id = R.string.appearance)
    )
    ThemeSelectionRow(
        currentSelection = state.appearance.theme,
        onThemeSelection = onThemeSelection,
        modifier = Modifier
            .semantics { testTag = "ThemeChooser" }
            .fillMaxWidth(),
    )

    LanguageSelectionRow(
        currentSelection = state.appearance.language,
        onLanguageSelection = onLanguageSelection,
        modifier = Modifier
            .semantics { testTag = "LanguageChooser" }
            .fillMaxWidth(),
    )
}

@Composable
private fun LanguageSelectionRow(
    currentSelection: AppLanguage,
    onLanguageSelection: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    var languageChangedDialogState: BasicDialogState by rememberSaveable {
        mutableStateOf(BasicDialogState.Hidden)
    }
    var shouldShowLanguageSelectionDialog by rememberSaveable { mutableStateOf(false) }

    BitwardenBasicDialog(
        visibilityState = languageChangedDialogState,
        onDismissRequest = { languageChangedDialogState = BasicDialogState.Hidden },
    )

    BitwardenTextRow(
        text = stringResource(id = R.string.language),
        onClick = { shouldShowLanguageSelectionDialog = true },
        modifier = modifier,
        withDivider = true,
    ) {
        Icon(
            modifier = Modifier
                .mirrorIfRtl()
                .size(24.dp),
            painter = painterResource(id = R.drawable.ic_navigate_next),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }

    if (shouldShowLanguageSelectionDialog) {
        BitwardenSelectionDialog(
            title = stringResource(id = R.string.language),
            onDismissRequest = { shouldShowLanguageSelectionDialog = false },
        ) {
            AppLanguage.entries.forEach { option ->
                BitwardenSelectionRow(
                    text = option.text,
                    isSelected = option == currentSelection,
                    onClick = {
                        shouldShowLanguageSelectionDialog = false
                        onLanguageSelection(option)
                        languageChangedDialogState = BasicDialogState.Shown(
                            title = R.string.language.asText(),
                            message = R.string.language_change_x_description.asText(option.text),
                        )
                    },
                )
            }
        }
    }
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
                id = R.drawable.ic_navigate_next
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
        label = stringResource(id = R.string.help)
    )
    BitwardenTextRow(
        text = stringResource(id = R.string.launch_tutorial),
        onClick = onTutorialClick,
        modifier = modifier,
        withDivider = true,
    )
    Spacer(modifier = Modifier.height(8.dp))
    BitwardenExternalLinkRow(
        text = stringResource(id = R.string.bitwarden_help_center),
        onConfirmClick = onHelpCenterClick,
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
        label = stringResource(id = R.string.about)
    )
    BitwardenWideSwitch(
        modifier = modifier,
        label = stringResource(id = R.string.submit_crash_logs),
        isChecked = state.isSubmitCrashLogsEnabled,
        onCheckedChange = onSubmitCrashLogsCheckedChange,
    )
    BitwardenExternalLinkRow(
        text = stringResource(id = R.string.privacy_policy),
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
                indication = rememberRipple(color = MaterialTheme.colorScheme.primary),
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
