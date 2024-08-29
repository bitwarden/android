package com.x8bit.bitwarden.ui.platform.feature.debugmenu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
import com.x8bit.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.feature.debugmenu.components.ListItemContent
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Top level screen for the debug menu.
 */
@OptIn(ExperimentalMaterial3Api::class)
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

    BitwardenScaffold(
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(R.string.debug_menu),
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
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
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding),
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            FeatureFlagContent(
                featureFlagMap = state.featureFlags,
                onValueChange = { key, value ->
                    viewModel.trySendAction(DebugMenuAction.UpdateFeatureFlag(key, value))
                },
                onResetValues = {
                    viewModel.trySendAction(DebugMenuAction.ResetFeatureFlagValues)
                },
            )
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
        HorizontalDivider()
        featureFlagMap.forEach { featureFlag ->
            featureFlag.key.ListItemContent(
                currentValue = featureFlag.value,
                onValueChange = onValueChange,
                modifier = Modifier.standardHorizontalMargin(),
            )
            HorizontalDivider()
        }
        Spacer(modifier = Modifier.height(16.dp))
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
