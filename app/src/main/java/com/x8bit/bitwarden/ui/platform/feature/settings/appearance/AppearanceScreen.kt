package com.x8bit.bitwarden.ui.platform.feature.settings.appearance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenSelectionDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.row.BitwardenSelectionRow
import com.x8bit.bitwarden.ui.platform.components.row.BitwardenTextRow
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.toggle.BitwardenSwitch
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppLanguage
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.platform.util.displayLabel

/**
 * Displays the appearance screen.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    onNavigateBack: () -> Unit,
    viewModel: AppearanceViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            AppearanceEvent.NavigateBack -> onNavigateBack.invoke()
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.appearance),
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = R.drawable.ic_back),
                navigationIconContentDescription = stringResource(id = R.string.back),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(AppearanceAction.BackClick) }
                },
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            LanguageSelectionRow(
                currentSelection = state.language,
                onLanguageSelection = remember(viewModel) {
                    { viewModel.trySendAction(AppearanceAction.LanguageChange(it)) }
                },
                modifier = Modifier
                    .testTag("LanguageChooser")
                    .fillMaxWidth(),
            )

            ThemeSelectionRow(
                currentSelection = state.theme,
                onThemeSelection = remember(viewModel) {
                    { viewModel.trySendAction(AppearanceAction.ThemeChange(it)) }
                },
                modifier = Modifier
                    .testTag("ThemeChooser")
                    .fillMaxWidth(),
            )

            BitwardenSwitch(
                label = stringResource(id = R.string.show_website_icons),
                description = stringResource(id = R.string.show_website_icons_description),
                isChecked = state.showWebsiteIcons,
                onCheckedChange = remember(viewModel) {
                    { viewModel.trySendAction(AppearanceAction.ShowWebsiteIconsToggle(it)) }
                },
                modifier = Modifier
                    .testTag("ShowWebsiteIconsSwitch")
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun LanguageSelectionRow(
    currentSelection: AppLanguage,
    onLanguageSelection: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    var languageChangedDialogOption: Text? by rememberSaveable { mutableStateOf(value = null) }
    var shouldShowLanguageSelectionDialog by rememberSaveable { mutableStateOf(value = false) }

    languageChangedDialogOption?.let {
        BitwardenBasicDialog(
            title = stringResource(id = R.string.language),
            message = stringResource(id = R.string.language_change_x_description, it.invoke()),
            onDismissRequest = { languageChangedDialogOption = null },
        )
    }
    BitwardenTextRow(
        text = stringResource(id = R.string.language),
        onClick = { shouldShowLanguageSelectionDialog = true },
        modifier = modifier,
    ) {
        Text(
            text = currentSelection.text(),
            style = BitwardenTheme.typography.labelSmall,
            color = BitwardenTheme.colorScheme.text.primary,
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
                        languageChangedDialogOption = option.text
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
            style = BitwardenTheme.typography.labelSmall,
            color = BitwardenTheme.colorScheme.text.primary,
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
