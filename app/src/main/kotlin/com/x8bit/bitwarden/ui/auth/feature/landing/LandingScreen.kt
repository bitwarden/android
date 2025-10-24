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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.data.repository.model.Environment
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.account.BitwardenAccountSwitcher
import com.bitwarden.ui.platform.components.account.BitwardenPlaceholderAccountActionItem
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.bitwarden.ui.platform.components.field.BitwardenTextField
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.snackbar.BitwardenSnackbarHost
import com.bitwarden.ui.platform.components.snackbar.model.rememberBitwardenSnackbarHostState
import com.bitwarden.ui.platform.components.toggle.BitwardenSwitch
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.platform.components.dropdown.EnvironmentSelector
import kotlinx.collections.immutable.toImmutableList

/**
 * The top level composable for the Landing screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongMethod")
fun LandingScreen(
    onNavigateToLogin: (emailAddress: String) -> Unit,
    onNavigateToEnvironment: () -> Unit,
    onNavigateToStartRegistration: () -> Unit,
    onNavigateToPreAuthSettings: () -> Unit,
    viewModel: LandingViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = rememberBitwardenSnackbarHostState()
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is LandingEvent.NavigateToLogin -> onNavigateToLogin(event.emailAddress)
            LandingEvent.NavigateToEnvironment -> onNavigateToEnvironment()
            LandingEvent.NavigateToStartRegistration -> onNavigateToStartRegistration()
            LandingEvent.NavigateToSettings -> onNavigateToPreAuthSettings()
            is LandingEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.data)
        }
    }

    when (val dialog = state.dialog) {
        is LandingState.DialogState.AccountAlreadyAdded -> {
            BitwardenTwoButtonDialog(
                title = stringResource(id = BitwardenString.account_already_added),
                message = stringResource(
                    id = BitwardenString.switch_to_already_added_account_confirmation,
                ),
                confirmButtonText = stringResource(id = BitwardenString.yes),
                dismissButtonText = stringResource(id = BitwardenString.cancel),
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
                title = stringResource(id = BitwardenString.an_error_has_occurred),
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
        snackbarHost = {
            BitwardenSnackbarHost(bitwardenHostState = snackbarHostState)
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
            onAppSettingsClick = remember(viewModel) {
                { viewModel.trySendAction(LandingAction.AppSettingsClick) }
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
    onAppSettingsClick: () -> Unit,
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
            painter = rememberVectorPainter(id = BitwardenDrawable.logo_bitwarden),
            contentDescription = null,
            modifier = Modifier
                .standardHorizontalMargin()
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(height = 12.dp))

        Text(
            text = stringResource(id = BitwardenString.login_to_bitwarden),
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
            label = stringResource(id = BitwardenString.email_address),
            keyboardType = KeyboardType.Email,
            textFieldTestTag = "EmailAddressEntry",
            cardStyle = CardStyle.Full,
            supportingContentPadding = PaddingValues(),
            supportingContent = {
                EnvironmentSelector(
                    labelText = stringResource(id = BitwardenString.logging_in_on_with_colon),
                    dialogTitle = stringResource(id = BitwardenString.logging_in_on),
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
            label = stringResource(id = BitwardenString.remember_email),
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
            label = stringResource(id = BitwardenString.continue_text),
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
                text = stringResource(id = BitwardenString.new_to_bitwarden),
                style = BitwardenTheme.typography.bodyMedium,
                color = BitwardenTheme.colorScheme.text.secondary,
            )

            BitwardenTextButton(
                label = stringResource(id = BitwardenString.create_an_account),
                onClick = onCreateAccountClick,
                modifier = Modifier
                    .testTag("CreateAccountLabel"),
            )
        }
        Spacer(modifier = Modifier.height(height = 8.dp))
        BitwardenTextButton(
            label = stringResource(id = BitwardenString.app_settings),
            onClick = onAppSettingsClick,
            icon = rememberVectorPainter(id = BitwardenDrawable.ic_cog),
            modifier = Modifier
                .standardHorizontalMargin()
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(height = 12.dp))
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}
