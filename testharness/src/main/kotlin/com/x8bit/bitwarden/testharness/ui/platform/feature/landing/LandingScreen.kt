package com.x8bit.bitwarden.testharness.ui.platform.feature.landing

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.row.BitwardenPushRow
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.testharness.R

/**
 * Landing screen displaying test category options.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandingScreen(
    onNavigateToAutofill: () -> Unit,
    onNavigateToCredentialManager: () -> Unit,
    viewModel: LandingViewModel = hiltViewModel(),
) {

    EventsEffect(viewModel) { event ->
        when (event) {
            LandingEvent.NavigateToAutofill -> onNavigateToAutofill()
            LandingEvent.NavigateToCredentialManager -> onNavigateToCredentialManager()
        }
    }

    LandingScreenContent(
        onAutofillClick = {
            viewModel.trySendAction(LandingAction.OnAutofillClick)
        },
        onCredentialManagerClick = {
            viewModel.trySendAction(LandingAction.OnCredentialManagerClick)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LandingScreenContent(
    onAutofillClick: () -> Unit,
    onCredentialManagerClick: () -> Unit,
) {
    BitwardenScaffold(
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.app_name),
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
                navigationIcon = null,
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
        ) {
            BitwardenListHeaderText(
                label = stringResource(id = R.string.test_categories),
                modifier = Modifier
                    .standardHorizontalMargin()
                    .padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))

            BitwardenPushRow(
                text = stringResource(id = R.string.autofill),
                onClick = onAutofillClick,
                cardStyle = CardStyle.Top(),
                modifier = Modifier.standardHorizontalMargin(),
            )

            BitwardenPushRow(
                text = stringResource(id = R.string.credential_manager),
                onClick = onCredentialManagerClick,
                cardStyle = CardStyle.Bottom,
                modifier = Modifier.standardHorizontalMargin(),
            )

            Spacer(modifier = Modifier.height(16.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LandingScreenPreview() {
    LandingScreenContent(
        onAutofillClick = {},
        onCredentialManagerClick = {},
    )
}
