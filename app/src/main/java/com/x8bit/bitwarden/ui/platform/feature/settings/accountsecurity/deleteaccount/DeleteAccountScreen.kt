package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.deleteaccount

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenFilledErrorButton
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenOutlinedErrorButton
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenMasterPasswordDialog
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Displays the delete account screen.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteAccountScreen(
    viewModel: DeleteAccountViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToDeleteAccountConfirmation: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = context.resources
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            DeleteAccountEvent.NavigateBack -> onNavigateBack()

            is DeleteAccountEvent.ShowToast -> {
                Toast.makeText(context, event.message(resources), Toast.LENGTH_SHORT).show()
            }

            DeleteAccountEvent.NavigateToDeleteAccountConfirmationScreen -> {
                onNavigateToDeleteAccountConfirmation()
            }
        }
    }

    when (val dialog = state.dialog) {
        DeleteAccountState.DeleteAccountDialog.DeleteSuccess -> BitwardenBasicDialog(
            title = null,
            message = stringResource(id = R.string.your_account_has_been_permanently_deleted),
            onDismissRequest = remember(viewModel) {
                { viewModel.trySendAction(DeleteAccountAction.AccountDeletionConfirm) }
            },
        )

        is DeleteAccountState.DeleteAccountDialog.Error -> BitwardenBasicDialog(
            title = stringResource(id = R.string.an_error_has_occurred),
            message = dialog.message(),
            onDismissRequest = remember(viewModel) {
                { viewModel.trySendAction(DeleteAccountAction.DismissDialog) }
            },
        )

        DeleteAccountState.DeleteAccountDialog.Loading -> BitwardenLoadingDialog(
            text = stringResource(id = R.string.loading),
        )

        null -> Unit
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.delete_account),
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = R.drawable.ic_close),
                navigationIconContentDescription = stringResource(id = R.string.close),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(DeleteAccountAction.CloseClick) }
                },
            )
        },
    ) {
        Column(
            modifier = Modifier
                .imePadding()
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Icon(
                painter = rememberVectorPainter(id = R.drawable.ic_warning),
                contentDescription = null,
                tint = BitwardenTheme.colorScheme.status.error,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.deleting_your_account_is_permanent),
                style = BitwardenTheme.typography.headlineSmall,
                color = BitwardenTheme.colorScheme.status.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(id = R.string.delete_account_explanation),
                style = BitwardenTheme.typography.bodyMedium,
                color = BitwardenTheme.colorScheme.text.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(24.dp))
            DeleteAccountButton(
                onDeleteAccountConfirmDialogClick = remember(viewModel) {
                    {
                        viewModel.trySendAction(
                            DeleteAccountAction.DeleteAccountConfirmDialogClick(it),
                        )
                    }
                },
                onDeleteAccountClick = remember(viewModel) {
                    { viewModel.trySendAction(DeleteAccountAction.DeleteAccountClick) }
                },
                isUnlockWithPasswordEnabled = state.isUnlockWithPasswordEnabled,
                modifier = Modifier
                    .testTag("DELETE ACCOUNT")
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            BitwardenOutlinedErrorButton(
                label = stringResource(id = R.string.cancel),
                onClick = remember(viewModel) {
                    { viewModel.trySendAction(DeleteAccountAction.CancelClick) }
                },
                modifier = Modifier
                    .testTag("CANCEL")
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun DeleteAccountButton(
    onDeleteAccountConfirmDialogClick: (masterPassword: String) -> Unit,
    onDeleteAccountClick: () -> Unit,
    isUnlockWithPasswordEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    var showPasswordDialog by remember { mutableStateOf(false) }
    if (showPasswordDialog) {
        BitwardenMasterPasswordDialog(
            onConfirmClick = {
                showPasswordDialog = false
                onDeleteAccountConfirmDialogClick(it)
            },
            onDismissRequest = { showPasswordDialog = false },
        )
    }

    BitwardenFilledErrorButton(
        label = stringResource(id = R.string.delete_account),
        onClick = {
            if (isUnlockWithPasswordEnabled) {
                showPasswordDialog = true
            } else {
                onDeleteAccountClick()
            }
        },
        modifier = modifier,
    )
}
