package com.x8bit.bitwarden.ui.auth.feature.accountsetup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Top level composable for the setup complete screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupCompleteScreen(
    viewModel: SetupCompleteViewModel = hiltViewModel(),
) {
    val setupCompleteAction: () -> Unit = remember(viewModel) {
        {
            viewModel.trySendAction(SetupCompleteAction.CompleteSetup)
        }
    }

    // Handle system back action to complete the setup.
    BackHandler(onBack = setupCompleteAction)

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    BitwardenScaffold(
        topBar = {
            BitwardenTopAppBar(
                scrollBehavior = scrollBehavior,
                title = stringResource(BitwardenString.account_setup),
                navigationIcon = null,
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
    ) {
        SetupCompleteContent(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            onContinue = setupCompleteAction,
        )
    }
}

@Composable
private fun SetupCompleteContent(
    modifier: Modifier = Modifier,
    onContinue: () -> Unit,
) {
    Column(
        modifier = modifier,
    ) {
        Spacer(Modifier.height(32.dp))
        Image(
            painter = rememberVectorPainter(BitwardenDrawable.ill_setup_complete),
            contentDescription = null,
            modifier = Modifier
                .align(CenterHorizontally)
                .standardHorizontalMargin()
                .size(size = 100.dp),
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(BitwardenString.youre_all_set),
            style = BitwardenTheme.typography.titleMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(CenterHorizontally)
                .standardHorizontalMargin(),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(BitwardenString.what_bitwarden_has_to_offer),
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(CenterHorizontally)
                .standardHorizontalMargin(),
        )
        Spacer(Modifier.height(24.dp))
        BitwardenFilledButton(
            label = stringResource(BitwardenString.continue_text),
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SetupCompleteContent_preview() {
    BitwardenTheme {
        Surface {
            SetupCompleteContent(
                modifier = Modifier.fillMaxSize(),
                onContinue = {},
            )
        }
    }
}
