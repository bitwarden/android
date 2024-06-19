package com.x8bit.bitwarden.ui.auth.feature.startregistration

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationAction.CloseClick
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationAction.EmailInputChange
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationAction.ErrorDialogDismiss
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationAction.NameInputChange
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationAction.PrivacyPolicyClick
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationAction.TermsClick
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationEvent.NavigateToPrivacyPolicy
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationEvent.NavigateToTerms
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.LoadingDialogState
import com.x8bit.bitwarden.ui.platform.components.dropdown.EnvironmentSelector
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.composition.LocalIntentManager
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager

/**
 * Top level composable for the start registration screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun StartRegistrationScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCompleteRegistration: (
        emailAddress: String,
        verificationToken: String,
        captchaToken: String) -> Unit,
    onNavigateToCheckEmail: (email: String) -> Unit,
    onNavigateToEnvironment: () -> Unit,
    intentManager: IntentManager = LocalIntentManager.current,
    viewModel: StartRegistrationViewModel = hiltViewModel(),
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

            is StartRegistrationEvent.NavigateToUnsubscribe -> {
                intentManager.launchUri("https://bitwarden.com/email-preferences/".toUri())
            }

            is StartRegistrationEvent.NavigateBack -> onNavigateBack.invoke()
            is StartRegistrationEvent.ShowToast -> {
                Toast.makeText(context, event.text, Toast.LENGTH_SHORT).show()
            }

            is StartRegistrationEvent.NavigateToCaptcha -> {
                intentManager.startCustomTabsActivity(uri = event.uri)
            }

            is StartRegistrationEvent.NavigateToCompleteRegistration -> {
                onNavigateToCompleteRegistration(
                    event.email,
                    event.verificationToken,
                    event.captchaToken,
                )
            }

            is StartRegistrationEvent.NavigateToCheckEmail -> {
                onNavigateToCheckEmail(
                    event.email
                )
            }

            StartRegistrationEvent.NavigateToEnvironment -> onNavigateToEnvironment()
        }
    }

    // Show dialog if needed:
    when (val dialog = state.dialog) {
        is StartRegistrationDialog.Error -> {
            BitwardenBasicDialog(
                visibilityState = dialog.state,
                onDismissRequest = remember(viewModel) {
                    { viewModel.trySendAction(ErrorDialogDismiss) }
                },
            )
        }

        StartRegistrationDialog.Loading -> {
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
                }
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
            Spacer(modifier = Modifier.height(2.dp))
            EnvironmentSelector(
                labelText = stringResource(id = R.string.creating_on),
                selectedOption = state.selectedEnvironmentType,
                onOptionSelected = remember(viewModel) {
                    { viewModel.trySendAction(StartRegistrationAction.EnvironmentTypeSelect(it)) }
                },
                modifier = Modifier
                    .testTag("RegionSelectorDropdown")
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            BitwardenTextField(
                label = stringResource(id = R.string.name),
                value = state.nameInput,
                onValueChange = remember(viewModel) {
                    { viewModel.trySendAction(NameInputChange(it)) }
                },
                modifier = Modifier
                    .testTag("NameEntry")
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            ReceiveMarketingEmailsSwitch(
                isChecked = state.isReceiveMarketingEmailsToggled,
                onCheckedChange =  remember(viewModel) {
                    { viewModel.trySendAction(StartRegistrationAction.ReceiveMarketingEmailsToggle(it)) }
                },
                onUnsubscribeClick = remember(viewModel) {
                    { viewModel.trySendAction(StartRegistrationAction.UnsubscribeMarketingEmailsClick) }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            BitwardenFilledButton(
                label = stringResource(id = R.string.continue_text),
                onClick = remember(viewModel) {
                    { viewModel.trySendAction(StartRegistrationAction.ContinueClick) }
                },
                isEnabled = state.isContinueButtonEnabled,
                modifier = Modifier
                    .testTag("ContinueButton")
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            TermsAndPrivacyText(
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
private fun TermsAndPrivacyText(
    onTermsClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
) {
    val annotatedLinkString: AnnotatedString = buildAnnotatedString {
        val strTermsAndPrivacy = stringResource(id = R.string.by_continuing_you_agree_to_the_terms_of_service_and_privacy_policy)
        val strTerms = stringResource(id = R.string.terms_of_service)
        val strPrivacy = stringResource(id = R.string.privacy_policy)
        val startIndexTerms = strTermsAndPrivacy.indexOf(strTerms)
        val endIndexTerms = startIndexTerms + strTerms.length
        val startIndexPrivacy = strTermsAndPrivacy.indexOf(strPrivacy)
        val endIndexPrivacy = startIndexPrivacy + strPrivacy.length
        append(strTermsAndPrivacy)
        addStyle(
            style = SpanStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = MaterialTheme.typography.bodyMedium.fontSize
            ),
            start = 0,
            end = strTermsAndPrivacy.length
        )
        addStyle(
            style = SpanStyle(
                color = MaterialTheme.colorScheme.primary,
                fontSize = MaterialTheme.typography.bodyMedium.fontSize
            ),
            start = startIndexTerms,
            end = endIndexTerms
        )
        addStyle(
            style = SpanStyle(
                color = MaterialTheme.colorScheme.primary,
                fontSize = MaterialTheme.typography.bodyMedium.fontSize
            ),
            start = startIndexPrivacy,
            end = endIndexPrivacy
        )
        addStringAnnotation(
            tag = "URL",
            annotation = strTerms,
            start = startIndexTerms,
            end = endIndexTerms
        )
        addStringAnnotation(
            tag = "URL",
            annotation = strPrivacy,
            start = startIndexPrivacy,
            end = endIndexPrivacy
        )
    }
    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .semantics(mergeDescendants = true) {
                testTag = "AcceptPoliciesText"
            }
            .fillMaxWidth(),
    ) {
        val termsUrl = stringResource(id = R.string.terms_of_service)
        Column(Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp)) {
            ClickableText(
                text = annotatedLinkString,
                style = MaterialTheme.typography.bodyMedium,
                onClick = {
                    annotatedLinkString
                        .getStringAnnotations("URL", it, it)
                        .firstOrNull()?.let { stringAnnotation ->
                            if (stringAnnotation.item == termsUrl)
                                onTermsClick()
                            else
                                onPrivacyPolicyClick()
                        }
                }
            )
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun ReceiveMarketingEmailsSwitch(
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onUnsubscribeClick: () -> Unit,
) {
    val annotatedLinkString: AnnotatedString = buildAnnotatedString {
        val strMarketingEmail = stringResource(id = R.string.get_emails_from_bitwarden_for_announcements_advices_and_research_opportunities_unsubscribe_any_time)
        val strUnsubscribe = stringResource(id = R.string.unsubscribe)
        val startIndexUnsubscribe = strMarketingEmail.indexOf(strUnsubscribe, ignoreCase = true)
        val endIndexUnsubscribe = startIndexUnsubscribe + strUnsubscribe.length
        append(strMarketingEmail)
        addStyle(
            style = SpanStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = MaterialTheme.typography.bodyMedium.fontSize
            ),
            start = 0,
            end = strMarketingEmail.length
        )
        addStyle(
            style = SpanStyle(
                color = MaterialTheme.colorScheme.primary,
                fontSize = MaterialTheme.typography.bodyMedium.fontSize
            ),
            start = startIndexUnsubscribe,
            end = endIndexUnsubscribe
        )
        addStringAnnotation(
            tag = "URL",
            annotation = strUnsubscribe,
            start = startIndexUnsubscribe,
            end = endIndexUnsubscribe
        )
    }
    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .semantics(mergeDescendants = true) {
                testTag = "ReceiveMarketingEmailsToggle"
                toggleableState = ToggleableState(isChecked)
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
            ClickableText(
                text = annotatedLinkString,
                style = MaterialTheme.typography.bodyMedium,
                onClick = {
                    annotatedLinkString
                        .getStringAnnotations("URL", it, it)
                        .firstOrNull()?.let {
                            onUnsubscribeClick()
                        }
                }
            )
        }
    }
}


