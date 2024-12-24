package com.x8bit.bitwarden.ui.auth.feature.newdevicenotice

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
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
import com.x8bit.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.x8bit.bitwarden.ui.platform.base.util.toAnnotatedString
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.toggle.BitwardenSwitch
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * The top level composable for the new device notice email access screen.
 */
@Composable
fun NewDeviceNoticeEmailAccessScreen(
    onNavigateBackToVault: () -> Unit,
    onNavigateToTwoFactorOptions: () -> Unit,
    viewModel: NewDeviceNoticeEmailAccessViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            NavigateToTwoFactorOptions -> onNavigateToTwoFactorOptions()
            NewDeviceNoticeEmailAccessEvent.NavigateBackToVault -> onNavigateBackToVault()
        }
    }

    BitwardenScaffold {
        NewDeviceNoticeEmailAccessContent(
            email = state.email,
            isEmailAccessEnabled = state.isEmailAccessEnabled,
            onEmailAccessToggleChanged = remember(viewModel) {
                { newState ->
                    viewModel.trySendAction(EmailAccessToggle(isEnabled = newState))
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
    onEmailAccessToggleChanged: (Boolean) -> Unit,
    onContinueClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .standardHorizontalMargin()
            .fillMaxSize()
            .verticalScroll(state = rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.height(104.dp))
        HeaderContent()
        Spacer(modifier = Modifier.height(24.dp))
        MainContent(
            email = email,
            isEmailAccessEnabled = isEmailAccessEnabled,
            onEmailAccessToggleChanged = onEmailAccessToggleChanged,
        )
        Spacer(modifier = Modifier.height(24.dp))
        BitwardenFilledButton(
            label = stringResource(R.string.continue_text),
            onClick = onContinueClick,
            modifier = Modifier
                .fillMaxSize()
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
private fun ColumnScope.HeaderContent() {
    Image(
        painter = rememberVectorPainter(id = R.drawable.warning),
        contentDescription = null,
        modifier = Modifier.size(120.dp),
    )
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = stringResource(R.string.important_notice),
        style = BitwardenTheme.typography.titleMedium,
        color = BitwardenTheme.colorScheme.text.primary,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = stringResource(
            R.string.bitwarden_will_soon_send_a_code_to_your_account_email_to_verify_logins_from_new_devices_in_february,
        ),
        style = BitwardenTheme.typography.bodyMedium,
        color = BitwardenTheme.colorScheme.text.primary,
        textAlign = TextAlign.Center,
    )
}

/**
 * The main content of the screen.
 */
@Composable
private fun MainContent(
    email: String,
    isEmailAccessEnabled: Boolean,
    onEmailAccessToggleChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        Text(
            text = R.string.do_you_have_reliable_access_to_your_email.toAnnotatedString(
                args = arrayOf(email),
                style = SpanStyle(
                    color = BitwardenTheme.colorScheme.text.primary,
                    fontSize = BitwardenTheme.typography.bodyLarge.fontSize,
                    fontWeight = FontWeight.Normal,
                ),
                emphasisHighlightStyle = SpanStyle(
                    color = BitwardenTheme.colorScheme.text.primary,
                    fontSize = BitwardenTheme.typography.bodyLarge.fontSize,
                    fontWeight = FontWeight.Bold,
                ),
            ),
        )
        Column {
            BitwardenSwitch(
                label = stringResource(id = R.string.yes_i_can_reliably_access_my_email),
                isChecked = isEmailAccessEnabled,
                onCheckedChange = onEmailAccessToggleChanged,
                modifier = Modifier
                    .testTag("EmailAccessToggle"),
            )
        }
    }
}

@PreviewScreenSizes
@Composable
private fun NewDeviceNoticeEmailAccessScreen_preview() {
    BitwardenTheme {
        NewDeviceNoticeEmailAccessContent(
            email = "test@bitwarden.com",
            isEmailAccessEnabled = true,
            onEmailAccessToggleChanged = {},
            onContinueClick = {},
        )
    }
}
