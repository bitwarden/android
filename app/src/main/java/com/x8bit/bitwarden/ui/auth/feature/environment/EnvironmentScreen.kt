package com.x8bit.bitwarden.ui.auth.feature.environment

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.BuildConfig
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenOutlinedButton
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenClientCertificateDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.x8bit.bitwarden.ui.platform.components.model.CardStyle
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.composition.LocalIntentManager
import com.x8bit.bitwarden.ui.platform.composition.LocalKeyChainManager
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
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
    val context = LocalContext.current
    val certificateImportFilePickerLauncher = intentManager.getActivityResultLauncher { result ->
        intentManager.getFileDataFromActivityResult(result)?.let {
            viewModel.trySendAction(
                EnvironmentAction.ImportCertificateFilePickerResultReceive(it),
            )
        }
    }
    val scope = rememberCoroutineScope()
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is EnvironmentEvent.NavigateBack -> onNavigateBack.invoke()
            is EnvironmentEvent.ShowToast -> {
                Toast.makeText(context, event.message(context.resources), Toast.LENGTH_SHORT).show()
            }

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
                title = stringResource(id = R.string.an_error_has_occurred),
                message = dialog.message(),
                onDismissRequest = remember(viewModel) {
                    { viewModel.trySendAction(EnvironmentAction.ErrorDialogDismiss) }
                },
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
                title = stringResource(R.string.warning),
                message = stringResource(
                    R.string.system_certificates_are_not_as_secure_as_importing_certificates_to_bitwarden,
                ),
                confirmButtonText = stringResource(R.string.continue_text),
                onConfirmClick = remember(viewModel) {
                    { viewModel.trySendAction(EnvironmentAction.ConfirmChooseSystemCertificateClick) }
                },
                dismissButtonText = stringResource(R.string.cancel),
                onDismissClick = remember(viewModel) {
                    { viewModel.trySendAction(EnvironmentAction.ErrorDialogDismiss) }
                },
                onDismissRequest = remember(viewModel) {
                    { viewModel.trySendAction(EnvironmentAction.ErrorDialogDismiss) }
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
                title = stringResource(id = R.string.settings),
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = R.drawable.ic_close),
                navigationIconContentDescription = stringResource(id = R.string.close),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(EnvironmentAction.CloseClick) }
                },
                actions = {
                    BitwardenTextButton(
                        label = stringResource(id = R.string.save),
                        onClick = remember(viewModel) {
                            { viewModel.trySendAction(EnvironmentAction.SaveClick) }
                        },
                        modifier = Modifier.testTag("SaveButton"),
                    )
                },
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(height = 12.dp))
            BitwardenListHeaderText(
                label = stringResource(id = R.string.self_hosted_environment),
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(height = 8.dp))

            BitwardenTextField(
                label = stringResource(id = R.string.server_url),
                value = state.serverUrl,
                placeholder = "ex. https://bitwarden.company.com",
                supportingText = stringResource(id = R.string.self_hosted_environment_footer),
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
                label = stringResource(id = R.string.custom_environment),
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(height = 8.dp))

            BitwardenTextField(
                label = stringResource(id = R.string.web_vault_url),
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
                label = stringResource(id = R.string.api_url),
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
                label = stringResource(id = R.string.identity_url),
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
                label = stringResource(id = R.string.icons_url),
                value = state.iconsServerUrl,
                onValueChange = remember(viewModel) {
                    { viewModel.trySendAction(EnvironmentAction.IconsServerUrlChange(it)) }
                },
                supportingText = stringResource(id = R.string.custom_environment_footer),
                keyboardType = KeyboardType.Uri,
                textFieldTestTag = "IconsUrlEntry",
                cardStyle = CardStyle.Full,
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )

            if (state.showMutualTlsOptions) {
                Spacer(modifier = Modifier.height(height = 16.dp))

                BitwardenListHeaderText(
                    label = stringResource(id = R.string.client_certificate_mtls),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .padding(horizontal = 16.dp),
                )
                Spacer(modifier = Modifier.height(height = 8.dp))

                BitwardenTextField(
                    label = stringResource(id = R.string.certificate_alias),
                    value = state.keyAlias,
                    supportingText = stringResource(
                        id = R.string.certificate_used_for_client_authentication,
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
                    label = stringResource(id = R.string.import_certificate),
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
                    label = stringResource(id = R.string.choose_system_certificate),
                    onClick = remember(viewModel) {
                        { viewModel.trySendAction(EnvironmentAction.ChooseSystemCertificateClick) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .testTag("ChooseSystemCertificateButton"),
                )
            }
            Spacer(modifier = Modifier.height(height = 16.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}
