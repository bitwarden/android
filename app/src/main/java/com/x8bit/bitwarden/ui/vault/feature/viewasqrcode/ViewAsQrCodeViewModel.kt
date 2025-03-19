package com.x8bit.bitwarden.ui.vault.feature.viewasqrcode

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.FieldView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.concat
import com.x8bit.bitwarden.ui.vault.feature.viewasqrcode.model.QrCodeType
import com.x8bit.bitwarden.ui.vault.feature.viewasqrcode.model.QrCodeTypeField
//import com.x8bit.bitwarden.ui.vault.feature.viewasqrcode.util.toViewState
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.IgnoredOnParcel
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
        val qrCodeTypes = QrCodeType.entries
        val selectedQrCodeType = qrCodeTypes.first()

        ViewAsQrCodeState(
            cipherId = args.vaultItemId,
            cipherType = args.vaultItemCipherType,
            selectedQrCodeType = selectedQrCodeType,
            qrCodeTypes = qrCodeTypes,
            qrCodeTypeFields = selectedQrCodeType.fields,
            cipherFields = emptyList(),
            cipher = null,

//            viewState = ViewAsQrCodeState.ViewState.Loading,
//            dialogState = null,
        )
    },
) {
    private val args = ViewAsQrCodeArgs(savedStateHandle)


    init {
        //TODO get args.vaultItemCipherType and auto-map
        mutableStateFlow.update {
            it.copy(
                cipherFields = cipherFieldsFor(it.cipherType),
            )
        }
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
            is DataState.Loaded -> {
                //TODO fix nullable access
                mutableStateFlow.update {
                    it.copy(
                        cipher = dataState.data!!,
                    )
                }

            }

            //TODO do we need to handle these?
            is DataState.Error -> {}
            is DataState.Loading -> {}
            is DataState.NoNetwork<*> -> {}
            is DataState.Pending -> {}
//            is DataState.Error -> {
//                mutableStateFlow.update {
//                    it.copy(
//                        viewState = ViewAsQrCodeState.ViewState.Error(
//                            message = R.string.generic_error_message.asText(),
//                        ),
//                    )
//                }
//            }

//            is DataState.Loaded -> {
//                mutableStateFlow.update {
//                    it.copy(
//                        viewState = dataState
//                            .data
//                            ?.toViewState()
//                            ?: ViewAsQrCodeState.ViewState.Error(
//                                message = R.string.generic_error_message.asText(),
//                            ),
//                    )
//                }
//            }

//            DataState.Loading -> {
//                mutableStateFlow.update {
//                    it.copy(viewState = ViewAsQrCodeState.ViewState.Loading)
//                }
//            }
//

//            is DataState.Pending -> {
//                mutableStateFlow.update {
//                    it.copy(
//                        viewState = dataState
//                            .data
//                            ?.toViewState()
//                            ?: ViewAsQrCodeState.ViewState.Error(
//                                message = R.string.generic_error_message.asText(),
//                            ),
//                    )
//                }
//            }
        }
    }


    private fun handleQrCodeTypeSelect(action: ViewAsQrCodeAction.QrCodeTypeSelect) {
        mutableStateFlow.update {
            it.copy(
                selectedQrCodeType = action.qrCodeType,
                qrCodeTypeFields = action.qrCodeType.fields
            )
        }
//        val currentState = state as? ViewAsQrCodeState.Content ?: return
//        val cipher = currentState.cipher
//
//
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

    private fun cipherFieldsFor(cipherType: VaultItemCipherType) :List<Text> = when(cipherType){
        VaultItemCipherType.LOGIN -> listOf(
            R.string.name.asText(),
            R.string.username.asText(),
            R.string.password.asText(),
            R.string.notes.asText(),
            )
        VaultItemCipherType.CARD -> listOf(

            R.string.cardholder_name.asText(),
            R.string.number.asText(),
            //TODO finish
        )
        VaultItemCipherType.IDENTITY -> listOf(
            R.string.title.asText(),
            R.string.first_name.asText(),
            //TODO finish
        )
        VaultItemCipherType.SECURE_NOTE -> listOf(
            R.string.name.asText(),
            R.string.notes.asText(),
        )
        VaultItemCipherType.SSH_KEY -> listOf(
            R.string.public_key.asText(),
            //TODO finish
        )
    }

}


/**
 * Represents the state for viewing attachments.
 */
@Parcelize
data class ViewAsQrCodeState(
    val cipherId: String,
    val cipherType: VaultItemCipherType,
    //        val qrCodeBitmap: Bitmap,
    val selectedQrCodeType: QrCodeType,
    val qrCodeTypes: List<QrCodeType>,
    val qrCodeTypeFields: Map<String, QrCodeTypeField>,
    @IgnoredOnParcel
    val cipherFields: List<Text> =  emptyList(),
    @IgnoredOnParcel
    val cipher: CipherView? = null, //TODO do we need to use null?
    ) : Parcelable

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
