package com.x8bit.bitwarden.ui.vault.feature.viewasqrcode

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.concat
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemArgs
import com.x8bit.bitwarden.ui.vault.feature.viewasqrcode.model.QrCodeConfig
import com.x8bit.bitwarden.ui.vault.feature.viewasqrcode.model.QrCodeType
import com.x8bit.bitwarden.ui.vault.feature.viewasqrcode.util.QrCodeGenerator
import com.x8bit.bitwarden.ui.vault.feature.viewasqrcode.util.toViewState
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * ViewModel responsible for handling user interactions in the attachments screen.
 */
@HiltViewModel
class ViewAsQrCodeViewModel @Inject constructor(
    private val vaultRepository: VaultRepository,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<ViewAsQrCodeState, ViewAsQrCodeEvent, ViewAsQrCodeAction>(
    // We load the state from the savedStateHandle for testing purposes.
    initialState = savedStateHandle[KEY_STATE] ?: run {
        val args = ViewAsQrCodeArgs(savedStateHandle)
        ViewAsQrCodeState(
            cipherId = args.vaultItemId,
            cipherType = args.vaultItemCipherType,
            viewState = ViewAsQrCodeState.ViewState.Loading,
            dialogState = null,
        )
    },
) {
    private val args = ViewAsQrCodeArgs(savedStateHandle)

    init {
        //TODO get args.vaultItemCipherType and auto-map
        vaultRepository
            .getVaultItemStateFlow(args.vaultItemId)
            .map { ViewAsQrCodeAction.Internal.CipherReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: ViewAsQrCodeAction) {
        when (action) {
            ViewAsQrCodeAction.BackClick -> handleBackClick()
            is ViewAsQrCodeAction.QrCodeTypeSelect -> handleQrCodeTypeSelect(action)
            is ViewAsQrCodeAction.FieldValueChange -> handleFieldValueChange(action)
            is ViewAsQrCodeAction.Internal.CipherReceive -> handleInternalAction(action)
        }
    }

    private fun handleBackClick() {
        sendEvent(ViewAsQrCodeEvent.NavigateBack)
    }

    private fun handleInternalAction(action: ViewAsQrCodeAction.Internal) {
        when (action) {
            is ViewAsQrCodeAction.Internal.CipherReceive -> handleCipherReceive(action)
        }
    }

    private fun handleCipherReceive(action: ViewAsQrCodeAction.Internal.CipherReceive) {
        when (val dataState = action.cipherDataState) {
            is DataState.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = ViewAsQrCodeState.ViewState.Error(
                            message = R.string.generic_error_message.asText(),
                        ),
                    )
                }
            }

            is DataState.Loaded -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = dataState
                            .data
                            ?.toViewState()
                            ?: ViewAsQrCodeState.ViewState.Error(
                                message = R.string.generic_error_message.asText(),
                            ),
                    )
                }
            }

            DataState.Loading -> {
                mutableStateFlow.update {
                    it.copy(viewState = ViewAsQrCodeState.ViewState.Loading)
                }
            }

            is DataState.NoNetwork -> mutableStateFlow.update {
                it.copy(
                    viewState = ViewAsQrCodeState.ViewState.Error(
                        message = R.string.internet_connection_required_title
                            .asText()
                            .concat(
                                " ".asText(),
                                R.string.internet_connection_required_message.asText(),
                            ),
                    ),
                )
            }

            is DataState.Pending -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = dataState
                            .data
                            ?.toViewState()
                            ?: ViewAsQrCodeState.ViewState.Error(
                                message = R.string.generic_error_message.asText(),
                            ),
                    )
                }
            }
        }
    }


        private fun handleQrCodeTypeSelect(action: ViewAsQrCodeAction.QrCodeTypeSelect) {
//        val currentState = state as? ViewAsQrCodeState.Content ?: return
//        val cipher = currentState.cipher
//
//        // Generate default fields based on the selected QR code type and cipher data
//        val fields = when (action.qrCodeType) {
//            QrCodeType.Text -> mapOf("text" to cipher.name)
//            QrCodeType.Url -> {
//                val loginUri = cipher.login?.uris?.firstOrNull()?.uri ?: ""
//                mapOf("url" to loginUri)
//            }
//            QrCodeType.Email -> {
//                val email = cipher.login?.username.orEmpty()
//                mapOf(
//                    "email" to email,
//                    "subject" to "",
//                    "body" to ""
//                )
//            }
//            QrCodeType.Phone -> {
//                val phone = when {
//                    cipher.identity?.phone != null -> cipher.identity.phone
//                    cipher.card?.cardholderName != null -> ""
//                    else -> ""
//                }
//                mapOf("phone" to phone)
//            }
//            QrCodeType.SMS -> {
//                val phone = when {
//                    cipher.identity?.phone != null -> cipher.identity.phone
//                    else -> ""
//                }
//                mapOf(
//                    "phone" to phone,
//                    "message" to ""
//                )
//            }
//            QrCodeType.WiFi -> mapOf(
//                "ssid" to "",
//                "password" to "",
//                "type" to "WPA",
//                "hidden" to "false"
//            )
//            QrCodeType.Contact -> {
//                val name = when {
//                    cipher.identity != null -> "${cipher.identity.firstName} ${cipher.identity.lastName}"
//                    cipher.card?.cardholderName != null -> cipher.card.cardholderName
//                    else -> cipher.name
//                }
//                val email = when {
//                    cipher.identity?.email != null -> cipher.identity.email
//                    cipher.login?.username != null -> cipher.login.username
//                    else -> ""
//                }
//                val phone = when {
//                    cipher.identity?.phone != null -> cipher.identity.phone
//                    else -> ""
//                }
//                val organization = cipher.identity?.company ?: ""
//                val address = when {
//                    cipher.identity != null -> "${cipher.identity.address1} ${cipher.identity.address2} ${cipher.identity.city} ${cipher.identity.state} ${cipher.identity.postalCode} ${cipher.identity.country}"
//                    else -> ""
//                }
//
//                mapOf(
//                    "name" to name,
//                    "phone" to phone,
//                    "email" to email,
//                    "organization" to organization,
//                    "address" to address
//                )
//            }
//        }
//
//        val config = QrCodeConfig(action.qrCodeType, fields)
//        val qrCodeBitmap = QrCodeGenerator.generateQrCode(config)
//
//        updateState {
//            (it as ViewAsQrCodeState.Content).copy(
//                selectedQrCodeType = action.qrCodeType,
//                fields = fields,
//                qrCodeBitmap = qrCodeBitmap
//            )
//        }
    }

    private fun handleFieldValueChange(action: ViewAsQrCodeAction.FieldValueChange) {
//        val currentState = state as? ViewAsQrCodeState.Content ?: return
//
//        val updatedFields = currentState.fields.toMutableMap().apply {
//            put(action.fieldKey, action.value)
//        }
//
//        val config = QrCodeConfig(currentState.selectedQrCodeType, updatedFields)
//        val qrCodeBitmap = QrCodeGenerator.generateQrCode(config)
//
//        updateState {
//            (it as ViewAsQrCodeState.Content).copy(
//                fields = updatedFields,
//                qrCodeBitmap = qrCodeBitmap
//            )
//        }
    }
}

