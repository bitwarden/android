package com.x8bit.bitwarden.ui.auth.feature.createaccount

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.AcceptPoliciesToggle
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.CheckDataBreachesToggle
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.CloseClick
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.ConfirmPasswordInputChange
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
import com.x8bit.bitwarden.ui.platform.base.util.IntentHandler
import com.x8bit.bitwarden.ui.platform.components.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.BitwardenPasswordField
import com.x8bit.bitwarden.ui.platform.components.BitwardenSwitch
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextButtonTopAppBar
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.theme.clickableSpanStyle

/**
 * Top level composable for the create account screen.
 */
@Suppress("LongMethod")
@Composable
fun CreateAccountScreen(
    onNavigateBack: () -> Unit,
    intentHandler: IntentHandler = IntentHandler(context = LocalContext.current),
    viewModel: CreateAccountViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsState()
    val context = LocalContext.current
    EventsEffect(viewModel) { event ->
        when (event) {
            is NavigateToPrivacyPolicy -> {
                intentHandler.launchUri("https://bitwarden.com/privacy/".toUri())
            }

            is NavigateToTerms -> {
                intentHandler.launchUri("https://bitwarden.com/terms/".toUri())
            }

            is CreateAccountEvent.NavigateBack -> onNavigateBack.invoke()
            is CreateAccountEvent.ShowToast -> {
                Toast.makeText(context, event.text, Toast.LENGTH_SHORT).show()
            }
        }
    }
    BitwardenBasicDialog(
        visibilityState = state.errorDialogState,
        onDismissRequest = remember(viewModel) { { viewModel.trySendAction(ErrorDialogDismiss) } },
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState()),
    ) {
        BitwardenTextButtonTopAppBar(
            title = stringResource(id = R.string.create_account),
            navigationIcon = painterResource(id = R.drawable.ic_close),
            navigationIconContentDescription = stringResource(id = R.string.close),
            onNavigationIconClick = remember(viewModel) {
                { viewModel.trySendAction(CloseClick) }
            },
            buttonText = stringResource(id = R.string.submit),
            onButtonClick = remember(viewModel) {
                { viewModel.trySendAction(SubmitClick) }
            },
            isButtonEnabled = true,
        )
        Spacer(modifier = Modifier.height(16.dp))
        BitwardenTextField(
            label = stringResource(id = R.string.email_address),
            value = state.emailInput,
            onValueChange = remember(viewModel) {
                { viewModel.trySendAction(EmailInputChange(it)) }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        var showPassword by rememberSaveable { mutableStateOf(false) }
        BitwardenPasswordField(
            label = stringResource(id = R.string.master_password),
            showPassword = showPassword,
            showPasswordChange = { showPassword = it },
            value = state.passwordInput,
            onValueChange = remember(viewModel) {
                { viewModel.trySendAction(PasswordInputChange(it)) }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        BitwardenPasswordField(
            label = stringResource(id = R.string.retype_master_password),
            value = state.confirmPasswordInput,
            showPassword = showPassword,
            showPasswordChange = { showPassword = it },
            onValueChange = remember(viewModel) {
                { viewModel.trySendAction(ConfirmPasswordInputChange(it)) }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        BitwardenTextField(
            label = stringResource(id = R.string.master_password_hint),
            value = state.passwordHintInput,
            onValueChange = remember(viewModel) {
                { viewModel.trySendAction(PasswordHintChange(it)) }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(id = R.string.master_password_hint_description),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .padding(horizontal = 32.dp),
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
    }
}

@Suppress("LongMethod")
@Composable
private fun TermsAndPrivacySwitch(
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
) {
    val clickableStyle = clickableSpanStyle()
    val termsString = stringResource(id = R.string.terms_of_service)
    val privacyString = stringResource(id = R.string.privacy_policy)
    val annotatedString = remember {
        buildAnnotatedString {
            withStyle(style = clickableStyle) {
                pushStringAnnotation(tag = termsString, annotation = termsString)
                append("$termsString,")
            }
            append(" ")
            withStyle(style = clickableStyle) {
                pushStringAnnotation(tag = privacyString, annotation = privacyString)
                append(privacyString)
            }
        }
    }
    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .semantics(mergeDescendants = true) {
                customActions = listOf(
                    CustomAccessibilityAction(
                        label = termsString,
                        action = {
                            onTermsClick.invoke()
                            true
                        },
                    ),
                    CustomAccessibilityAction(
                        label = privacyString,
                        action = {
                            onPrivacyPolicyClick.invoke()
                            true
                        },
                    ),
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(color = MaterialTheme.colorScheme.primary),
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
        )
        Column(Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp)) {
            Text(
                text = stringResource(id = R.string.accept_policies),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            ClickableText(
                text = annotatedString,
                onClick = { offset ->
                    annotatedString
                        .getStringAnnotations(offset, offset)
                        .firstOrNull()
                        ?.let { span ->
                            when (span.tag) {
                                termsString -> onTermsClick.invoke()
                                privacyString -> onPrivacyPolicyClick.invoke()
                                else -> onCheckedChange.invoke(!isChecked)
                            }
                        } ?: onCheckedChange.invoke(!isChecked)
                },
            )
        }
    }
}
