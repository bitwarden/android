package com.x8bit.bitwarden.ui.platform.feature.accessibilitydisclosure

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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.bitwarden.ui.platform.components.button.BitwardenOutlinedButton
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.composition.LocalExitManager
import com.bitwarden.ui.platform.manager.exit.ExitManager
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Top-level composable for the Accessibility Disclosure screen.
 */
@Composable
fun AccessibilityDisclosureScreen(
    onDismiss: () -> Unit,
    viewModel: AccessibilityDisclosureViewModel = hiltViewModel(),
    exitManager: ExitManager = LocalExitManager.current,
) {
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is AccessibilityDisclosureEvent.Dismiss -> onDismiss()
            is AccessibilityDisclosureEvent.CloseApp -> exitManager.exitApplication()
        }
    }

    BackHandler { viewModel.trySendAction(AccessibilityDisclosureAction.CloseAppClick) }
    BitwardenScaffold(
        contentWindowInsets = ScaffoldDefaults
            .contentWindowInsets
            .union(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
    ) {
        AccessibilityDisclosureContent(
            onAcceptClick = {
                viewModel.trySendAction(AccessibilityDisclosureAction.AcceptClicked)
            },
            onCloseAppClick = {
                viewModel.trySendAction(AccessibilityDisclosureAction.CloseAppClick)
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun AccessibilityDisclosureContent(
    onAcceptClick: () -> Unit,
    onCloseAppClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(state = rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(height = 32.dp))

        Image(
            painter = rememberVectorPainter(id = BitwardenDrawable.ill_autofill),
            contentDescription = null,
            contentScale = ContentScale.FillHeight,
            modifier = Modifier
                .standardHorizontalMargin()
                .size(size = 100.dp)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(height = 24.dp))

        Text(
            text = stringResource(id = BitwardenString.accessibility_service_disclosure),
            style = BitwardenTheme.typography.headlineSmall,
            color = BitwardenTheme.colorScheme.text.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )

        Spacer(modifier = Modifier.height(height = 12.dp))

        Text(
            text = stringResource(id = BitwardenString.accessibility_disclosure_start_up_text),
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )

        Spacer(modifier = Modifier.height(height = 24.dp))

        BitwardenFilledButton(
            label = stringResource(id = BitwardenString.accept),
            onClick = onAcceptClick,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )

        Spacer(modifier = Modifier.height(height = 12.dp))

        BitwardenOutlinedButton(
            label = stringResource(id = BitwardenString.close_app),
            onClick = onCloseAppClick,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )

        Spacer(modifier = Modifier.height(height = 16.dp))
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Preview(showBackground = true)
@Composable
private fun AccessibilityDisclosureContent_preview() {
    BitwardenTheme {
        AccessibilityDisclosureContent(
            onAcceptClick = {},
            onCloseAppClick = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
