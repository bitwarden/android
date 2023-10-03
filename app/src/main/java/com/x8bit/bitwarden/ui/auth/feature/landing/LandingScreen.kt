package com.x8bit.bitwarden.ui.auth.feature.landing

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.components.BitwardenFilledButton
import com.x8bit.bitwarden.ui.platform.components.BitwardenSwitch
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextButton
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * The top level composable for the Landing screen.
 */
@Composable
@Suppress("LongMethod")
fun LandingScreen(
    onNavigateToCreateAccount: () -> Unit,
    onNavigateToLogin: (emailAddress: String, regionLabel: String) -> Unit,
    viewModel: LandingViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            LandingEvent.NavigateToCreateAccount -> onNavigateToCreateAccount()
            is LandingEvent.NavigateToLogin -> onNavigateToLogin(
                event.emailAddress,
                event.regionLabel,
            )
        }
    }

    val scrollState = rememberScrollState()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .fillMaxHeight()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState),
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
            contentDescription = null,
            modifier = Modifier
                .padding(top = 40.dp, bottom = 8.dp)
                .width(220.dp)
                .height(74.dp)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = stringResource(id = R.string.login_or_create_new_account),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(8.dp)
                .wrapContentHeight(),
        )

        Spacer(modifier = Modifier.weight(1f))

        BitwardenTextField(
            modifier = Modifier
                .padding(
                    top = 32.dp,
                    bottom = 10.dp,
                )
                .fillMaxWidth(),
            value = state.emailInput,
            onValueChange = remember(viewModel) {
                { viewModel.trySendAction(LandingAction.EmailInputChanged(it)) }
            },
            label = stringResource(id = R.string.email_address),
        )

        RegionSelector(
            selectedOption = state.selectedRegion,
            options = LandingState.RegionOption.values().toList(),
            onOptionSelected = remember(viewModel) {
                { viewModel.trySendAction(LandingAction.RegionOptionSelect(it)) }
            },
        )

        BitwardenSwitch(
            label = stringResource(id = R.string.remember_me),
            isChecked = state.isRememberMeEnabled,
            onCheckedChange = remember(viewModel) {
                { viewModel.trySendAction(LandingAction.RememberMeToggle(it)) }
            },
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth(),
        )

        BitwardenFilledButton(
            label = stringResource(id = R.string.continue_text),
            onClick = remember(viewModel) {
                { viewModel.trySendAction(LandingAction.ContinueButtonClick) }
            },
            isEnabled = state.isContinueButtonEnabled,
            modifier = Modifier
                .padding(top = 32.dp)
                .fillMaxWidth(),
        )

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(
                    top = 8.dp,
                    bottom = 58.dp,
                )
                .fillMaxWidth()
                .wrapContentHeight(),
        ) {
            Text(
                text = stringResource(id = R.string.new_around_here),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            BitwardenTextButton(
                label = stringResource(id = R.string.create_account),
                onClick = remember(viewModel) {
                    { viewModel.trySendAction(LandingAction.CreateAccountClick) }
                },
            )
        }
    }
}

/**
 * A dropdown selector UI component specific to region url selection on the Landing screen.
 *
 * This composable displays a dropdown menu allowing users to select a region
 * from a list of options. When an option is selected, it invokes the provided callback
 * and displays the currently selected region on the UI.
 *
 * @param selectedOption The currently selected region option.
 * @param options A list of region options available for selection.
 * @param onOptionSelected A callback that gets invoked when a region option is selected
 * and passes the selected option as an argument.
 *
 */
@Composable
private fun RegionSelector(
    selectedOption: LandingState.RegionOption,
    options: List<LandingState.RegionOption>,
    onOptionSelected: (LandingState.RegionOption) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .fillMaxWidth()
                .padding(start = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(id = R.string.logging_in_on),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(end = 12.dp),
            )
            Text(
                text = selectedOption.label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 8.dp),
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_region_select_dropdown),
                contentDescription = stringResource(id = R.string.region),
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { optionString ->
                DropdownMenuItem(
                    text = { Text(text = optionString.label) },
                    onClick = {
                        expanded = false
                        onOptionSelected(optionString)
                    },
                )
            }
        }
    }
}

@Preview
@Composable
private fun LandingScreen_preview() {
    BitwardenTheme {
        LandingScreen(
            onNavigateToCreateAccount = {},
            onNavigateToLogin = { _, _ -> },
            viewModel = LandingViewModel(SavedStateHandle()),
        )
    }
}
