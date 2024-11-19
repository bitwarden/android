package com.x8bit.bitwarden.ui.tools.feature.generator.passwordhistory

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
import com.x8bit.bitwarden.ui.platform.components.appbar.action.BitwardenOverflowActionItem
import com.x8bit.bitwarden.ui.platform.components.appbar.action.OverflowMenuItemData
import com.x8bit.bitwarden.ui.platform.components.divider.BitwardenHorizontalDivider
import com.x8bit.bitwarden.ui.platform.components.indicator.BitwardenCircularProgressIndicator
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import kotlinx.collections.immutable.persistentListOf

/**
 * Displays the password history screen
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordHistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: PasswordHistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            PasswordHistoryEvent.NavigateBack -> onNavigateBack.invoke()

            is PasswordHistoryEvent.ShowToast -> {
                Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.password_history),
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = R.drawable.ic_close),
                navigationIconContentDescription = stringResource(id = R.string.close),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(PasswordHistoryAction.CloseClick) }
                },
                actions = {
                    if (state.menuEnabled) {
                        BitwardenOverflowActionItem(
                            menuItemDataList = persistentListOf(
                                OverflowMenuItemData(
                                    text = stringResource(id = R.string.clear),
                                    onClick = remember(viewModel) {
                                        {
                                            viewModel.trySendAction(
                                                PasswordHistoryAction.PasswordClearClick,
                                            )
                                        }
                                    },
                                ),
                            ),
                        )
                    }
                },
            )
        },
        content = {
            when (val viewState = state.viewState) {
                is PasswordHistoryState.ViewState.Loading -> {
                    PasswordHistoryLoading(
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                is PasswordHistoryState.ViewState.Error -> {
                    PasswordHistoryError(
                        state = viewState,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                is PasswordHistoryState.ViewState.Empty -> {
                    PasswordHistoryEmpty(
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                is PasswordHistoryState.ViewState.Content -> {
                    PasswordHistoryContent(
                        state = viewState,
                        modifier = Modifier
                            .fillMaxSize()
                            .imePadding(),
                        onPasswordCopyClick = { password ->
                            viewModel.trySendAction(
                                PasswordHistoryAction.PasswordCopyClick(password),
                            )
                        },
                    )
                }
            }
        },
    )
}

@Composable
private fun PasswordHistoryLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BitwardenCircularProgressIndicator()
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun PasswordHistoryContent(
    state: PasswordHistoryState.ViewState.Content,
    onPasswordCopyClick: (PasswordHistoryState.GeneratedPassword) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        items(state.passwords) { password ->
            PasswordHistoryListItem(
                label = password.password,
                supportingLabel = password.date,
                onCopyClick = { onPasswordCopyClick(password) },
                modifier = Modifier
                    .testTag("GeneratedPasswordRow")
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            BitwardenHorizontalDivider(modifier = Modifier.fillMaxWidth())
        }
        item {
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun PasswordHistoryError(
    state: PasswordHistoryState.ViewState.Error,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = state.message.invoke(),
            style = BitwardenTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun PasswordHistoryEmpty(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            modifier = Modifier.testTag("NoPasswordsDisplayedLabel"),
            text = stringResource(id = R.string.no_passwords_to_list),
            style = BitwardenTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}
