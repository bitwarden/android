package com.x8bit.bitwarden.ui.auth.feature.welcome

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenOutlinedButton
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.platform.util.isPortrait

/**
 * The custom horizontal margin that is specific to this screen.
 */
private val LANDSCAPE_HORIZONTAL_MARGIN: Dp = 128.dp

/**
 * Top level composable for the welcome screen.
 */
@Composable
fun WelcomeScreen(
    onNavigateToCreateAccount: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToStartRegistration: () -> Unit,
    viewModel: WelcomeViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { state.pages.size })

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is WelcomeEvent.UpdatePager -> {
                pagerState.animateScrollToPage(event.index)
            }

            WelcomeEvent.NavigateToCreateAccount -> onNavigateToCreateAccount()
            WelcomeEvent.NavigateToLogin -> onNavigateToLogin()
            WelcomeEvent.NavigateToStartRegistration -> onNavigateToStartRegistration()
        }
    }

    BitwardenScaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BitwardenTheme.colorScheme.background.secondary,
        contentColor = BitwardenTheme.colorScheme.text.secondary,
    ) {
        WelcomeScreenContent(
            state = state,
            pagerState = pagerState,
            onPagerSwipe = remember(viewModel) {
                { viewModel.trySendAction(WelcomeAction.PagerSwipe(it)) }
            },
            onDotClick = remember(viewModel) {
                { viewModel.trySendAction(WelcomeAction.DotClick(it)) }
            },
            onCreateAccountClick = remember(viewModel) {
                { viewModel.trySendAction(WelcomeAction.CreateAccountClick) }
            },
            onLoginClick = remember(viewModel) {
                { viewModel.trySendAction(WelcomeAction.LoginClick) }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun WelcomeScreenContent(
    state: WelcomeState,
    pagerState: PagerState,
    onPagerSwipe: (Int) -> Unit,
    onDotClick: (Int) -> Unit,
    onCreateAccountClick: () -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(pagerState.currentPage) {
        onPagerSwipe(pagerState.currentPage)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.verticalScroll(rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.weight(1f))

        HorizontalPager(state = pagerState) { index ->
            val pageNumberContentDescription =
                stringResource(R.string.page_number_x_of_y, index + 1, state.pages.size)
            val pagerSemanticsModifier = Modifier.semantics(mergeDescendants = true) {
                contentDescription = pageNumberContentDescription
            }
            if (LocalConfiguration.current.isPortrait) {
                WelcomeCardPortrait(
                    state = state.pages[index],
                    modifier = pagerSemanticsModifier.standardHorizontalMargin(),
                )
            } else {
                WelcomeCardLandscape(
                    state = state.pages[index],
                    modifier = pagerSemanticsModifier
                        .standardHorizontalMargin(landscape = LANDSCAPE_HORIZONTAL_MARGIN),
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        IndicatorDots(
            selectedIndexProvider = { state.index },
            totalCount = state.pages.size,
            onDotClick = onDotClick,
            modifier = Modifier
                .padding(bottom = 32.dp)
                .height(44.dp),
        )

        BitwardenFilledButton(
            label = stringResource(id = R.string.create_account),
            onClick = onCreateAccountClick,
            modifier = Modifier
                .standardHorizontalMargin(landscape = LANDSCAPE_HORIZONTAL_MARGIN)
                .fillMaxWidth(),
        )

        BitwardenOutlinedButton(
            label = stringResource(id = R.string.log_in_verb),
            onClick = onLoginClick,
            modifier = Modifier
                .standardHorizontalMargin(landscape = LANDSCAPE_HORIZONTAL_MARGIN)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(32.dp))
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun WelcomeCardLandscape(
    state: WelcomeState.WelcomeCard,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.semantics(mergeDescendants = true) {},
    ) {
        Image(
            painter = rememberVectorPainter(id = state.imageRes),
            contentDescription = null,
            modifier = Modifier.size(124.dp),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(start = 40.dp),
        ) {
            Text(
                text = stringResource(id = state.titleRes),
                textAlign = TextAlign.Center,
                style = BitwardenTheme.typography.headlineMedium,
                color = BitwardenTheme.colorScheme.text.primary,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            Text(
                text = stringResource(id = state.messageRes),
                textAlign = TextAlign.Center,
                style = BitwardenTheme.typography.bodyLarge,
                color = BitwardenTheme.colorScheme.text.primary,
            )
        }
    }
}

@Composable
private fun WelcomeCardPortrait(
    state: WelcomeState.WelcomeCard,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .semantics(mergeDescendants = true) {},
    ) {
        Image(
            painter = rememberVectorPainter(id = state.imageRes),
            contentDescription = null,
            modifier = Modifier.size(124.dp),
        )

        Text(
            text = stringResource(id = state.titleRes),
            textAlign = TextAlign.Center,
            style = BitwardenTheme.typography.headlineMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            modifier = Modifier
                .padding(
                    top = 48.dp,
                    bottom = 16.dp,
                ),
        )

        Text(
            text = stringResource(id = state.messageRes),
            textAlign = TextAlign.Center,
            style = BitwardenTheme.typography.bodyLarge,
            color = BitwardenTheme.colorScheme.text.primary,
        )
    }
}

@Composable
private fun IndicatorDots(
    selectedIndexProvider: () -> Int,
    totalCount: Int,
    onDotClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        items(totalCount) { index ->
            val color = animateColorAsState(
                targetValue = BitwardenTheme.colorScheme.text.primary.copy(
                    alpha = if (index == selectedIndexProvider()) 1.0f else 0.3f,
                ),
                label = "dotColor",
            )

            Box(
                modifier = Modifier
                    .clearAndSetSemantics {
                        // clear semantics so indicator dots are skipped by screen reader
                    }
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color.value)
                    .clickable { onDotClick(index) },
            )
        }
    }
}
