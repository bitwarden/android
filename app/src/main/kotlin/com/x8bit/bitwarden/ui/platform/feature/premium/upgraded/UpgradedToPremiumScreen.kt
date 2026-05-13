package com.x8bit.bitwarden.ui.platform.feature.premium.upgraded

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.bitwarden.ui.platform.components.button.BitwardenOutlinedButton
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.composition.LocalIntentManager
import com.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Top-level composable for the "Upgraded to Premium" screen.
 */
@Composable
fun UpgradedToPremiumScreen(
    onDismiss: () -> Unit,
    viewModel: UpgradedToPremiumViewModel = hiltViewModel(),
    intentManager: IntentManager = LocalIntentManager.current,
) {
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            UpgradedToPremiumEvent.NavigateBack -> onDismiss()
            is UpgradedToPremiumEvent.NavigateToUrl -> {
                intentManager.launchUri(event.url.toUri())
            }
        }
    }

    // Route system back through the close handler so the acknowledgment is consumed before
    // the destination pops. A default Navigation Compose pop would leave pending=true on disk
    // and the action card would still surface on the next root-nav visit.
    BackHandler {
        viewModel.trySendAction(UpgradedToPremiumAction.CloseClick)
    }

    BitwardenScaffold(
        contentWindowInsets = ScaffoldDefaults
            .contentWindowInsets
            .union(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
    ) {
        UpgradedToPremiumContent(
            onLearnMoreClick = {
                viewModel.trySendAction(UpgradedToPremiumAction.LearnMoreClick)
            },
            onCloseClick = {
                viewModel.trySendAction(UpgradedToPremiumAction.CloseClick)
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun UpgradedToPremiumContent(
    onLearnMoreClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Image(
            painter = rememberVectorPainter(id = BitwardenDrawable.ill_celebration),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .standardHorizontalMargin()
                .size(100.dp),
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(id = BitwardenString.upgraded_to_premium),
            style = BitwardenTheme.typography.titleLarge,
            color = BitwardenTheme.colorScheme.text.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(
                id = BitwardenString
                    .you_now_have_access_to_advanced_security_features_learn_more_about_premium,
            ),
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )

        Spacer(modifier = Modifier.height(32.dp))

        BitwardenFilledButton(
            label = stringResource(id = BitwardenString.learn_more),
            onClick = onLearnMoreClick,
            icon = rememberVectorPainter(id = BitwardenDrawable.ic_external_link),
            isExternalLink = true,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        BitwardenOutlinedButton(
            label = stringResource(id = BitwardenString.close),
            onClick = onCloseClick,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )

        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Preview(showBackground = true)
@Composable
private fun UpgradedToPremiumContent_preview() {
    BitwardenTheme {
        BitwardenScaffold {
            UpgradedToPremiumContent(
                onLearnMoreClick = {},
                onCloseClick = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UpgradedToPremiumContent_darkPreview() {
    BitwardenTheme(theme = AppTheme.DARK) {
        BitwardenScaffold {
            UpgradedToPremiumContent(
                onLearnMoreClick = {},
                onCloseClick = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
