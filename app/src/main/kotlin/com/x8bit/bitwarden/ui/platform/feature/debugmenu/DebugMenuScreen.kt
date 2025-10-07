package com.x8bit.bitwarden.ui.platform.feature.debugmenu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.core.data.manager.model.FlagKey
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.base.util.toListItemCardStyle
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.appbar.NavigationIcon
import com.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.bitwarden.ui.platform.components.debug.ListItemContent
import com.bitwarden.ui.platform.components.divider.BitwardenHorizontalDivider
import com.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf

/**
 * Top level screen for the debug menu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun DebugMenuScreen(
    onNavigateBack: () -> Unit,
    viewModel: DebugMenuViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            DebugMenuEvent.NavigateBack -> onNavigateBack()
        }
    }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(BitwardenString.debug_menu),
                scrollBehavior = scrollBehavior,
                navigationIcon = NavigationIcon(
                    navigationIcon = rememberVectorPainter(BitwardenDrawable.ic_back),
                    navigationIconContentDescription = stringResource(id = BitwardenString.back),
                    onNavigationIconClick = remember(viewModel) {
                        {
                            viewModel.trySendAction(DebugMenuAction.NavigateBack)
                        }
                    },
                ),
            )
        },
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(height = 12.dp))
            if (state.featureFlags.isNotEmpty()) {
                FeatureFlagContent(
                    featureFlagMap = state.featureFlags,
                    onValueChange = remember(viewModel) {
                        { key, value ->
                            viewModel.trySendAction(DebugMenuAction.UpdateFeatureFlag(key, value))
                        }
                    },
                    onResetValues = remember(viewModel) {
                        { viewModel.trySendAction(DebugMenuAction.ResetFeatureFlagValues) }
                    },
                )
                Spacer(Modifier.height(height = 16.dp))
                BitwardenHorizontalDivider()
                Spacer(Modifier.height(height = 16.dp))
            }
            OnboardingOverrideContent(
                onStartOnboarding = remember(viewModel) {
                    {
                        viewModel.trySendAction(DebugMenuAction.RestartOnboarding)
                    }
                },
                onStartOnboardingCarousel = remember(viewModel) {
                    {
                        viewModel.trySendAction(DebugMenuAction.RestartOnboardingCarousel)
                    }
                },
            )
            Spacer(Modifier.height(16.dp))
            BitwardenFilledButton(
                label = stringResource(BitwardenString.reset_coach_mark_tour_status),
                onClick = remember(viewModel) {
                    {
                        viewModel.trySendAction(DebugMenuAction.ResetCoachMarkTourStatuses)
                    }
                },
                isEnabled = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )
            Spacer(Modifier.height(height = 16.dp))
            BitwardenHorizontalDivider()
            Spacer(Modifier.height(height = 16.dp))
            BitwardenListHeaderText(
                label = stringResource(BitwardenString.error_reports),
                modifier = Modifier
                    .standardHorizontalMargin()
                    .padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(height = 8.dp))
            BitwardenFilledButton(
                label = stringResource(BitwardenString.generate_error_report),
                onClick = remember(viewModel) {
                    { viewModel.trySendAction(DebugMenuAction.GenerateErrorReportClick) }
                },
                isEnabled = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )
            Spacer(modifier = Modifier.height(height = 8.dp))
            BitwardenFilledButton(
                label = stringResource(BitwardenString.generate_crash),
                onClick = remember(viewModel) {
                    { viewModel.trySendAction(DebugMenuAction.GenerateCrashClick) }
                },
                isEnabled = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )
            Spacer(modifier = Modifier.height(height = 16.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun FeatureFlagContent(
    featureFlagMap: ImmutableMap<FlagKey<Any>, Any>,
    onValueChange: (key: FlagKey<Any>, value: Any) -> Unit,
    onResetValues: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        BitwardenListHeaderText(
            label = stringResource(BitwardenString.feature_flags),
            modifier = Modifier
                .standardHorizontalMargin()
                .padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        featureFlagMap.forEach { featureFlag ->
            featureFlag.key.ListItemContent(
                currentValue = featureFlag.value,
                onValueChange = onValueChange,
                cardStyle = featureFlagMap.keys.toListItemCardStyle(
                    index = featureFlagMap.keys.indexOf(element = featureFlag.key),
                ),
                modifier = Modifier
                    .standardHorizontalMargin()
                    .fillMaxWidth(),
            )
        }
        Spacer(modifier = Modifier.height(height = 16.dp))
        BitwardenFilledButton(
            label = stringResource(BitwardenString.reset_values),
            onClick = onResetValues,
            modifier = Modifier
                .standardHorizontalMargin()
                .fillMaxWidth(),
        )
    }
}

/**
 * The content for the onboarding override feature flag.
 */
@Composable
private fun OnboardingOverrideContent(
    onStartOnboarding: () -> Unit,
    onStartOnboardingCarousel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        BitwardenListHeaderText(
            label = stringResource(BitwardenString.onboarding_override),
            modifier = Modifier
                .standardHorizontalMargin()
                .padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenFilledButton(
            label = stringResource(BitwardenString.restart_onboarding_cta),
            onClick = onStartOnboarding,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(BitwardenString.restart_onboarding_details),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .standardHorizontalMargin(),
            style = BitwardenTheme.typography.bodySmall,
            color = BitwardenTheme.colorScheme.text.primary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        BitwardenFilledButton(
            label = stringResource(BitwardenString.restart_onboarding_carousel),
            onClick = onStartOnboardingCarousel,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(BitwardenString.restart_onboarding_carousel_details),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .standardHorizontalMargin(),
            style = BitwardenTheme.typography.bodySmall,
            color = BitwardenTheme.colorScheme.text.primary,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FeatureFlagContent_preview() {
    BitwardenTheme {
        FeatureFlagContent(
            featureFlagMap = persistentMapOf(
                FlagKey.DummyBoolean to true,
            ),
            onValueChange = { _, _ -> },
            onResetValues = { },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingOverrideContent_preview() {
    BitwardenTheme {
        OnboardingOverrideContent(
            onStartOnboarding = {},
            onStartOnboardingCarousel = {},
        )
    }
}
