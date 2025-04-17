package com.x8bit.bitwarden.ui.auth.feature.environment

import android.os.Parcelable
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.data.datasource.disk.model.EnvironmentUrlDataJson
import com.bitwarden.data.repository.model.Environment
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.datasource.disk.model.MutualTlsKeyHost
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.KeyManager
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import com.x8bit.bitwarden.data.platform.manager.model.ImportPrivateKeyResult
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.vault.manager.FileManager
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.isValidUri
import com.x8bit.bitwarden.ui.platform.base.util.orNullIfBlank
import com.x8bit.bitwarden.ui.platform.base.util.prefixHttpsIfNecessaryOrNull
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.manager.keychain.model.PrivateKeyAliasSelectionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * View model for the self-hosted/custom environment screen.
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class EnvironmentViewModel @Inject constructor(
    private val environmentRepository: EnvironmentRepository,
    private val fileManager: FileManager,
    private val keyManager: KeyManager,
    private val featureFlagManager: FeatureFlagManager,
    private val savedStateHandle: SavedStateHandle,
) : BaseViewModel<EnvironmentState, EnvironmentEvent, EnvironmentAction>(
    initialState = savedStateHandle[KEY_STATE] ?: run {
        val environmentUrlData = when (val environment = environmentRepository.environment) {
            Environment.Us,
            Environment.Eu,
                -> EnvironmentUrlDataJson(base = "")

            is Environment.SelfHosted -> environment.environmentUrlData
        }
        val keyUri = environmentUrlData.keyUri?.toUri()
        val keyAlias = keyUri?.path?.trim('/').orEmpty()
        val keyHost = MutualTlsKeyHost.entries.find { it.name == keyUri?.authority }
        EnvironmentState(
            serverUrl = environmentUrlData.base,
            webVaultServerUrl = environmentUrlData.webVault.orEmpty(),
            apiServerUrl = environmentUrlData.api.orEmpty(),
            identityServerUrl = environmentUrlData.identity.orEmpty(),
            iconsServerUrl = environmentUrlData.icon.orEmpty(),
            keyAlias = keyAlias,
            keyHost = keyHost,
            dialog = null,
            showMutualTlsOptions = featureFlagManager.getFeatureFlag(FlagKey.MutualTls),
        )
    },
) {

    init {
        stateFlow
            .onEach {
                savedStateHandle[KEY_STATE] = it
            }
            .launchIn(viewModelScope)

        featureFlagManager.getFeatureFlagFlow(FlagKey.MutualTls)
            .map { EnvironmentAction.Internal.MutualTlsFeatureFlagUpdate(it) }
            .onEach(::handleAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: EnvironmentAction): Unit = when (action) {
        is EnvironmentAction.CloseClick -> handleCloseClickAction()
        is EnvironmentAction.SaveClick -> handleSaveClickAction()
        is EnvironmentAction.ErrorDialogDismiss -> handleErrorDialogDismiss()
        is EnvironmentAction.ServerUrlChange -> handleServerUrlChangeAction(action)
        is EnvironmentAction.WebVaultServerUrlChange -> handleWebVaultServerUrlChangeAction(action)
        is EnvironmentAction.ApiServerUrlChange -> handleApiServerUrlChangeAction(action)
        is EnvironmentAction.IdentityServerUrlChange -> handleIdentityServerUrlChangeAction(action)
        is EnvironmentAction.IconsServerUrlChange -> handleIconsServerUrlChangeAction(action)
        is EnvironmentAction.ImportCertificateClick -> handleImportCertificateClick()
        is EnvironmentAction.ImportCertificateFilePickerResultReceive -> {
            handleCertificateFilePickerResultReceive(action)
        }

        is EnvironmentAction.SetCertificatePasswordDialogDismiss -> {
            handleSetCertificatePasswordDialogDismiss()
        }

        is EnvironmentAction.CertificateInstallationResultReceive -> {
            handleCertificateInstallationResultReceive(action)
        }

        is EnvironmentAction.SetCertificateInfoResultReceive -> {
            handleSetCertificateInfoResultReceive(action)
        }

        is EnvironmentAction.ChooseSystemCertificateClick -> {
            handleChooseSystemCertificateClickAction()
        }

        is EnvironmentAction.ConfirmChooseSystemCertificateClick -> {
            handleConfirmChooseSystemCertificateClick()
        }

        is EnvironmentAction.SystemCertificateSelectionResultReceive -> {
            handleSystemCertificateSelectionResultReceive(action)
        }

        is EnvironmentAction.Internal -> handleInternalAction(action)
    }

    private fun handleCloseClickAction() {
        sendEvent(EnvironmentEvent.NavigateBack)
    }

    private fun handleSaveClickAction() {
        val urlsAreAllNullOrValid = listOf(
            state.serverUrl,
            state.webVaultServerUrl,
            state.apiServerUrl,
            state.identityServerUrl,
            state.iconsServerUrl,
        )
            .map { it.orNullIfBlank() }
            .all { url ->
                url == null || url.isValidUri()
            }

        if (!urlsAreAllNullOrValid) {
            showErrorDialog(message = R.string.environment_page_urls_error.asText())
            return
        }

        // Ensure all non-null/non-empty values have "http(s)://" prefixed.
        val updatedServerUrl = state.serverUrl.prefixHttpsIfNecessaryOrNull() ?: ""
        val updatedWebVaultServerUrl = state.webVaultServerUrl.prefixHttpsIfNecessaryOrNull()
        val updatedApiServerUrl = state.apiServerUrl.prefixHttpsIfNecessaryOrNull()
        val updatedIdentityServerUrl = state.identityServerUrl.prefixHttpsIfNecessaryOrNull()
        val updatedIconsServerUrl = state.iconsServerUrl.prefixHttpsIfNecessaryOrNull()
        environmentRepository.environment = Environment.SelfHosted(
            environmentUrlData = EnvironmentUrlDataJson(
                base = updatedServerUrl,
                api = updatedApiServerUrl,
                identity = updatedIdentityServerUrl,
                icon = updatedIconsServerUrl,
                webVault = updatedWebVaultServerUrl,
                keyUri = state.keyUri,
            ),
        )

        showToast(message = R.string.environment_saved.asText())
        sendEvent(EnvironmentEvent.NavigateBack)
    }

    private fun handleErrorDialogDismiss() {
        mutableStateFlow.update { it.copy(dialog = null) }
    }

    private fun handleServerUrlChangeAction(
        action: EnvironmentAction.ServerUrlChange,
    ) {
        mutableStateFlow.update {
            it.copy(serverUrl = action.serverUrl)
        }
    }

    private fun handleCertificateInstallationResultReceive(
        action: EnvironmentAction.CertificateInstallationResultReceive,
    ) {
        showToast(
            message = if (action.success) {
                R.string.certificate_installed.asText()
            } else {
                R.string.certificate_installation_failed.asText()
            },
        )
    }

    private fun handleSetCertificatePasswordDialogDismiss() {
        mutableStateFlow.update { it.copy(dialog = null) }
    }

    private fun handleCertificateFilePickerResultReceive(
        action: EnvironmentAction.ImportCertificateFilePickerResultReceive,
    ) {
        mutableStateFlow.update {
            it.copy(
                dialog = EnvironmentState.DialogState.SetCertificateData(
                    certificateBytes = action.certificateFileData,
                ),
            )
        }
    }

    private fun handleConfirmChooseSystemCertificateClick() {
        mutableStateFlow.update {
            it.copy(dialog = null)
        }
        sendEvent(
            EnvironmentEvent.ShowSystemCertificateSelectionDialog(
                serverUrl = state.serverUrl.prefixHttpsIfNecessaryOrNull(),
            ),
        )
    }

    private fun handleWebVaultServerUrlChangeAction(
        action: EnvironmentAction.WebVaultServerUrlChange,
    ) {
        mutableStateFlow.update {
            it.copy(webVaultServerUrl = action.webVaultServerUrl)
        }
    }

    private fun handleApiServerUrlChangeAction(
        action: EnvironmentAction.ApiServerUrlChange,
    ) {
        mutableStateFlow.update {
            it.copy(apiServerUrl = action.apiServerUrl)
        }
    }

    private fun handleIdentityServerUrlChangeAction(
        action: EnvironmentAction.IdentityServerUrlChange,
    ) {
        mutableStateFlow.update {
            it.copy(identityServerUrl = action.identityServerUrl)
        }
    }

    private fun handleIconsServerUrlChangeAction(
        action: EnvironmentAction.IconsServerUrlChange,
    ) {
        mutableStateFlow.update {
            it.copy(iconsServerUrl = action.iconsServerUrl)
        }
    }

    private fun handleImportCertificateClick() {
        sendEvent(EnvironmentEvent.ShowCertificateImportFileChooser)
    }

    private fun handleInternalAction(action: EnvironmentAction.Internal) {
        when (action) {
            is EnvironmentAction.Internal.ImportKeyResultReceive -> {
                handleSaveKeyResultReceive(action)
            }

            is EnvironmentAction.Internal.MutualTlsFeatureFlagUpdate -> {
                handleMutualTlsFeatureFlagUpdate(action)
            }
        }
    }

    private fun handleSetCertificateInfoResultReceive(
        action: EnvironmentAction.SetCertificateInfoResultReceive,
    ) {
        if (action.password.isBlank()) {
            showErrorDialog(
                message = R.string.validation_field_required.asText(
                    R.string.password.asText(),
                ),
            )
            return
        }

        if (action.alias.isBlank()) {
            showErrorDialog(
                message = R.string.validation_field_required.asText(
                    R.string.alias.asText(),
                ),
            )
            return
        }

        mutableStateFlow.update { it.copy(dialog = null) }

        viewModelScope.launch {
            fileManager
                .uriToByteArray(action.certificateFileData.uri)
                .map { bytes ->
                    keyManager.importMutualTlsCertificate(
                        key = bytes,
                        alias = action.alias,
                        password = action.password,
                    )
                }
                .map { result ->
                    sendAction(
                        EnvironmentAction.Internal.ImportKeyResultReceive(result),
                    )
                }
        }
    }

    private fun handleSaveKeyResultReceive(
        action: EnvironmentAction.Internal.ImportKeyResultReceive,
    ) {
        when (val result = action.result) {
            is ImportPrivateKeyResult.Success -> {
                mutableStateFlow.update { state ->
                    state.copy(
                        keyAlias = result.alias,
                        keyHost = MutualTlsKeyHost.ANDROID_KEY_STORE,
                    )
                }
            }

            is ImportPrivateKeyResult.Error.UnsupportedKey -> {
                showToast(message = R.string.unsupported_certificate_type.asText())
            }

            is ImportPrivateKeyResult.Error.KeyStoreOperationFailed -> {
                showToast(message = R.string.certificate_installation_failed.asText())
            }

            is ImportPrivateKeyResult.Error.UnrecoverableKey -> {
                showToast(message = R.string.certificate_password_incorrect.asText())
            }

            is ImportPrivateKeyResult.Error.InvalidCertificateChain -> {
                showToast(R.string.invalid_certificate_chain.asText())
            }

            ImportPrivateKeyResult.Error.DuplicateAlias -> {
                // TODO [PM-17686] Improve duplicate alias handling.
                showToast(R.string.certificate_alias_already_exists.asText())
            }
        }
    }

    private fun handleMutualTlsFeatureFlagUpdate(
        action: EnvironmentAction.Internal.MutualTlsFeatureFlagUpdate,
    ) {
        mutableStateFlow.update {
            it.copy(
                showMutualTlsOptions = action.enabled,
            )
        }
    }

    private fun handleChooseSystemCertificateClickAction() {
        mutableStateFlow.update {
            it.copy(
                dialog = EnvironmentState.DialogState.SystemCertificateWarningDialog,
            )
        }
    }

    private fun handleSystemCertificateSelectionResultReceive(
        action: EnvironmentAction.SystemCertificateSelectionResultReceive,
    ) {
        when (val result = action.privateKeyAliasSelectionResult) {
            is PrivateKeyAliasSelectionResult.Success -> {
                mutableStateFlow.update {
                    it.copy(
                        keyAlias = result.alias.orEmpty(),
                        keyHost = result.alias?.let { MutualTlsKeyHost.KEY_CHAIN },
                    )
                }
            }

            is PrivateKeyAliasSelectionResult.Error -> {
                sendEvent(
                    EnvironmentEvent.ShowToast(
                        message = R.string.error_loading_certificate.asText(),
                    ),
                )
            }
        }
    }

    private fun showToast(message: Text) {
        sendEvent(EnvironmentEvent.ShowToast(message))
    }

    private fun showErrorDialog(message: Text) {
        mutableStateFlow.update {
            it.copy(dialog = EnvironmentState.DialogState.Error(message = message))
        }
    }
}

/**
 * Models the state of the environment screen.
 */
@Parcelize
data class EnvironmentState(
    val serverUrl: String,
    val webVaultServerUrl: String,
    val apiServerUrl: String,
    val identityServerUrl: String,
    val iconsServerUrl: String,
    val keyAlias: String,
    val dialog: DialogState?,
    val showMutualTlsOptions: Boolean,
    // internal
    private val keyHost: MutualTlsKeyHost?,
) : Parcelable {

    val keyUri: String?
        get() = "cert://$keyHost/$keyAlias"
            .takeUnless { keyHost == null || keyAlias.isEmpty() }

    /**
     * Models the dialog states of the environment screen.
     */
    @Parcelize
    sealed class DialogState : Parcelable {

        /**
         * Show an error dialog.
         */
        data class Error(
            val message: Text,
        ) : DialogState()

        /**
         * Show a dialog to capture the certificate alias and password.
         */
        data class SetCertificateData(
            val certificateBytes: IntentManager.FileData,
        ) : DialogState()

        /**
         * Show a dialog warning the user that system certificates are not as secure.
         */
        data object SystemCertificateWarningDialog : DialogState()
    }
}

/**
 * Models events for the environment screen.
 */
sealed class EnvironmentEvent {
    /**
     * Navigate back.
     */
    data object NavigateBack : EnvironmentEvent()

    /**
     * Show the File chooser dialog for certificate import.
     */
    data object ShowCertificateImportFileChooser : EnvironmentEvent()

    /**
     * Show the system certificate selection dialog.
     */
    data class ShowSystemCertificateSelectionDialog(
        val serverUrl: String?,
    ) : EnvironmentEvent()

    /**
     * Show a toast with the given message.
     */
    data class ShowToast(
        val message: Text,
    ) : EnvironmentEvent()
}

/**
 * Models actions for the environment screen.
 */
sealed class EnvironmentAction {
    /**
     * User clicked back button.
     */
    data object CloseClick : EnvironmentAction()

    /**
     * User clicked the save button.
     */
    data object SaveClick : EnvironmentAction()

    /**
     * User dismissed an error dialog.
     */
    data object ErrorDialogDismiss : EnvironmentAction()

    /**
     * User clicked the import certificate button.
     */
    data object ImportCertificateClick : EnvironmentAction()

    /**
     * User dismissed the set certificate password dialog without providing a password.
     */
    data object SetCertificatePasswordDialogDismiss : EnvironmentAction()

    /**
     * User clicked the choose system certificate button.
     */
    data object ChooseSystemCertificateClick : EnvironmentAction()

    /**
     * User confirmed choosing the system certificate.
     */
    data object ConfirmChooseSystemCertificateClick : EnvironmentAction()

    /**
     * Indicates that the overall server URL has changed.
     */
    data class ServerUrlChange(
        val serverUrl: String,
    ) : EnvironmentAction()

    /**
     * Indicates that the certificate installation result was received.
     */
    data class CertificateInstallationResultReceive(
        val success: Boolean,
    ) : EnvironmentAction()

    /**
     * Indicates that the web vault server URL has changed.
     */
    data class WebVaultServerUrlChange(
        val webVaultServerUrl: String,
    ) : EnvironmentAction()

    /**
     * Indicates that the API server URL has changed.
     */
    data class ApiServerUrlChange(
        val apiServerUrl: String,
    ) : EnvironmentAction()

    /**
     * Indicates that the identity server URL has changed.
     */
    data class IdentityServerUrlChange(
        val identityServerUrl: String,
    ) : EnvironmentAction()

    /**
     * Indicates that the icons server URL has changed.
     */
    data class IconsServerUrlChange(
        val iconsServerUrl: String,
    ) : EnvironmentAction()

    /**
     * Indicates that the certificate file selection result was received.
     */
    data class ImportCertificateFilePickerResultReceive(
        val certificateFileData: IntentManager.FileData,
    ) : EnvironmentAction()

    /**
     * Indicates the certificate info data was received.
     */
    data class SetCertificateInfoResultReceive(
        val certificateFileData: IntentManager.FileData,
        val password: String,
        val alias: String,
    ) : EnvironmentAction()

    /**
     * User has selected a system certificate alias.
     */
    data class SystemCertificateSelectionResultReceive(
        val privateKeyAliasSelectionResult: PrivateKeyAliasSelectionResult,
    ) : EnvironmentAction()

    /**
     * Models actions the EnvironmentViewModel itself may trigger.
     */
    sealed class Internal : EnvironmentAction() {
        /**
         * Indicates the result of importing a key was received.
         */
        data class ImportKeyResultReceive(
            val result: ImportPrivateKeyResult,
        ) : Internal()

        /**
         * Indicates the mutual TLS feature flag was updated.
         */
        data class MutualTlsFeatureFlagUpdate(
            val enabled: Boolean,
        ) : Internal()
    }
}
