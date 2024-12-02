package com.x8bit.bitwarden.ui.auth.feature.newdevicenotice

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.auth.feature.newdevicenotice.NewDeviceNoticeTwoFactorEvent.NavigateToTurnOnTwoFactor
import com.x8bit.bitwarden.ui.auth.feature.newdevicenotice.NewDeviceNoticeTwoFactorEvent.NavigateToChangeAccountEmail
import com.x8bit.bitwarden.ui.auth.feature.newdevicenotice.NewDeviceNoticeTwoFactorEvent.NavigateBack
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenOutlinedButton
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.text.BitwardenClickableText
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.composition.LocalIntentManager
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * The top level composable for the new device notice email access screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun NewDeviceNoticeTwoFactorScreen(
    onNavigateBack: () -> Unit,
    intentManager: IntentManager = LocalIntentManager.current,
    viewModel: NewDeviceNoticeTwoFactorViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is NavigateToTurnOnTwoFactor -> {
                intentManager.launchUri(event.url.toUri())
            }

            is NavigateToChangeAccountEmail -> {
                intentManager.launchUri(event.url.toUri())
            }

            NavigateBack -> onNavigateBack()
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
    ) {
        NewDeviceNoticeTwoFactorContent(
            onTurnOnTwoFactorClick = {},
            onChangeAccountEmailClick = {},
            onRemindMeLaterClick = {},
        )
    }
}

@Composable
private fun NewDeviceNoticeTwoFactorContent(
    onTurnOnTwoFactorClick: () -> Unit = {},
    onChangeAccountEmailClick: () -> Unit = {},
    onRemindMeLaterClick: () -> Unit = {},
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(state = rememberScrollState()),
    ) {
        HeaderContent()
        MainContent(
            onTurnOnTwoFactorClick = onTurnOnTwoFactorClick,
            onChangeAccountEmailClick = onChangeAccountEmailClick,
            onRemindMeLaterClick = onRemindMeLaterClick,
        )
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

/**
 * Header content containing the warning icon and title.
 */

@Suppress("MaxLineLength")
@Composable
private fun HeaderContent() {
    Image(
        painter = rememberVectorPainter(id = R.drawable.user_lock),
        contentDescription = null,
        modifier = Modifier.size(120.dp),
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 24.dp),
    ) {
        Text(
            text = stringResource(R.string.set_up_two_step_login),
            style = BitwardenTheme.typography.headlineMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(
                R.string.you_can_set_up_two_step_login_as_an_alternative_way_to_protect_your_account_or_change_your_email_to_one_you_can_access,
            ),
            style = BitwardenTheme.typography.bodyLarge,
            color = BitwardenTheme.colorScheme.text.primary,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * The main content of the screen.
 */
@Composable
private fun MainContent(
    onTurnOnTwoFactorClick: () -> Unit = {},
    onChangeAccountEmailClick: () -> Unit = {},
    onRemindMeLaterClick: () -> Unit = {},
) {
    Spacer(modifier = Modifier.size(24.dp))
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 22.dp)
            .fillMaxWidth(),
    ) {
        BitwardenFilledButton(
            label = stringResource(R.string.turn_on_two_step_login),
            onClick = onTurnOnTwoFactorClick,
            iconRight = rememberVectorPainter(id = R.drawable.ic_external_link),
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenOutlinedButton(
            label = stringResource(R.string.change_account_email),
            onClick = onChangeAccountEmailClick,
            iconRight = rememberVectorPainter(id = R.drawable.ic_external_link),
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
        )
        Spacer(modifier = Modifier.height(16.dp))
        BitwardenClickableText(
            label = stringResource(id = R.string.remind_me_later),
            onClick = onRemindMeLaterClick,
            style = BitwardenTheme.typography.bodyLarge,
            innerPadding = PaddingValues(vertical = 4.dp, horizontal = 0.dp),
        )
    }
}

@PreviewScreenSizes
@Composable
private fun NewDeviceNoticeTwoFactorScreen_preview() {
    BitwardenTheme {
        NewDeviceNoticeTwoFactorContent()
    }
}
