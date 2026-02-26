package com.bitwarden.testharness.ui.platform.feature.createpasskey

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.testharness.R
import com.bitwarden.ui.platform.base.util.EventsEffect
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
 * Create Passkey test screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePasskeyScreen(
    onNavigateBack: () -> Unit,
    viewModel: CreatePasskeyViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            CreatePasskeyEvent.NavigateBack -> onNavigateBack()
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    CreatePasskeyScreenContent(
        state = state,
        onBackClick = { viewModel.trySendAction(CreatePasskeyAction.BackClick) },
        onUsernameChange = { viewModel.trySendAction(CreatePasskeyAction.UsernameChanged(it)) },
        onRpIdChange = { viewModel.trySendAction(CreatePasskeyAction.RpIdChanged(it)) },
        onOriginChange = { viewModel.trySendAction(CreatePasskeyAction.OriginChanged(it)) },
        onExecuteClick = { viewModel.trySendAction(CreatePasskeyAction.ExecuteClick) },
        onClearResultClick = { viewModel.trySendAction(CreatePasskeyAction.ClearResultClick) },
        scrollBehavior = scrollBehavior,
    )
}

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePasskeyScreenContent(
    state: CreatePasskeyState,
    onBackClick: () -> Unit,
    onUsernameChange: (String) -> Unit,
    onRpIdChange: (String) -> Unit,
    onOriginChange: (String) -> Unit,
    onExecuteClick: () -> Unit,
    onClearResultClick: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    BitwardenScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.create_passkey_title),
                scrollBehavior = scrollBehavior,
                navigationIcon = NavigationIcon(
                    navigationIcon = rememberVectorPainter(id = BitwardenDrawable.ic_back),
                    navigationIconContentDescription = stringResource(BitwardenString.back),
                    onNavigationIconClick = onBackClick,
                ),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(state = rememberScrollState())
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .imePadding(),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            BitwardenTextField(
                label = stringResource(R.string.username),
                value = state.username,
                onValueChange = onUsernameChange,
                cardStyle = null,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            BitwardenTextField(
                label = stringResource(R.string.relying_party_id),
                value = state.rpId,
                onValueChange = onRpIdChange,
                placeholder = stringResource(R.string.rp_id_hint),
                cardStyle = null,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            BitwardenTextField(
                label = stringResource(R.string.origin_optional),
                value = state.origin,
                onValueChange = onOriginChange,
                placeholder = stringResource(R.string.origin_hint),
                cardStyle = null,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            BitwardenFilledButton(
                label = stringResource(R.string.execute),
                onClick = onExecuteClick,
                isEnabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            BitwardenTextButton(
                label = stringResource(BitwardenString.clear),
                onClick = onClearResultClick,
                isEnabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            BitwardenTextField(
                label = stringResource(R.string.result),
                value = state.resultText,
                onValueChange = { },
                cardStyle = null,
                readOnly = true,
                singleLine = false,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Preview
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePasskeyScreenPreview() {
    CreatePasskeyScreenContent(
        state = CreatePasskeyState(
            username = "user@bitwarden.com",
            rpId = "passkeys.example.com",
            origin = "https://passkeys.example.com",
            isLoading = false,
            resultText = "This is the result of the operation.",
        ),
        onBackClick = {},
        onUsernameChange = {},
        onRpIdChange = {},
        onOriginChange = {},
        onExecuteClick = {},
        onClearResultClick = {},
        scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()),
    )
}
