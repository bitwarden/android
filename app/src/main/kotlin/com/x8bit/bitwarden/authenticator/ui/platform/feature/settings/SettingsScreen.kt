package com.x8bit.bitwarden.authenticator.ui.platform.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.authenticator.R
import com.x8bit.bitwarden.authenticator.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.authenticator.ui.platform.base.util.asText
import com.x8bit.bitwarden.authenticator.ui.platform.components.appbar.BitwardenMediumTopAppBar
import com.x8bit.bitwarden.authenticator.ui.platform.components.dialog.BasicDialogState
import com.x8bit.bitwarden.authenticator.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.authenticator.ui.platform.components.dialog.BitwardenSelectionDialog
import com.x8bit.bitwarden.authenticator.ui.platform.components.dialog.BitwardenSelectionRow
import com.x8bit.bitwarden.authenticator.ui.platform.components.header.BitwardenListHeaderText
import com.x8bit.bitwarden.authenticator.ui.platform.components.row.BitwardenTextRow
import com.x8bit.bitwarden.authenticator.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.authenticator.ui.platform.components.toggle.BitwardenWideSwitch
import com.x8bit.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppLanguage
import com.x8bit.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppTheme
import com.x8bit.bitwarden.authenticator.ui.platform.util.displayLabel

/**
 * Display the settings screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateToTutorial: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            SettingsEvent.NavigateToTutorial -> onNavigateToTutorial()
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
                onShowWebsiteIconsChange = remember(viewModel) {
                    {
                        viewModel.trySendAction(
                            SettingsAction.AppearanceChange.ShowWebsiteIconsChange(it)
                        )
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

//region Appearance settings

@Composable
private fun AppearanceSettings(
    state: SettingsState,
    onLanguageSelection: (language: AppLanguage) -> Unit,
    onThemeSelection: (theme: AppTheme) -> Unit,
    onShowWebsiteIconsChange: (showWebsiteIcons: Boolean) -> Unit,
) {
    BitwardenListHeaderText(
        modifier = Modifier.padding(horizontal = 16.dp),
        label = stringResource(id = R.string.appearance)
    )
    LanguageSelectionRow(
        currentSelection = state.appearance.language,
        onLanguageSelection = onLanguageSelection,
        modifier = Modifier
            .semantics { testTag = "LanguageChooser" }
            .fillMaxWidth(),
    )

    ThemeSelectionRow(
        currentSelection = state.appearance.theme,
        onThemeSelection = onThemeSelection,
        modifier = Modifier
            .semantics { testTag = "ThemeChooser" }
            .fillMaxWidth(),
    )

    BitwardenWideSwitch(
        label = stringResource(id = R.string.show_website_icons),
        description = stringResource(id = R.string.show_website_icons_description),
        isChecked = state.appearance.showWebsiteIcons,
        onCheckedChange = onShowWebsiteIconsChange,
        modifier = Modifier
            .semantics { testTag = "ShowWebsiteIconsSwitch" }
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
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
    ) {
        Text(
            text = currentSelection.text(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        description = stringResource(id = R.string.theme_description),
        onClick = { shouldShowThemeSelectionDialog = true },
        modifier = modifier,
    ) {
        Text(
            text = currentSelection.displayLabel(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        text = stringResource(id = R.string.tutorial),
        onClick = onTutorialClick,
        modifier = modifier,
    )
}

//region Help settings
