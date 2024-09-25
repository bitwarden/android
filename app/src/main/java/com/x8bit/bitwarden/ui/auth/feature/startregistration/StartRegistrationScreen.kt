package com.x8bit.bitwarden.ui.auth.feature.startregistration

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationAction.ErrorDialogDismiss
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationEvent.NavigateToPrivacyPolicy
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationEvent.NavigateToTerms
import com.x8bit.bitwarden.ui.auth.feature.startregistration.handlers.StartRegistrationHandler
import com.x8bit.bitwarden.ui.auth.feature.startregistration.handlers.rememberStartRegistrationHandler
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.createAnnotatedString
import com.x8bit.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.LoadingDialogState
import com.x8bit.bitwarden.ui.platform.components.dropdown.EnvironmentSelector
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.composition.LocalIntentManager
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Constant string to be used in string annotation tag field
 */
private const val TAG_URL = "URL"

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
                navigationIcon = rememberVectorPainter(id = R.drawable.ic_back),
                navigationIconContentDescription = stringResource(id = R.string.back),
                onNavigationIconClick = handler.onBackClick,
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
            StartRegistrationContent(
                emailInput = state.emailInput,
                selectedEnvironmentType = state.selectedEnvironmentType,
                nameInput = state.nameInput,
                isReceiveMarketingEmailsToggled = state.isReceiveMarketingEmailsToggled,
                isContinueButtonEnabled = state.isContinueButtonEnabled,
                isNewOnboardingUiEnabled = state.showNewOnboardingUi,
                handler = handler,
            )
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
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
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(16.dp))
        if (isNewOnboardingUiEnabled) {
            Image(
                painter = rememberVectorPainter(id = R.drawable.vault),
                contentDescription = null,
                modifier = Modifier
                    .size(132.dp)
                    .align(Alignment.CenterHorizontally),
            )
            Spacer(modifier = Modifier.height(48.dp))
        }
        BitwardenTextField(
            label = stringResource(id = R.string.name),
            value = nameInput,
            onValueChange = handler.onNameInputChange,
            modifier = Modifier
                .testTag("NameEntry")
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.height(16.dp))
        BitwardenTextField(
            label = stringResource(
                id = R.string.email_address,
            ),
            placeholder = stringResource(R.string.email_address_required),
            value = emailInput,
            onValueChange = handler.onEmailInputChange,
            modifier = Modifier
                .testTag("EmailAddressEntry")
                .fillMaxWidth()
                .standardHorizontalMargin(),
            keyboardType = KeyboardType.Email,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            EnvironmentSelector(
                labelText = stringResource(id = R.string.creating_on),
                selectedOption = selectedEnvironmentType,
                onOptionSelected = handler.onEnvironmentTypeSelect,
                modifier = Modifier
                    .testTag("RegionSelectorDropdown"),
            )
            if (isNewOnboardingUiEnabled) {
                BitwardenStandardIconButton(
                    vectorIconRes = R.drawable.ic_tooltip_small,
                    contentDescription = stringResource(R.string.help_with_server_geolocations),
                    onClick = handler.onServerGeologyHelpClick,
                    contentColor = MaterialTheme.colorScheme.primary,
                    // Align with design but keep accessible touch target of IconButton.
                    modifier = Modifier.offset(y = (-8f).dp, x = 16.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        if (selectedEnvironmentType != Environment.Type.SELF_HOSTED) {
            ReceiveMarketingEmailsSwitch(
                isChecked = isReceiveMarketingEmailsToggled,
                onCheckedChange = handler.onReceiveMarketingEmailsToggle,
                onUnsubscribeClick = handler.onUnsubscribeMarketingEmailsClick,
                modifier = Modifier.standardHorizontalMargin(),
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        BitwardenFilledButton(
            label = stringResource(id = R.string.continue_text),
            onClick = handler.onContinueClick,
            isEnabled = isContinueButtonEnabled,
            modifier = Modifier
                .testTag("ContinueButton")
                .standardHorizontalMargin()
                .fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))
        TermsAndPrivacyText(
            onTermsClick = handler.onTermsClick,
            onPrivacyPolicyClick = handler.onPrivacyPolicyClick,
            modifier = Modifier.standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Suppress("LongMethod")
@Composable
private fun TermsAndPrivacyText(
    onTermsClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strTerms = stringResource(id = R.string.terms_of_service)
    val strPrivacy = stringResource(id = R.string.privacy_policy)
    val annotatedLinkString: AnnotatedString = buildAnnotatedString {
        val strTermsAndPrivacy = stringResource(
            id = R.string.by_continuing_you_agree_to_the_terms_of_service_and_privacy_policy,
        )
        val startIndexTerms = strTermsAndPrivacy.indexOf(strTerms)
        val endIndexTerms = startIndexTerms + strTerms.length
        val startIndexPrivacy = strTermsAndPrivacy.indexOf(strPrivacy)
        val endIndexPrivacy = startIndexPrivacy + strPrivacy.length
        append(strTermsAndPrivacy)
        addStyle(
            style = SpanStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
            ),
            start = 0,
            end = strTermsAndPrivacy.length,
        )
        addStyle(
            style = SpanStyle(
                color = MaterialTheme.colorScheme.primary,
                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                fontWeight = FontWeight.Bold,
            ),
            start = startIndexTerms,
            end = endIndexTerms,
        )
        addStyle(
            style = SpanStyle(
                color = MaterialTheme.colorScheme.primary,
                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                fontWeight = FontWeight.Bold,
            ),
            start = startIndexPrivacy,
            end = endIndexPrivacy,
        )
        addStringAnnotation(
            tag = TAG_URL,
            annotation = strTerms,
            start = startIndexTerms,
            end = endIndexTerms,
        )
        addStringAnnotation(
            tag = TAG_URL,
            annotation = strPrivacy,
            start = startIndexPrivacy,
            end = endIndexPrivacy,
        )
    }
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
        val termsUrl = stringResource(id = R.string.terms_of_service)
        ClickableText(
            text = annotatedLinkString,
            style = MaterialTheme.typography.bodyMedium.copy(
                textAlign = TextAlign.Center,
            ),
            onClick = {
                annotatedLinkString
                    .getStringAnnotations(TAG_URL, it, it)
                    .firstOrNull()?.let { stringAnnotation ->
                        if (stringAnnotation.item == termsUrl) {
                            onTermsClick()
                        } else {
                            onPrivacyPolicyClick()
                        }
                    }
            },
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun ReceiveMarketingEmailsSwitch(
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onUnsubscribeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val unsubscribeString = stringResource(id = R.string.unsubscribe)

    @Suppress("MaxLineLength")
    val annotatedLinkString = createAnnotatedString(
        mainString = stringResource(id = R.string.get_advice_announcements_and_research_opportunities_from_bitwarden_in_your_inbox_unsubscribe_any_time),
        highlights = listOf(unsubscribeString),
        tag = TAG_URL,
    )
    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .semantics(mergeDescendants = true) {
                testTag = "ReceiveMarketingEmailsToggle"
                toggleableState = ToggleableState(isChecked)
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
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = MaterialTheme.colorScheme.primary),
                onClick = { onCheckedChange.invoke(!isChecked) },
            )
            .fillMaxWidth(),
    ) {
        Switch(
            modifier = Modifier
                .height(32.dp)
                .width(52.dp),
            checked = isChecked,
            onCheckedChange = null,
        )
        Spacer(modifier = Modifier.width(16.dp))
        ClickableText(
            text = annotatedLinkString,
            style = MaterialTheme.typography.bodyMedium,
            onClick = {
                annotatedLinkString
                    .getStringAnnotations(TAG_URL, it, it)
                    .firstOrNull()?.let {
                        onUnsubscribeClick()
                    }
            },
        )
    }
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
                onBackClick = {},
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
                onBackClick = {},
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
                onBackClick = {},
            ),
        )
    }
}
