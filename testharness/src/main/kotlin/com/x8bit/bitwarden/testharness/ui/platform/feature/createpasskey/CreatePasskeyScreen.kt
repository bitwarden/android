package com.x8bit.bitwarden.testharness.ui.platform.feature.createpasskey

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.appbar.NavigationIcon
import com.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.bitwarden.ui.platform.components.field.BitwardenTextField
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.x8bit.bitwarden.testharness.R

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

    CreatePasskeyScreenContent(
        onBackClick = { viewModel.trySendAction(CreatePasskeyAction.BackClick) },
        username = state.username,
        onUsernameChange = { viewModel.trySendAction(CreatePasskeyAction.UsernameChanged(it)) },
        rpId = state.rpId,
        onRpIdChange = { viewModel.trySendAction(CreatePasskeyAction.RpIdChanged(it)) },
        origin = state.origin,
        onOriginChange = { viewModel.trySendAction(CreatePasskeyAction.OriginChanged(it)) },
        onExecuteClick = { viewModel.trySendAction(CreatePasskeyAction.ExecuteClick) },
        isLoading = state.isLoading,
        onClearResultClick = { viewModel.trySendAction(CreatePasskeyAction.ClearResultClick) },
        resultText = state.resultText,
    )
}

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePasskeyScreenContent(
    onBackClick: () -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    rpId: String,
    onRpIdChange: (String) -> Unit,
    origin: String,
    onOriginChange: (String) -> Unit,
    onExecuteClick: () -> Unit,
    isLoading: Boolean,
    onClearResultClick: () -> Unit,
    resultText: String,
) {
    BitwardenScaffold(
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.create_passkey_title),
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
                navigationIcon = NavigationIcon(
                    navigationIcon = rememberVectorPainter(id = BitwardenDrawable.ic_back),
                    navigationIconContentDescription = "Back",
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
            verticalArrangement = Arrangement.spacedBy(16.dp),

            ) {
            Spacer(modifier = Modifier.height(8.dp))

            BitwardenTextField(
                label = stringResource(R.string.username),
                value = username,
                onValueChange = onUsernameChange,
                cardStyle = null,
                modifier = Modifier.fillMaxWidth(),
            )

            BitwardenTextField(
                label = stringResource(R.string.relying_party_id),
                value = rpId,
                onValueChange = onRpIdChange,
                placeholder = stringResource(R.string.rp_id_hint),
                cardStyle = null,
                modifier = Modifier.fillMaxWidth(),
            )

            BitwardenTextField(
                label = stringResource(R.string.origin_optional),
                value = origin,
                onValueChange = onOriginChange,
                placeholder = stringResource(R.string.origin_hint),
                cardStyle = null,
                modifier = Modifier.fillMaxWidth(),
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BitwardenFilledButton(
                    label = stringResource(R.string.execute),
                    onClick = onExecuteClick,
                    isEnabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )

                BitwardenTextButton(
                    label = stringResource(R.string.clear),
                    onClick = onClearResultClick,
                    isEnabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            BitwardenTextField(
                label = stringResource(R.string.result),
                value = resultText,
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
@Composable
private fun CreatePasskeyScreenPreview() {
    CreatePasskeyScreenContent(
        onBackClick = {},
        username = "user@bitwarden.com",
        onUsernameChange = {},
        rpId = "passkeys.example.com",
        onRpIdChange = {},
        origin = "https://passkeys.example.com",
        onOriginChange = {},
        onExecuteClick = {},
        isLoading = false,
        onClearResultClick = {},
        resultText = "This is the result of the operation.",
    )
}
