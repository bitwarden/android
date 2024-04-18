package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.deleteaccountconfirmation

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.dialog.BasicDialogState
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.LoadingDialogState
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold

/**
 * Displays the delete account confirmation screen.
 */
@Composable
fun DeleteAccountConfirmationScreen(
    viewModel: DeleteAccountConfirmationViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsState()
    val context = LocalContext.current
    val resources = context.resources
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            DeleteAccountConfirmationEvent.NavigateBack -> onNavigateBack()

            is DeleteAccountConfirmationEvent.ShowToast -> {
                Toast.makeText(context, event.message(resources), Toast.LENGTH_SHORT).show()
            }
        }
    }

    DeleteAccountConfirmationDialogs(
        dialogState = state.dialog,
        onDeleteAccountAcknowledge = remember(viewModel) {
            { viewModel.trySendAction(DeleteAccountConfirmationAction.DeleteAccountAcknowledge) }
        },
        onDismissDialog = remember(viewModel) {
            { viewModel.trySendAction(DeleteAccountConfirmationAction.DismissDialog) }
        },
    )

    DeleteAccountConfirmationScaffold(
        state = state,
        onCloseClick = remember(viewModel) {
            { viewModel.trySendAction(DeleteAccountConfirmationAction.CloseClick) }
        },
    )
}

@Composable
private fun DeleteAccountConfirmationDialogs(
    dialogState: DeleteAccountConfirmationState.DeleteAccountConfirmationDialog?,
    onDismissDialog: () -> Unit,
    onDeleteAccountAcknowledge: () -> Unit,
) {
    when (dialogState) {
        is DeleteAccountConfirmationState.DeleteAccountConfirmationDialog.DeleteSuccess -> {
            BitwardenBasicDialog(
                visibilityState = BasicDialogState.Shown(
                    title = null,
                    message = dialogState.message,
                ),
                onDismissRequest = onDeleteAccountAcknowledge,
            )
        }

        is DeleteAccountConfirmationState.DeleteAccountConfirmationDialog.Error -> {
            BitwardenBasicDialog(
                visibilityState = BasicDialogState.Shown(
                    title = dialogState.title,
                    message = dialogState.message,
                ),
                onDismissRequest = onDismissDialog,
            )
        }

        is DeleteAccountConfirmationState.DeleteAccountConfirmationDialog.Loading -> {
            BitwardenLoadingDialog(
                visibilityState = LoadingDialogState.Shown(dialogState.title),
            )
        }
        null -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteAccountConfirmationScaffold(
    state: DeleteAccountConfirmationState,
    onCloseClick: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.verification_code),
                scrollBehavior = scrollBehavior,
                navigationIcon = painterResource(id = R.drawable.ic_close),
                navigationIconContentDescription = stringResource(id = R.string.close),
                onNavigationIconClick = onCloseClick,
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // TODO finish UI in BIT-2234
        }
    }
}
