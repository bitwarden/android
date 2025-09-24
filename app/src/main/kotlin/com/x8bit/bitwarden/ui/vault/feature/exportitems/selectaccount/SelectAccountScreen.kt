package com.x8bit.bitwarden.ui.vault.feature.exportitems.selectaccount

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.credentials.providerevents.exception.ImportCredentialsUnknownErrorException
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.cxf.manager.CredentialExchangeCompletionManager
import com.bitwarden.cxf.manager.model.ExportCredentialsResult
import com.bitwarden.cxf.ui.composition.LocalCredentialExchangeCompletionManager
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.base.util.toListItemCardStyle
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.vault.feature.exportitems.component.AccountSummaryListItem
import com.x8bit.bitwarden.ui.vault.feature.exportitems.component.ExportItemsScaffold
import com.x8bit.bitwarden.ui.vault.feature.exportitems.model.AccountSelectionListItem
import com.x8bit.bitwarden.ui.vault.feature.exportitems.selectaccount.handlers.rememberSelectAccountHandlers
import kotlinx.collections.immutable.persistentListOf

/**
 * Top level screen for selecting an account to export items from.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectAccountScreen(
    onAccountSelected: (userId: String) -> Unit,
    viewModel: SelectAccountViewModel = hiltViewModel(),
    credentialExchangeCompletionManager: CredentialExchangeCompletionManager =
        LocalCredentialExchangeCompletionManager.current,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val handlers = rememberSelectAccountHandlers(viewModel)
    EventsEffect(viewModel) { event ->
        when (event) {
            SelectAccountEvent.CancelExport -> {
                credentialExchangeCompletionManager
                    .completeCredentialExport(
                        exportResult = ExportCredentialsResult.Failure(
                            // TODO: [PM-26094] Use ImportCredentialsCancellationException once
                            //  public.
                            error = ImportCredentialsUnknownErrorException(
                                errorMessage = "User cancelled import.",
                            ),
                        ),
                    )
            }

            is SelectAccountEvent.NavigateToPasswordVerification -> {
                onAccountSelected(event.userId)
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    ExportItemsScaffold(
        navIcon = rememberVectorPainter(BitwardenDrawable.ic_close),
        navigationIconContentDescription = stringResource(BitwardenString.close),
        onNavigationIconClick = handlers.onCloseClick,
        scrollBehavior = scrollBehavior,
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
    ) {
        SelectAccountContent(
            state = state,
            onAccountClick = handlers.onAccountClick,
            modifier = Modifier
                .fillMaxSize()
                .standardHorizontalMargin(),
        )
    }
}

@Composable
private fun SelectAccountContent(
    state: SelectAccountState,
    onAccountClick: (userId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        item { Spacer(Modifier.height(24.dp)) }

        item {
            Text(
                text = stringResource(BitwardenString.select_account),
                textAlign = TextAlign.Center,
                style = BitwardenTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item { Spacer(Modifier.height(16.dp)) }

        itemsIndexed(
            items = state.accountSelectionListItems,
            key = { _, item -> "AccountSummaryItem_${item.userId}" },
        ) { index, item ->
            AccountSummaryListItem(
                item = item,
                cardStyle = state.accountSelectionListItems.toListItemCardStyle(index),
                clickable = true,
                onClick = onAccountClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem(),
            )
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Preview(showBackground = true)
@Composable
private fun SelectAccountContentPreview() {
    val state = SelectAccountState(
        accountSelectionListItems = persistentListOf(
            AccountSelectionListItem(
                userId = "1",
                email = "john.doe@example.com",
                initials = "JD",
                avatarColorHex = "#FFFF0000",
                isItemRestricted = false,
            ),
            AccountSelectionListItem(
                userId = "2",
                email = "jane.smith@example.com",
                initials = "JS",
                avatarColorHex = "#FF00FF00",
                isItemRestricted = true,
            ),
            AccountSelectionListItem(
                userId = "3",
                email = "another.user@example.com",
                initials = "AU",
                avatarColorHex = "#FF0000FF",
                isItemRestricted = false,
            ),
        ),
    )
    BitwardenScaffold {
        SelectAccountContent(
            state = state,
            onAccountClick = { },
        )
    }
}
