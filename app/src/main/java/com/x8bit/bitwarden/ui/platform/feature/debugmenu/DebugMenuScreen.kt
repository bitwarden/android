package com.x8bit.bitwarden.ui.platform.feature.debugmenu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.appbar.NavigationIcon
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.x8bit.bitwarden.ui.platform.components.divider.BitwardenHorizontalDivider
import com.x8bit.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.feature.debugmenu.components.ListItemContent
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

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
                title = stringResource(R.string.debug_menu),
                scrollBehavior = scrollBehavior,
                navigationIcon = NavigationIcon(
                    navigationIcon = rememberVectorPainter(R.drawable.ic_back),
                    navigationIconContentDescription = stringResource(id = R.string.back),
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
            Spacer(modifier = Modifier.height(16.dp))
            FeatureFlagContent(
                featureFlagMap = state.featureFlags,
                onValueChange = remember(viewModel) {
                    { key, value ->
                        viewModel.trySendAction(DebugMenuAction.UpdateFeatureFlag(key, value))
                    }
                },
                onResetValues = remember(viewModel) {
                    {
                        viewModel.trySendAction(DebugMenuAction.ResetFeatureFlagValues)
                    }
                },
            )
            Spacer(Modifier.height(12.dp))
            // Pulled these into variable to avoid over-nested formatting in the composable call.
            val isRestartOnboardingEnabled = state.featureFlags[FlagKey.OnboardingFlow] as? Boolean
            val isRestartOnboardingCarouselEnabled = state
                .featureFlags[FlagKey.OnboardingCarousel] as? Boolean
            OnboardingOverrideContent(
                isRestartOnboardingEnabled = isRestartOnboardingEnabled == true,
                onStartOnboarding = remember(viewModel) {
                    {
                        viewModel.trySendAction(DebugMenuAction.RestartOnboarding)
                    }
                },
                isCarouselOverrideEnabled = isRestartOnboardingCarouselEnabled == true,
                onStartOnboardingCarousel = remember(viewModel) {
                    {
                        viewModel.trySendAction(DebugMenuAction.RestartOnboardingCarousel)
                    }
                },
            )
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun FeatureFlagContent(
    featureFlagMap: Map<FlagKey<Any>, Any>,
    onValueChange: (key: FlagKey<Any>, value: Any) -> Unit,
    onResetValues: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenListHeaderText(
            label = stringResource(R.string.feature_flags),
            modifier = Modifier.standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenHorizontalDivider()
        featureFlagMap.forEach { featureFlag ->
            featureFlag.key.ListItemContent(
                currentValue = featureFlag.value,
                onValueChange = onValueChange,
                modifier = Modifier.standardHorizontalMargin(),
            )
            BitwardenHorizontalDivider()
        }
        Spacer(modifier = Modifier.height(12.dp))
        BitwardenFilledButton(
            label = stringResource(R.string.reset_values),
            onClick = onResetValues,
            modifier = Modifier
                .standardHorizontalMargin()
                .fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * The content for the onboarding override feature flag.
 */
@Composable
private fun OnboardingOverrideContent(
    isRestartOnboardingEnabled: Boolean,
    onStartOnboarding: () -> Unit,
    isCarouselOverrideEnabled: Boolean,
    onStartOnboardingCarousel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        BitwardenListHeaderText(
            label = stringResource(R.string.onboarding_override),
            modifier = Modifier.standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenHorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))
        BitwardenFilledButton(
            label = stringResource(R.string.restart_onboarding_cta),
            onClick = onStartOnboarding,
            isEnabled = isRestartOnboardingEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.restart_onboarding_details),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .standardHorizontalMargin(),
            style = BitwardenTheme.typography.bodySmall,
            color = BitwardenTheme.colorScheme.text.primary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        BitwardenFilledButton(
            label = stringResource(R.string.restart_onboarding_carousel),
            onClick = onStartOnboardingCarousel,
            isEnabled = isCarouselOverrideEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.restart_onboarding_carousel_details),
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
            featureFlagMap = mapOf(
                FlagKey.EmailVerification to true,
                FlagKey.OnboardingCarousel to true,
                FlagKey.OnboardingFlow to false,
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
            isRestartOnboardingEnabled = true,
            onStartOnboardingCarousel = {},
            isCarouselOverrideEnabled = true,
        )
    }
}