/**
 * Represents the state for viewing attachments.
 */
@Parcelize
data class ViewAsQrCodeState(
    val cipherId: String,
    val cipherType: VaultItemCipherType,
    val viewState: ViewState,
    val dialogState: DialogState?,
) : Parcelable {
    /**
     * Represents the specific view states for the [ViewAsQrCodeScreen].
     */
    sealed class ViewState : Parcelable {
        /**
         * Represents an error state for the [ViewAsQrCodeScreen].
         */
        @Parcelize
        data class Error(val message: Text) : ViewState()

        /**
         * Loading state for the [ViewAsQrCodeScreen], signifying that the content is being
         * processed.
         */
        @Parcelize
        data object Loading : ViewState()

        /**
         * Represents a loaded content state for the [ViewAsQrCodeScreen].
         */
        @Parcelize
        data class Content(

            val title: String,
//        val qrCodeBitmap: Bitmap,
//        val selectedQrCodeType: QrCodeType,
//        val qrCodeTypes: ImmutableList<QrCodeType>,
//        val fields: Map<String, String>,
//        val cipher: com.x8bit.bitwarden.data.vault.datasource.model.Cipher
//        ) : ViewAsQrCodeState()
            //TODO add content?
        ) : ViewState()
    }
    //TODO do we need dialogs?
    /**
     * Represents the current state of any dialogs on the screen.
     */
    sealed class DialogState : Parcelable {
        /**
         * Represents a dismissible dialog with the given error [message].
         */
        @Parcelize
        data class Error(
            val title: Text?,
            val message: Text,
        ) : DialogState()

        /**
         * Represents a loading dialog with the given [message].
         */
        @Parcelize
        data class Loading(
            val message: Text,
        ) : DialogState()
    }
}


/**
 * Models events for the [ViewAsQrCodeScreen].
 */
sealed class ViewAsQrCodeEvent {
    /**
     * Navigate back.
     */
    data object NavigateBack : ViewAsQrCodeEvent()
}

/**
 * Represents a set of actions for [ViewAsQrCodeScreen].
 */
sealed class ViewAsQrCodeAction {
    /**
     * User clicked the back button.
     */
    data object BackClick : ViewAsQrCodeAction()

    //TODO deleteme
    /**
     * User selected a QR code type.
     */
    data class QrCodeTypeSelect(val qrCodeType: QrCodeType) : ViewAsQrCodeAction()

    /**
     * User changed a field value.
     */
    data class FieldValueChange(val fieldKey: String, val value: String) : ViewAsQrCodeAction()

    /**
     * Internal ViewModel actions.
     */
    sealed class Internal : ViewAsQrCodeAction() {
        /**
         * The cipher data has been received.
         */
        data class CipherReceive(
            val cipherDataState: DataState<CipherView?>,
        ) : Internal()
    }
}
