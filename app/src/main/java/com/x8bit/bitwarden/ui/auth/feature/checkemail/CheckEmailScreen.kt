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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.createAnnotatedString
import com.x8bit.bitwarden.ui.platform.base.util.standardHorizontalMargin
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
    intentManager: IntentManager = LocalIntentManager.current,
    viewModel: CheckEmailViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    EventsEffect(viewModel) { event ->
        when (event) {
            is CheckEmailEvent.NavigateBack -> {
                onNavigateBack.invoke()
            }

            is CheckEmailEvent.NavigateToEmailApp -> {
                intentManager.startDefaultEmailApplication()
            }
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
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(CheckEmailAction.BackClick) }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .imePadding()
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            CheckEmailContent(
                email = state.email,
                onOpenEmailAppClick = remember(viewModel) {
                    {
                        viewModel.trySendAction(CheckEmailAction.OpenEmailClick)
                    }
                },
                onChangeEmailClick = remember(viewModel) {
                    {
                        viewModel.trySendAction(CheckEmailAction.ChangeEmailClick)
                    }
                },
                modifier = Modifier.standardHorizontalMargin(),
            )
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
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .wrapContentHeight()
                .fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))

        val descriptionAnnotatedString = createAnnotatedString(
            mainString = stringResource(
                id = R.string.we_sent_an_email_to,
                email,
            ),
            highlights = listOf(email),
            highlightStyle = SpanStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                fontWeight = FontWeight.Bold,
            ),
            tag = "EMAIL",
        )
        Text(
            text = descriptionAnnotatedString,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .fillMaxWidth()
                .wrapContentHeight(),
        )
        Spacer(modifier = Modifier.height(16.dp))
        @Suppress("MaxLineLength")
        Text(
            text = stringResource(R.string.select_the_link_in_the_email_to_verify_your_email_address_and_continue_creating_your_account),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
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

@Preview(showBackground = true)
@Composable
private fun CheckEmailScreenPreview() {
    BitwardenTheme {
        CheckEmailContent(
            email = "email@fake.com",
            onOpenEmailAppClick = { },
            onChangeEmailClick = { },
            modifier = Modifier.standardHorizontalMargin(),
        )
    }
}
