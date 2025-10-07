package com.x8bit.bitwarden.ui.auth.feature.environment

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.bitwarden.ui.platform.components.button.BitwardenOutlinedButton
import com.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.bitwarden.ui.platform.components.field.BitwardenTextField
import com.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.snackbar.BitwardenSnackbarHost
import com.bitwarden.ui.platform.components.snackbar.model.rememberBitwardenSnackbarHostState
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.composition.LocalIntentManager
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.BuildConfig
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenClientCertificateDialog
import com.x8bit.bitwarden.ui.platform.composition.LocalKeyChainManager
import com.x8bit.bitwarden.ui.platform.manager.keychain.KeyChainManager
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

/**
 * Displays the about self-hosted/custom environment screen.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnvironmentScreen(
    onNavigateBack: () -> Unit,
    intentManager: IntentManager = LocalIntentManager.current,
    keyChainManager: KeyChainManager = LocalKeyChainManager.current,
    viewModel: EnvironmentViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val certificateImportFilePickerLauncher = intentManager.getActivityResultLauncher { result ->
        intentManager.getFileDataFromActivityResult(result)?.let {
            viewModel.trySendAction(
                EnvironmentAction.ImportCertificateFilePickerResultReceive(it),
            )
        }
    }
    val scope = rememberCoroutineScope()
    val snackbarHostState = rememberBitwardenSnackbarHostState()
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is EnvironmentEvent.NavigateBack -> onNavigateBack.invoke()
            is EnvironmentEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.data)
            is EnvironmentEvent.ShowCertificateImportFileChooser -> {
                certificateImportFilePickerLauncher.launch(
                    intentManager.createFileChooserIntent(withCameraIntents = false),
                )
            }

            is EnvironmentEvent.ShowSystemCertificateSelectionDialog -> {
                scope.launch {
                    val result = keyChainManager.choosePrivateKeyAlias(
                        currentServerUrl = event.serverUrl?.takeUnless { it.isEmpty() },
                    )
                    viewModel.trySendAction(
                        action = EnvironmentAction.SystemCertificateSelectionResultReceive(result),
                    )
                }
            }
        }
    }

    when (val dialog = state.dialog) {
        is EnvironmentState.DialogState.Error -> {
            BitwardenBasicDialog(
                title = stringResource(id = BitwardenString.an_error_has_occurred),
                message = dialog.message(),
                onDismissRequest = remember(viewModel) {
                    { viewModel.trySendAction(EnvironmentAction.DialogDismiss) }
                },
                throwable = dialog.throwable,
            )
        }

        is EnvironmentState.DialogState.SetCertificateData -> {
            BitwardenClientCertificateDialog(
                onConfirmClick = remember(viewModel) {
                    { alias, password ->
                        viewModel.trySendAction(
                            EnvironmentAction.SetCertificateInfoResultReceive(
                                certificateFileData = dialog.certificateBytes,
                                password = password,
                                alias = alias,
                            ),
                        )
                    }
                },
                onDismissRequest = remember(viewModel) {
                    {
                        viewModel.trySendAction(
                            action = EnvironmentAction.SetCertificatePasswordDialogDismiss,
                        )
                    }
                },
            )
        }

        is EnvironmentState.DialogState.SystemCertificateWarningDialog -> {
            @Suppress("MaxLineLength")
            BitwardenTwoButtonDialog(
                title = stringResource(BitwardenString.warning),
                message = stringResource(
                    BitwardenString.system_certificates_are_not_as_secure_as_importing_certificates_to_bitwarden,
                ),
                confirmButtonText = stringResource(BitwardenString.continue_text),
                onConfirmClick = remember(viewModel) {
                    { viewModel.trySendAction(EnvironmentAction.ConfirmChooseSystemCertificateClick) }
                },
                dismissButtonText = stringResource(BitwardenString.cancel),
                onDismissClick = remember(viewModel) {
                    { viewModel.trySendAction(EnvironmentAction.DialogDismiss) }
                },
                onDismissRequest = remember(viewModel) {
                    { viewModel.trySendAction(EnvironmentAction.DialogDismiss) }
                },
            )
        }

        is EnvironmentState.DialogState.ConfirmOverwriteAlias -> {
            BitwardenTwoButtonDialog(
                title = dialog.title(),
                message = dialog.message(),
                confirmButtonText = stringResource(BitwardenString.replace_certificate),
                dismissButtonText = stringResource(BitwardenString.cancel),
                onConfirmClick = remember(viewModel) {
                    {
                        viewModel.trySendAction(
                            EnvironmentAction.ConfirmOverwriteCertificateClick(
                                triggeringAction = dialog.triggeringAction,
                            ),
                        )
                    }
                },
                onDismissClick = remember(viewModel) {
                    { viewModel.trySendAction(EnvironmentAction.DialogDismiss) }
                },
                onDismissRequest = remember(viewModel) {
                    { viewModel.trySendAction(EnvironmentAction.DialogDismiss) }
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
                title = stringResource(id = BitwardenString.settings),
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = BitwardenDrawable.ic_close),
                navigationIconContentDescription = stringResource(id = BitwardenString.close),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(EnvironmentAction.CloseClick) }
                },
                actions = {
                    BitwardenTextButton(
                        label = stringResource(id = BitwardenString.save),
                        onClick = remember(viewModel) {
                            { viewModel.trySendAction(EnvironmentAction.SaveClick) }
                        },
                        modifier = Modifier.testTag("SaveButton"),
                    )
                },
            )
        },
        snackbarHost = {
            BitwardenSnackbarHost(bitwardenHostState = snackbarHostState)
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(height = 12.dp))
            BitwardenListHeaderText(
                label = stringResource(id = BitwardenString.self_hosted_environment),
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(height = 8.dp))

            BitwardenTextField(
                label = stringResource(id = BitwardenString.server_url),
                value = state.serverUrl,
                placeholder = "ex. https://bitwarden.company.com",
                supportingText = stringResource(
                    id = BitwardenString.self_hosted_environment_footer,
                ),
                onValueChange = remember(viewModel) {
                    { viewModel.trySendAction(EnvironmentAction.ServerUrlChange(it)) }
                },
                autoCompleteOptions = if (BuildConfig.BUILD_TYPE != "release") {
                    persistentListOf(
                        "https://vault.qa.bitwarden.pw",
                        "https://qa-team.sh.bitwarden.pw",
                        "https://vault.usdev.bitwarden.pw",
                    )
                } else {
                    persistentListOf()
                },
                keyboardType = KeyboardType.Uri,
                textFieldTestTag = "ServerUrlEntry",
                cardStyle = CardStyle.Full,
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            BitwardenListHeaderText(
                label = stringResource(id = BitwardenString.custom_environment),
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(height = 8.dp))

            BitwardenTextField(
                label = stringResource(id = BitwardenString.web_vault_url),
                value = state.webVaultServerUrl,
                onValueChange = remember(viewModel) {
                    { viewModel.trySendAction(EnvironmentAction.WebVaultServerUrlChange(it)) }
                },
                keyboardType = KeyboardType.Uri,
                textFieldTestTag = "WebVaultUrlEntry",
                cardStyle = CardStyle.Full,
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )

            Spacer(modifier = Modifier.height(height = 8.dp))

            BitwardenTextField(
                label = stringResource(id = BitwardenString.api_url),
                value = state.apiServerUrl,
                onValueChange = remember(viewModel) {
                    { viewModel.trySendAction(EnvironmentAction.ApiServerUrlChange(it)) }
                },
                keyboardType = KeyboardType.Uri,
                cardStyle = CardStyle.Full,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ApiUrlEntry")
                    .standardHorizontalMargin(),
            )

            Spacer(modifier = Modifier.height(height = 8.dp))

            BitwardenTextField(
                label = stringResource(id = BitwardenString.identity_url),
                value = state.identityServerUrl,
                onValueChange = remember(viewModel) {
                    { viewModel.trySendAction(EnvironmentAction.IdentityServerUrlChange(it)) }
                },
                keyboardType = KeyboardType.Uri,
                textFieldTestTag = "IdentityUrlEntry",
                cardStyle = CardStyle.Full,
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )

            Spacer(modifier = Modifier.height(height = 8.dp))

            BitwardenTextField(
                label = stringResource(id = BitwardenString.icons_url),
                value = state.iconsServerUrl,
                onValueChange = remember(viewModel) {
                    { viewModel.trySendAction(EnvironmentAction.IconsServerUrlChange(it)) }
                },
                supportingText = stringResource(id = BitwardenString.custom_environment_footer),
                keyboardType = KeyboardType.Uri,
                textFieldTestTag = "IconsUrlEntry",
                cardStyle = CardStyle.Full,
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )

            Spacer(modifier = Modifier.height(height = 16.dp))

            BitwardenListHeaderText(
                label = stringResource(id = BitwardenString.client_certificate_mtls),
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(height = 8.dp))

            BitwardenTextField(
                label = stringResource(id = BitwardenString.certificate_alias),
                value = state.keyAlias,
                supportingText = stringResource(
                    id = BitwardenString.certificate_used_for_client_authentication,
                ),
                onValueChange = {},
                readOnly = true,
                cardStyle = CardStyle.Full,
                textFieldTestTag = "KeyAliasEntry",
                modifier = Modifier
                    .fillMaxWidth()
                    .focusProperties { canFocus = false }
                    .standardHorizontalMargin(),
            )
            Spacer(modifier = Modifier.height(height = 16.dp))

            BitwardenFilledButton(
                label = stringResource(id = BitwardenString.import_certificate),
                onClick = remember(viewModel) {
                    { viewModel.trySendAction(EnvironmentAction.ImportCertificateClick) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .testTag("ImportCertificateButton"),
            )

            Spacer(modifier = Modifier.height(height = 12.dp))

            BitwardenOutlinedButton(
                label = stringResource(id = BitwardenString.choose_system_certificate),
                onClick = remember(viewModel) {
                    { viewModel.trySendAction(EnvironmentAction.ChooseSystemCertificateClick) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .testTag("ChooseSystemCertificateButton"),
            )
            Spacer(modifier = Modifier.height(height = 16.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}
