package com.x8bit.bitwarden.ui.auth.feature.enterprisesignon

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.bitwarden.ui.platform.components.field.BitwardenTextField
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.composition.LocalIntentManager
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.composition.LocalAuthTabLaunchers
import com.x8bit.bitwarden.ui.platform.model.AuthTabLaunchers

/**
 * The top level composable for the Enterprise Single Sign On screen.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterpriseSignOnScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSetPassword: () -> Unit,
    onNavigateToTwoFactorLogin: (email: String, orgIdentifier: String) -> Unit,
    authTabLaunchers: AuthTabLaunchers = LocalAuthTabLaunchers.current,
    intentManager: IntentManager = LocalIntentManager.current,
    viewModel: EnterpriseSignOnViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            EnterpriseSignOnEvent.NavigateBack -> onNavigateBack()

            is EnterpriseSignOnEvent.NavigateToSsoLogin -> {
                intentManager.startAuthTab(
                    uri = event.uri,
                    redirectScheme = event.scheme,
                    launcher = authTabLaunchers.sso,
                )
            }

            is EnterpriseSignOnEvent.NavigateToSetPassword -> {
                onNavigateToSetPassword()
            }

            is EnterpriseSignOnEvent.NavigateToTwoFactorLogin -> {
                onNavigateToTwoFactorLogin(event.emailAddress, event.orgIdentifier)
            }
        }
    }

    EnterpriseSignOnDialogs(
        dialogState = state.dialogState,
        onConfirmKeyConnectorDomain = remember(viewModel) {
            { viewModel.trySendAction(EnterpriseSignOnAction.ConfirmKeyConnectorDomainClick) }
        },
        onDismissKeyConnectorDomain = remember(viewModel) {
            { viewModel.trySendAction(EnterpriseSignOnAction.CancelKeyConnectorDomainClick) }
        },
        onDismissRequest = remember(viewModel) {
            { viewModel.trySendAction(EnterpriseSignOnAction.DialogDismiss) }
        },
    )

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.app_name),
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = BitwardenDrawable.ic_close),
                navigationIconContentDescription = stringResource(id = BitwardenString.close),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(EnterpriseSignOnAction.CloseButtonClick) }
                },
                actions = {
                    BitwardenTextButton(
                        label = stringResource(id = BitwardenString.log_in_verb),
                        onClick = remember(viewModel) {
                            { viewModel.trySendAction(EnterpriseSignOnAction.LogInClick) }
                        },
                        modifier = Modifier.testTag("LoginButton"),
                    )
                },
            )
        },
    ) {
        EnterpriseSignOnScreenContent(
            state = state,
            onOrgIdentifierInputChange = remember(viewModel) {
                { viewModel.trySendAction(EnterpriseSignOnAction.OrgIdentifierInputChange(it)) }
            },
            modifier = Modifier.fillMaxSize(),
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
            .verticalScroll(rememberScrollState())
            .fillMaxWidth(),
    ) {
        Spacer(modifier = Modifier.height(height = 12.dp))
        Text(
            text = stringResource(id = BitwardenString.log_in_sso_summary),
            textAlign = TextAlign.Start,
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            modifier = Modifier
                .standardHorizontalMargin()
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(height = 8.dp))

        BitwardenTextField(
            modifier = Modifier
                .standardHorizontalMargin()
                .fillMaxWidth(),
            value = state.orgIdentifierInput,
            onValueChange = onOrgIdentifierInputChange,
            label = stringResource(id = BitwardenString.org_identifier),
            textFieldTestTag = "OrgSSOIdentifierEntry",
            cardStyle = CardStyle.Full,
        )

        Spacer(modifier = Modifier.height(16.dp))
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun EnterpriseSignOnDialogs(
    dialogState: EnterpriseSignOnState.DialogState?,
    onDismissRequest: () -> Unit,
    onConfirmKeyConnectorDomain: () -> Unit,
    onDismissKeyConnectorDomain: () -> Unit,
) {
    when (dialogState) {
        is EnterpriseSignOnState.DialogState.Error -> {
            BitwardenBasicDialog(
                title = dialogState.title(),
                message = dialogState.message(),
                throwable = dialogState.error,
                onDismissRequest = onDismissRequest,
            )
        }

        is EnterpriseSignOnState.DialogState.Loading -> {
            BitwardenLoadingDialog(text = dialogState.message())
        }

        is EnterpriseSignOnState.DialogState.KeyConnectorDomain -> {
            BitwardenTwoButtonDialog(
                title = stringResource(BitwardenString.confirm_key_connector_domain),
                message = stringResource(
                    BitwardenString.please_confirm_domain_with_admin,
                    dialogState.keyConnectorDomain,
                ),
                confirmButtonText = stringResource(BitwardenString.confirm),
                dismissButtonText = stringResource(BitwardenString.cancel),
                onConfirmClick = onConfirmKeyConnectorDomain,
                onDismissRequest = onDismissKeyConnectorDomain,
                onDismissClick = onDismissKeyConnectorDomain,
            )
        }

        null -> Unit
    }
}
