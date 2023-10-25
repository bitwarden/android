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
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.components.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.BitwardenFilledButton
import com.x8bit.bitwarden.ui.platform.components.BitwardenSwitch
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextButton
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextField

/**
 * The top level composable for the Landing screen.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
@Suppress("LongMethod")
fun LandingScreen(
    onNavigateToCreateAccount: () -> Unit,
    onNavigateToLogin: (emailAddress: String) -> Unit,
    viewModel: LandingViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            LandingEvent.NavigateToCreateAccount -> onNavigateToCreateAccount()
            is LandingEvent.NavigateToLogin -> onNavigateToLogin(
                event.emailAddress,
            )
        }
    }

    BitwardenBasicDialog(
        visibilityState = state.errorDialogState,
        onDismissRequest = remember(viewModel) {
            { viewModel.trySendAction(LandingAction.ErrorDialogDismiss) }
        },
    )

    val scrollState = rememberScrollState()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .semantics { testTagsAsResourceId = true }
            .background(MaterialTheme.colorScheme.surface)
            .safeDrawingPadding()
            .fillMaxHeight()
            .verticalScroll(scrollState),
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Image(
            painter = painterResource(id = R.drawable.logo),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
            contentDescription = null,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .width(220.dp)
                .height(74.dp)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = stringResource(id = R.string.login_or_create_new_account),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(
                    horizontal = 24.dp,
                    vertical = 8.dp,
                )
                .wrapContentHeight(),
        )

        Spacer(modifier = Modifier.weight(1f))

        Spacer(modifier = Modifier.height(32.dp))

        BitwardenTextField(
            modifier = Modifier
                .semantics { testTag = "EmailAddressEntry" }
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            value = state.emailInput,
            onValueChange = remember(viewModel) {
                { viewModel.trySendAction(LandingAction.EmailInputChanged(it)) }
            },
            label = stringResource(id = R.string.email_address),
        )

        Spacer(modifier = Modifier.height(10.dp))

        EnvironmentSelector(
            selectedOption = state.selectedEnvironment.type,
            onOptionSelected = remember(viewModel) {
                { viewModel.trySendAction(LandingAction.EnvironmentTypeSelect(it)) }
            },
            modifier = Modifier
                .semantics { testTag = "RegionSelectorDropdown" }
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        BitwardenSwitch(
            label = stringResource(id = R.string.remember_me),
            isChecked = state.isRememberMeEnabled,
            onCheckedChange = remember(viewModel) {
                { viewModel.trySendAction(LandingAction.RememberMeToggle(it)) }
            },
            modifier = Modifier
                .semantics { testTag = "RememberMeSwitch" }
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(32.dp))

        BitwardenFilledButton(
            label = stringResource(id = R.string.continue_text),
            onClick = remember(viewModel) {
                { viewModel.trySendAction(LandingAction.ContinueButtonClick) }
            },
            isEnabled = state.isContinueButtonEnabled,
            modifier = Modifier
                .semantics { testTag = "ContinueButton" }
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 16.dp)
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
                modifier = Modifier
                    .semantics { testTag = "CreateAccountLabel" },
            )
        }

        Spacer(modifier = Modifier.height(58.dp))
    }
}

/**
 * A dropdown selector UI component specific to region url selection on the Landing screen.
 *
 * This composable displays a dropdown menu allowing users to select a region
 * from a list of options. When an option is selected, it invokes the provided callback
 * and displays the currently selected region on the UI.
 *
 * @param selectedOption The currently selected environment option.
 * @param onOptionSelected A callback that gets invoked when an environment option is selected
 * and passes the selected option as an argument.
 * @param modifier A [Modifier] for the composable.
 *
 */
@Composable
private fun EnvironmentSelector(
    selectedOption: Environment.Type,
    onOptionSelected: (Environment.Type) -> Unit,
    modifier: Modifier,
) {
    val options = Environment.Type.values()
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp),
            )
            Text(
                text = selectedOption.label(),
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
                    text = { Text(text = optionString.label()) },
                    onClick = {
                        expanded = false
                        onOptionSelected(optionString)
                    },
                )
            }
        }
    }
}
