package com.x8bit.bitwarden.ui.auth.feature.masterpasswordhint

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * The top level composable for the Login screen.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterPasswordHintScreen(
    onNavigateBack: () -> Unit,
    viewModel: MasterPasswordHintViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            MasterPasswordHintEvent.NavigateBack -> onNavigateBack()
        }
    }

    when (val dialogState = state.dialog) {
        is MasterPasswordHintState.DialogState.PasswordHintSent -> {
            BitwardenBasicDialog(
                title = stringResource(id = R.string.password_hint),
                message = stringResource(id = R.string.password_hint_alert),
                onDismissRequest = remember(viewModel) {
                    { viewModel.trySendAction(MasterPasswordHintAction.DismissDialog) }
                },
            )
        }

        is MasterPasswordHintState.DialogState.Loading -> {
            BitwardenLoadingDialog(text = dialogState.message())
        }

        is MasterPasswordHintState.DialogState.Error -> {
            BitwardenBasicDialog(
                title = dialogState
                    .title
                    ?.invoke()
                    ?: stringResource(id = R.string.an_error_has_occurred),
                message = dialogState.message(),
                onDismissRequest = remember(viewModel) {
                    { viewModel.trySendAction(MasterPasswordHintAction.DismissDialog) }
                },
            )
        }

        null -> Unit
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.password_hint),
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = R.drawable.ic_close),
                navigationIconContentDescription = stringResource(id = R.string.close),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(MasterPasswordHintAction.CloseClick) }
                },
                actions = {
                    BitwardenTextButton(
                        label = stringResource(id = R.string.submit),
                        onClick = remember(viewModel) {
                            { viewModel.trySendAction(MasterPasswordHintAction.SubmitClick) }
                        },
                        modifier = Modifier.testTag("SubmitButton"),
                    )
                },
            )
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            BitwardenTextField(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                value = state.emailInput,
                onValueChange = remember(viewModel) {
                    { viewModel.trySendAction(MasterPasswordHintAction.EmailInputChange(it)) }
                },
                label = stringResource(id = R.string.email_address),
                keyboardType = KeyboardType.Email,
            )

            Text(
                text = stringResource(id = R.string.enter_email_for_hint),
                style = BitwardenTheme.typography.bodySmall,
                color = BitwardenTheme.colorScheme.text.secondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = 4.dp,
                        horizontal = 16.dp,
                    ),
            )
        }
    }
}
