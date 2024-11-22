package com.x8bit.bitwarden.ui.auth.feature.loginwithdevice

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.content.BitwardenLoadingContent
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.indicator.BitwardenCircularProgressIndicator
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.text.BitwardenClickableText
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.composition.LocalIntentManager
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * The top level composable for the Login with Device screen.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginWithDeviceScreen(
    onNavigateBack: () -> Unit,
    onNavigateToTwoFactorLogin: (emailAddress: String) -> Unit,
    viewModel: LoginWithDeviceViewModel = hiltViewModel(),
    intentManager: IntentManager = LocalIntentManager.current,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = context.resources
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            LoginWithDeviceEvent.NavigateBack -> onNavigateBack()
            is LoginWithDeviceEvent.NavigateToCaptcha -> {
                intentManager.startCustomTabsActivity(uri = event.uri)
            }

            is LoginWithDeviceEvent.NavigateToTwoFactorLogin -> {
                onNavigateToTwoFactorLogin(event.emailAddress)
            }

            is LoginWithDeviceEvent.ShowToast -> {
                Toast.makeText(context, event.message(resources), Toast.LENGTH_SHORT).show()
            }
        }
    }

    LoginWithDeviceDialogs(
        state = state.dialogState,
        onDismissDialog = remember(viewModel) {
            { viewModel.trySendAction(LoginWithDeviceAction.DismissDialog) }
        },
    )

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = state.toolbarTitle(),
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = R.drawable.ic_close),
                navigationIconContentDescription = stringResource(id = R.string.close),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(LoginWithDeviceAction.CloseButtonClick) }
                },
            )
        },
    ) {
        when (val viewState = state.viewState) {
            is LoginWithDeviceState.ViewState.Content -> {
                LoginWithDeviceScreenContent(
                    state = viewState,
                    onResendNotificationClick = remember(viewModel) {
                        { viewModel.trySendAction(LoginWithDeviceAction.ResendNotificationClick) }
                    },
                    onViewAllLogInOptionsClick = remember(viewModel) {
                        { viewModel.trySendAction(LoginWithDeviceAction.ViewAllLogInOptionsClick) }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            LoginWithDeviceState.ViewState.Loading -> BitwardenLoadingContent(
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun LoginWithDeviceScreenContent(
    state: LoginWithDeviceState.ViewState.Content,
    onResendNotificationClick: () -> Unit,
    onViewAllLogInOptionsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .imePadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = state.title(),
            textAlign = TextAlign.Start,
            style = BitwardenTheme.typography.headlineMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = state.subtitle(),
            textAlign = TextAlign.Start,
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = state.description(),
            textAlign = TextAlign.Start,
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(id = R.string.fingerprint_phrase),
            textAlign = TextAlign.Start,
            style = BitwardenTheme.typography.titleLarge,
            color = BitwardenTheme.colorScheme.text.primary,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = state.fingerprintPhrase,
            textAlign = TextAlign.Start,
            color = BitwardenTheme.colorScheme.text.codePink,
            style = BitwardenTheme.typography.sensitiveInfoSmall,
            minLines = 2,
            modifier = Modifier
                .testTag("FingerprintPhraseValue")
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )

        if (state.allowsResend) {
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .defaultMinSize(minHeight = 40.dp)
                    .align(Alignment.Start),
            ) {
                if (state.isResendNotificationLoading) {
                    BitwardenCircularProgressIndicator(
                        modifier = Modifier
                            .padding(horizontal = 64.dp)
                            .size(size = 16.dp),
                    )
                } else {
                    BitwardenClickableText(
                        modifier = Modifier.testTag("ResendNotificationButton"),
                        label = stringResource(id = R.string.resend_notification),
                        style = BitwardenTheme.typography.labelLarge,
                        innerPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
                        onClick = onResendNotificationClick,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = state.otherOptions(),
            textAlign = TextAlign.Start,
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )

        BitwardenClickableText(
            modifier = Modifier.testTag("ViewAllLoginOptionsButton"),
            label = stringResource(id = R.string.view_all_login_options),
            innerPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
            style = BitwardenTheme.typography.labelLarge,
            onClick = onViewAllLogInOptionsClick,
        )

        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun LoginWithDeviceDialogs(
    state: LoginWithDeviceState.DialogState?,
    onDismissDialog: () -> Unit,
) {
    when (state) {
        is LoginWithDeviceState.DialogState.Loading -> BitwardenLoadingDialog(
            text = state.message(),
        )

        is LoginWithDeviceState.DialogState.Error -> BitwardenBasicDialog(
            title = state.title?.invoke(),
            message = state.message(),
            onDismissRequest = onDismissDialog,
        )

        null -> Unit
    }
}
