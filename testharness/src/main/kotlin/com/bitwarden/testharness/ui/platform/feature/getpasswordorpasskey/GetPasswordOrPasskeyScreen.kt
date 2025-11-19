package com.bitwarden.testharness.ui.platform.feature.getpasswordorpasskey

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.testharness.R
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.appbar.NavigationIcon
import com.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.bitwarden.ui.platform.components.field.BitwardenTextField
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString

/**
 * Get Password or Passkey test screen.
 *
 * This screen tests the combined credential retrieval use case where both GetPasswordOption
 * and GetPublicKeyCredentialOption are included in a single GetCredentialRequest. The system
 * credential picker will show both passwords and passkeys, allowing the user to select either
 * credential type.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GetPasswordOrPasskeyScreen(
    onNavigateBack: () -> Unit,
    viewModel: GetPasswordOrPasskeyViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            GetPasswordOrPasskeyEvent.NavigateBack -> onNavigateBack()
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    BitwardenScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.get_password_or_passkey_title),
                scrollBehavior = scrollBehavior,
                navigationIcon = NavigationIcon(
                    navigationIcon = rememberVectorPainter(id = BitwardenDrawable.ic_back),
                    navigationIconContentDescription = stringResource(BitwardenString.back),
                    onNavigationIconClick = remember(viewModel) {
                        { viewModel.trySendAction(GetPasswordOrPasskeyAction.BackClick) }
                    },
                ),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(state = rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            BitwardenTextField(
                label = stringResource(R.string.relying_party_id),
                value = state.rpId,
                onValueChange = remember(viewModel) {
                    { viewModel.trySendAction(GetPasswordOrPasskeyAction.RpIdChanged(it)) }
                },
                placeholder = stringResource(R.string.rp_id_hint),
                cardStyle = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            BitwardenTextField(
                label = stringResource(R.string.origin_optional),
                value = state.origin,
                onValueChange = remember(viewModel) {
                    { viewModel.trySendAction(GetPasswordOrPasskeyAction.OriginChanged(it)) }
                },
                placeholder = stringResource(R.string.origin_hint),
                cardStyle = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            BitwardenFilledButton(
                label = stringResource(R.string.execute),
                onClick = remember(viewModel) {
                    { viewModel.trySendAction(GetPasswordOrPasskeyAction.ExecuteClick) }
                },
                isEnabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            BitwardenTextButton(
                label = stringResource(R.string.clear),
                onClick = remember(viewModel) {
                    { viewModel.trySendAction(GetPasswordOrPasskeyAction.ClearResultClick) }
                },
                isEnabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            BitwardenTextField(
                label = stringResource(R.string.result),
                value = state.resultText,
                onValueChange = { },
                cardStyle = null,
                readOnly = true,
                singleLine = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )

            Spacer(modifier = Modifier.height(16.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}
