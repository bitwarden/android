package com.x8bit.bitwarden.ui.auth.feature.landing

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.x8bit.bitwarden.ui.platform.components.account.BitwardenAccountSwitcher
import com.x8bit.bitwarden.ui.platform.components.account.BitwardenPlaceholderAccountActionItem
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.ui.platform.components.dropdown.EnvironmentSelector
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.model.CardStyle
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.toggle.BitwardenSwitch
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
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
    onNavigateToStartRegistration: () -> Unit,
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
            LandingEvent.NavigateToStartRegistration -> onNavigateToStartRegistration()
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
                title = stringResource(id = R.string.an_error_has_occurred),
                message = dialog.message(),
                onDismissRequest = remember(viewModel) {
                    { viewModel.trySendAction(LandingAction.DialogDismiss) }
                },
            )
        }

        null -> Unit
    }

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
            if (state.isAppBarVisible) {
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
        overlay = {
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
                modifier = Modifier.fillMaxSize(),
            )
        },
    ) {
        LandingScreenContent(
            state = state,
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
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun LandingScreenContent(
    state: LandingState,
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
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding(),
    ) {
        Spacer(modifier = Modifier.height(height = 12.dp))
        Spacer(modifier = Modifier.weight(1f))

        Image(
            painter = rememberVectorPainter(id = R.drawable.bitwarden_logo),
            colorFilter = ColorFilter.tint(BitwardenTheme.colorScheme.icon.secondary),
            contentDescription = null,
            modifier = Modifier
                .standardHorizontalMargin()
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(height = 12.dp))

        Text(
            text = stringResource(id = R.string.login_to_bitwarden),
            textAlign = TextAlign.Center,
            style = BitwardenTheme.typography.headlineSmall,
            color = BitwardenTheme.colorScheme.text.primary,
            modifier = Modifier
                .standardHorizontalMargin()
                .wrapContentHeight(),
        )

        Spacer(modifier = Modifier.height(height = 24.dp))

        BitwardenTextField(
            modifier = Modifier
                .standardHorizontalMargin()
                .fillMaxWidth(),
            value = state.emailInput,
            onValueChange = onEmailInputChange,
            label = stringResource(id = R.string.email_address),
            keyboardType = KeyboardType.Email,
            textFieldTestTag = "EmailAddressEntry",
            cardStyle = CardStyle.Full,
            supportingContentPadding = PaddingValues(),
            supportingContent = {
                EnvironmentSelector(
                    labelText = stringResource(id = R.string.logging_in_on_with_colon),
                    dialogTitle = stringResource(id = R.string.logging_in_on),
                    selectedOption = state.selectedEnvironmentType,
                    onOptionSelected = onEnvironmentTypeSelect,
                    isHelpEnabled = false,
                    onHelpClick = {},
                    modifier = Modifier
                        .testTag("RegionSelectorDropdown")
                        .fillMaxWidth(),
                )
            },
        )

        Spacer(modifier = Modifier.height(height = 8.dp))

        BitwardenSwitch(
            label = stringResource(id = R.string.remember_email),
            isChecked = state.isRememberEmailEnabled,
            onCheckedChange = onRememberMeToggle,
            cardStyle = CardStyle.Full,
            modifier = Modifier
                .testTag("RememberMeSwitch")
                .standardHorizontalMargin()
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(height = 24.dp))

        BitwardenFilledButton(
            label = stringResource(id = R.string.continue_text),
            onClick = onContinueClick,
            isEnabled = state.isContinueButtonEnabled,
            modifier = Modifier
                .testTag("ContinueButton")
                .standardHorizontalMargin()
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(height = 24.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .standardHorizontalMargin()
                .fillMaxWidth()
                .wrapContentHeight(),
        ) {
            Text(
                text = stringResource(id = R.string.new_to_bitwarden),
                style = BitwardenTheme.typography.bodyMedium,
                color = BitwardenTheme.colorScheme.text.secondary,
            )

            BitwardenTextButton(
                label = stringResource(id = R.string.create_an_account),
                onClick = onCreateAccountClick,
                modifier = Modifier
                    .testTag("CreateAccountLabel"),
            )
        }

        Spacer(modifier = Modifier.height(height = 12.dp))
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}
