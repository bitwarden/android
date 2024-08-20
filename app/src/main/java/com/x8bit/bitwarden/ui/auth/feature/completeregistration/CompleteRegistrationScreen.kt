package com.x8bit.bitwarden.ui.auth.feature.completeregistration

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.auth.feature.completeregistration.handlers.CompleteRegistrationHandler
import com.x8bit.bitwarden.ui.auth.feature.completeregistration.handlers.rememberCompleteRegistrationHandler
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.x8bit.bitwarden.ui.platform.components.card.BitwardenActionCard
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.LoadingDialogState
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.text.BitwardenClickableText
import com.x8bit.bitwarden.ui.platform.components.toggle.BitwardenSwitch
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.composition.LocalIntentManager
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager

/**
 * Top level composable for the complete registration screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun CompleteRegistrationScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPasswordGuidance: () -> Unit,
    onNavigateToPreventAccountLockout: () -> Unit,
    onNavigateToLogin: (email: String, token: String) -> Unit,
    viewModel: CompleteRegistrationViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val handler = rememberCompleteRegistrationHandler(viewModel = viewModel)
    val context = LocalContext.current
    EventsEffect(viewModel) { event ->
        when (event) {
            is CompleteRegistrationEvent.NavigateBack -> onNavigateBack.invoke()
            is CompleteRegistrationEvent.ShowToast -> {
                Toast.makeText(context, event.message(context.resources), Toast.LENGTH_SHORT).show()
            }

            CompleteRegistrationEvent.NavigateToMakePasswordStrong -> onNavigateToPasswordGuidance()
            CompleteRegistrationEvent.NavigateToPreventAccountLockout -> {
                onNavigateToPreventAccountLockout()
            }

            is CompleteRegistrationEvent.NavigateToLogin -> {
                onNavigateToLogin(
                    event.email,
                    event.captchaToken,
                )
            }

            CompleteRegistrationEvent.NavigateToMakePasswordStrong -> onNavigateToPasswordGuidance()
            CompleteRegistrationEvent.NavigateToPreventAccountLockout -> {
                onNavigateToPreventAccountLockout()
            }

            CompleteRegistrationEvent.NavigateToOnboarding -> onNavigateToOnboarding()
        }
    }

    // Show dialog if needed:
    when (val dialog = state.dialog) {
        is CompleteRegistrationDialog.Error -> {
            BitwardenBasicDialog(
                visibilityState = dialog.state,
                onDismissRequest = handler.onDismissErrorDialog,
            )
        }

        is CompleteRegistrationDialog.HaveIBeenPwned -> {
            BitwardenTwoButtonDialog(
                title = dialog.title(),
                message = dialog.message(),
                confirmButtonText = stringResource(id = R.string.yes),
                dismissButtonText = stringResource(id = R.string.no),
                onConfirmClick = handler.onContinueWithBreachedPasswordClick,
                onDismissClick = handler.onDismissErrorDialog,
                onDismissRequest = handler.onDismissErrorDialog,
            )
        }

        CompleteRegistrationDialog.Loading -> {
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
            CompleteRegistrationContent(
                passwordInput = state.passwordInput,
                passwordStrengthState = state.passwordStrengthState,
                confirmPasswordInput = state.confirmPasswordInput,
                passwordHintInput = state.passwordHintInput,
                isCheckDataBreachesToggled = state.isCheckDataBreachesToggled,
                handler = handler,
                modifier = Modifier.standardHorizontalMargin(),
                nextButtonEnabled = state.hasValidMasterPassword,
                callToActionText = state.callToActionText(),
                minimumPasswordLength = state.minimumPasswordLength,
            )
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun CompleteRegistrationContent(
    passwordInput: String,
    passwordStrengthState: PasswordStrengthState,
    confirmPasswordInput: String,
    passwordHintInput: String,
    isCheckDataBreachesToggled: Boolean,
    nextButtonEnabled: Boolean,
    minimumPasswordLength: Int,
    callToActionText: String,
    handler: CompleteRegistrationHandler,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth(),
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        CompleteRegistrationContentHeader(
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(modifier = Modifier.height(24.dp))
        BitwardenActionCard(
            actionIcon = rememberVectorPainter(id = R.drawable.ic_tooltip),
            actionText = stringResource(id = R.string.what_makes_a_password_strong),
            callToActionText = stringResource(id = R.string.learn_more),
            onCardClicked = handler.onMakeStrongPassword,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(24.dp))

        var showPassword by rememberSaveable { mutableStateOf(false) }
        BitwardenPasswordField(
            label = stringResource(id = R.string.master_password),
            showPassword = showPassword,
            showPasswordChange = { showPassword = it },
            value = passwordInput,
            onValueChange = handler.onPasswordInputChange,
            modifier = Modifier
                .testTag("MasterPasswordEntry")
                .fillMaxWidth(),
            showPasswordTestTag = "PasswordVisibilityToggle",
            imeAction = ImeAction.Next,
        )
        Spacer(modifier = Modifier.height(8.dp))
        PasswordStrengthIndicator(
            state = passwordStrengthState,
            currentCharacterCount = passwordInput.length,
            minimumCharacterCount = minimumPasswordLength,
        )
        Spacer(modifier = Modifier.height(16.dp))
        BitwardenPasswordField(
            label = stringResource(id = R.string.retype_master_password),
            value = confirmPasswordInput,
            showPassword = showPassword,
            showPasswordChange = { showPassword = it },
            onValueChange = handler.onConfirmPasswordInputChange,
            modifier = Modifier
                .testTag("ConfirmMasterPasswordEntry")
                .fillMaxWidth(),
            showPasswordTestTag = "ConfirmPasswordVisibilityToggle",
        )
        Spacer(modifier = Modifier.height(16.dp))
        BitwardenTextField(
            label = stringResource(id = R.string.master_password_hint),
            value = passwordHintInput,
            onValueChange = handler.onPasswordHintChange,
            hint = stringResource(
                R.string.bitwarden_cannot_recover_a_lost_or_forgotten_master_password,
            ),
            modifier = Modifier
                .testTag("MasterPasswordHintLabel")
                .fillMaxWidth(),
        )
        BitwardenClickableText(
            label = stringResource(id = R.string.learn_about_other_ways_to_prevent_account_lockout),
            onClick = handler.onLearnToPreventLockout,
            style = nonMaterialTypography.labelMediumProminent,
        )
        Spacer(modifier = Modifier.height(24.dp))
        BitwardenSwitch(
            label = stringResource(id = R.string.check_known_data_breaches_for_this_password),
            isChecked = isCheckDataBreachesToggled,
            onCheckedChange = handler.onCheckDataBreachesToggle,
            modifier = Modifier.testTag("CheckExposedMasterPasswordToggle"),
        )
        Spacer(modifier = Modifier.height(24.dp))
        BitwardenFilledButton(
            label = callToActionText,
            isEnabled = nextButtonEnabled,
            onClick = handler.onCallToAction,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CompleteRegistrationContentHeader(
    modifier: Modifier = Modifier,
    configuration: Configuration = LocalConfiguration.current,
) {
    if (configuration.isPortrait) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OrderedHeaderContent()
        }
    } else {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OrderedHeaderContent()
        }
    }
}

/**
 * Header content ordered with the image "first" and the text "second" which can be placed in a
 * [Column] or [Row].
 */
