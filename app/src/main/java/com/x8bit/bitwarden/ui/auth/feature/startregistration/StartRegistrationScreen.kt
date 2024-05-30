package com.x8bit.bitwarden.ui.auth.feature.startregistration

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
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationAction.AcceptPoliciesToggle
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationAction.CloseClick
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationAction.EmailInputChange
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationAction.ErrorDialogDismiss
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationAction.PrivacyPolicyClick
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationAction.TermsClick
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationAction.NameInputChange
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationEvent.NavigateToPrivacyPolicy
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationEvent.NavigateToTerms
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.LoadingDialogState
import com.x8bit.bitwarden.ui.platform.components.dropdown.EnvironmentSelector
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.text.BitwardenClickableText
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.composition.LocalIntentManager
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager

/**
 * Top level composable for the create account screen.
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
                },
                actions = {
                    BitwardenTextButton(
                        label = stringResource(id = R.string.continue_text),
                        onClick = remember(viewModel) {
                            { viewModel.trySendAction(StartRegistrationAction.ContinueClick) }
                        },
                        modifier = Modifier.testTag("ContinueButton"),
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
                    style = MaterialTheme.typography.bodyMedium,
                    innerPadding = PaddingValues(vertical = 4.dp, horizontal = 0.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = ",",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                BitwardenClickableText(
                    label = stringResource(id = R.string.privacy_policy),
                    onClick = onPrivacyPolicyClick,
                    style = MaterialTheme.typography.bodyMedium,
                    innerPadding = PaddingValues(vertical = 4.dp, horizontal = 0.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
