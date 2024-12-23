package com.x8bit.bitwarden.ui.auth.feature.checkemail

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.auth.feature.checkemail.handlers.rememberCheckEmailHandler
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.x8bit.bitwarden.ui.platform.base.util.toAnnotatedString
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.composition.LocalIntentManager
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Top level composable for the check email screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun CheckEmailScreen(
    onNavigateBack: () -> Unit,
    onNavigateBackToLanding: () -> Unit,
    intentManager: IntentManager = LocalIntentManager.current,
    viewModel: CheckEmailViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val handler = rememberCheckEmailHandler(viewModel = viewModel)
    EventsEffect(viewModel) { event ->
        when (event) {
            is CheckEmailEvent.NavigateBack -> {
                onNavigateBack()
            }

            is CheckEmailEvent.NavigateToEmailApp -> {
                intentManager.startDefaultEmailApplication()
            }

            CheckEmailEvent.NavigateBackToLanding -> onNavigateBackToLanding()
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.create_account),
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = R.drawable.ic_back),
                navigationIconContentDescription = stringResource(id = R.string.back),
                onNavigationIconClick = handler.onBackClick,
            )
        },
    ) {
        Column(
            modifier = Modifier
                .imePadding()
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            if (state.showNewOnboardingUi) {
                CheckEmailContent(
                    email = state.email,
                    onOpenEmailAppClick = handler.onOpenEmailAppClick,
                    onChangeEmailClick = handler.onChangeEmailClick,
                    modifier = Modifier.standardHorizontalMargin(),
                )
            } else {
                CheckEmailLegacyContent(
                    email = state.email,
                    onOpenEmailAppClick = handler.onOpenEmailAppClick,
                    onChangeEmailClick = handler.onChangeEmailClick,
                    onLoginClick = handler.onLoginClick,
                )
            }
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun CheckEmailContent(
    email: String,
    onOpenEmailAppClick: () -> Unit,
    onChangeEmailClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Image(
            painter = rememberVectorPainter(id = R.drawable.open_email),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(100.dp),
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = stringResource(id = R.string.check_your_email),
            textAlign = TextAlign.Center,
            style = BitwardenTheme.typography.titleMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .wrapContentHeight()
                .fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))

        val descriptionAnnotatedString = R.string.we_sent_an_email_to.toAnnotatedString(
            args = arrayOf(email),
            emphasisHighlightStyle = SpanStyle(
                color = BitwardenTheme.colorScheme.text.primary,
                fontSize = BitwardenTheme.typography.bodyMedium.fontSize,
                fontWeight = FontWeight.Bold,
            ),
        )
        Text(
            text = descriptionAnnotatedString,
            textAlign = TextAlign.Center,
            style = BitwardenTheme.typography.bodyMedium,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .fillMaxWidth()
                .wrapContentHeight(),
        )
        Spacer(modifier = Modifier.height(16.dp))
        @Suppress("MaxLineLength")
        Text(
            text = stringResource(R.string.select_the_link_in_the_email_to_verify_your_email_address_and_continue_creating_your_account),
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .fillMaxWidth()
                .wrapContentHeight(),
        )
        Spacer(modifier = Modifier.height(32.dp))
        BitwardenFilledButton(
            label = stringResource(id = R.string.open_email_app),
            onClick = onOpenEmailAppClick,
            modifier = Modifier
                .testTag("OpenEmailApp")
                .fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        BitwardenTextButton(
            label = stringResource(R.string.change_email_address),
            onClick = onChangeEmailClick,
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun CheckEmailLegacyContent(
    email: String,
    onOpenEmailAppClick: () -> Unit,
    onChangeEmailClick: () -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Image(
            painter = rememberVectorPainter(id = R.drawable.email_check),
            colorFilter = ColorFilter.tint(BitwardenTheme.colorScheme.icon.secondary),
            contentDescription = null,
            contentScale = ContentScale.FillHeight,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .height(112.dp)
                .fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = stringResource(id = R.string.check_your_email),
            textAlign = TextAlign.Center,
            style = BitwardenTheme.typography.headlineSmall,
            color = BitwardenTheme.colorScheme.text.primary,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .wrapContentHeight()
                .fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))

        @Suppress("MaxLineLength")
        val descriptionAnnotatedString =
            R.string.follow_the_instructions_in_the_email_sent_to_x_to_continue_creating_your_account.toAnnotatedString(
                email,
                emphasisHighlightStyle = SpanStyle(
                color = BitwardenTheme.colorScheme.text.primary,
                fontSize = BitwardenTheme.typography.bodyMedium.fontSize,
                fontWeight = FontWeight.Bold,
            ),
        )
        Text(
            text = descriptionAnnotatedString,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .wrapContentHeight(),
        )
        Spacer(modifier = Modifier.height(32.dp))
        BitwardenFilledButton(
            label = stringResource(id = R.string.open_email_app),
            onClick = onOpenEmailAppClick,
            modifier = Modifier
                .testTag("OpenEmailApp")
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(32.dp))
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = R.string.no_email_go_back_to_edit_your_email_address.toAnnotatedString {
                    onChangeEmailClick()
                },
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = R.string.or_log_in_you_may_already_have_an_account
                    .toAnnotatedString {
                        onLoginClick()
                    },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CheckEmailScreenNewUi_preview() {
    BitwardenTheme {
        CheckEmailContent(
            email = "email@fake.com",
            onOpenEmailAppClick = { },
            onChangeEmailClick = { },
            modifier = Modifier.standardHorizontalMargin(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CheckEmailScreenLegacy_preview() {
    BitwardenTheme {
        CheckEmailLegacyContent(
            email = "email@fake.com",
            onOpenEmailAppClick = { },
            onChangeEmailClick = { },
            onLoginClick = {},
        )
    }
}
