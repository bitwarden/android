package com.x8bit.bitwarden.ui.platform.feature.settings.appearance

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.BasicDialogState
import com.x8bit.bitwarden.ui.platform.components.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.BitwardenSelectionDialog
import com.x8bit.bitwarden.ui.platform.components.BitwardenSelectionRow
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextRow
import com.x8bit.bitwarden.ui.platform.components.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.BitwardenWideSwitch
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppLanguage

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
                navigationIcon = painterResource(id = R.drawable.ic_back),
                navigationIconContentDescription = stringResource(id = R.string.back),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(AppearanceAction.BackClick) }
                },
            )
        },
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            LanguageSelectionRow(
                currentSelection = state.language,
                onLanguageSelection = remember(viewModel) {
                    { viewModel.trySendAction(AppearanceAction.LanguageChange(it)) }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            ThemeSelectionRow(
                currentSelection = state.theme,
                onThemeSelection = remember(viewModel) {
                    { viewModel.trySendAction(AppearanceAction.ThemeChange(it)) }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            BitwardenWideSwitch(
                label = stringResource(id = R.string.show_website_icons),
                description = stringResource(id = R.string.show_website_icons_description),
                isChecked = state.showWebsiteIcons,
                onCheckedChange = remember(viewModel) {
                    { viewModel.trySendAction(AppearanceAction.ShowWebsiteIconsToggle(it)) }
                },
                modifier = Modifier
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
    currentSelection: AppearanceState.Theme,
    onThemeSelection: (AppearanceState.Theme) -> Unit,
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
            text = currentSelection.text(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (shouldShowThemeSelectionDialog) {
        BitwardenSelectionDialog(
            title = stringResource(id = R.string.theme),
            onDismissRequest = { shouldShowThemeSelectionDialog = false },
        ) {
            AppearanceState.Theme.entries.forEach { option ->
                BitwardenSelectionRow(
                    text = option.text,
                    isSelected = option == currentSelection,
                    onClick = {
                        shouldShowThemeSelectionDialog = false
                        onThemeSelection(
                            AppearanceState.Theme.entries.first { it == option },
                        )
                    },
                )
            }
        }
    }
}
