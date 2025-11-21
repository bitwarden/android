package com.bitwarden.testharness.ui.platform.feature.credentialmanager

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.bitwarden.testharness.R
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.appbar.NavigationIcon
import com.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.row.BitwardenPushRow
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString

/**
 * Screen displaying available Credential Manager test flows.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialManagerListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGetPassword: () -> Unit,
    onNavigateToCreatePassword: () -> Unit,
    onNavigateToGetPasskey: () -> Unit,
    onNavigateToCreatePasskey: () -> Unit,
    onNavigateToGetPasswordOrPasskey: () -> Unit,
    viewModel: CredentialManagerListViewModel = hiltViewModel(),
) {
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            CredentialManagerListEvent.NavigateToGetPassword -> {
                onNavigateToGetPassword()
            }

            CredentialManagerListEvent.NavigateToCreatePassword -> {
                onNavigateToCreatePassword()
            }

            CredentialManagerListEvent.NavigateToGetPasskey -> {
                onNavigateToGetPasskey()
            }

            CredentialManagerListEvent.NavigateToCreatePasskey -> {
                onNavigateToCreatePasskey()
            }

            CredentialManagerListEvent.NavigateToGetPasswordOrPasskey -> {
                onNavigateToGetPasswordOrPasskey()
            }

            CredentialManagerListEvent.NavigateBack -> {
                onNavigateBack()
            }
        }
    }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    BitwardenScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.credential_manager),
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
                navigationIcon = NavigationIcon(
                    navigationIcon = rememberVectorPainter(id = BitwardenDrawable.ic_back),
                    navigationIconContentDescription = stringResource(BitwardenString.back),
                    onNavigationIconClick = remember(viewModel) {
                        { viewModel.trySendAction(CredentialManagerListAction.BackClick) }
                    },
                ),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(8.dp))

            BitwardenListHeaderText(
                label = stringResource(id = R.string.credential_manager_flows),
                modifier = Modifier
                    .standardHorizontalMargin()
                    .padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))

            BitwardenPushRow(
                text = stringResource(id = R.string.get_password),
                onClick = remember(viewModel) {
                    {
                        viewModel.trySendAction(CredentialManagerListAction.GetPasswordClick)
                    }
                },
                cardStyle = CardStyle.Top(),
                modifier = Modifier.standardHorizontalMargin(),
            )

            BitwardenPushRow(
                text = stringResource(id = R.string.create_password),
                onClick = remember(viewModel) {
                    {
                        viewModel.trySendAction(CredentialManagerListAction.CreatePasswordClick)
                    }
                },
                cardStyle = CardStyle.Middle(),
                modifier = Modifier.standardHorizontalMargin(),
            )

            BitwardenPushRow(
                text = stringResource(id = R.string.get_passkey),
                onClick = remember(viewModel) {
                    {
                        viewModel.trySendAction(CredentialManagerListAction.GetPasskeyClick)
                    }
                },
                cardStyle = CardStyle.Middle(),
                modifier = Modifier.standardHorizontalMargin(),
            )

            BitwardenPushRow(
                text = stringResource(id = R.string.create_passkey),
                onClick = remember(viewModel) {
                    {
                        viewModel.trySendAction(CredentialManagerListAction.CreatePasskeyClick)
                    }
                },
                cardStyle = CardStyle.Middle(),
                modifier = Modifier.standardHorizontalMargin(),
            )

            BitwardenPushRow(
                text = stringResource(id = R.string.get_password_or_passkey),
                onClick = remember(viewModel) {
                    {
                        viewModel.trySendAction(
                            CredentialManagerListAction.GetPasswordOrPasskeyClick,
                        )
                    }
                },
                cardStyle = CardStyle.Bottom,
                modifier = Modifier.standardHorizontalMargin(),
            )

            Spacer(Modifier.height(16.dp))
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}
