package com.x8bit.bitwarden.ui.auth.feature.createaccount

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.annotatedStringResource
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.theme.BitwardenTheme
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
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.toggle.BitwardenSwitch
import com.x8bit.bitwarden.ui.platform.composition.LocalIntentManager
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager

/**
 * Top level composable for the create account screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun CreateAccountScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: (emailAddress: String, captchaToken: String?) -> Unit,
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
                title = dialog.title?.invoke(),
                message = dialog.message(),
                throwable = dialog.error,
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
            BitwardenLoadingDialog(text = stringResource(id = R.string.create_account))
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
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(height = 12.dp))
            BitwardenTextField(
                label = stringResource(id = R.string.email_address),
                value = state.emailInput,
                onValueChange = remember(viewModel) {
                    { viewModel.trySendAction(EmailInputChange(it)) }
                },
                keyboardType = KeyboardType.Email,
                textFieldTestTag = "EmailAddressEntry",
                cardStyle = CardStyle.Full,
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )
            Spacer(modifier = Modifier.height(height = 8.dp))
            var showPassword by rememberSaveable { mutableStateOf(false) }
            BitwardenPasswordField(
                label = stringResource(id = R.string.master_password),
                showPassword = showPassword,
                showPasswordChange = { showPassword = it },
                value = state.passwordInput,
                onValueChange = remember(viewModel) {
                    { viewModel.trySendAction(PasswordInputChange(it)) }
                },
                showPasswordTestTag = "PasswordVisibilityToggle",
                supportingContent = {
                    PasswordStrengthIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        state = state.passwordStrengthState,
                        currentCharacterCount = state.passwordInput.length,
                    )
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = state.passwordLengthLabel(),
                        style = BitwardenTheme.typography.bodySmall,
                        color = BitwardenTheme.colorScheme.text.secondary,
                    )
                },
                passwordFieldTestTag = "MasterPasswordEntry",
                cardStyle = CardStyle.Top(dividerPadding = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )
            BitwardenPasswordField(
                label = stringResource(id = R.string.retype_master_password),
                value = state.confirmPasswordInput,
                showPassword = showPassword,
                showPasswordChange = { showPassword = it },
                onValueChange = remember(viewModel) {
                    { viewModel.trySendAction(ConfirmPasswordInputChange(it)) }
                },
                showPasswordTestTag = "ConfirmPasswordVisibilityToggle",
                passwordFieldTestTag = "ConfirmMasterPasswordEntry",
                cardStyle = CardStyle.Middle(dividerPadding = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )
            BitwardenTextField(
                label = stringResource(id = R.string.master_password_hint),
                value = state.passwordHintInput,
                onValueChange = remember(viewModel) {
                    { viewModel.trySendAction(PasswordHintChange(it)) }
                },
                supportingText = stringResource(id = R.string.master_password_hint_description),
                textFieldTestTag = "MasterPasswordHintLabel",
                cardStyle = CardStyle.Bottom,
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )
            Spacer(modifier = Modifier.height(height = 8.dp))
            BitwardenSwitch(
                label = stringResource(id = R.string.check_known_data_breaches_for_this_password),
                isChecked = state.isCheckDataBreachesToggled,
                onCheckedChange = remember(viewModel) {
                    { newState ->
                        viewModel.trySendAction(CheckDataBreachesToggle(newState = newState))
                    }
                },
                cardStyle = CardStyle.Top(),
                modifier = Modifier
                    .testTag("CheckExposedMasterPasswordToggle")
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )
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
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )
            Spacer(modifier = Modifier.height(height = 16.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun TermsAndPrivacySwitch(
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strTerms = stringResource(id = R.string.terms_of_service)
    val strPrivacy = stringResource(id = R.string.privacy_policy)
    BitwardenSwitch(
        modifier = modifier.semantics(mergeDescendants = true) {
            customActions = listOf(
                CustomAccessibilityAction(
                    label = strTerms,
                    action = {
                        onTermsClick()
                        true
                    },
                ),
                CustomAccessibilityAction(
                    label = strPrivacy,
                    action = {
                        onPrivacyPolicyClick()
                        true
                    },
                ),
            )
        },
        label = annotatedStringResource(
            id = R.string
                .by_activating_this_switch_you_agree_to_the_terms_of_service_and_privacy_policy,
            onAnnotationClick = {
                when (it) {
                    "termsOfService" -> onTermsClick()
                    "privacyPolicy" -> onPrivacyPolicyClick()
                }
            },
        ),
        isChecked = isChecked,
        contentDescription = "AcceptPoliciesToggle",
        onCheckedChange = onCheckedChange,
        cardStyle = CardStyle.Bottom,
    )
}
