package com.bitwarden.authenticator.ui.platform.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.ui.platform.base.util.EventsEffect
import com.bitwarden.authenticator.ui.platform.base.util.asText
import com.bitwarden.authenticator.ui.platform.base.util.mirrorIfRtl
import com.bitwarden.authenticator.ui.platform.components.appbar.BitwardenMediumTopAppBar
import com.bitwarden.authenticator.ui.platform.components.dialog.BasicDialogState
import com.bitwarden.authenticator.ui.platform.components.dialog.BitwardenBasicDialog
import com.bitwarden.authenticator.ui.platform.components.dialog.BitwardenSelectionDialog
import com.bitwarden.authenticator.ui.platform.components.dialog.BitwardenSelectionRow
import com.bitwarden.authenticator.ui.platform.components.header.BitwardenListHeaderText
import com.bitwarden.authenticator.ui.platform.components.row.BitwardenTextRow
import com.bitwarden.authenticator.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppLanguage
import com.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppTheme
import com.bitwarden.authenticator.ui.platform.util.displayLabel

/**
 * Display the settings screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
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
        }
    }

    BitwardenScaffold(
        topBar = {
            BitwardenMediumTopAppBar(
                title = stringResource(id = R.string.settings),
                scrollBehavior = scrollBehavior
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(state = rememberScrollState())
        ) {
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

            HelpSettings(
                onTutorialClick = remember(viewModel) {
                    {
                        viewModel.trySendAction(SettingsAction.HelpClick.ShowTutorialClick)
                    }
                }
            )
        }
    }
}

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
}

//endregion Help settings
