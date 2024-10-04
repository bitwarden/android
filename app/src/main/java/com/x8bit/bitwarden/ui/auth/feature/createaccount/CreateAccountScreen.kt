package com.x8bit.bitwarden.ui.auth.feature.createaccount

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.auth.feature.completeregistration.PasswordStrengthIndicator
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.AcceptPoliciesToggle
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.CheckDataBreachesToggle
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.CloseClick
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.ConfirmPasswordInputChange
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.ContinueWithBreachedPasswordClick
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.EmailInputChange
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.ErrorDialogDismiss
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.PasswordHintChange
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.PasswordInputChange
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.PrivacyPolicyClick
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.SubmitClick
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.TermsClick
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountEvent.NavigateToPrivacyPolicy
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountEvent.NavigateToTerms
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.LoadingDialogState
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.text.BitwardenClickableText
import com.x8bit.bitwarden.ui.platform.components.toggle.BitwardenSwitch
import com.x8bit.bitwarden.ui.platform.components.toggle.color.bitwardenSwitchColors
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.composition.LocalIntentManager
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Top level composable for the create account screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun CreateAccountScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: (emailAddress: String, captchaToken: String) -> Unit,
    intentManager: IntentManager = LocalIntentManager.current,
    viewModel: CreateAccountViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    EventsEffect(viewModel) { event ->
        when (event) {
            is NavigateToPrivacyPolicy -> {
                intentManager.launchUri("https://bitwarden.com/privacy/".toUri())
            }

            is NavigateToTerms -> {
                intentManager.launchUri("https://bitwarden.com/terms/".toUri())
            }

            is CreateAccountEvent.NavigateBack -> onNavigateBack.invoke()
            is CreateAccountEvent.ShowToast -> {
                Toast.makeText(context, event.text, Toast.LENGTH_SHORT).show()
            }

            is CreateAccountEvent.NavigateToCaptcha -> {
                intentManager.startCustomTabsActivity(uri = event.uri)
            }

            is CreateAccountEvent.NavigateToLogin -> {
                onNavigateToLogin(
                    event.email,
                    event.captchaToken,
                )
            }
        }
    }

    // Show dialog if needed:
    when (val dialog = state.dialog) {
        is CreateAccountDialog.Error -> {
            BitwardenBasicDialog(
                visibilityState = dialog.state,
                onDismissRequest = remember(viewModel) {
                    { viewModel.trySendAction(ErrorDialogDismiss) }
                },
            )
        }

        is CreateAccountDialog.HaveIBeenPwned -> {
            BitwardenTwoButtonDialog(
                title = dialog.title(),
                message = dialog.message(),
                confirmButtonText = stringResource(id = R.string.yes),
                dismissButtonText = stringResource(id = R.string.no),
                onConfirmClick = remember(viewModel) {
                    { viewModel.trySendAction(ContinueWithBreachedPasswordClick) }
                },
                onDismissClick = remember(viewModel) {
                    { viewModel.trySendAction(ErrorDialogDismiss) }
                },
                onDismissRequest = remember(viewModel) {
                    { viewModel.trySendAction(ErrorDialogDismiss) }
                },
            )
        }

        CreateAccountDialog.Loading -> {
            BitwardenLoadingDialog(
                visibilityState = LoadingDialogState.Shown(R.string.create_account.asText()),
            )
        }

        null -> Unit
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.create_account),
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = R.drawable.ic_close),
                navigationIconContentDescription = stringResource(id = R.string.close),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(CloseClick) }
                },
                actions = {
                    BitwardenTextButton(
                        label = stringResource(id = R.string.submit),
                        onClick = remember(viewModel) {
                            { viewModel.trySendAction(SubmitClick) }
                        },
                        modifier = Modifier.testTag("SubmitButton"),
                    )
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .imePadding()
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            BitwardenTextField(
                label = stringResource(id = R.string.email_address),
                value = state.emailInput,
                onValueChange = remember(viewModel) {
                    { viewModel.trySendAction(EmailInputChange(it)) }
                },
                modifier = Modifier
                    .testTag("EmailAddressEntry")
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                keyboardType = KeyboardType.Email,
            )
            Spacer(modifier = Modifier.height(16.dp))
            var showPassword by rememberSaveable { mutableStateOf(false) }
            BitwardenPasswordField(
                label = stringResource(id = R.string.master_password),
                showPassword = showPassword,
                showPasswordChange = { showPassword = it },
                value = state.passwordInput,
                hint = state.passwordLengthLabel(),
                onValueChange = remember(viewModel) {
                    { viewModel.trySendAction(PasswordInputChange(it)) }
                },
                modifier = Modifier
                    .testTag("MasterPasswordEntry")
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                showPasswordTestTag = "PasswordVisibilityToggle",
            )
            Spacer(modifier = Modifier.height(8.dp))
            PasswordStrengthIndicator(
                modifier = Modifier.padding(horizontal = 16.dp),
                state = state.passwordStrengthState,
                currentCharacterCount = state.passwordInput.length,
            )
            Spacer(modifier = Modifier.height(8.dp))
            BitwardenPasswordField(
                label = stringResource(id = R.string.retype_master_password),
                value = state.confirmPasswordInput,
                showPassword = showPassword,
                showPasswordChange = { showPassword = it },
                onValueChange = remember(viewModel) {
                    { viewModel.trySendAction(ConfirmPasswordInputChange(it)) }
                },
                modifier = Modifier
                    .testTag("ConfirmMasterPasswordEntry")
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                showPasswordTestTag = "ConfirmPasswordVisibilityToggle",
            )
            Spacer(modifier = Modifier.height(16.dp))
            BitwardenTextField(
                label = stringResource(id = R.string.master_password_hint),
                value = state.passwordHintInput,
                onValueChange = remember(viewModel) {
                    { viewModel.trySendAction(PasswordHintChange(it)) }
                },
                hint = stringResource(id = R.string.master_password_hint_description),
                modifier = Modifier
                    .testTag("MasterPasswordHintLabel")
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(24.dp))
            BitwardenSwitch(
                label = stringResource(id = R.string.check_known_data_breaches_for_this_password),
                isChecked = state.isCheckDataBreachesToggled,
                onCheckedChange = remember(viewModel) {
                    { newState ->
                        viewModel.trySendAction(CheckDataBreachesToggle(newState = newState))
                    }
                },
                modifier = Modifier
                    .testTag("CheckExposedMasterPasswordToggle")
                    .padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            TermsAndPrivacySwitch(
                isChecked = state.isAcceptPoliciesToggled,
                onCheckedChange = remember(viewModel) {
                    { viewModel.trySendAction(AcceptPoliciesToggle(it)) }
                },
                onTermsClick = remember(viewModel) {
                    { viewModel.trySendAction(TermsClick) }
                },
                onPrivacyPolicyClick = remember(viewModel) {
                    { viewModel.trySendAction(PrivacyPolicyClick) }
                },
            )
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Suppress("LongMethod")
@Composable
private fun TermsAndPrivacySwitch(
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .semantics(mergeDescendants = true) {
                testTag = "AcceptPoliciesToggle"
                toggleableState = ToggleableState(isChecked)
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(
                    color = BitwardenTheme.colorScheme.background.pressed,
                ),
                onClick = { onCheckedChange.invoke(!isChecked) },
            )
            .padding(start = 16.dp)
            .fillMaxWidth(),
    ) {
        Switch(
            modifier = Modifier
                .height(32.dp)
                .width(52.dp),
            checked = isChecked,
            onCheckedChange = null,
            colors = bitwardenSwitchColors(),
        )
        Column(Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp)) {
            Text(
                text = stringResource(id = R.string.accept_policies),
                style = BitwardenTheme.typography.bodyLarge,
                color = BitwardenTheme.colorScheme.text.primary,
            )
            FlowRow(
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .fillMaxWidth()
                    .wrapContentHeight(),
            ) {
                BitwardenClickableText(
                    label = stringResource(id = R.string.terms_of_service),
                    onClick = onTermsClick,
                    style = BitwardenTheme.typography.bodyMedium,
                    innerPadding = PaddingValues(vertical = 4.dp, horizontal = 0.dp),
                )
                Text(
                    text = ",",
                    style = BitwardenTheme.typography.bodyMedium,
                    color = BitwardenTheme.colorScheme.text.primary,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                BitwardenClickableText(
                    label = stringResource(id = R.string.privacy_policy),
                    onClick = onPrivacyPolicyClick,
                    style = BitwardenTheme.typography.bodyMedium,
                    innerPadding = PaddingValues(vertical = 4.dp, horizontal = 0.dp),
                )
            }
        }
    }
}
