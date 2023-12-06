package com.x8bit.bitwarden.ui.vault.feature.item

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.IntentHandler
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.toAnnotatedString
import com.x8bit.bitwarden.ui.platform.components.BasicDialogState
import com.x8bit.bitwarden.ui.platform.components.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.BitwardenMasterPasswordDialog
import com.x8bit.bitwarden.ui.platform.components.BitwardenOverflowActionItem
import com.x8bit.bitwarden.ui.platform.components.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.LoadingDialogState

/**
 * Displays the vault item screen.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultItemScreen(
    viewModel: VaultItemViewModel = hiltViewModel(),
    clipboardManager: ClipboardManager = LocalClipboardManager.current,
    intentHandler: IntentHandler = IntentHandler(context = LocalContext.current),
    onNavigateBack: () -> Unit,
    onNavigateToVaultEditItem: (vaultItemId: String) -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = context.resources
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is VaultItemEvent.CopyToClipboard -> {
                clipboardManager.setText(event.message.toString(resources).toAnnotatedString())
            }

            VaultItemEvent.NavigateBack -> onNavigateBack()

            is VaultItemEvent.NavigateToEdit -> onNavigateToVaultEditItem(event.itemId)

            is VaultItemEvent.NavigateToPasswordHistory -> {
                Toast.makeText(context, "Not yet implemented.", Toast.LENGTH_SHORT).show()
            }

            is VaultItemEvent.NavigateToUri -> intentHandler.launchUri(event.uri.toUri())

            is VaultItemEvent.ShowToast -> {
                Toast.makeText(context, event.message(resources), Toast.LENGTH_SHORT).show()
            }
        }
    }

    VaultItemDialogs(
        dialog = state.dialog,
        onDismissRequest = remember(viewModel) {
            { viewModel.trySendAction(VaultItemAction.DismissDialogClick) }
        },
        onSubmitMasterPassword = remember(viewModel) {
            { viewModel.trySendAction(VaultItemAction.MasterPasswordSubmit(it)) }
        },
    )

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.view_item),
                scrollBehavior = scrollBehavior,
                navigationIcon = painterResource(id = R.drawable.ic_close),
                navigationIconContentDescription = stringResource(id = R.string.close),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(VaultItemAction.CloseClick) }
                },
                actions = {
                    BitwardenOverflowActionItem()
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                onClick = remember(viewModel) {
                    { viewModel.trySendAction(VaultItemAction.EditClick) }
                },
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_edit),
                    contentDescription = stringResource(id = R.string.edit_item),
                )
            }
        },
    ) { innerPadding ->
        VaultItemContent(
            viewState = state.viewState,
            modifier = Modifier
                .imePadding()
                .fillMaxSize()
                .padding(innerPadding),
            onRefreshClick = remember(viewModel) {
                { viewModel.trySendAction(VaultItemAction.RefreshClick) }
            },
            loginHandlers = remember(viewModel) {
                LoginHandlers.create(viewModel)
            },
        )
    }
}

@Composable
private fun VaultItemDialogs(
    dialog: VaultItemState.DialogState?,
    onDismissRequest: () -> Unit,
    onSubmitMasterPassword: (String) -> Unit,
) {
    when (dialog) {
        is VaultItemState.DialogState.Generic -> BitwardenBasicDialog(
            visibilityState = BasicDialogState.Shown(
                title = null,
                message = dialog.message,
            ),
            onDismissRequest = onDismissRequest,
        )

        VaultItemState.DialogState.Loading -> BitwardenLoadingDialog(
            visibilityState = LoadingDialogState.Shown(text = R.string.loading.asText()),
        )

        VaultItemState.DialogState.MasterPasswordDialog -> {
            BitwardenMasterPasswordDialog(
                onConfirmClick = onSubmitMasterPassword,
                onDismissRequest = onDismissRequest,
            )
        }

        null -> Unit
    }
}

@Composable
private fun VaultItemContent(
    viewState: VaultItemState.ViewState,
    modifier: Modifier = Modifier,
    onRefreshClick: () -> Unit,
    loginHandlers: LoginHandlers,
) {
    when (viewState) {
        is VaultItemState.ViewState.Error -> VaultItemError(
            errorState = viewState,
            onRefreshClick = onRefreshClick,
            modifier = modifier,
        )

        is VaultItemState.ViewState.Content -> when (viewState) {
            is VaultItemState.ViewState.Content.Login -> VaultItemLoginContent(
                viewState = viewState,
                modifier = modifier,
                loginHandlers = loginHandlers,
            )
        }

        VaultItemState.ViewState.Loading -> VaultItemLoading(
            modifier = modifier,
        )
    }
}
