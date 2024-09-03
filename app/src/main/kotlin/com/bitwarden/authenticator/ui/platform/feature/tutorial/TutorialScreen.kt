package com.bitwarden.authenticator.ui.platform.feature.tutorial

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.ui.platform.base.util.EventsEffect
import com.bitwarden.authenticator.ui.platform.components.button.BitwardenFilledTonalButton
import com.bitwarden.authenticator.ui.platform.components.button.BitwardenTextButton
import com.bitwarden.authenticator.ui.platform.components.scaffold.BitwardenScaffold

private const val INTRO_PAGE = 0
private const val QR_SCANNER_PAGE = 1
private const val UNIQUE_CODES_PAGE = 2
private const val PAGE_COUNT = 3

/**
 * Top level composable for the tutorial screen.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TutorialScreen(
    viewModel: TutorialViewModel = hiltViewModel(),
    onTutorialFinished: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            TutorialEvent.NavigateToAuthenticator -> {
                onTutorialFinished()
            }

            TutorialEvent.NavigateToQrScannerSlide -> {
                pagerState.animateScrollToPage(page = QR_SCANNER_PAGE)
            }

            TutorialEvent.NavigateToUniqueCodesSlide -> {
                pagerState.animateScrollToPage(page = UNIQUE_CODES_PAGE)
            }
        }
    }

    BitwardenScaffold(
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding),
            verticalArrangement = Arrangement.Center,
        ) {
            HorizontalPager(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .weight(1f),
                state = pagerState,
                userScrollEnabled = true,
            ) { page ->
                viewModel.trySendAction(
                    TutorialAction.TutorialPageChange(pagerState.targetPage),
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    when (page) {
                        INTRO_PAGE -> VerificationCodesContent()

                        QR_SCANNER_PAGE -> TutorialQrScannerScreen()

                        UNIQUE_CODES_PAGE -> UniqueCodesContent()
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.Bottom,
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .height(50.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        repeat(PAGE_COUNT) {
                            val color = if (pagerState.currentPage == it) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.secondary
                            }
                            Box(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .background(color, CircleShape)
                                    .size(10.dp),
                            )
                        }
                    }
                }

                item {
                    BitwardenFilledTonalButton(
                        modifier = Modifier.fillMaxWidth(),
                        label = state.continueButtonText(),
                        onClick = remember(viewModel) {
                            {
                                viewModel.trySendAction(
                                    TutorialAction.ContinueClick,
                                )
                            }
                        },
                    )
                }

                item {
                    val alpha = remember(state) {
                        if (state.isLastPage) {
                            0f
                        } else {
                            1f
                        }
                    }
                    BitwardenTextButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(alpha),
                        isEnabled = !state.isLastPage,
                        label = stringResource(id = R.string.skip),
                        onClick = remember(viewModel) {
                            {
                                viewModel.trySendAction(TutorialAction.SkipClick)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun VerificationCodesContent() {
    Image(
        painter = painterResource(R.drawable.ic_tutorial_verification_codes),
        contentDescription = stringResource(
            id = R.string.secure_your_accounts_with_bitwarden_authenticator,
        ),
    )
    Spacer(Modifier.height(24.dp))
    Text(
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
        text = stringResource(R.string.secure_your_accounts_with_bitwarden_authenticator),
    )
    Spacer(Modifier.height(8.dp))
    Text(
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        text = stringResource(R.string.get_verification_codes_for_all_your_accounts),
    )
}

@Composable
private fun TutorialQrScannerScreen() {
    Image(
        painter = painterResource(id = R.drawable.ic_tutorial_qr_scanner),
        contentDescription = stringResource(id = R.string.scan_qr_code),
    )
    Spacer(Modifier.height(24.dp))
    Text(
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
        text = stringResource(
            R.string.use_your_device_camera_to_scan_codes,
        ),
    )
    Spacer(Modifier.height(8.dp))
    Text(
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        text = stringResource(
            R.string.scan_the_qr_code_in_your_2_step_verification_settings_for_any_account,
        ),
    )
}

@Suppress("MaxLineLength")
@Composable
private fun UniqueCodesContent() {
    Image(
        painter = painterResource(id = R.drawable.ic_tutorial_2fa),
        contentDescription = stringResource(id = R.string.unique_codes),
    )
    Spacer(Modifier.height(24.dp))
    Text(
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
        text = stringResource(R.string.sign_in_using_unique_codes),
    )
    Spacer(Modifier.height(8.dp))
    Text(
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        text = stringResource(
            R.string.when_using_2_step_verification_youll_enter_your_username_and_password_and_a_code_generated_in_this_app,
        ),
    )
}

@Preview
@Composable
private fun TutorialScreenPreview() {
    Box {
        TutorialScreen(
            onTutorialFinished = {},
        )
    }
}
