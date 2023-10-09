@file:Suppress("TooManyFunctions")

package com.x8bit.bitwarden.ui.tools.feature.generator

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Passcode
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Passcode.PasscodeType.Passphrase
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Passcode.PasscodeType.Passphrase.Companion.PASSPHRASE_MAX_NUMBER_OF_WORDS
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Passcode.PasscodeType.Passphrase.Companion.PASSPHRASE_MIN_NUMBER_OF_WORDS
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Passcode.PasscodeType.Password
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Passcode.PasscodeTypeOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.lang.Integer.max
import java.lang.Integer.min
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * ViewModel responsible for handling user interactions in the generator screen.
 *
 * This ViewModel processes UI actions, manages the state of the generator screen,
 * and provides data for the UI to render. It extends a `BaseViewModel` and works
 * with a `SavedStateHandle` for state restoration.
 *
 * @property savedStateHandle Handles the saved state of this ViewModel.
 */
@HiltViewModel
class GeneratorViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
) : BaseViewModel<GeneratorState, GeneratorEvent, GeneratorAction>(
    initialState = savedStateHandle[KEY_STATE] ?: INITIAL_STATE,
) {

    //region Initialization and Overrides

    init {
        viewModelScope.launch {
            stateFlow.onEach { savedStateHandle[KEY_STATE] = it }.launchIn(viewModelScope)
        }
    }

    override fun handleAction(action: GeneratorAction) {
        when (action) {
            is GeneratorAction.RegenerateClick -> {
                handleRegenerationClick()
            }

            is GeneratorAction.CopyClick -> {
                handleCopyClick()
            }

            is GeneratorAction.MainTypeOptionSelect -> {
                handleMainTypeOptionSelect(action)
            }

            is GeneratorAction.MainType.Passcode.PasscodeTypeOptionSelect -> {
                handlePasscodeTypeOptionSelect(action)
            }

            is GeneratorAction.MainType.Passcode.PasscodeType.Passphrase -> {
                handlePassphraseSpecificAction(action)
            }
        }
    }

    //endregion Initialization and Overrides

    //region Generated Field Handlers

    private fun handleRegenerationClick() {
        mutableStateFlow.update { currentState ->
            currentState.copy(
                // TODO(BIT-277): Replace placeholder text with function to generate new text
                generatedText = currentState.generatedText.reversed(),
            )
        }
    }

    private fun handleCopyClick() {
        viewModelScope.launch {
            sendEvent(
                event = GeneratorEvent.ShowToast(
                    message = "Copied",
                ),
            )
        }
    }

    //endregion Generated Field Handlers

    //region Main Type Option Handlers

    private fun handleMainTypeOptionSelect(action: GeneratorAction.MainTypeOptionSelect) {
        when (action.mainTypeOption) {
            GeneratorState.MainTypeOption.PASSWORD -> handleSwitchToPasscode()
            GeneratorState.MainTypeOption.USERNAME -> handleSwitchToUsername()
        }
    }

    private fun handleSwitchToPasscode() {
        mutableStateFlow.update { currentState ->
            currentState.copy(
                selectedType = Passcode(),
            )
        }
    }

    private fun handleSwitchToUsername() {
        mutableStateFlow.update { currentState ->
            currentState.copy(
                selectedType = GeneratorState.MainType.Username,
            )
        }
    }

    //endregion Main Type Option Handlers

    //region Passcode Type Handlers

    private fun handlePasscodeTypeOptionSelect(
        action: GeneratorAction.MainType.Passcode.PasscodeTypeOptionSelect,
    ) {
        when (action.passcodeTypeOption) {
            PasscodeTypeOption.PASSWORD -> handleSwitchToPasswordType()
            PasscodeTypeOption.PASSPHRASE -> handleSwitchToPassphraseType()
        }
    }

    private fun handleSwitchToPasswordType() {
        mutableStateFlow.update { currentState ->
            currentState.copy(
                selectedType = Passcode(
                    selectedType = Password(),
                ),
            )
        }
    }

    private fun handleSwitchToPassphraseType() {
        mutableStateFlow.update { currentState ->
            currentState.copy(
                selectedType = Passcode(
                    selectedType = Passphrase(),
                ),
            )
        }
    }

    //endregion Passcode Type Handlers

    //region Passphrase Specific Handlers

    private fun handlePassphraseSpecificAction(
        action: GeneratorAction.MainType.Passcode.PasscodeType.Passphrase,
    ) {
        when (action) {
            is GeneratorAction.MainType.Passcode.PasscodeType.Passphrase.NumWordsCounterChange,
            -> {
                handleNumWordsCounterChange(action)
            }

            is GeneratorAction.MainType.Passcode.PasscodeType.Passphrase.ToggleCapitalizeChange,
            -> {
                handleToggleCapitalizeChange(action)
            }

            is GeneratorAction.MainType.Passcode.PasscodeType.Passphrase.ToggleIncludeNumberChange,
            -> {
                handleToggleIncludeNumberChange(action)
            }

            is GeneratorAction.MainType.Passcode.PasscodeType.Passphrase.WordSeparatorTextChange,
            -> {
                handleWordSeparatorTextInputChange(action)
            }
        }
    }

    private fun handleToggleCapitalizeChange(
        action: GeneratorAction.MainType.Passcode.PasscodeType.Passphrase.ToggleCapitalizeChange,
    ) {
        updatePassphraseType { currentPassphraseType ->
            currentPassphraseType.copy(
                capitalize = action.capitalize,
            )
        }
    }

    private fun handleToggleIncludeNumberChange(
        action: GeneratorAction.MainType.Passcode.PasscodeType.Passphrase.ToggleIncludeNumberChange,
    ) {
        updatePassphraseType { currentPassphraseType ->
            currentPassphraseType.copy(
                includeNumber = action.includeNumber,
            )
        }
    }

    private fun handleNumWordsCounterChange(
        action: GeneratorAction.MainType.Passcode.PasscodeType.Passphrase.NumWordsCounterChange,
    ) {
        updatePassphraseType { passphraseType ->
            val newNumWords = max(
                PASSPHRASE_MIN_NUMBER_OF_WORDS,
                min(PASSPHRASE_MAX_NUMBER_OF_WORDS, action.numWords),
            )
            passphraseType.copy(numWords = newNumWords)
        }
    }

    private fun handleWordSeparatorTextInputChange(
        action: GeneratorAction.MainType.Passcode.PasscodeType.Passphrase.WordSeparatorTextChange,
    ) {
        updatePassphraseType { passphraseType ->
            val newWordSeparator =
                action.wordSeparator
            passphraseType.copy(wordSeparator = newWordSeparator)
        }
    }

    //endregion Passphrase Specific Handlers

    //region Utility Functions

    private inline fun updateGeneratorMainTypePassword(
        crossinline block: (Passcode) -> Passcode,
    ) {
        mutableStateFlow.update { currentState ->
            val currentSelectedType = currentState.selectedType
            if (currentSelectedType !is Passcode) return@update currentState

            val updatedPasscode = block(currentSelectedType)

            // TODO(BIT-277): Replace placeholder text with function to generate new text
            val newText = currentState.generatedText.reversed()

            currentState.copy(selectedType = updatedPasscode, generatedText = newText)
        }
    }

    private inline fun updatePassphraseType(
        crossinline block: (Passphrase) -> Passphrase,
    ) {
        updateGeneratorMainTypePassword { currentSelectedType ->
            val currentPasswordType = currentSelectedType.selectedType
            if (currentPasswordType !is Passphrase) {
                return@updateGeneratorMainTypePassword currentSelectedType
            }
            currentSelectedType.copy(selectedType = block(currentPasswordType))
        }
    }

    //endregion Utility Functions

    companion object {
        private const val PLACEHOLDER_GENERATED_TEXT = "Placeholder"

        val INITIAL_STATE: GeneratorState = GeneratorState(
            generatedText = PLACEHOLDER_GENERATED_TEXT,
            selectedType = Passcode(
                // TODO (BIT-634): Update the initial state to Password
                selectedType = Passphrase(),
            ),
        )
    }
}

