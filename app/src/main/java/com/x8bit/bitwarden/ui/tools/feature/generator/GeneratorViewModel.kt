@file:Suppress("TooManyFunctions")

package com.x8bit.bitwarden.ui.tools.feature.generator

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.core.PassphraseGeneratorRequest
import com.bitwarden.core.PasswordGeneratorRequest
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.tools.generator.repository.GeneratorRepository
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedForwardedServiceUsernameResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedPassphraseResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedPasswordResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.PasscodeGenerationOptions
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Passcode
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Passcode.PasscodeType.Passphrase
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Passcode.PasscodeType.Password
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Passcode.PasscodeTypeOption
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Username
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Username.UsernameType.CatchAllEmail
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias.ServiceType.AddyIo
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias.ServiceType.DuckDuckGo
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias.ServiceType.FastMail
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias.ServiceType.FirefoxRelay
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias.ServiceType.SimpleLogin
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Username.UsernameType.PlusAddressedEmail
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Username.UsernameType.RandomWord
import com.x8bit.bitwarden.ui.tools.feature.generator.util.toUsernameGeneratorRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
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
@Suppress("LargeClass")
@HiltViewModel
class GeneratorViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val generatorRepository: GeneratorRepository,
) : BaseViewModel<GeneratorState, GeneratorEvent, GeneratorAction>(
    initialState = savedStateHandle[KEY_STATE] ?: INITIAL_STATE,
) {

    //region Initialization and Overrides

    private var generateTextJob: Job = Job().apply { complete() }

    init {
        stateFlow.onEach { savedStateHandle[KEY_STATE] = it }.launchIn(viewModelScope)
        when (val selectedType = mutableStateFlow.value.selectedType) {
            is Passcode -> loadPasscodeOptions(selectedType)
            is Username -> loadUsernameOptions(selectedType)
        }
    }

    @Suppress("MaxLineLength", "LongMethod")
    override fun handleAction(action: GeneratorAction) {
        when (action) {
            is GeneratorAction.PasswordHistoryClick -> {
                handlePasswordHistoryClick()
            }

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

            is GeneratorAction.MainType.Passcode.PasscodeType.Password -> {
                handlePasswordSpecificAction(action)
            }

            is GeneratorAction.MainType.Passcode.PasscodeType.Passphrase -> {
                handlePassphraseSpecificAction(action)
            }

            is GeneratorAction.Internal.UpdateGeneratedPasswordResult -> {
                handleUpdateGeneratedPasswordResult(action)
            }

            is GeneratorAction.Internal.UpdateGeneratedPassphraseResult -> {
                handleUpdateGeneratedPassphraseResult(action)
            }

            is GeneratorAction.Internal.UpdateGeneratedUsernameResult -> {
                handleUpdateGeneratedUsernameResult(action)
            }

            is GeneratorAction.MainType.Username.UsernameTypeOptionSelect -> {
                handleUsernameTypeOptionSelect(action)
            }

            is GeneratorAction.MainType.Username.UsernameType.ForwardedEmailAlias.ServiceTypeOptionSelect -> {
                handleServiceTypeOptionSelect(action)
            }

            is GeneratorAction.MainType.Username.UsernameType.ForwardedEmailAlias.AddyIo -> {
                handleAddyIoSpecificAction(action)
            }

            is GeneratorAction.MainType.Username.UsernameType.ForwardedEmailAlias.DuckDuckGo.ApiKeyTextChange -> {
                handleDuckDuckGoTextInputChange(action)
            }

            is GeneratorAction.MainType.Username.UsernameType.ForwardedEmailAlias.FastMail.ApiKeyTextChange -> {
                handleFastMailTextInputChange(action)
            }

            is GeneratorAction.MainType.Username.UsernameType.ForwardedEmailAlias.FirefoxRelay.AccessTokenTextChange -> {
                handleFirefoxRelayTextInputChange(action)
            }

            is GeneratorAction.MainType.Username.UsernameType.ForwardedEmailAlias.SimpleLogin.ApiKeyTextChange -> {
                handleSimpleLoginTextInputChange(action)
            }

            is GeneratorAction.MainType.Username.UsernameType.PlusAddressedEmail.EmailTextChange -> {
                handlePlusAddressedEmailTextInputChange(action)
            }

            is GeneratorAction.MainType.Username.UsernameType.CatchAllEmail.DomainTextChange -> {
                handleCatchAllEmailTextInputChange(action)
            }

            is GeneratorAction.MainType.Username.UsernameType.RandomWord -> {
                handleRandomWordSpecificAction(action)
            }
        }
    }

    //endregion Initialization and Overrides

    //region Top Level Handlers

    private fun handlePasswordHistoryClick() {
        sendEvent(GeneratorEvent.NavigateToPasswordHistory)
    }

    //endregion Top Level Handlers

    //region Generation Handlers

    private fun loadPasscodeOptions(selectedType: Passcode) {
        val options = generatorRepository.getPasscodeGenerationOptions()
            ?: generatePasscodeDefaultOptions()

        when (selectedType.selectedType) {
            is Passphrase -> {
                val passphrase = Passphrase(
                    numWords = options.numWords,
                    wordSeparator = options.wordSeparator.toCharArray().first(),
                    capitalize = options.allowCapitalize,
                    includeNumber = options.allowIncludeNumber,
                )
                updateGeneratorMainType {
                    Passcode(selectedType = passphrase)
                }
            }

            is Password -> {
                val password = Password(
                    length = options.length,
                    useCapitals = options.hasUppercase,
                    useLowercase = options.hasLowercase,
                    useNumbers = options.hasNumbers,
                    useSpecialChars = options.allowSpecial,
                    minNumbers = options.minNumber,
                    minSpecial = options.minSpecial,
                    avoidAmbiguousChars = options.allowAmbiguousChar,
                )
                updateGeneratorMainType {
                    Passcode(selectedType = password)
                }
            }
        }
    }

    private fun loadUsernameOptions(selectedType: Username) {
        mutableStateFlow.update {
            it.copy(selectedType = selectedType)
        }
        // TODO: Generate different username types. Plus addressed email: BIT-655
    }

    private fun savePasswordOptionsToDisk(password: Password) {
        val options = generatorRepository
            .getPasscodeGenerationOptions() ?: generatePasscodeDefaultOptions()
        val newOptions = options.copy(
            length = password.length,
            allowAmbiguousChar = password.avoidAmbiguousChars,
            hasNumbers = password.useNumbers,
            minNumber = password.minNumbers,
            hasUppercase = password.useCapitals,
            hasLowercase = password.useLowercase,
            allowSpecial = password.useSpecialChars,
            minSpecial = password.minSpecial,
        )
        generatorRepository.savePasscodeGenerationOptions(newOptions)
    }

    private fun savePassphraseOptionsToDisk(passphrase: Passphrase) {
        val options = generatorRepository
            .getPasscodeGenerationOptions() ?: generatePasscodeDefaultOptions()
        val newOptions = options.copy(
            numWords = passphrase.numWords,
            wordSeparator = passphrase.wordSeparator.toString(),
            allowCapitalize = passphrase.capitalize,
            allowIncludeNumber = passphrase.includeNumber,
        )
        generatorRepository.savePasscodeGenerationOptions(newOptions)
    }

    private fun generatePasscodeDefaultOptions(): PasscodeGenerationOptions {
        val defaultPassword = Password()
        val defaultPassphrase = Passphrase()

        return PasscodeGenerationOptions(
            length = defaultPassword.length,
            allowAmbiguousChar = defaultPassword.avoidAmbiguousChars,
            hasNumbers = defaultPassword.useNumbers,
            minNumber = defaultPassword.minNumbers,
            hasUppercase = defaultPassword.useCapitals,
            hasLowercase = defaultPassword.useLowercase,
            allowSpecial = defaultPassword.useSpecialChars,
            minSpecial = defaultPassword.minSpecial,
            allowCapitalize = defaultPassphrase.capitalize,
            allowIncludeNumber = defaultPassphrase.includeNumber,
            wordSeparator = defaultPassphrase.wordSeparator.toString(),
            numWords = defaultPassphrase.numWords,
        )
    }

    private suspend fun generatePassword(password: Password) {
        val request = PasswordGeneratorRequest(
            lowercase = password.useLowercase,
            uppercase = password.useCapitals,
            numbers = password.useNumbers,
            special = password.useSpecialChars,
            length = password.length.toUByte(),
            avoidAmbiguous = password.avoidAmbiguousChars,
            minLowercase = null,
            minUppercase = null,
            minNumber = null,
            minSpecial = null,
        )
        val shouldSave = !password.isUserInteracting

        val result = generatorRepository.generatePassword(request, shouldSave)
        sendAction(GeneratorAction.Internal.UpdateGeneratedPasswordResult(result))
    }

    private suspend fun generatePassphrase(passphrase: Passphrase) {
        val request = PassphraseGeneratorRequest(
            numWords = passphrase.numWords.toUByte(),
            wordSeparator = passphrase.wordSeparator.toString(),
            capitalize = passphrase.capitalize,
            includeNumber = passphrase.includeNumber,
        )

        val result = generatorRepository.generatePassphrase(request)
        sendAction(GeneratorAction.Internal.UpdateGeneratedPassphraseResult(result))
    }

    //endregion Generation Handlers

    //region Generated Field Handlers

    private fun handleRegenerationClick() {
        // Go through the update process with the current state to trigger a
        // regeneration of the generated text for the same state.
        updateGeneratorMainType(isManualRegeneration = true) { mutableStateFlow.value.selectedType }
    }

    private fun handleCopyClick() {
        sendEvent(GeneratorEvent.CopyTextToClipboard)
    }

    private fun handleUpdateGeneratedPasswordResult(
        action: GeneratorAction.Internal.UpdateGeneratedPasswordResult,
    ) {
        when (val result = action.result) {
            is GeneratedPasswordResult.Success -> {
                mutableStateFlow.update {
                    it.copy(generatedText = result.generatedString)
                }
            }

            GeneratedPasswordResult.InvalidRequest -> {
                sendEvent(GeneratorEvent.ShowSnackbar(R.string.an_error_has_occurred.asText()))
            }
        }
    }

    private fun handleUpdateGeneratedPassphraseResult(
        action: GeneratorAction.Internal.UpdateGeneratedPassphraseResult,
    ) {
        when (val result = action.result) {
            is GeneratedPassphraseResult.Success -> {
                mutableStateFlow.update {
                    it.copy(generatedText = result.generatedString)
                }
            }

            GeneratedPassphraseResult.InvalidRequest -> {
                sendEvent(GeneratorEvent.ShowSnackbar(R.string.an_error_has_occurred.asText()))
            }
        }
    }

    private fun handleUpdateGeneratedUsernameResult(
        action: GeneratorAction.Internal.UpdateGeneratedUsernameResult,
    ) {
        when (val result = action.result) {
            is GeneratedForwardedServiceUsernameResult.Success -> {
                mutableStateFlow.update {
                    it.copy(generatedText = result.generatedEmailAddress)
                }
            }

            GeneratedForwardedServiceUsernameResult.InvalidRequest -> {
                sendEvent(GeneratorEvent.ShowSnackbar(R.string.an_error_has_occurred.asText()))
            }
        }
    }

    //endregion Generated Field Handlers

    //region Main Type Option Handlers

    private fun handleMainTypeOptionSelect(action: GeneratorAction.MainTypeOptionSelect) {
        when (action.mainTypeOption) {
            GeneratorState.MainTypeOption.PASSWORD -> loadPasscodeOptions(Passcode())
            GeneratorState.MainTypeOption.USERNAME -> loadUsernameOptions(Username())
        }
    }

    //endregion Main Type Option Handlers

    //region Passcode Type Handlers

    private fun handlePasscodeTypeOptionSelect(
        action: GeneratorAction.MainType.Passcode.PasscodeTypeOptionSelect,
    ) {
        when (action.passcodeTypeOption) {
            PasscodeTypeOption.PASSWORD -> loadPasscodeOptions(
                selectedType = Passcode(selectedType = Password()),
            )

            PasscodeTypeOption.PASSPHRASE -> loadPasscodeOptions(
                selectedType = Passcode(selectedType = Passphrase()),
            )
        }
    }

    //endregion Passcode Type Handlers

    //region Password Specific Handlers

    private fun handlePasswordSpecificAction(
        action: GeneratorAction.MainType.Passcode.PasscodeType.Password,
    ) {
        when (action) {
            is GeneratorAction.MainType.Passcode.PasscodeType.Password.SliderLengthChange,
            -> {
                handlePasswordLengthSliderChange(action)
            }

            is GeneratorAction.MainType.Passcode.PasscodeType.Password.ToggleCapitalLettersChange,
            -> {
                handleToggleCapitalLetters(action)
            }

            is GeneratorAction.MainType.Passcode.PasscodeType.Password.ToggleLowercaseLettersChange,
            -> {
                handleToggleLowercaseLetters(action)
            }

            is GeneratorAction.MainType.Passcode.PasscodeType.Password.ToggleNumbersChange,
            -> {
                handleToggleNumbers(action)
            }

            is GeneratorAction.MainType.Passcode.PasscodeType.Password
            .ToggleSpecialCharactersChange,
            -> {
                handleToggleSpecialChars(action)
            }

            is GeneratorAction.MainType.Passcode.PasscodeType.Password.MinNumbersCounterChange,
            -> {
                handleMinNumbersChange(action)
            }

            is GeneratorAction.MainType.Passcode.PasscodeType.Password.MinSpecialCharactersChange,
            -> {
                handleMinSpecialChange(action)
            }

            is GeneratorAction.MainType.Passcode.PasscodeType.Password
            .ToggleAvoidAmbigousCharactersChange,
            -> {
                handleToggleAmbiguousChars(action)
            }
        }
    }

    private fun handlePasswordLengthSliderChange(
        action: GeneratorAction.MainType.Passcode.PasscodeType.Password.SliderLengthChange,
    ) {
        val adjustedLength = action.length

        updatePasswordType { currentPasswordType ->
            currentPasswordType.copy(
                length = adjustedLength,
                isUserInteracting = action.isUserInteracting,
            )
        }
    }

    private fun handleToggleCapitalLetters(
        action: GeneratorAction.MainType.Passcode.PasscodeType.Password.ToggleCapitalLettersChange,
    ) {
        updatePasswordType { currentPasswordType ->
            currentPasswordType.copy(
                useCapitals = action.useCapitals,
            )
        }
    }

    private fun handleToggleLowercaseLetters(
        action: GeneratorAction.MainType.Passcode.PasscodeType.Password
        .ToggleLowercaseLettersChange,
    ) {
        updatePasswordType { currentPasswordType ->
            currentPasswordType.copy(useLowercase = action.useLowercase)
        }
    }

    private fun handleToggleNumbers(
        action: GeneratorAction.MainType.Passcode.PasscodeType.Password.ToggleNumbersChange,
    ) {
        updatePasswordType { currentPasswordType ->
            currentPasswordType.copy(useNumbers = action.useNumbers)
        }
    }

    private fun handleToggleSpecialChars(
        action: GeneratorAction.MainType.Passcode.PasscodeType.Password
        .ToggleSpecialCharactersChange,
    ) {
        updatePasswordType { currentPasswordType ->
            currentPasswordType.copy(useSpecialChars = action.useSpecialChars)
        }
    }

    private fun handleMinNumbersChange(
        action: GeneratorAction.MainType.Passcode.PasscodeType.Password.MinNumbersCounterChange,
    ) {
        updatePasswordType { currentPasswordType ->
            currentPasswordType.copy(minNumbers = action.minNumbers)
        }
    }

    private fun handleMinSpecialChange(
        action: GeneratorAction.MainType.Passcode.PasscodeType.Password.MinSpecialCharactersChange,
    ) {
        updatePasswordType { currentPasswordType ->
            currentPasswordType.copy(minSpecial = action.minSpecial)
        }
    }

    private fun handleToggleAmbiguousChars(
        action: GeneratorAction.MainType.Passcode.PasscodeType.Password
        .ToggleAvoidAmbigousCharactersChange,
    ) {
        updatePasswordType { currentPasswordType ->
            currentPasswordType.copy(
                avoidAmbiguousChars = action.avoidAmbiguousChars,
            )
        }
    }

    //endregion Password Specific Handlers

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
                handlePassphraseToggleCapitalizeChange(action)
            }

            is GeneratorAction.MainType.Passcode.PasscodeType.Passphrase.ToggleIncludeNumberChange,
            -> {
                handlePassphraseToggleIncludeNumberChange(action)
            }

            is GeneratorAction.MainType.Passcode.PasscodeType.Passphrase.WordSeparatorTextChange,
            -> {
                handleWordSeparatorTextInputChange(action)
            }
        }
    }

    private fun handlePassphraseToggleCapitalizeChange(
        action: GeneratorAction.MainType.Passcode.PasscodeType.Passphrase.ToggleCapitalizeChange,
    ) {
        updatePassphraseType { currentPassphraseType ->
            currentPassphraseType.copy(
                capitalize = action.capitalize,
            )
        }
    }

    private fun handlePassphraseToggleIncludeNumberChange(
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
            passphraseType.copy(numWords = action.numWords)
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

    //region Username Type Handlers

    private fun handleUsernameTypeOptionSelect(
        action: GeneratorAction.MainType.Username.UsernameTypeOptionSelect,
    ) {
        when (action.usernameTypeOption) {
            Username.UsernameTypeOption.PLUS_ADDRESSED_EMAIL -> loadUsernameOptions(
                selectedType = Username(selectedType = PlusAddressedEmail()),
            )

            Username.UsernameTypeOption.CATCH_ALL_EMAIL -> loadUsernameOptions(
                selectedType = Username(selectedType = CatchAllEmail()),
            )

            Username.UsernameTypeOption.FORWARDED_EMAIL_ALIAS -> loadUsernameOptions(
                selectedType = Username(selectedType = ForwardedEmailAlias()),
            )

            Username.UsernameTypeOption.RANDOM_WORD -> loadUsernameOptions(
                selectedType = Username(selectedType = RandomWord()),
            )
        }
    }

    //endregion Username Type Handlers

    //region Forwarded Email Alias Specific Handlers

    private fun handleServiceTypeOptionSelect(
        action: GeneratorAction
        .MainType
        .Username
        .UsernameType
        .ForwardedEmailAlias
        .ServiceTypeOptionSelect,
    ) {
        when (action.serviceTypeOption) {
            ForwardedEmailAlias.ServiceTypeOption.ADDY_IO -> updateForwardedEmailAliasType {
                ForwardedEmailAlias(selectedServiceType = AddyIo())
            }

            ForwardedEmailAlias.ServiceTypeOption.DUCK_DUCK_GO -> updateForwardedEmailAliasType {
                ForwardedEmailAlias(selectedServiceType = DuckDuckGo())
            }

            ForwardedEmailAlias.ServiceTypeOption.FAST_MAIL -> updateForwardedEmailAliasType {
                ForwardedEmailAlias(selectedServiceType = FastMail())
            }

            ForwardedEmailAlias.ServiceTypeOption.FIREFOX_RELAY -> updateForwardedEmailAliasType {
                ForwardedEmailAlias(selectedServiceType = FirefoxRelay())
            }

            ForwardedEmailAlias.ServiceTypeOption.SIMPLE_LOGIN -> updateForwardedEmailAliasType {
                ForwardedEmailAlias(selectedServiceType = SimpleLogin())
            }
        }
    }

    //endregion Forwarded Email Alias Specific Handlers

    //region Addy.Io Service Specific Handlers

    private fun handleAddyIoSpecificAction(
        action: GeneratorAction.MainType.Username.UsernameType.ForwardedEmailAlias.AddyIo,
    ) {
        when (action) {
            is GeneratorAction
            .MainType
            .Username
            .UsernameType
            .ForwardedEmailAlias
            .AddyIo
            .AccessTokenTextChange,
            -> {
                handleAddyIoAccessTokenTextChange(action)
            }

            is GeneratorAction
            .MainType
            .Username
            .UsernameType
            .ForwardedEmailAlias
            .AddyIo
            .DomainTextChange,
            -> {
                handleAddyIoDomainNameTextChange(action)
            }
        }
    }

    private fun handleAddyIoAccessTokenTextChange(
        action: GeneratorAction
        .MainType
        .Username
        .UsernameType
        .ForwardedEmailAlias
        .AddyIo
        .AccessTokenTextChange,
    ) {
        updateAddyIoServiceType { addyIoServiceType ->
            val newAccessToken = action.accessToken
            addyIoServiceType.copy(apiAccessToken = newAccessToken)
        }
    }

    private fun handleAddyIoDomainNameTextChange(
        action: GeneratorAction
        .MainType
        .Username
        .UsernameType
        .ForwardedEmailAlias
        .AddyIo
        .DomainTextChange,
    ) {
        updateAddyIoServiceType { addyIoServiceType ->
            val newDomain = action.domain
            addyIoServiceType.copy(domainName = newDomain)
        }
    }

    //endregion Addy.Io Service Specific Handlers

    //region DuckDuckGo Service Specific Handlers

    private fun handleDuckDuckGoTextInputChange(
        action: GeneratorAction
        .MainType
        .Username
        .UsernameType
        .ForwardedEmailAlias
        .DuckDuckGo
        .ApiKeyTextChange,
    ) {
        updateDuckDuckGoServiceType { duckDuckGoServiceType ->
            val newApiKey = action.apiKey
            duckDuckGoServiceType.copy(apiKey = newApiKey)
        }
    }

    //endregion DuckDuckGo Service Specific Handlers

    //region FastMail Service Specific Handlers

    private fun handleFastMailTextInputChange(
        action: GeneratorAction
        .MainType
        .Username
        .UsernameType
        .ForwardedEmailAlias
        .FastMail
        .ApiKeyTextChange,
    ) {
        updateFastMailServiceType { fastMailServiceType ->
            val newApiKey = action.apiKey
            fastMailServiceType.copy(apiKey = newApiKey)
        }
    }

    //endregion FastMail Service Specific Handlers

    //region FirefoxRelay Service Specific Handlers

    private fun handleFirefoxRelayTextInputChange(
        action: GeneratorAction
        .MainType
        .Username
        .UsernameType
        .ForwardedEmailAlias
        .FirefoxRelay
        .AccessTokenTextChange,
    ) {
        updateFirefoxRelayServiceType { firefoxRelayServiceType ->
            val newAccessToken = action.accessToken
            firefoxRelayServiceType.copy(apiAccessToken = newAccessToken)
        }
    }

    //endregion FirefoxRelay Service Specific Handlers

    //region SimpleLogin Service Specific Handlers

    private fun handleSimpleLoginTextInputChange(
        action: GeneratorAction
        .MainType
        .Username
        .UsernameType
        .ForwardedEmailAlias
        .SimpleLogin
        .ApiKeyTextChange,
    ) {
        updateSimpleLoginServiceType { simpleLoginServiceType ->
            val newApiKey = action.apiKey
            simpleLoginServiceType.copy(apiKey = newApiKey)
        }
    }

    //endregion SimpleLogin Service Specific Handlers

    //region Plus Addressed Email Specific Handlers

    private fun handlePlusAddressedEmailTextInputChange(
        action: GeneratorAction.MainType.Username.UsernameType.PlusAddressedEmail.EmailTextChange,
    ) {
        updatePlusAddressedEmailType { plusAddressedEmailType ->
            val newEmail = action.email
            plusAddressedEmailType.copy(email = newEmail)
        }
    }

    //endregion Plus Addressed Email Specific Handlers

    //region Catch-All Email Specific Handlers

    private fun handleCatchAllEmailTextInputChange(
        action: GeneratorAction.MainType.Username.UsernameType.CatchAllEmail.DomainTextChange,
    ) {
        updateCatchAllEmailType { catchAllEmailType ->
            val newDomain = action.domain
            catchAllEmailType.copy(domainName = newDomain)
        }
    }

    //endregion Catch-All Email Specific Handlers

    //region Random Word Specific Handlers

    private fun handleRandomWordSpecificAction(
        action: GeneratorAction.MainType.Username.UsernameType.RandomWord,
    ) {
        when (action) {
            is GeneratorAction.MainType.Username.UsernameType.RandomWord.ToggleCapitalizeChange -> {
                handleRandomWordToggleCapitalizeChange(action)
            }

            is GeneratorAction
            .MainType
            .Username
            .UsernameType
            .RandomWord
            .ToggleIncludeNumberChange,
            -> {
                handleRandomWordToggleIncludeNumberChange(action)
            }
        }
    }

    private fun handleRandomWordToggleCapitalizeChange(
        action: GeneratorAction.MainType.Username.UsernameType.RandomWord.ToggleCapitalizeChange,
    ) {
        updateRandomWordType { currentRandomWordType ->
            currentRandomWordType.copy(
                capitalize = action.capitalize,
            )
        }
    }

    private fun handleRandomWordToggleIncludeNumberChange(
        action: GeneratorAction.MainType.Username.UsernameType.RandomWord.ToggleIncludeNumberChange,
    ) {
        updateRandomWordType { currentRandomWordType ->
            currentRandomWordType.copy(
                includeNumber = action.includeNumber,
            )
        }
    }

    //endregion Random Word Specific Handlers

    //region Utility Functions

    private inline fun updateGeneratorMainType(
        isManualRegeneration: Boolean = false,
        crossinline block: (GeneratorState.MainType) -> GeneratorState.MainType?,
    ) {
        val currentSelectedType = mutableStateFlow.value.selectedType
        val updatedMainType = block(currentSelectedType) ?: return
        mutableStateFlow.update { it.copy(selectedType = updatedMainType) }

        generateTextJob.cancel()
        generateTextJob = viewModelScope.launch {
            when (updatedMainType) {
                is Passcode -> when (val selectedType = updatedMainType.selectedType) {
                    is Passphrase -> {
                        savePassphraseOptionsToDisk(selectedType)
                        generatePassphrase(selectedType)
                    }

                    is Password -> {
                        savePasswordOptionsToDisk(selectedType)
                        generatePassword(selectedType)
                    }
                }

                is Username -> when (val selectedType = updatedMainType.selectedType) {
                    is ForwardedEmailAlias -> {
                        if (isManualRegeneration) {
                            generateForwardedEmailAlias(selectedType)
                        }
                    }

                    is CatchAllEmail -> {
                        // TODO: Implement catch all email generation (BIT-1334)
                    }

                    is PlusAddressedEmail -> {
                        // TODO: Implement plus addressed email generation (BIT-1335)
                    }

                    is RandomWord -> {
                        // TODO: Implement random word generation (BIT-1336)
                    }
                }
            }
        }
    }

    private suspend fun generateForwardedEmailAlias(alias: ForwardedEmailAlias) {
        val request = alias.selectedServiceType?.toUsernameGeneratorRequest() ?: return
        val result = generatorRepository.generateForwardedServiceUsername(request)
        sendAction(GeneratorAction.Internal.UpdateGeneratedUsernameResult(result))
    }

    private inline fun updateGeneratorMainTypePasscode(
        crossinline block: (Passcode) -> Passcode,
    ) {
        updateGeneratorMainType {
            if (it !is Passcode) null else block(it)
        }
    }

    private inline fun updatePasswordType(
        crossinline block: (Password) -> Password,
    ) {
        updateGeneratorMainTypePasscode { currentSelectedType ->
            val currentPasswordType = currentSelectedType.selectedType
            if (currentPasswordType !is Password) {
                return@updateGeneratorMainTypePasscode currentSelectedType
            }

            val updatedPasswordType = currentPasswordType
                .let(block)
                .enforceAtLeastOneToggleOn()

            currentSelectedType.copy(selectedType = updatedPasswordType)
        }
    }

    private inline fun updatePassphraseType(
        crossinline block: (Passphrase) -> Passphrase,
    ) {
        updateGeneratorMainTypePasscode { currentSelectedType ->
            val currentPasswordType = currentSelectedType.selectedType
            if (currentPasswordType !is Passphrase) {
                return@updateGeneratorMainTypePasscode currentSelectedType
            }
            currentSelectedType.copy(selectedType = block(currentPasswordType))
        }
    }

    private inline fun updateGeneratorMainTypeUsername(
        crossinline block: (Username) -> Username,
    ) {
        updateGeneratorMainType {
            if (it !is Username) null else block(it)
        }
    }

    private inline fun updateForwardedEmailAliasType(
        crossinline block: (ForwardedEmailAlias) -> ForwardedEmailAlias,
    ) {
        updateGeneratorMainTypeUsername { currentSelectedType ->
            val currentUsernameType = currentSelectedType.selectedType
            if (currentUsernameType !is ForwardedEmailAlias) {
                return@updateGeneratorMainTypeUsername currentSelectedType
            }
            currentSelectedType.copy(selectedType = block(currentUsernameType))
        }
    }

    private inline fun updateAddyIoServiceType(
        crossinline block: (AddyIo) -> AddyIo,
    ) {
        updateGeneratorMainTypeUsername { currentUsernameType ->
            if (currentUsernameType.selectedType !is ForwardedEmailAlias) {
                return@updateGeneratorMainTypeUsername currentUsernameType
            }

            val currentServiceType = (currentUsernameType.selectedType).selectedServiceType
            if (currentServiceType !is AddyIo) {
                return@updateGeneratorMainTypeUsername currentUsernameType
            }

            val updatedServiceType = block(currentServiceType)

            currentUsernameType.copy(
                selectedType = ForwardedEmailAlias(
                    selectedServiceType = updatedServiceType,
                    obfuscatedText = currentUsernameType.selectedType.obfuscatedText,
                ),
            )
        }
    }

    private inline fun updateDuckDuckGoServiceType(
        crossinline block: (DuckDuckGo) -> DuckDuckGo,
    ) {
        updateGeneratorMainTypeUsername { currentUsernameType ->
            if (currentUsernameType.selectedType !is ForwardedEmailAlias) {
                return@updateGeneratorMainTypeUsername currentUsernameType
            }

            val currentServiceType = (currentUsernameType.selectedType).selectedServiceType
            if (currentServiceType !is DuckDuckGo) {
                return@updateGeneratorMainTypeUsername currentUsernameType
            }

            val updatedServiceType = block(currentServiceType)

            currentUsernameType.copy(
                selectedType = ForwardedEmailAlias(
                    selectedServiceType = updatedServiceType,
                    obfuscatedText = currentUsernameType.selectedType.obfuscatedText,
                ),
            )
        }
    }

    private inline fun updateFastMailServiceType(
        crossinline block: (FastMail) -> FastMail,
    ) {
        updateGeneratorMainTypeUsername { currentUsernameType ->
            if (currentUsernameType.selectedType !is ForwardedEmailAlias) {
                return@updateGeneratorMainTypeUsername currentUsernameType
            }

            val currentServiceType = (currentUsernameType.selectedType).selectedServiceType
            if (currentServiceType !is FastMail) {
                return@updateGeneratorMainTypeUsername currentUsernameType
            }

            val updatedServiceType = block(currentServiceType)

            currentUsernameType.copy(
                selectedType = ForwardedEmailAlias(
                    selectedServiceType = updatedServiceType,
                    obfuscatedText = currentUsernameType.selectedType.obfuscatedText,
                ),
            )
        }
    }

    private inline fun updateFirefoxRelayServiceType(
        crossinline block: (FirefoxRelay) -> FirefoxRelay,
    ) {
        updateGeneratorMainTypeUsername { currentUsernameType ->
            if (currentUsernameType.selectedType !is ForwardedEmailAlias) {
                return@updateGeneratorMainTypeUsername currentUsernameType
            }

            val currentServiceType = (currentUsernameType.selectedType).selectedServiceType
            if (currentServiceType !is FirefoxRelay) {
                return@updateGeneratorMainTypeUsername currentUsernameType
            }

            val updatedServiceType = block(currentServiceType)

            currentUsernameType.copy(
                selectedType = ForwardedEmailAlias(
                    selectedServiceType = updatedServiceType,
                    obfuscatedText = currentUsernameType.selectedType.obfuscatedText,
                ),
            )
        }
    }

    private inline fun updateSimpleLoginServiceType(
        crossinline block: (SimpleLogin) -> SimpleLogin,
    ) {
        updateGeneratorMainTypeUsername { currentUsernameType ->
            if (currentUsernameType.selectedType !is ForwardedEmailAlias) {
                return@updateGeneratorMainTypeUsername currentUsernameType
            }

            val currentServiceType = (currentUsernameType.selectedType).selectedServiceType
            if (currentServiceType !is SimpleLogin) {
                return@updateGeneratorMainTypeUsername currentUsernameType
            }

            val updatedServiceType = block(currentServiceType)

            currentUsernameType.copy(
                selectedType = ForwardedEmailAlias(
                    selectedServiceType = updatedServiceType,
                    obfuscatedText = currentUsernameType.selectedType.obfuscatedText,
                ),
            )
        }
    }

    private inline fun updatePlusAddressedEmailType(
        crossinline block: (PlusAddressedEmail) -> PlusAddressedEmail,
    ) {
        updateGeneratorMainTypeUsername { currentSelectedType ->
            val currentUsernameType = currentSelectedType.selectedType
            if (currentUsernameType !is PlusAddressedEmail) {
                return@updateGeneratorMainTypeUsername currentSelectedType
            }
            currentSelectedType.copy(selectedType = block(currentUsernameType))
        }
    }

    private inline fun updateCatchAllEmailType(
        crossinline block: (CatchAllEmail) -> CatchAllEmail,
    ) {
        updateGeneratorMainTypeUsername { currentSelectedType ->
            val currentUsernameType = currentSelectedType.selectedType
            if (currentUsernameType !is CatchAllEmail) {
                return@updateGeneratorMainTypeUsername currentSelectedType
            }
            currentSelectedType.copy(selectedType = block(currentUsernameType))
        }
    }

    private inline fun updateRandomWordType(
        crossinline block: (RandomWord) -> RandomWord,
    ) {
        updateGeneratorMainTypeUsername { currentSelectedType ->
            val currentUsernameType = currentSelectedType.selectedType
            if (currentUsernameType !is RandomWord) {
                return@updateGeneratorMainTypeUsername currentSelectedType
            }
            currentSelectedType.copy(selectedType = block(currentUsernameType))
        }
    }

    //endregion Utility Functions

    companion object {
        private const val PLACEHOLDER_GENERATED_TEXT = "Placeholder"

        private val INITIAL_STATE: GeneratorState = GeneratorState(
            generatedText = PLACEHOLDER_GENERATED_TEXT,
            selectedType = Passcode(
                selectedType = Password(),
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
                 * Represents a standard PASSWORD type, with configurable options for
                 * length, character types, and requirements.
                 *
                 * @property length The length of the generated password.
                 * @property useCapitals Whether to include capital letters.
                 * @property useLowercase Whether to include lowercase letters.
                 * @property useNumbers Whether to include numbers.
                 * @property useSpecialChars Whether to include special characters.
                 * @property minNumbers The minimum number of numeric characters.
                 * @property minSpecial The minimum number of special characters.
                 * @property avoidAmbiguousChars Whether to avoid characters that look similar.
                 * @property isUserInteracting Indicates whether the user is currently interacting
                 * with a control. This flag can be used to prevent unnecessary updates or
                 * processing during continuous interaction.
                 */
                @Parcelize
                data class Password(
                    val length: Int = DEFAULT_PASSWORD_LENGTH,
                    val useCapitals: Boolean = true,
                    val useLowercase: Boolean = true,
                    val useNumbers: Boolean = true,
                    val useSpecialChars: Boolean = false,
                    val minNumbers: Int = MIN_NUMBERS,
                    val minSpecial: Int = MIN_SPECIAL,
                    val avoidAmbiguousChars: Boolean = false,
                    val isUserInteracting: Boolean = false,
                ) : PasscodeType(), Parcelable {
                    override val displayStringResId: Int
                        get() = PasscodeTypeOption.PASSWORD.labelRes

                    companion object {
                        private const val DEFAULT_PASSWORD_LENGTH: Int = 14
                        private const val MIN_NUMBERS: Int = 1
                        private const val MIN_SPECIAL: Int = 1

                        const val PASSWORD_LENGTH_SLIDER_MIN: Int = 5
                        const val PASSWORD_LENGTH_SLIDER_MAX: Int = 128
                        const val PASSWORD_COUNTER_MIN: Int = 0
                        const val PASSWORD_COUNTER_MAX: Int = 5
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
        data class Username(
            val selectedType: UsernameType = PlusAddressedEmail(),
        ) : MainType(), Parcelable {
            override val displayStringResId: Int
                get() = MainTypeOption.USERNAME.labelRes

            /**
             * Enum representing the types of usernames,
             * allowing for different username configurations.
             *
             * @property labelRes The ID of the string that represents the label for each type.
             */
            enum class UsernameTypeOption(val labelRes: Int) {
                PLUS_ADDRESSED_EMAIL(R.string.plus_addressed_email),
                CATCH_ALL_EMAIL(R.string.catch_all_email),
                FORWARDED_EMAIL_ALIAS(R.string.forwarded_email_alias),
                RANDOM_WORD(R.string.random_word),
            }

            /**
             * A sealed class representing the different types of USERNAME
             * each with its own properties.
             */
            @Parcelize
            sealed class UsernameType : Parcelable {

                /**
                 * Represents the resource ID for the display string specific to each
                 * UsernameType subclass.
                 */
                abstract val displayStringResId: Int

                /**
                 * Represents the resource ID for the supporting display string specific to each
                 * UsernameType subclass.
                 */
                abstract val supportingStringResId: Int?

                /**
                 * Represents a PlusAddressedEmail type.
                 *
                 * @property email The email used to generate a plus addressed email.
                 */
                @Parcelize
                data class PlusAddressedEmail(
                    val email: String = "PLACEHOLDER",
                ) : UsernameType(), Parcelable {
                    override val displayStringResId: Int
                        get() = UsernameTypeOption.PLUS_ADDRESSED_EMAIL.labelRes

                    override val supportingStringResId: Int
                        get() = R.string.plus_addressed_email_description
                }

                /**
                 * Represents a Catch All Email type, with a configurable option for
                 * domain name.
                 *
                 * @property domainName The domain name used for generation.
                 */
                @Parcelize
                data class CatchAllEmail(
                    val domainName: String = "",
                ) : UsernameType(), Parcelable {
                    override val displayStringResId: Int
                        get() = UsernameTypeOption.CATCH_ALL_EMAIL.labelRes

                    override val supportingStringResId: Int
                        get() = R.string.catch_all_email_description
                }

                /**
                 * Represents a Random word type, with a configurable option for
                 * capitalizing letters and including numbers
                 *
                 * @property capitalize Whether to capitalize the first letter of each word.
                 * @property includeNumber Whether to include a numbers in the random words.
                 */
                @Parcelize
                data class RandomWord(
                    val capitalize: Boolean = false,
                    val includeNumber: Boolean = false,
                ) : UsernameType(), Parcelable {
                    override val displayStringResId: Int
                        get() = UsernameTypeOption.RANDOM_WORD.labelRes

                    override val supportingStringResId: Int?
                        get() = null
                }

                /**
                 * Represents a Forwarded email alias type, with a configurable option for
                 * service and api key.
                 *
                 * @property selectedServiceType The service name used for generation.
                 */
                @Parcelize
                data class ForwardedEmailAlias(
                    val selectedServiceType: ServiceType? = null,
                    val obfuscatedText: String = "",
                ) : UsernameType(), Parcelable {
                    override val displayStringResId: Int
                        get() = UsernameTypeOption.FORWARDED_EMAIL_ALIAS.labelRes

                    override val supportingStringResId: Int
                        get() = R.string.forwarded_email_description

                    /**
                     * Enum representing the types of services,
                     * allowing for different service configurations.
                     *
                     * @property labelRes The ID of the string that represents
                     * the label for each type.
                     */
                    enum class ServiceTypeOption(val labelRes: Int) {
                        ADDY_IO(R.string.addy_io),
                        DUCK_DUCK_GO(R.string.duck_duck_go),
                        FAST_MAIL(R.string.fastmail),
                        FIREFOX_RELAY(R.string.firefox_relay),
                        SIMPLE_LOGIN(R.string.simple_login),
                    }

                    /**
                     * A sealed class representing the different types of services.
                     */
                    @Parcelize
                    sealed class ServiceType : Parcelable {

                        /**
                         * Represents the resource ID for the display string specific to each
                         * ServiceType subclass. Every subclass of ServiceType must override
                         * this property to provide the appropriate string resource ID for
                         * its display string.
                         */
                        abstract val displayStringResId: Int?

                        /**
                         * Represents the Addy Io service type, with a configurable option for
                         * service and api key.
                         *
                         * @property apiAccessToken The token used for generation.
                         * @property domainName The domain name used for generation.
                         */
                        @Parcelize
                        data class AddyIo(
                            val apiAccessToken: String = "",
                            val domainName: String = "",
                            val baseUrl: String = "",
                        ) : ServiceType(), Parcelable {
                            override val displayStringResId: Int
                                get() = ServiceTypeOption.ADDY_IO.labelRes
                        }

                        /**
                         * Represents the Duck Duck Go service type, with a configurable option for
                         * api key.
                         *
                         * @property apiKey The api key used for generation.
                         */
                        @Parcelize
                        data class DuckDuckGo(
                            val apiKey: String = "",
                        ) : ServiceType(), Parcelable {
                            override val displayStringResId: Int
                                get() = ServiceTypeOption.DUCK_DUCK_GO.labelRes
                        }

                        /**
                         * Represents the Fast Mail service type, with a configurable option for
                         * api key.
                         *
                         * @property apiKey The api key used for generation.
                         */
                        @Parcelize
                        data class FastMail(
                            val apiKey: String = "",
                        ) : ServiceType(), Parcelable {
                            override val displayStringResId: Int
                                get() = ServiceTypeOption.FAST_MAIL.labelRes
                        }

                        /**
                         * Represents the Firefox Relay service type, with a configurable option for
                         * api access token.
                         *
                         * @property apiAccessToken The api access token used for generation.
                         */
                        @Parcelize
                        data class FirefoxRelay(
                            val apiAccessToken: String = "",
                        ) : ServiceType(), Parcelable {
                            override val displayStringResId: Int
                                get() = ServiceTypeOption.FIREFOX_RELAY.labelRes
                        }

                        /**
                         * Represents the SimpleLogin service type, with a configurable option for
                         * api key.
                         *
                         * @property apiKey The api key used for generation.
                         */
                        @Parcelize
                        data class SimpleLogin(
                            val apiKey: String = "",
                        ) : ServiceType(), Parcelable {
                            override val displayStringResId: Int
                                get() = ServiceTypeOption.SIMPLE_LOGIN.labelRes
                        }
                    }
                }
            }
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
     * Indicates that the overflow option for password history has been clicked.
     */
    data object PasswordHistoryClick : GeneratorAction()

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
                sealed class Password : PasscodeType() {
                    /**
                     * Represents a change action for the length of the password,
                     * adjusted using a slider.
                     *
                     * @property length The new desired length for the password.
                     */
                    data class SliderLengthChange(
                        val length: Int,
                        val isUserInteracting: Boolean,
                    ) : Password()

                    /**
                     * Represents a change action to toggle the usage of
                     * capital letters in the password.
                     *
                     * @property useCapitals Flag indicating whether capital letters
                     * should be used.
                     */
                    data class ToggleCapitalLettersChange(
                        val useCapitals: Boolean,
                    ) : Password()

                    /**
                     * Represents a change action to toggle the usage of lowercase letters
                     * in the password.
                     *
                     * @property useLowercase Flag indicating whether lowercase letters
                     * should be used.
                     */
                    data class ToggleLowercaseLettersChange(
                        val useLowercase: Boolean,
                    ) : Password()

                    /**
                     * Represents a change action to toggle the inclusion of numbers
                     * in the password.
                     *
                     * @property useNumbers Flag indicating whether numbers
                     * should be used.
                     */
                    data class ToggleNumbersChange(
                        val useNumbers: Boolean,
                    ) : Password()

                    /**
                     * Represents a change action to toggle the usage of special characters
                     * in the password.
                     *
                     * @property useSpecialChars Flag indicating whether special characters
                     * should be used.
                     */
                    data class ToggleSpecialCharactersChange(
                        val useSpecialChars: Boolean,
                    ) : Password()

                    /**
                     * Represents a change action for the minimum required number of numbers
                     * in the password.
                     *
                     * @property minNumbers The minimum required number of numbers
                     * for the password.
                     */
                    data class MinNumbersCounterChange(
                        val minNumbers: Int,
                    ) : Password()

                    /**
                     * Represents a change action for the minimum required number of special
                     * characters in the password.
                     *
                     * @property minSpecial The minimum required number of special characters
                     * for the password.
                     */
                    data class MinSpecialCharactersChange(
                        val minSpecial: Int,
                    ) : Password()

                    /**
                     * Represents a change action to toggle the avoidance of ambiguous
                     * characters in the password.
                     *
                     * @property avoidAmbiguousChars Flag indicating whether ambiguous characters
                     * should be avoided.
                     */
                    data class ToggleAvoidAmbigousCharactersChange(
                        val avoidAmbiguousChars: Boolean,
                    ) : Password()
                }

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
        sealed class Username : MainType() {

            /**
             * Represents the action of selecting a username type option.
             *
             * @property usernameTypeOption The selected username type option.
             */
            data class UsernameTypeOptionSelect(
                val usernameTypeOption: GeneratorState.MainType.Username.UsernameTypeOption,
            ) : Username()

            /**
             * Represents actions related to the different types of usernames.
             */
            sealed class UsernameType : Username() {

                /**
                 * Represents actions specifically related to Forwarded Email Alias.
                 */
                sealed class ForwardedEmailAlias : UsernameType() {

                    /**
                     * Represents the action of selecting a service type option.
                     *
                     * @property serviceTypeOption The selected service type option.
                     */
                    data class ServiceTypeOptionSelect(
                        val serviceTypeOption: GeneratorState
                        .MainType
                        .Username
                        .UsernameType
                        .ForwardedEmailAlias
                        .ServiceTypeOption,
                    ) : ForwardedEmailAlias()

                    /**
                     * Represents actions specifically related to the AddyIo service.
                     */
                    sealed class AddyIo : ForwardedEmailAlias() {

                        /**
                         * Fired when the access token input text is changed.
                         *
                         * @property accessToken The new access token text.
                         */
                        data class AccessTokenTextChange(val accessToken: String) : AddyIo()

                        /**
                         * Fired when the domain text input is changed.
                         *
                         * @property domain The new domain text.
                         */
                        data class DomainTextChange(val domain: String) : AddyIo()
                    }

                    /**
                     * Represents actions specifically related to the DuckDuckGo service.
                     */
                    sealed class DuckDuckGo : ForwardedEmailAlias() {

                        /**
                         * Fired when the api key input text is changed.
                         *
                         * @property apiKey The new api key text.
                         */
                        data class ApiKeyTextChange(val apiKey: String) : DuckDuckGo()
                    }

                    /**
                     * Represents actions specifically related to the FastMail service.
                     */
                    sealed class FastMail : ForwardedEmailAlias() {

                        /**
                         * Fired when the api key input text is changed.
                         *
                         * @property apiKey The new api key text.
                         */
                        data class ApiKeyTextChange(val apiKey: String) : FastMail()
                    }

                    /**
                     * Represents actions specifically related to the FirefoxRelay service.
                     */
                    sealed class FirefoxRelay : ForwardedEmailAlias() {

                        /**
                         * Fired when the access token input text is changed.
                         *
                         * @property accessToken The new access token text.
                         */
                        data class AccessTokenTextChange(val accessToken: String) : FirefoxRelay()
                    }

                    /**
                     * Represents actions specifically related to the SimpleLogin service.
                     */
                    sealed class SimpleLogin : ForwardedEmailAlias() {

                        /**
                         * Fired when the api key input text is changed.
                         *
                         * @property apiKey The new api key text.
                         */
                        data class ApiKeyTextChange(val apiKey: String) : SimpleLogin()
                    }
                }

                /**
                 * Represents actions specifically related to Plus Addressed Email.
                 */
                sealed class PlusAddressedEmail : UsernameType() {

                    /**
                     * Fired when the email text input is changed.
                     *
                     * @property email The new email text.
                     */
                    data class EmailTextChange(val email: String) : PlusAddressedEmail()
                }

                /**
                 * Represents actions specifically related to Catch-All Email.
                 */
                sealed class CatchAllEmail : UsernameType() {

                    /**
                     * Fired when the domain text input is changed.
                     *
                     * @property domain The new domain text.
                     */
                    data class DomainTextChange(val domain: String) : CatchAllEmail()
                }

                /**
                 * Represents actions specifically related to Random Word.
                 */
                sealed class RandomWord : UsernameType() {

                    /**
                     * Fired when the "capitalize" toggle is changed.
                     *
                     * @property capitalize The new value of the "capitalize" toggle.
                     */
                    data class ToggleCapitalizeChange(val capitalize: Boolean) : RandomWord()

                    /**
                     * Fired when the "include number" toggle is changed.
                     *
                     * @property includeNumber The new value of the "include number" toggle.
                     */
                    data class ToggleIncludeNumberChange(val includeNumber: Boolean) : RandomWord()
                }
            }
        }
    }

    /**
     * Models actions that the [GeneratorViewModel] itself might send.
     */
    sealed class Internal : GeneratorAction() {
        /**
         * Indicates a generated text update is received.
         */
        data class UpdateGeneratedPasswordResult(
            val result: GeneratedPasswordResult,
        ) : Internal()

        /**
         * Indicates a generated text update is received.
         */
        data class UpdateGeneratedPassphraseResult(
            val result: GeneratedPassphraseResult,
        ) : Internal()

        /**
         * Indicates a generated text update is received.
         */
        data class UpdateGeneratedUsernameResult(
            val result: GeneratedForwardedServiceUsernameResult,
        ) : Internal()
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
     * Navigates to the Password History screen.
     */
    data object NavigateToPasswordHistory : GeneratorEvent()

    /**
     * Copies text to the clipboard.
     */
    data object CopyTextToClipboard : GeneratorEvent()

    /**
     * Displays the message in a snackbar.
     */
    data class ShowSnackbar(
        val message: Text,
    ) : GeneratorEvent()
}

@Suppress("ComplexCondition")
private fun Password.enforceAtLeastOneToggleOn(): Password =
    // If all toggles are off, turn on useLowercase
    if (!this.useCapitals &&
        !this.useLowercase &&
        !this.useNumbers &&
        !this.useSpecialChars
    ) {
        this.copy(useLowercase = true)
    } else {
        this
    }
