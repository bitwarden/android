package com.bitwarden.authenticator.ui.platform.feature.tutorial

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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.authenticator.ui.platform.util.isPortrait
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import kotlinx.coroutines.launch

/**
 * Top level composable for the tutorial screen.
 */
@Composable
fun TutorialScreen(
    viewModel: TutorialViewModel = hiltViewModel(),
    onTutorialFinished: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { state.pages.size })
    val coroutineScope = rememberCoroutineScope()

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            TutorialEvent.NavigateToAuthenticator -> {
                onTutorialFinished()
            }

            is TutorialEvent.UpdatePager -> {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(event.index)
                }
            }
        }
    }

    BitwardenScaffold(
        modifier = Modifier.fillMaxSize(),
    ) {
        TutorialScreenContent(
            state = state,
            pagerState = pagerState,
            onPagerSwipe = remember(viewModel) {
                { viewModel.trySendAction(TutorialAction.PagerSwipe(it)) }
            },
            onDotClick = remember(viewModel) {
                { viewModel.trySendAction(TutorialAction.DotClick(it)) }
            },
            continueClick = remember(viewModel) {
                { viewModel.trySendAction(TutorialAction.ContinueClick(it)) }
            },
            skipClick = remember(viewModel) {
                { viewModel.trySendAction(TutorialAction.SkipClick) }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun TutorialScreenContent(
    state: TutorialState,
    pagerState: PagerState,
    onPagerSwipe: (Int) -> Unit,
    onDotClick: (Int) -> Unit,
    continueClick: (Int) -> Unit,
    skipClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(pagerState.currentPage) {
        onPagerSwipe(pagerState.currentPage)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.weight(1f))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
        ) { index ->
            if (LocalConfiguration.current.isPortrait) {
                TutorialScreenPortrait(
                    state = state.pages[index],
                    modifier = Modifier
                        .standardHorizontalMargin()
                        .statusBarsPadding(),
                )
            } else {
                TutorialScreenLandscape(
                    state = state.pages[index],
                    modifier = Modifier
                        .standardHorizontalMargin()
                        .statusBarsPadding(),
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        IndicatorDots(
            selectedIndexProvider = { state.index },
            totalCount = state.pages.size,
            onDotClick = onDotClick,
            modifier = Modifier
                .padding(bottom = 12.dp)
                .height(44.dp),
        )

        BitwardenFilledButton(
            label = state.actionButtonText,
            onClick = { continueClick(state.index) },
            modifier = Modifier
                .standardHorizontalMargin()
                .fillMaxWidth(),
        )

        BitwardenTextButton(
            isEnabled = !state.isLastPage,
            label = stringResource(id = BitwardenString.skip),
            onClick = skipClick,
            modifier = Modifier
                .standardHorizontalMargin()
                .fillMaxWidth()
                .alpha(if (state.isLastPage) 0f else 1f)
                .padding(bottom = 12.dp),
        )

        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun TutorialScreenPortrait(
    state: TutorialState.TutorialSlide,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Image(
            painter = rememberVectorPainter(id = state.image),
            contentDescription = null,
            modifier = Modifier.size(size = 124.dp),
        )

        Text(
            text = stringResource(id = state.title),
            textAlign = TextAlign.Center,
            style = BitwardenTheme.typography.headlineMedium,
            modifier = Modifier
                .padding(
                    top = 48.dp,
                    bottom = 16.dp,
                ),
        )
        Text(
            text = stringResource(id = state.message),
            textAlign = TextAlign.Center,
            style = BitwardenTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun TutorialScreenLandscape(
    state: TutorialState.TutorialSlide,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Image(
            painter = rememberVectorPainter(id = state.image),
            contentDescription = null,
            modifier = Modifier.size(size = 124.dp),
        )

        Spacer(modifier = Modifier.weight(1f))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(start = 40.dp),
        ) {
            Text(
                text = stringResource(id = state.title),
                textAlign = TextAlign.Center,
                style = BitwardenTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            Text(
                text = stringResource(id = state.message),
                textAlign = TextAlign.Center,
                style = BitwardenTheme.typography.bodyLarge,
            )
        }

        Spacer(modifier = Modifier.weight(1f))
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
                targetValue = BitwardenTheme.colorScheme.icon.secondary.copy(
                    alpha = if (index == selectedIndexProvider()) 1.0f else 0.3f,
                ),
                label = "dotColor",
            )

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color.value)
                    .clickable { onDotClick(index) },
            )
        }
    }
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
