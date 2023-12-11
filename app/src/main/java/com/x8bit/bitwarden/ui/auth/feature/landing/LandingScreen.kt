package com.x8bit.bitwarden.ui.auth.feature.landing

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.BasicDialogState
import com.x8bit.bitwarden.ui.platform.components.BitwardenAccountSwitcher
import com.x8bit.bitwarden.ui.platform.components.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.BitwardenFilledButton
import com.x8bit.bitwarden.ui.platform.components.BitwardenPlaceholderAccountActionItem
import com.x8bit.bitwarden.ui.platform.components.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.BitwardenSelectionDialog
import com.x8bit.bitwarden.ui.platform.components.BitwardenSelectionRow
import com.x8bit.bitwarden.ui.platform.components.BitwardenSwitch
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextButton
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.ui.platform.util.displayLabel
import kotlinx.collections.immutable.toImmutableList

/**
 * The top level composable for the Landing screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongMethod")
fun LandingScreen(
    onNavigateToCreateAccount: () -> Unit,
    onNavigateToLogin: (emailAddress: String) -> Unit,
    onNavigateToEnvironment: () -> Unit,
    viewModel: LandingViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            LandingEvent.NavigateToCreateAccount -> onNavigateToCreateAccount()
            is LandingEvent.NavigateToLogin -> onNavigateToLogin(
                event.emailAddress,
            )

            LandingEvent.NavigateToEnvironment -> onNavigateToEnvironment()
        }
    }

    when (val dialog = state.dialog) {
        is LandingState.DialogState.AccountAlreadyAdded -> {
            BitwardenTwoButtonDialog(
                title = stringResource(id = R.string.account_already_added),
                message = stringResource(
                    id = R.string.switch_to_already_added_account_confirmation,
                ),
                confirmButtonText = stringResource(id = R.string.yes),
                dismissButtonText = stringResource(id = R.string.cancel),
                onConfirmClick = remember(viewModel) {
                    {
                        viewModel.trySendAction(
                            LandingAction.ConfirmSwitchToMatchingAccountClick(
                                accountSummary = dialog.accountSummary,
                            ),
                        )
                    }
                },
                onDismissClick = remember(viewModel) {
                    { viewModel.trySendAction(LandingAction.DialogDismiss) }
                },
                onDismissRequest = remember(viewModel) {
                    { viewModel.trySendAction(LandingAction.DialogDismiss) }
                },
            )
        }

        is LandingState.DialogState.Error -> {
            BitwardenBasicDialog(
                visibilityState = BasicDialogState.Shown(
                    title = R.string.an_error_has_occurred.asText(),
                    message = dialog.message,
                ),
                onDismissRequest = remember(viewModel) {
                    { viewModel.trySendAction(LandingAction.DialogDismiss) }
                },
            )
        }

        null -> Unit
    }

    val isAppBarVisible = state.accountSummaries.isNotEmpty()
    var isAccountMenuVisible by rememberSaveable { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(
        state = rememberTopAppBarState(),
        canScroll = { !isAccountMenuVisible },
    )
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (isAppBarVisible) {
                BitwardenTopAppBar(
                    title = "",
                    scrollBehavior = scrollBehavior,
                    navigationIcon = null,
                    actions = {
                        BitwardenPlaceholderAccountActionItem(
                            onClick = { isAccountMenuVisible = !isAccountMenuVisible },
                        )
                    },
                )
            }
        },
    ) { innerPadding ->
        LandingScreenContent(
            state = state,
            isAppBarVisible = isAppBarVisible,
            onEmailInputChange = remember(viewModel) {
                { viewModel.trySendAction(LandingAction.EmailInputChanged(it)) }
            },
            onEnvironmentTypeSelect = remember(viewModel) {
                { viewModel.trySendAction(LandingAction.EnvironmentTypeSelect(it)) }
            },
            onRememberMeToggle = remember(viewModel) {
                { viewModel.trySendAction(LandingAction.RememberMeToggle(it)) }
            },
            onContinueClick = remember(viewModel) {
                { viewModel.trySendAction(LandingAction.ContinueButtonClick) }
            },
            onCreateAccountClick = remember(viewModel) {
                { viewModel.trySendAction(LandingAction.CreateAccountClick) }
            },
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        )

        val context = LocalContext.current
        BitwardenAccountSwitcher(
            isVisible = isAccountMenuVisible,
            accountSummaries = state.accountSummaries.toImmutableList(),
            onSwitchAccountClick = remember(viewModel) {
                { viewModel.trySendAction(LandingAction.SwitchAccountClick(it)) }
            },
            onLockAccountClick = remember(viewModel) {
                { viewModel.trySendAction(LandingAction.LockAccountClick(it)) }
            },
            onLogoutAccountClick = remember(viewModel) {
                { viewModel.trySendAction(LandingAction.LogoutAccountClick(it)) }
            },
            onAddAccountClick = {
                // Not available
            },
            onDismissRequest = { isAccountMenuVisible = false },
            isAddAccountAvailable = false,
            topAppBarScrollBehavior = scrollBehavior,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        )
    }
}

@Suppress("LongMethod")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun LandingScreenContent(
    state: LandingState,
    isAppBarVisible: Boolean,
    onEmailInputChange: (String) -> Unit,
    onEnvironmentTypeSelect: (Environment.Type) -> Unit,
    onRememberMeToggle: (Boolean) -> Unit,
    onContinueClick: () -> Unit,
    onCreateAccountClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .semantics { testTagsAsResourceId = true }
            .imePadding()
            .verticalScroll(rememberScrollState()),
    ) {
        val topPadding = if (isAppBarVisible) 40.dp else 104.dp
        Spacer(modifier = Modifier.height(topPadding))

        Image(
            painter = painterResource(id = R.drawable.logo),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
            contentDescription = null,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .width(220.dp)
                .height(74.dp)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = stringResource(id = R.string.login_or_create_new_account),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(
                    horizontal = 24.dp,
                    vertical = 8.dp,
                )
                .wrapContentHeight(),
        )

        Spacer(modifier = Modifier.weight(1f))

        Spacer(modifier = Modifier.height(32.dp))

        BitwardenTextField(
            modifier = Modifier
                .semantics { testTag = "EmailAddressEntry" }
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            value = state.emailInput,
            onValueChange = onEmailInputChange,
            label = stringResource(id = R.string.email_address),
            keyboardType = KeyboardType.Email,
        )

        Spacer(modifier = Modifier.height(10.dp))

        EnvironmentSelector(
            selectedOption = state.selectedEnvironmentType,
            onOptionSelected = onEnvironmentTypeSelect,
            modifier = Modifier
                .semantics { testTag = "RegionSelectorDropdown" }
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        BitwardenSwitch(
            label = stringResource(id = R.string.remember_me),
            isChecked = state.isRememberMeEnabled,
            onCheckedChange = onRememberMeToggle,
            modifier = Modifier
                .semantics { testTag = "RememberMeSwitch" }
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(32.dp))

        BitwardenFilledButton(
            label = stringResource(id = R.string.continue_text),
            onClick = onContinueClick,
            isEnabled = state.isContinueButtonEnabled,
            modifier = Modifier
                .semantics { testTag = "ContinueButton" }
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .wrapContentHeight(),
        ) {
            Text(
                text = stringResource(id = R.string.new_around_here),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            BitwardenTextButton(
                label = stringResource(id = R.string.create_account),
                onClick = onCreateAccountClick,
                modifier = Modifier
                    .semantics { testTag = "CreateAccountLabel" },
            )
        }

        Spacer(modifier = Modifier.height(58.dp))
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

/**
 * A dropdown selector UI component specific to region url selection on the Landing screen.
 *
 * This composable displays a dropdown menu allowing users to select a region
 * from a list of options. When an option is selected, it invokes the provided callback
 * and displays the currently selected region on the UI.
 *
 * @param selectedOption The currently selected environment option.
 * @param onOptionSelected A callback that gets invoked when an environment option is selected
 * and passes the selected option as an argument.
 * @param modifier A [Modifier] for the composable.
 *
 */
@Composable
private fun EnvironmentSelector(
    selectedOption: Environment.Type,
    onOptionSelected: (Environment.Type) -> Unit,
    modifier: Modifier,
) {
    val options = Environment.Type.values()
    var shouldShowDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clickable { shouldShowDialog = !shouldShowDialog }
                .fillMaxWidth()
                .padding(start = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(id = R.string.logging_in_on),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp),
            )
            Text(
                text = selectedOption.displayLabel(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 8.dp),
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_region_select_dropdown),
                contentDescription = stringResource(id = R.string.region),
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        if (shouldShowDialog) {
            BitwardenSelectionDialog(
                title = stringResource(id = R.string.logging_in_on),
                onDismissRequest = { shouldShowDialog = false },
            ) {
                options.forEach {
                    BitwardenSelectionRow(
                        text = it.displayLabel,
                        onClick = {
                            onOptionSelected.invoke(it)
                            shouldShowDialog = false
                        },
                        isSelected = it == selectedOption,
                    )
                }
            }
        }
    }
}
