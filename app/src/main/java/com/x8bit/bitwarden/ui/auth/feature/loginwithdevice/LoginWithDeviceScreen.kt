package com.x8bit.bitwarden.ui.auth.feature.loginwithdevice

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.components.BitwardenClickableText
import com.x8bit.bitwarden.ui.platform.components.BitwardenErrorContent
import com.x8bit.bitwarden.ui.platform.components.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.theme.LocalNonMaterialColors
import com.x8bit.bitwarden.ui.platform.theme.LocalNonMaterialTypography

/**
 * The top level composable for the Login with Device screen.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginWithDeviceScreen(
    onNavigateBack: () -> Unit,
    viewModel: LoginWithDeviceViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            LoginWithDeviceEvent.NavigateBack -> onNavigateBack()
            is LoginWithDeviceEvent.ShowToast -> {
                Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.log_in_with_device),
                scrollBehavior = scrollBehavior,
                navigationIcon = painterResource(id = R.drawable.ic_close),
                navigationIconContentDescription = stringResource(id = R.string.close),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(LoginWithDeviceAction.CloseButtonClick) }
                },
            )
        },
    ) { paddingValues ->
        val modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
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
                    modifier = modifier,
                )
            }

            is LoginWithDeviceState.ViewState.Error -> {
                BitwardenErrorContent(
                    message = viewState.message(),
                    modifier = modifier,
                )
            }

            LoginWithDeviceState.ViewState.Loading -> {
                Column(
                    modifier = modifier,
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.navigationBarsPadding())
                }
            }
        }
    }
}

@Suppress("LongMethod")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun LoginWithDeviceScreenContent(
    state: LoginWithDeviceState.ViewState.Content,
    onResendNotificationClick: () -> Unit,
    onViewAllLogInOptionsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .semantics { testTagsAsResourceId = true }
            .imePadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = stringResource(id = R.string.log_in_initiated),
            textAlign = TextAlign.Start,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(id = R.string.a_notification_has_been_sent_to_your_device),
            textAlign = TextAlign.Start,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        @Suppress("MaxLineLength")
        Text(
            text = stringResource(id = R.string.please_make_sure_your_vault_is_unlocked_and_the_fingerprint_phrase_matches_on_the_other_device),
            textAlign = TextAlign.Start,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(id = R.string.fingerprint_phrase),
            textAlign = TextAlign.Start,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = state.fingerprintPhrase,
            textAlign = TextAlign.Start,
            color = LocalNonMaterialColors.current.fingerprint,
            style = LocalNonMaterialTypography.current.sensitiveInfoSmall,
            modifier = Modifier
                .semantics { testTag = "FingerprintValueLabel" }
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .defaultMinSize(minHeight = 32.dp)
                .align(Alignment.Start),
        ) {
            if (state.isResendNotificationLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(horizontal = 64.dp)
                        .size(size = 16.dp),
                    )
            } else {
                BitwardenClickableText(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .semantics { testTag = "ResendNotificationButton" }
                        .fillMaxWidth(),
                    label = stringResource(id = R.string.resend_notification),
                    onClick = onResendNotificationClick,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(id = R.string.need_another_option),
            textAlign = TextAlign.Start,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        BitwardenClickableText(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .semantics { testTag = "ViewAllLoginOptionsButton" }
                .fillMaxWidth(),
            label = stringResource(id = R.string.view_all_login_options),
            onClick = onViewAllLogInOptionsClick,
        )

        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}