@Composable
private fun OrderedHeaderContent() {
    Image(
        painter = rememberVectorPainter(id = R.drawable.lock),
        contentDescription = null,
        modifier = Modifier.size(100.dp),
    )
    Spacer(modifier = Modifier.size(24.dp))
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.choose_your_master_password),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(
                R.string.choose_a_unique_and_strong_password_to_keep_your_information_safe,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

@PreviewScreenSizes
@Composable
private fun CompleteRegistrationContentPreview() {
    BitwardenTheme {
        CompleteRegistrationContent(
            passwordInput = "tortor",
            passwordStrengthState = PasswordStrengthState.WEAK_3,
            confirmPasswordInput = "consequat",
            passwordHintInput = "dissentiunt",
            isCheckDataBreachesToggled = false,
            handler = CompleteRegistrationHandler(
                onDismissErrorDialog = {},
                onContinueWithBreachedPasswordClick = {},
                onBackClick = {},
                onPasswordInputChange = {},
                onConfirmPasswordInputChange = {},
                onPasswordHintChange = {},
                onCheckDataBreachesToggle = {},
                onLearnToPreventLockout = {},
                onMakeStrongPassword = {},
                onCallToAction = {},
            ),
            callToActionText = "Next",
            nextButtonEnabled = true,
            minimumPasswordLength = 12,
            modifier = Modifier.standardHorizontalMargin(),
        )
    }

    if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            orderedContent()
        }
    } else {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            orderedContent()
        }
    }
}

@PreviewScreenSizes
@Composable
private fun CompleteRegistrationContentPreview() {
    BitwardenTheme {
        CompleteRegistrationContent(
            passwordInput = "tortor",
            passwordStrengthState = PasswordStrengthState.WEAK_3,
            confirmPasswordInput = "consequat",
            passwordHintInput = "dissentiunt",
            isCheckDataBreachesToggled = false,
            handler = CompleteRegistrationHandler(
                onDismissErrorDialog = {},
                onContinueWithBreachedPasswordClick = {},
                onBackClick = {},
                onPasswordInputChange = {},
                onConfirmPasswordInputChange = {},
                onPasswordHintChange = {},
                onCheckDataBreachesToggle = {},
                onLearnToPreventLockout = {},
                onMakeStrongPassword = {},
                onCallToAction = {},
            ),
            callToActionText = "Next",
            nextButtonEnabled = true,
            modifier = Modifier.standardHorizontalMargin(),
        )
    }
}
