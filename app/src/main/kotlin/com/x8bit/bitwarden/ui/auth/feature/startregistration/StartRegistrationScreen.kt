package com.x8bit.bitwarden.ui.auth.feature.startregistration

import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.data.repository.model.Environment
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.annotatedStringResource
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationAction.ErrorDialogDismiss
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationEvent.NavigateToPrivacyPolicy
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationEvent.NavigateToTerms
import com.x8bit.bitwarden.ui.auth.feature.startregistration.handlers.StartRegistrationHandler
import com.x8bit.bitwarden.ui.auth.feature.startregistration.handlers.rememberStartRegistrationHandler
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.dropdown.EnvironmentSelector
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.toggle.BitwardenSwitch
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
    ) -> Unit,
    onNavigateToCheckEmail: (email: String) -> Unit,
    onNavigateToEnvironment: () -> Unit,
    intentManager: IntentManager = LocalIntentManager.current,
    viewModel: StartRegistrationViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val handler = rememberStartRegistrationHandler(viewModel = viewModel)
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

            is StartRegistrationEvent.NavigateToServerSelectionInfo -> {
                intentManager.launchUri(
                    uri = "https://bitwarden.com/help/server-geographies/".toUri(),
                )
            }

            is StartRegistrationEvent.NavigateBack -> onNavigateBack.invoke()
            is StartRegistrationEvent.ShowToast -> {
                Toast.makeText(context, event.text, Toast.LENGTH_SHORT).show()
            }

            is StartRegistrationEvent.NavigateToCompleteRegistration -> {
                onNavigateToCompleteRegistration(
                    event.email,
                    event.verificationToken,
                )
            }

            is StartRegistrationEvent.NavigateToCheckEmail -> {
                onNavigateToCheckEmail(
                    event.email,
                )
            }

            StartRegistrationEvent.NavigateToEnvironment -> onNavigateToEnvironment()
        }
    }

    // Show dialog if needed:
    when (val dialog = state.dialog) {
        is StartRegistrationDialog.Error -> {
            BitwardenBasicDialog(
                title = dialog.title?.invoke(),
                message = dialog.message(),
                throwable = dialog.error,
                onDismissRequest = remember(viewModel) {
                    { viewModel.trySendAction(ErrorDialogDismiss) }
                },
            )
        }

        StartRegistrationDialog.Loading -> {
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
                onNavigationIconClick = handler.onCloseClick,
            )
        },
    ) {
        StartRegistrationContent(
            emailInput = state.emailInput,
            selectedEnvironmentType = state.selectedEnvironmentType,
            nameInput = state.nameInput,
            isReceiveMarketingEmailsToggled = state.isReceiveMarketingEmailsToggled,
            isContinueButtonEnabled = state.isContinueButtonEnabled,
            isNewOnboardingUiEnabled = state.showNewOnboardingUi,
            handler = handler,
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun StartRegistrationContent(
    emailInput: String,
    selectedEnvironmentType: Environment.Type,
    nameInput: String,
    isReceiveMarketingEmailsToggled: Boolean,
    isContinueButtonEnabled: Boolean,
    handler: StartRegistrationHandler,
    isNewOnboardingUiEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.height(height = 12.dp))

        if (isNewOnboardingUiEnabled) {
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
        }
        Spacer(modifier = Modifier.height(12.dp))

        BitwardenTextField(
            label = stringResource(id = R.string.email_address_required),
            value = emailInput,
            onValueChange = handler.onEmailInputChange,
            keyboardType = KeyboardType.Email,
            textFieldTestTag = "EmailAddressEntry",
            supportingContentPadding = PaddingValues(),
            supportingContent = {
                EnvironmentSelector(
                    labelText = stringResource(id = R.string.create_account_on_with_colon),
                    dialogTitle = stringResource(id = R.string.create_account_on),
                    selectedOption = selectedEnvironmentType,
                    onOptionSelected = handler.onEnvironmentTypeSelect,
                    onHelpClick = handler.onServerGeologyHelpClick,
                    isHelpEnabled = isNewOnboardingUiEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(tag = "RegionSelectorDropdown"),
                )
            },
            cardStyle = CardStyle.Full,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        BitwardenTextField(
            label = stringResource(id = R.string.name),
            value = nameInput,
            onValueChange = handler.onNameInputChange,
            textFieldTestTag = "NameEntry",
            cardStyle = CardStyle.Full,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )

        if (selectedEnvironmentType != Environment.Type.SELF_HOSTED) {
            Spacer(modifier = Modifier.height(8.dp))
            ReceiveMarketingEmailsSwitch(
                isChecked = isReceiveMarketingEmailsToggled,
                onCheckedChange = handler.onReceiveMarketingEmailsToggle,
                onUnsubscribeClick = handler.onUnsubscribeMarketingEmailsClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        BitwardenFilledButton(
            label = stringResource(id = R.string.continue_text),
            onClick = handler.onContinueClick,
            isEnabled = isContinueButtonEnabled,
            modifier = Modifier
                .testTag("ContinueButton")
                .standardHorizontalMargin()
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        TermsAndPrivacyText(
            onTermsClick = handler.onTermsClick,
            onPrivacyPolicyClick = handler.onPrivacyPolicyClick,
            modifier = Modifier.standardHorizontalMargin(),
        )

        Spacer(modifier = Modifier.height(12.dp))
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Suppress("LongMethod", "MaxLineLength")
@Composable
private fun TermsAndPrivacyText(
    onTermsClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strTerms = stringResource(id = R.string.terms_of_service)
    val strPrivacy = stringResource(id = R.string.privacy_policy)
    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .semantics(mergeDescendants = true) {
                testTag = "DisclaimerText"
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
            }
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
    ) {
        Text(
            text = annotatedStringResource(
                id = R.string.by_continuing_you_agree_to_the_terms_of_service_and_privacy_policy,
                onAnnotationClick = {
                    when (it) {
                        "termsOfService" -> onTermsClick()
                        "privacyPolicy" -> onPrivacyPolicyClick()
                    }
                },
            ),
            style = BitwardenTheme.typography.bodyMedium.copy(
                textAlign = TextAlign.Center,
            ),
        )
    }
}

@Composable
private fun ReceiveMarketingEmailsSwitch(
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onUnsubscribeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val unsubscribeString = stringResource(id = R.string.unsubscribe)
    @Suppress("MaxLineLength")
    BitwardenSwitch(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                customActions = listOf(
                    CustomAccessibilityAction(
                        label = unsubscribeString,
                        action = {
                            onUnsubscribeClick()
                            true
                        },
                    ),
                )
            }
            .testTag(tag = "ReceiveMarketingEmailsToggle"),
        label = annotatedStringResource(
            id = R.string.get_emails_from_bitwarden_for_announcements_advices_and_research_opportunities_unsubscribe_any_time,
            onAnnotationClick = { onUnsubscribeClick() },
        ),
        isChecked = isChecked,
        onCheckedChange = onCheckedChange,
        cardStyle = CardStyle.Full,
    )
}

@PreviewScreenSizes
@Composable
private fun StartRegistrationContentFilledOut_preview() {
    BitwardenTheme {
        StartRegistrationContent(
            emailInput = "e@mail.com",
            selectedEnvironmentType = Environment.Type.US,
            nameInput = "Test User",
            isReceiveMarketingEmailsToggled = true,
            isContinueButtonEnabled = true,
            isNewOnboardingUiEnabled = false,
            handler = StartRegistrationHandler(
                onEmailInputChange = {},
                onNameInputChange = {},
                onEnvironmentTypeSelect = {},
                onContinueClick = {},
                onTermsClick = {},
                onPrivacyPolicyClick = {},
                onReceiveMarketingEmailsToggle = {},
                onUnsubscribeMarketingEmailsClick = {},
                onServerGeologyHelpClick = {},
                onCloseClick = {},
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StartRegistrationContentEmpty_preview() {
    BitwardenTheme {
        StartRegistrationContent(
            emailInput = "",
            selectedEnvironmentType = Environment.Type.US,
            nameInput = "",
            isReceiveMarketingEmailsToggled = false,
            isContinueButtonEnabled = false,
            isNewOnboardingUiEnabled = false,
            handler = StartRegistrationHandler(
                onEmailInputChange = {},
                onNameInputChange = {},
                onEnvironmentTypeSelect = {},
                onContinueClick = {},
                onTermsClick = {},
                onPrivacyPolicyClick = {},
                onReceiveMarketingEmailsToggle = {},
                onUnsubscribeMarketingEmailsClick = {},
                onServerGeologyHelpClick = {},
                onCloseClick = {},
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StartRegistrationContentNewOnboardingUi_preview() {
    BitwardenTheme {
        StartRegistrationContent(
            emailInput = "",
            selectedEnvironmentType = Environment.Type.US,
            nameInput = "",
            isReceiveMarketingEmailsToggled = false,
            isContinueButtonEnabled = false,
            isNewOnboardingUiEnabled = true,
            handler = StartRegistrationHandler(
                onEmailInputChange = {},
                onNameInputChange = {},
                onEnvironmentTypeSelect = {},
                onContinueClick = {},
                onTermsClick = {},
                onPrivacyPolicyClick = {},
                onReceiveMarketingEmailsToggle = {},
                onUnsubscribeMarketingEmailsClick = {},
                onServerGeologyHelpClick = {},
                onCloseClick = {},
            ),
        )
    }
}