/**
 * Represents the state of the generator, maintaining the generated text and the
 * selected type along with the options for the type of generation (PASSWORD, USERNAME).
 *
 * @param generatedText The text that is generated based on the selected options.
 * @param selectedType The currently selected main type for generating text.
 */
@Parcelize
data class GeneratorState(
    val generatedText: String,
    val selectedType: MainType,
) : Parcelable {

    /**
     * Provides a list of available main types for the generator.
     */
    val typeOptions: List<MainTypeOption>
        get() = MainTypeOption.values().toList()

    /**
     * Enum representing the main type options for the generator, such as PASSWORD and USERNAME.
     *
     * @property labelRes The resource ID of the string that represents the label of each type.
     */
    enum class MainTypeOption(val labelRes: Int) {
        PASSWORD(R.string.password),
        USERNAME(R.string.username),
    }

    /**
     * A sealed class representing the main types that can be selected in the generator,
     * encapsulating the different configurations and properties each main type has.
     */
    @Parcelize
    sealed class MainType : Parcelable {

        /**
         * Represents the resource ID for the display string. This is an abstract property
         * that must be overridden by each subclass to provide the appropriate string resource ID
         * for display purposes.
         */
        abstract val displayStringResId: Int

        /**
         * Represents the Passcode main type, allowing the user to specify the kind of passcode,
         * such as a Password or a Passphrase, and configure their respective properties.
         *
         * @property selectedType The currently selected PasscodeType
         */
        @Parcelize
        data class Passcode(
            val selectedType: PasscodeType = Password(),
        ) : MainType(), Parcelable {
            override val displayStringResId: Int
                get() = MainTypeOption.PASSWORD.labelRes

            /**
             * Enum representing the types of passcodes,
             * allowing for different passcode configurations.
             *
             * @property labelRes The ID of the string that represents the label for each type.
             */
            enum class PasscodeTypeOption(val labelRes: Int) {
                PASSWORD(R.string.password),
                PASSPHRASE(R.string.passphrase),
            }

            /**
             * A sealed class representing the different types of PASSWORD,
             * such as standard Password and Passphrase, each with its own properties.
             */
            @Parcelize
            sealed class PasscodeType : Parcelable {

                /**
                 * Represents the resource ID for the display string specific to each
                 * PasscodeType subclass. Every subclass of PasscodeType must override
                 * this property to provide the appropriate string resource ID for
                 * its display string.
                 */
                abstract val displayStringResId: Int

                /**
                 * Represents a standard PASSWORD type, with a specified length.
                 *
                 * @property length The length of the generated password.
                 */
                @Parcelize
                data class Password(
                    val length: Int = DEFAULT_PASSWORD_LENGTH,
                ) : PasscodeType(), Parcelable {
                    override val displayStringResId: Int
                        get() = PasscodeTypeOption.PASSWORD.labelRes

                    companion object {
                        const val DEFAULT_PASSWORD_LENGTH: Int = 10
                    }
                }

                /**
                 * Represents a Passphrase type, configured with number of words, word separator,
                 * capitalization, and inclusion of numbers.
                 *
                 * @property numWords The number of words in the passphrase.
                 * @property wordSeparator The character used to separate words in the passphrase.
                 * @property capitalize Whether to capitalize the first letter of each word.
                 * @property includeNumber Whether to include a numbers in the passphrase.
                 */
                @Parcelize
                data class Passphrase(
                    val numWords: Int = DEFAULT_NUM_WORDS,
                    val wordSeparator: Char? = DEFAULT_PASSPHRASE_SEPARATOR,
                    val capitalize: Boolean = false,
                    val includeNumber: Boolean = false,
                ) : PasscodeType(), Parcelable {
                    override val displayStringResId: Int
                        get() = PasscodeTypeOption.PASSPHRASE.labelRes

                    companion object {
                        private const val DEFAULT_PASSPHRASE_SEPARATOR: Char = '-'
                        private const val DEFAULT_NUM_WORDS: Int = 3

                        const val PASSPHRASE_MIN_NUMBER_OF_WORDS: Int = 3
                        const val PASSPHRASE_MAX_NUMBER_OF_WORDS: Int = 20
                    }
                }
            }
        }

        /**
         * Represents the USERNAME main type, holding the configuration
         * and properties for generating usernames.
         */
        @Parcelize
        data object Username : MainType() {
            override val displayStringResId: Int
                get() = MainTypeOption.USERNAME.labelRes
        }
    }
}

