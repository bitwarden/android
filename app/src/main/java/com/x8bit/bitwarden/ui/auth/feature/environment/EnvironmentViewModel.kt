package com.x8bit.bitwarden.ui.auth.feature.environment

import android.os.Parcelable
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.datasource.disk.model.EnvironmentUrlDataJson
import com.x8bit.bitwarden.data.platform.datasource.disk.model.MutualTlsKeyHost
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.isValidUri
import com.x8bit.bitwarden.ui.platform.base.util.orNullIfBlank
import com.x8bit.bitwarden.ui.platform.manager.keychain.model.PrivateKeyAliasSelectionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
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
            shouldShowErrorDialog = false,
            keyAlias = keyAlias,
            keyHost = keyHost,
        )
    },
) {

    init {
        stateFlow
            .onEach {
                savedStateHandle[KEY_STATE] = it
            }
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
        is EnvironmentAction.UseSystemCertificateClick -> handleUseSystemCertificateClickAction()
        is EnvironmentAction.SystemCertificateSelectionResultReceive -> {
            handleSystemCertificateSelectionResultReceive(action)
        }
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
            mutableStateFlow.update { it.copy(shouldShowErrorDialog = true) }
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

        sendEvent(EnvironmentEvent.ShowToast(message = R.string.environment_saved.asText()))
        sendEvent(EnvironmentEvent.NavigateBack)
    }

    private fun handleErrorDialogDismiss() {
        mutableStateFlow.update { it.copy(shouldShowErrorDialog = false) }
    }

    private fun handleServerUrlChangeAction(
        action: EnvironmentAction.ServerUrlChange,
    ) {
        mutableStateFlow.update {
            it.copy(serverUrl = action.serverUrl)
        }
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

    private fun handleUseSystemCertificateClickAction() {
        sendEvent(EnvironmentEvent.ShowSystemCertificateSelectionDialog)
    }

    private fun handleSystemCertificateSelectionResultReceive(
        action: EnvironmentAction.SystemCertificateSelectionResultReceive,
    ) {
        when (val result = action.result) {
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
    val shouldShowErrorDialog: Boolean,
    val keyAlias: String,
    // internal
    val keyHost: MutualTlsKeyHost?,
) : Parcelable {
    val keyUri: String?
        get() = "cert://$keyHost/$keyAlias"
            .takeUnless { keyHost == null || keyAlias.isEmpty() }
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
     * Show the system certificate selection dialog.
     */
    data object ShowSystemCertificateSelectionDialog : EnvironmentEvent()

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
     * User clicked the use system certificate button.
     */
    data object UseSystemCertificateClick : EnvironmentAction()

    /**
     * Indicates that the overall server URL has changed.
     */
    data class ServerUrlChange(
        val serverUrl: String,
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
     * Indicates the result of system certificate selection has been received.
     */
    data class SystemCertificateSelectionResultReceive(
        val result: PrivateKeyAliasSelectionResult,
    ) : EnvironmentAction()
}

/**
 * If the given [String] is a valid URI, "https://" will be appended if it is not already present.
 * Otherwise `null` will be returned.
 */
private fun String.prefixHttpsIfNecessaryOrNull(): String? =
    when {
        this.isBlank() || !this.isValidUri() -> null
        "http://" in this || "https://" in this -> this
        else -> "https://$this"
    }
