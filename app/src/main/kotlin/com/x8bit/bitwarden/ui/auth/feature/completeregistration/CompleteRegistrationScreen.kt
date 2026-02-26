package com.x8bit.bitwarden.ui.auth.feature.completeregistration

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.bitwarden.ui.platform.components.card.BitwardenActionCardSmall
import com.bitwarden.ui.platform.components.card.color.bitwardenCardColors
import com.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.bitwarden.ui.platform.components.field.BitwardenTextField
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.snackbar.BitwardenSnackbarHost
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.components.snackbar.model.rememberBitwardenSnackbarHostState
import com.bitwarden.ui.platform.components.text.BitwardenClickableText
import com.bitwarden.ui.platform.components.toggle.BitwardenSwitch
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.model.WindowSize
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.bitwarden.ui.platform.util.rememberWindowSize
import com.x8bit.bitwarden.ui.auth.feature.completeregistration.handlers.CompleteRegistrationHandler
import com.x8bit.bitwarden.ui.auth.feature.completeregistration.handlers.rememberCompleteRegistrationHandler

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
    onNavigateToLogin: (email: String) -> Unit,
    viewModel: CompleteRegistrationViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val handler = rememberCompleteRegistrationHandler(viewModel = viewModel)
    val snackbarHostState = rememberBitwardenSnackbarHostState()
    // route OS back actions through the VM to clear the special circumstance
    BackHandler(onBack = handler.onBackClick)

    EventsEffect(viewModel) { event ->
        when (event) {
            is CompleteRegistrationEvent.NavigateBack -> onNavigateBack.invoke()
            is CompleteRegistrationEvent.ShowSnackbar -> {
                snackbarHostState.showSnackbar(BitwardenSnackbarData(message = event.message))
            }

            CompleteRegistrationEvent.NavigateToMakePasswordStrong -> onNavigateToPasswordGuidance()
            CompleteRegistrationEvent.NavigateToPreventAccountLockout -> {
                onNavigateToPreventAccountLockout()
            }

            is CompleteRegistrationEvent.NavigateToLogin -> {
                onNavigateToLogin(
                    event.email,
                )
            }
        }
    }

    // Show dialog if needed:
    when (val dialog = state.dialog) {
        is CompleteRegistrationDialog.Error -> {
            BitwardenBasicDialog(
                title = dialog.title?.invoke(),
                message = dialog.message(),
                throwable = dialog.error,
                onDismissRequest = handler.onDismissErrorDialog,
            )
        }

        is CompleteRegistrationDialog.HaveIBeenPwned -> {
            BitwardenTwoButtonDialog(
                title = dialog.title(),
                message = dialog.message(),
                confirmButtonText = stringResource(id = BitwardenString.yes),
                dismissButtonText = stringResource(id = BitwardenString.no),
                onConfirmClick = handler.onContinueWithBreachedPasswordClick,
                onDismissClick = handler.onDismissErrorDialog,
                onDismissRequest = handler.onDismissErrorDialog,
            )
        }

        CompleteRegistrationDialog.Loading -> {
            BitwardenLoadingDialog(text = stringResource(id = BitwardenString.create_account))
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
                title = stringResource(id = BitwardenString.create_account),
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = BitwardenDrawable.ic_back),
                navigationIconContentDescription = stringResource(id = BitwardenString.back),
                onNavigationIconClick = handler.onBackClick,
            )
        },
        snackbarHost = { BitwardenSnackbarHost(bitwardenHostState = snackbarHostState) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(height = 12.dp))
            CompleteRegistrationContent(
                passwordInput = state.passwordInput,
                passwordStrengthState = state.passwordStrengthState,
                confirmPasswordInput = state.confirmPasswordInput,
                passwordHintInput = state.passwordHintInput,
                isCheckDataBreachesToggled = state.isCheckDataBreachesToggled,
                handler = handler,
                nextButtonEnabled = state.validSubmissionReady,
                callToActionText = stringResource(BitwardenString.next),
                minimumPasswordLength = state.minimumPasswordLength,
            )
            Spacer(modifier = Modifier.height(height = 16.dp))
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
        CompleteRegistrationContentHeader(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.height(24.dp))
        BitwardenActionCardSmall(
            actionIcon = rememberVectorPainter(id = BitwardenDrawable.ic_question_circle),
            actionText = stringResource(id = BitwardenString.what_makes_a_password_strong),
            callToActionText = stringResource(id = BitwardenString.learn_more),
            callToActionTextColor = BitwardenTheme.colorScheme.text.interaction,
            colors = bitwardenCardColors(
                containerColor = BitwardenTheme.colorScheme.background.primary,
            ),
            onCardClicked = handler.onMakeStrongPassword,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.height(24.dp))

        var showPassword by rememberSaveable { mutableStateOf(false) }
        BitwardenPasswordField(
            label = stringResource(id = BitwardenString.master_password_required),
            showPassword = showPassword,
            showPasswordChange = { showPassword = it },
            value = passwordInput,
            onValueChange = handler.onPasswordInputChange,
            showPasswordTestTag = "PasswordVisibilityToggle",
            imeAction = ImeAction.Next,
            supportingContent = {
                PasswordStrengthIndicator(
                    state = passwordStrengthState,
                    currentCharacterCount = passwordInput.length,
                    minimumCharacterCount = minimumPasswordLength,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            passwordFieldTestTag = "MasterPasswordEntry",
            cardStyle = CardStyle.Top(dividerPadding = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        BitwardenPasswordField(
            label = stringResource(id = BitwardenString.retype_master_password_required),
            value = confirmPasswordInput,
            showPassword = showPassword,
            showPasswordChange = { showPassword = it },
            onValueChange = handler.onConfirmPasswordInputChange,
            showPasswordTestTag = "ConfirmPasswordVisibilityToggle",
            passwordFieldTestTag = "ConfirmMasterPasswordEntry",
            cardStyle = CardStyle.Middle(dividerPadding = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        BitwardenTextField(
            label = stringResource(
                id = BitwardenString.master_password_hint_not_specified,
            ),
            value = passwordHintInput,
            onValueChange = handler.onPasswordHintChange,
            supportingContent = {
                Text(
                    text = stringResource(
                        id = BitwardenString
                            .bitwarden_cannot_recover_a_lost_or_forgotten_master_password,
                    ),
                    style = BitwardenTheme.typography.bodySmall,
                    color = BitwardenTheme.colorScheme.text.secondary,
                    modifier = Modifier.fillMaxWidth(),
                )
                BitwardenClickableText(
                    label = stringResource(
                        id = BitwardenString.learn_about_other_ways_to_prevent_account_lockout,
                    ),
                    onClick = handler.onLearnToPreventLockout,
                    style = BitwardenTheme.typography.labelMedium,
                    innerPadding = PaddingValues(vertical = 4.dp),
                )
            },
            textFieldTestTag = "MasterPasswordHintLabel",
            cardStyle = CardStyle.Bottom,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.height(height = 8.dp))
        BitwardenSwitch(
            label = stringResource(
                id = BitwardenString.check_known_data_breaches_for_this_password,
            ),
            isChecked = isCheckDataBreachesToggled,
            onCheckedChange = handler.onCheckDataBreachesToggle,
            cardStyle = CardStyle.Full,
            modifier = Modifier
                .testTag("CheckExposedMasterPasswordToggle")
                .standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.height(height = 16.dp))
        BitwardenFilledButton(
            label = callToActionText,
            isEnabled = nextButtonEnabled,
            onClick = handler.onCallToAction,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }
}

@Composable
private fun CompleteRegistrationContentHeader(
    modifier: Modifier = Modifier,
) {
    when (rememberWindowSize()) {
        WindowSize.Compact -> {
            Column(
                modifier = modifier,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                OrderedHeaderContent()
            }
        }

        WindowSize.Medium -> {
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OrderedHeaderContent()
            }
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
        painter = rememberVectorPainter(id = BitwardenDrawable.ill_lock),
        contentDescription = null,
        modifier = Modifier.size(100.dp),
    )
    Spacer(modifier = Modifier.size(24.dp))
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(BitwardenString.choose_your_master_password),
            style = BitwardenTheme.typography.titleMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(
                BitwardenString.choose_a_unique_and_strong_password_to_keep_your_information_safe,
            ),
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            textAlign = TextAlign.Center,
        )
    }
}

@PreviewScreenSizes
@Composable
private fun CompleteRegistrationContentOldUI_preview() {
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
            minimumPasswordLength = 12,
        )
    }
}
