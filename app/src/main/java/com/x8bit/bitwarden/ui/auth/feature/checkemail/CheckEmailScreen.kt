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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.createAnnotatedString
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.composition.LocalIntentManager
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager

/**
 * Constant string to be used in string annotation tag field
 */
private const val TAG_URL = "URL"

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
    EventsEffect(viewModel) { event ->
        when (event) {
            is CheckEmailEvent.NavigateBack -> {
                onNavigateBack.invoke()
            }

            is CheckEmailEvent.NavigateToEmailApp -> {
                intentManager.startDefaultEmailApplication()
            }

            is CheckEmailEvent.NavigateBackToLanding -> {
                onNavigateBackToLanding.invoke()
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
                navigationIcon = rememberVectorPainter(id = R.drawable.ic_close),
                navigationIconContentDescription = stringResource(id = R.string.close),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(CheckEmailAction.CloseClick) }
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
            Spacer(modifier = Modifier.height(32.dp))
            Image(
                painter = rememberVectorPainter(id = R.drawable.email_check),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
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
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .wrapContentHeight()
                    .fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))

            @Suppress("MaxLineLength")
            val descriptionAnnotatedString = createAnnotatedString(
                mainString = stringResource(
                    id = R.string.follow_the_instructions_in_the_email_sent_to_x_to_continue_creating_your_account,
                    state.email,
                ),
                highlights = listOf(state.email),
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
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .wrapContentHeight(),
            )
            Spacer(modifier = Modifier.height(32.dp))
            BitwardenFilledButton(
                label = stringResource(id = R.string.open_email_app),
                onClick = remember(viewModel) {
                    { viewModel.trySendAction(CheckEmailAction.OpenEmailClick) }
                },
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
                val goBackAnnotatedString = createAnnotatedString(
                    mainString = stringResource(
                        id = R.string.no_email_go_back_to_edit_your_email_address,
                    ),
                    highlights = listOf(stringResource(id = R.string.go_back)),
                    tag = TAG_URL,
                )
                ClickableText(
                    text = goBackAnnotatedString,
                    onClick = {
                        goBackAnnotatedString
                            .getStringAnnotations(TAG_URL, it, it)
                            .firstOrNull()?.let {
                                viewModel.trySendAction(CheckEmailAction.CloseClick)
                            }
                    },
                )
                Spacer(modifier = Modifier.height(32.dp))
                val logInAnnotatedString = createAnnotatedString(
                    mainString = stringResource(
                        id = R.string.or_log_in_you_may_already_have_an_account,
                    ),
                    highlights = listOf(stringResource(id = R.string.log_in)),
                    tag = TAG_URL,
                )
                ClickableText(
                    text = logInAnnotatedString,
                    onClick = {
                        logInAnnotatedString
                            .getStringAnnotations(TAG_URL, it, it)
                            .firstOrNull()?.let {
                                viewModel.trySendAction(CheckEmailAction.LoginClick)
                            }
                    },
                )
            }
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}
