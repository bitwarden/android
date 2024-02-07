package com.x8bit.bitwarden.ui.auth.feature.enterprisesignon

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.BasicDialogState
import com.x8bit.bitwarden.ui.platform.components.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextButton
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.LoadingDialogState
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.theme.LocalIntentManager

/**
 * The top level composable for the Enterprise Single Sign On screen.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterpriseSignOnScreen(
    onNavigateBack: () -> Unit,
    onNavigateToTwoFactorLogin: (String) -> Unit,
    intentManager: IntentManager = LocalIntentManager.current,
    viewModel: EnterpriseSignOnViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            EnterpriseSignOnEvent.NavigateBack -> onNavigateBack()

            is EnterpriseSignOnEvent.NavigateToSsoLogin -> {
                intentManager.startCustomTabsActivity(event.uri)
            }

            is EnterpriseSignOnEvent.NavigateToCaptcha -> {
                intentManager.startCustomTabsActivity(event.uri)
            }

            is EnterpriseSignOnEvent.NavigateToTwoFactorLogin -> {
                onNavigateToTwoFactorLogin(event.emailAddress)
            }
        }
    }

    when (val dialog = state.dialogState) {
        is EnterpriseSignOnState.DialogState.Error -> {
            BitwardenBasicDialog(
                visibilityState = BasicDialogState.Shown(
                    title = dialog.title ?: R.string.an_error_has_occurred.asText(),
                    message = dialog.message,
                ),
                onDismissRequest = remember(viewModel) {
                    { viewModel.trySendAction(EnterpriseSignOnAction.DialogDismiss) }
                },
            )
        }

        is EnterpriseSignOnState.DialogState.Loading -> {
            BitwardenLoadingDialog(
                visibilityState = LoadingDialogState.Shown(
                    text = dialog.message,
                ),
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
                title = stringResource(id = R.string.app_name),
                scrollBehavior = scrollBehavior,
                navigationIcon = painterResource(id = R.drawable.ic_close),
                navigationIconContentDescription = stringResource(id = R.string.close),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(EnterpriseSignOnAction.CloseButtonClick) }
                },
                actions = {
                    BitwardenTextButton(
                        label = stringResource(id = R.string.log_in),
                        onClick = remember(viewModel) {
                            { viewModel.trySendAction(EnterpriseSignOnAction.LogInClick) }
                        },
                    )
                },
            )
        },
    ) { innerPadding ->
        EnterpriseSignOnScreenContent(
            state = state,
            onOrgIdentifierInputChange = remember(viewModel) {
                { viewModel.trySendAction(EnterpriseSignOnAction.OrgIdentifierInputChange(it)) }
            },
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        )
    }
}

@Composable
private fun EnterpriseSignOnScreenContent(
    state: EnterpriseSignOnState,
    onOrgIdentifierInputChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .imePadding()
            .verticalScroll(rememberScrollState())
            .fillMaxWidth(),
    ) {
        Text(
            text = stringResource(id = R.string.log_in_sso_summary),
            textAlign = TextAlign.Start,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        BitwardenTextField(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            value = state.orgIdentifierInput,
            onValueChange = onOrgIdentifierInputChange,
            label = stringResource(id = R.string.org_identifier),
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}
