package com.x8bit.bitwarden.ui.vault.feature.viewasqrcode

//import com.x8bit.bitwarden.ui.vault.feature.viewasqrcode.util.toViewState
import android.graphics.Bitmap
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
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.SELECT_TEXT
import com.x8bit.bitwarden.ui.vault.feature.viewasqrcode.model.QrCodeType
import com.x8bit.bitwarden.ui.vault.feature.viewasqrcode.model.QrCodeTypeField
import com.x8bit.bitwarden.ui.vault.feature.viewasqrcode.util.QrCodeGenerator
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
private const val DEFAULT_QR_CODE_DATA = "https://bitwarden.com"

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
            qrCodeBitmap = QrCodeGenerator.generateQrCodeBitmap(DEFAULT_QR_CODE_DATA),
            selectedQrCodeType = selectedQrCodeType,
            qrCodeTypes = qrCodeTypes,
            qrCodeTypeFields = selectedQrCodeType.fields,
            cipherFields = emptyList(),
            cipher = null,
            qrCodeContent = DEFAULT_QR_CODE_DATA,
//            viewState = ViewAsQrCodeState.ViewState.Loading,
//            dialogState = null,
        )
    },
) {
    private val args = ViewAsQrCodeArgs(savedStateHandle)

    init {
        //TODO get args.vaultItemCipherType and auto-map
//        mutableStateFlow.update {
//            it.copy(
//                cipherFields = cipherFieldsFor(it.cipherType, null),
//            )
//        }
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
                val cipher = dataState.data
                val cipherFields = cipherFieldsFor(state.cipherType, cipher)

                val updatedQrCodeFields = autoMapFields(
                    state.qrCodeTypeFields,
                    state.cipherType,
                    cipher
                )

                mutableStateFlow.update {
                    it.copy(
                        cipher = cipher,
                        cipherFields = cipherFields,
                        qrCodeTypeFields = updatedQrCodeFields
                    )
                }
            }

            //TODO do we need to handle these?
            is DataState.Error -> {}
            is DataState.Loading -> {}
            is DataState.NoNetwork -> {}
            is DataState.Pending -> {}
        }
    }

    private fun handleQrCodeTypeSelect(action: ViewAsQrCodeAction.QrCodeTypeSelect) {

        val updatedFields = autoMapFields(
            action.qrCodeType.fields,
            state.cipherType,
            state.cipher
        )

        setCipherValues(state.cipher, state.selectedQrCodeType, updatedFields)

        val updatedQrCodeContent = if (validateRequiredFields(updatedFields))
            QrCodeGenerator.createContentFor(state.selectedQrCodeType, updatedFields)
        else
            DEFAULT_QR_CODE_DATA

        val updatedQrCodeBitmap = QrCodeGenerator.generateQrCodeBitmap(updatedQrCodeContent)

        mutableStateFlow.update {
            it.copy(
                selectedQrCodeType = action.qrCodeType,
                qrCodeTypeFields = updatedFields,
                qrCodeBitmap = updatedQrCodeBitmap,
                qrCodeContent = updatedQrCodeContent,
            )
        }
    }

    private fun autoMapFields(
        qrCodeTypeFields: List<QrCodeTypeField>,
        cipherType: VaultItemCipherType,
        cipher: CipherView?,
    ): List<QrCodeTypeField> {
        return qrCodeTypeFields.map { field ->
            field.copy(value = autoMapField(field, cipherType, cipher))
        }
    }

    private fun handleFieldValueChange(action: ViewAsQrCodeAction.FieldValueChange) {
        val field = action.field
        val value = action.value

        // Create a Text object from the selected value
        val selectedText = value.asText()

        //TODO should we transition qrCodeTypeFields to a map again*2 and update using key?
        var updatedFields = state.qrCodeTypeFields.map { currentField ->
            if (currentField.key == field.key) {
                currentField.copy(value = selectedText)
            } else {
                currentField
            }
        }

        updatedFields = setCipherValues(state.cipher, state.selectedQrCodeType, updatedFields)

        val updatedQrCodeContent = if (validateRequiredFields(updatedFields))
            QrCodeGenerator.createContentFor(state.selectedQrCodeType, updatedFields)
        else
            DEFAULT_QR_CODE_DATA

        val updatedQrCodeBitmap = QrCodeGenerator.generateQrCodeBitmap(updatedQrCodeContent)
        mutableStateFlow.update {
            it.copy(
                qrCodeTypeFields = updatedFields,
                qrCodeBitmap = updatedQrCodeBitmap,
                qrCodeContent = updatedQrCodeContent
            )
        }
    }

    private fun setCipherValues(
        cipher: CipherView?,
        qrCodeType: QrCodeType,
        qrCodeTypeFields: List<QrCodeTypeField>,
    ): List<QrCodeTypeField> {
        if (cipher == null) {
            return qrCodeTypeFields
        }

        //TODO figure a way to reuse autoMapField code, by returning (field Text + Cipher value) perhaps
        return qrCodeTypeFields.map { field ->
            field.copy(cipherValue = getCipherValueForField(cipher, field))
        }
    }

    private fun getCipherValueForField(cipher: CipherView, qrCodeField: QrCodeTypeField): String {
        //TODO refactor - currently failing because asText() creates a different object
        return when (qrCodeField.value) {
            R.string.name.asText() -> cipher.name
            R.string.username.asText() -> cipher.login?.username ?: ""
            R.string.password.asText() -> cipher.login?.username ?: ""
            R.string.notes.asText() -> cipher.notes ?: ""
            "Custom: SSID".asText() -> cipher.fields?.find { it.name == "Custom: SSID" }?.value
                ?: ""

            else -> "TODO()" //TODO
        }
    }

    private fun validateRequiredFields(qrCodeTypeFields: List<QrCodeTypeField>): Boolean {
        return true // TODO
    }

    private fun autoMapField(
        qrCodeTypeField: QrCodeTypeField,
        cipherType: VaultItemCipherType,
        cipher: CipherView?,
    ): Text {
        val defaultText = SELECT_TEXT
        return when (qrCodeTypeField.key) {
            "ssid" -> when (cipherType) {
                VaultItemCipherType.LOGIN -> automapSsidToLoginItem(cipher, defaultText)
                else -> defaultText
            }

            "password" -> when (cipherType) {
                VaultItemCipherType.LOGIN -> automapPasswordToLoginItem(cipher, defaultText)
                else -> defaultText
            }

            //TODO automap everything else
            else -> defaultText

        }
    }

    private fun automapPasswordToLoginItem(cipher: CipherView?, defaultText: Text): Text =
        if (cipher?.login?.password?.isNotEmpty() == true) R.string.password.asText() else defaultText

    private fun automapSsidToLoginItem(
        cipher: CipherView?,
        defaultText: Text,
    ): Text = if (cipher?.login?.username?.isNotEmpty() == true)
        R.string.username.asText() //TODO transition cipherFieldsFor to a map and get field with key?
    else {
        val customSsid = cipher?.fields?.find { it.name == "Custom: SSID" }
        if (customSsid?.value?.isNotEmpty() == true)
            "Custom: SSID".asText()
        else
            defaultText
    }

    //TODO create list with common fields first like SELECT_TEXT
    private fun cipherFieldsFor(cipherType: VaultItemCipherType, cipher: CipherView?): List<Text> {
        //TODO add additional cipher fields like web links and custom fields
        //TODO filter base list depending on the cipher data
        return when (cipherType) {
            VaultItemCipherType.LOGIN -> listOf(
                SELECT_TEXT,
                R.string.name.asText(),
                R.string.username.asText(),
                R.string.password.asText(),
                R.string.notes.asText(),
            )

            VaultItemCipherType.CARD -> listOf(
                SELECT_TEXT,
                R.string.cardholder_name.asText(),
                R.string.number.asText(),
                //TODO finish
            )

            VaultItemCipherType.IDENTITY -> listOf(
                SELECT_TEXT,
                R.string.title.asText(),
                R.string.first_name.asText(),
                //TODO finish
            )

            VaultItemCipherType.SECURE_NOTE -> listOf(
                SELECT_TEXT,
                R.string.name.asText(),
                R.string.notes.asText(),
            )

            VaultItemCipherType.SSH_KEY -> listOf(
                SELECT_TEXT,
                R.string.public_key.asText(),
                //TODO finish
            )
        }
    }

}

/**
 * Represents the state for viewing attachments.
 */
@Parcelize
data class ViewAsQrCodeState(
    val cipherId: String,
    val cipherType: VaultItemCipherType,
    val qrCodeBitmap: Bitmap,
    val selectedQrCodeType: QrCodeType,
    val qrCodeTypes: List<QrCodeType>,
    val qrCodeTypeFields: List<QrCodeTypeField>,
    @IgnoredOnParcel
    val cipherFields: List<Text> = emptyList(),
    @IgnoredOnParcel
    val cipher: CipherView? = null,
    val qrCodeContent: String = "",
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
    data class FieldValueChange(val field: QrCodeTypeField, val value: String) :
        ViewAsQrCodeAction()

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