/**
 * Represents an action in the generator feature.
 *
 * This sealed class serves as a type for defining all the possible actions within
 * the generator feature, ensuring type safety and clear, structured action definitions.
 */
sealed class GeneratorAction {

    /**
     * Represents the action to regenerate a new passcode or username.
     */
    data object RegenerateClick : GeneratorAction()

    /**
     * Represents the action to copy the generated field.
     */
    data object CopyClick : GeneratorAction()

    /**
     * Represents the action of selecting a main type option.
     *
     * @property mainTypeOption The selected main type option.
     */
    data class MainTypeOptionSelect(
        val mainTypeOption: GeneratorState.MainTypeOption,
    ) : GeneratorAction()

    /**
     * Represents actions related to the [GeneratorState.MainType] in the generator feature.
     */
    sealed class MainType : GeneratorAction() {

        /**
         * Represents actions specifically related to [GeneratorState.MainType.Passcode].
         */
        sealed class Passcode : MainType() {

            /**
             * Represents the action of selecting a passcode type option.
             *
             * @property passcodeTypeOption The selected passcode type option.
             */
            data class PasscodeTypeOptionSelect(
                val passcodeTypeOption: PasscodeTypeOption,
            ) : Passcode()

            /**
             * Represents actions related to the different types of passcodes.
             */
            sealed class PasscodeType : Passcode() {

