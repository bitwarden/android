package com.x8bit.bitwarden.ui.vault.feature.edit

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.components.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextButton
import com.x8bit.bitwarden.ui.platform.components.BitwardenTopAppBar

/**
 * Top level composable for the vault edit item screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultEditItemScreen(
    onNavigateBack: () -> Unit,
    viewModel: VaultEditItemViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = context.resources
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            VaultEditItemEvent.NavigateBack -> onNavigateBack()

            is VaultEditItemEvent.ShowToast -> {
                Toast.makeText(context, event.message(resources), Toast.LENGTH_SHORT).show()
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .imePadding()
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.edit_item),
                navigationIcon = painterResource(id = R.drawable.ic_close),
                navigationIconContentDescription = stringResource(id = R.string.close),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(VaultEditItemAction.CloseClick) }
                },
                scrollBehavior = scrollBehavior,
                actions = {
                    BitwardenTextButton(
                        label = stringResource(id = R.string.save),
                        onClick = remember(viewModel) {
                            { viewModel.trySendAction(VaultEditItemAction.SaveClick) }
                        },
                    )
                },
            )
        },
    ) { innerPadding -> }
}
