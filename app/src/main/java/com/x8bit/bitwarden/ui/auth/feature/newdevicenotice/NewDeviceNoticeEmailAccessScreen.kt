package com.x8bit.bitwarden.ui.auth.feature.newdevicenotice

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.auth.feature.newdevicenotice.NewDeviceNoticeEmailAccessAction.ContinueClick
import com.x8bit.bitwarden.ui.auth.feature.newdevicenotice.NewDeviceNoticeEmailAccessAction.EmailAccessToggle
import com.x8bit.bitwarden.ui.auth.feature.newdevicenotice.NewDeviceNoticeEmailAccessEvent.NavigateToTwoFactorOptions
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.toggle.BitwardenSwitch
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * The top level composable for the new device notice email access screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun NewDeviceNoticeEmailAccessScreen(
    onNavigateToTwoFactorOptions: () -> Unit,
    viewModel: NewDeviceNoticeEmailAccessViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            NavigateToTwoFactorOptions -> onNavigateToTwoFactorOptions()
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
    ) {
        NewDeviceNoticeEmailAccessContent(
            email = state.email,
            isEmailAccessEnabled = state.isEmailAccessEnabled,
            onEmailAccessToggleChanged = remember(viewModel) {
                { newState ->
                    viewModel.trySendAction(EmailAccessToggle(newState = newState))
                }
            },
            onContinueClick = { viewModel.trySendAction(ContinueClick) },
        )
    }
}

@Composable
private fun NewDeviceNoticeEmailAccessContent(
    email: String,
    isEmailAccessEnabled: Boolean,
    onEmailAccessToggleChanged: (Boolean) -> Unit = {},
    onContinueClick: () -> Unit = {},
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
            email = email,
            isEmailAccessEnabled = isEmailAccessEnabled,
            onEmailAccessToggleChanged = onEmailAccessToggleChanged,
        )
        BitwardenFilledButton(
            label = stringResource(R.string.continue_text),
            onClick = onContinueClick,
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .imePadding(),
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
        painter = rememberVectorPainter(id = R.drawable.warning),
        contentDescription = null,
        modifier = Modifier.size(120.dp),
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 24.dp),
    ) {
        Text(
            text = stringResource(R.string.important_notice),
            style = BitwardenTheme.typography.headlineMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(
                R.string.bitwarden_will_soon_send_a_code_to_your_account_email_to_verify_logins_from_new_devices,
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
    email: String,
    isEmailAccessEnabled: Boolean = false,
    onEmailAccessToggleChanged: (Boolean) -> Unit = {},
) {
    Spacer(modifier = Modifier.size(24.dp))
    Column(
        modifier = Modifier
            .padding(horizontal = 22.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(size = 4.dp))
            .background(BitwardenTheme.colorScheme.background.secondary),
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(
                R.string.do_you_have_reliable_access_to_your_email,
                email,
            ),
            style = BitwardenTheme.typography.labelLarge,
            color = BitwardenTheme.colorScheme.text.primary,
            modifier = Modifier
                .padding(horizontal = 16.dp),
        )
        BitwardenSwitch(
            label = stringResource(id = R.string.yes_i_can_reliably_access_my_email),
            isChecked = isEmailAccessEnabled,
            onCheckedChange = onEmailAccessToggleChanged,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .testTag("EmailAccessToggle")
                .fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@PreviewScreenSizes
@Composable
private fun NewDeviceNoticeEmailAccessScreen_preview() {
    BitwardenTheme {
        NewDeviceNoticeEmailAccessContent(
            email = "test@bitwarden.com",
            isEmailAccessEnabled = true,
        )
    }
}
