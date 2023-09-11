package com.x8bit.bitwarden.ui.auth.feature.landing

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextField

/**
 * The top level composable for the Landing screen.
 */
@Composable
@Suppress("LongMethod")
fun LandingScreen(
    onNavigateToCreateAccount: () -> Unit,
    onNavigateToLogin: (emailAddress: String) -> Unit,
    viewModel: LandingViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            LandingEvent.NavigateToCreateAccount -> onNavigateToCreateAccount()
            is LandingEvent.NavigateToLogin -> onNavigateToLogin(event.emailAddress)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_legacy),
            contentDescription = null,
            modifier = Modifier
                .padding(start = 48.dp, top = 48.dp, end = 48.dp)
                .fillMaxWidth(),
        )

        Text(
            text = stringResource(id = R.string.log_in_or_create_account),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .align(alignment = Alignment.CenterHorizontally)
                .padding(horizontal = 24.dp),
        )

        BitwardenTextField(
            modifier = Modifier.testTag("Email address"),
            value = state.emailInput,
            onValueChange = { viewModel.trySendAction(LandingAction.EmailInputChanged(it)) },
            label = stringResource(id = R.string.email_address),
        )

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = stringResource(id = R.string.remember_me),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall,
            )

            Switch(
                modifier = Modifier.testTag("Remember me"),
                checked = state.isRememberMeEnabled,
                onCheckedChange = {
                    viewModel.trySendAction(LandingAction.RememberMeToggle(it))
                },
            )
        }

        Button(
            onClick = {
                viewModel.trySendAction(LandingAction.ContinueButtonClick)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("Continue button"),
            enabled = state.isContinueButtonEnabled,
        ) {
            Text(
                text = stringResource(id = R.string.continue_button),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = stringResource(id = R.string.new_around_here),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall,
            )

            Text(
                modifier = Modifier
                    .clickable {
                        viewModel.trySendAction(LandingAction.CreateAccountClick)
                    }
                    .padding(start = 2.dp),
                text = stringResource(id = R.string.create_account),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
