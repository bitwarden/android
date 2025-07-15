package com.bitwarden.authenticator.ui.platform.feature.debugmenu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.data.platform.manager.model.FlagKey
import com.bitwarden.authenticator.ui.platform.components.appbar.AuthenticatorTopAppBar
import com.bitwarden.authenticator.ui.platform.components.button.AuthenticatorFilledButton
import com.bitwarden.authenticator.ui.platform.components.header.BitwardenListHeaderText
import com.bitwarden.authenticator.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.authenticator.ui.platform.feature.debugmenu.components.ListItemContent
import com.bitwarden.authenticator.ui.platform.theme.AuthenticatorTheme
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.appbar.NavigationIcon
import com.bitwarden.ui.platform.components.divider.BitwardenHorizontalDivider
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable

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
            AuthenticatorTopAppBar(
                title = stringResource(R.string.debug_menu),
                scrollBehavior = scrollBehavior,
                navigationIcon = NavigationIcon(
                    navigationIcon = rememberVectorPainter(BitwardenDrawable.ic_back),
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
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(innerPadding),
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
        BitwardenHorizontalDivider(
            color = MaterialTheme.colorScheme.outline,
            thickness = 1.dp,
        )
        featureFlagMap.forEach { featureFlag ->
            featureFlag.key.ListItemContent(
                currentValue = featureFlag.value,
                onValueChange = onValueChange,
                modifier = Modifier.standardHorizontalMargin(),
            )
            BitwardenHorizontalDivider(
                color = MaterialTheme.colorScheme.outline,
                thickness = 1.dp,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        AuthenticatorFilledButton(
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
    AuthenticatorTheme {
        FeatureFlagContent(
            featureFlagMap = mapOf(
                FlagKey.BitwardenAuthenticationEnabled to true,
            ),
            onValueChange = { _, _ -> },
            onResetValues = { },
        )
    }
}
