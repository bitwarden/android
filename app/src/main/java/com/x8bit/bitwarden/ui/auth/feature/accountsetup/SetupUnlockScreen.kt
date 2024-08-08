package com.x8bit.bitwarden.ui.auth.feature.accountsetup

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.handlers.SetupUnlockHandler
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.toggle.BitwardenUnlockWithBiometricsSwitch
import com.x8bit.bitwarden.ui.platform.components.toggle.BitwardenUnlockWithPinSwitch
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.composition.LocalBiometricsManager
import com.x8bit.bitwarden.ui.platform.manager.biometrics.BiometricsManager
import com.x8bit.bitwarden.ui.platform.util.isPortrait

/**
 * Top level composable for the setup unlock screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupUnlockScreen(
    onNavigateToSetupAutofill: () -> Unit,
    viewModel: SetupUnlockViewModel = hiltViewModel(),
    biometricsManager: BiometricsManager = LocalBiometricsManager.current,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val handler = remember(viewModel) { SetupUnlockHandler.create(viewModel = viewModel) }
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            SetupUnlockEvent.NavigateToSetupAutofill -> onNavigateToSetupAutofill()
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.account_setup),
                scrollBehavior = scrollBehavior,
                navigationIcon = null,
            )
        },
    ) { innerPadding ->
        SetupUnlockScreenContent(
            state = state,
            handler = handler,
            biometricsManager = biometricsManager,
            modifier = Modifier
                .padding(paddingValues = innerPadding)
                .fillMaxSize(),
        )
    }
}

@Composable
private fun SetupUnlockScreenContent(
    state: SetupUnlockState,
    handler: SetupUnlockHandler,
    modifier: Modifier = Modifier,
    biometricsManager: BiometricsManager,
    config: Configuration = LocalConfiguration.current,
) {
    val marginHorizontal = remember(config.orientation) {
        if (config.isPortrait) 16.dp else 48.dp
    }
    Column(
        modifier = modifier.verticalScroll(state = rememberScrollState()),
    ) {
        if (config.isPortrait) {
            SetupUnlockHeaderPortrait()
        } else {
            SetupUnlockHeaderLandscape()
        }

        Spacer(modifier = Modifier.height(height = 24.dp))
        BitwardenUnlockWithBiometricsSwitch(
            isBiometricsSupported = biometricsManager.isBiometricsSupported,
            isChecked = state.isUnlockWithBiometricsEnabled,
            onDisableBiometrics = handler.onDisableBiometrics,
            onEnableBiometrics = handler.onEnableBiometrics,
            modifier = Modifier
                .testTag(tag = "UnlockWithBiometricsSwitch")
                .fillMaxWidth()
                .padding(horizontal = marginHorizontal),
        )
        BitwardenUnlockWithPinSwitch(
            isUnlockWithPasswordEnabled = state.isUnlockWithPasswordEnabled,
            isUnlockWithPinEnabled = state.isUnlockWithPinEnabled,
            onUnlockWithPinToggleAction = handler.onUnlockWithPinToggle,
            modifier = Modifier
                .testTag(tag = "UnlockWithPinSwitch")
                .fillMaxWidth()
                .padding(horizontal = marginHorizontal),
        )

        Spacer(modifier = Modifier.height(height = 24.dp))
        BitwardenFilledButton(
            label = stringResource(id = R.string.continue_text),
            onClick = handler.onContinueClick,
            isEnabled = state.isContinueButtonEnabled,
            modifier = Modifier
                .testTag(tag = "ContinueButton")
                .fillMaxWidth()
                .padding(horizontal = marginHorizontal),
        )

        Spacer(modifier = Modifier.height(height = 12.dp))
        SetUpLaterButton(
            onConfirmClick = handler.onSetUpLaterClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = marginHorizontal),
        )

        Spacer(modifier = Modifier.height(height = 12.dp))
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun SetUpLaterButton(
    modifier: Modifier,
    onConfirmClick: () -> Unit,
) {
    var displayConfirmation by rememberSaveable { mutableStateOf(value = false) }
    if (displayConfirmation) {
        @Suppress("MaxLineLength")
        BitwardenTwoButtonDialog(
            title = stringResource(id = R.string.set_up_unlock_later),
            message = stringResource(
                id = R.string.you_can_return_to_complete_this_step_anytime_from_account_security_in_settings,
            ),
            confirmButtonText = stringResource(id = R.string.confirm),
            dismissButtonText = stringResource(id = R.string.cancel),
            onConfirmClick = {
                onConfirmClick()
                displayConfirmation = false
            },
            onDismissClick = { displayConfirmation = false },
            onDismissRequest = { displayConfirmation = false },
        )
    }

    BitwardenTextButton(
        label = stringResource(id = R.string.set_up_later),
        onClick = { displayConfirmation = true },
        modifier = modifier.testTag(tag = "SetUpLaterButton"),
    )
}

@Composable
private fun ColumnScope.SetupUnlockHeaderPortrait() {
    Spacer(modifier = Modifier.height(height = 32.dp))
    Image(
        painter = rememberVectorPainter(id = R.drawable.account_setup),
        contentDescription = null,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .size(size = 100.dp)
            .align(alignment = Alignment.CenterHorizontally),
    )

    Spacer(modifier = Modifier.height(height = 24.dp))
    Text(
        text = stringResource(id = R.string.set_up_unlock),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    )

    Spacer(modifier = Modifier.height(height = 8.dp))
    @Suppress("MaxLineLength")
    Text(
        text = stringResource(
            id = R.string.set_up_biometrics_or_choose_a_pin_code_to_quickly_access_your_vault_and_autofill_your_logins,
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    )
}

@Composable
private fun SetupUnlockHeaderLandscape(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(horizontal = 112.dp)
            .padding(horizontal = 48.dp),
    ) {
        Image(
            painter = rememberVectorPainter(id = R.drawable.account_setup),
            contentDescription = null,
            modifier = Modifier
                .size(size = 100.dp)
                .align(alignment = Alignment.CenterVertically),
        )

        Spacer(modifier = Modifier.width(width = 24.dp))
        Column(
            modifier = Modifier.align(alignment = Alignment.CenterVertically),
        ) {
            Text(
                text = stringResource(id = R.string.set_up_unlock),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(height = 8.dp))
            @Suppress("MaxLineLength")
            Text(
                text = stringResource(
                    id = R.string.set_up_biometrics_or_choose_a_pin_code_to_quickly_access_your_vault_and_autofill_your_logins,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