                /**
                 * Represents actions specifically related to passwords, a subtype of passcode.
                 */
                sealed class Password : PasscodeType()

                /**
                 * Represents actions specifically related to passphrases, a subtype of passcode.
                 */
                sealed class Passphrase : PasscodeType() {

                    /**
                     * Fired when the number of words counter is changed.
                     *
                     * @property numWords The new value for the number of words.
                     */
                    data class NumWordsCounterChange(val numWords: Int) : Passphrase()

                    /**
                     * Fired when the word separator text input is changed.
                     *
                     * @property wordSeparator The new word separator text.
                     */
                    data class WordSeparatorTextChange(val wordSeparator: Char?) : Passphrase()

                    /**
                     * Fired when the "capitalize" toggle is changed.
                     *
                     * @property capitalize The new value of the "capitalize" toggle.
                     */
                    data class ToggleCapitalizeChange(val capitalize: Boolean) : Passphrase()

                    /**
                     * Fired when the "include number" toggle is changed.
                     *
                     * @property includeNumber The new value of the "include number" toggle.
                     */
                    data class ToggleIncludeNumberChange(val includeNumber: Boolean) : Passphrase()
                }
            }
        }

        /**
         * Represents actions related to the [GeneratorState.MainType.Username] in the generator.
         *
         * This sealed class serves as a placeholder for future extensions
         * related to the username actions in the generator.
         */
        sealed class Username : MainType()
    }
}

/**
 * Sealed class representing the different types of UI events that can be triggered.
 *
 * These events are meant to represent various types of user interactions within
 * the generator screen.
 */
sealed class GeneratorEvent {

    /**
     * Shows a toast with the given [message].
     */
    data class ShowToast(val message: String) : GeneratorEvent()
}
