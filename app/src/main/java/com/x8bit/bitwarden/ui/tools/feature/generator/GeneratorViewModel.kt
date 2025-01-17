@file:Suppress("TooManyFunctions")

package com.x8bit.bitwarden.ui.tools.feature.generator

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.generators.AppendType
import com.bitwarden.generators.PassphraseGeneratorRequest
import com.bitwarden.generators.PasswordGeneratorRequest
import com.bitwarden.generators.UsernameGeneratorRequest
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.PolicyInformation
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.ReviewPromptManager
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.manager.util.getActivePolicies
import com.x8bit.bitwarden.data.platform.manager.util.getActivePoliciesFlow
import com.x8bit.bitwarden.data.tools.generator.repository.GeneratorRepository
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedCatchAllUsernameResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedForwardedServiceUsernameResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedPassphraseResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedPasswordResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedPlusAddressedUsernameResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedRandomWordUsernameResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratorResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.PasscodeGenerationOptions
import com.x8bit.bitwarden.data.tools.generator.repository.model.UsernameGenerationOptions
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.orNullIfBlank
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Username.UsernameType.CatchAllEmail
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias.ServiceType.AddyIo
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias.ServiceType.DuckDuckGo
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias.ServiceType.FastMail
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias.ServiceType.FirefoxRelay
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias.ServiceType.ForwardEmail
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias.ServiceType.SimpleLogin
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Username.UsernameType.PlusAddressedEmail
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Username.UsernameType.RandomWord
import com.x8bit.bitwarden.ui.tools.feature.generator.model.GeneratorMode
import com.x8bit.bitwarden.ui.tools.feature.generator.util.toServiceType
import com.x8bit.bitwarden.ui.tools.feature.generator.util.toStrictestPolicy
import com.x8bit.bitwarden.ui.tools.feature.generator.util.toUsernameGeneratorRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import kotlin.math.max

private const val KEY_STATE = "state"
private const val NO_GENERATED_TEXT: String = "-"

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
    private val clipboardManager: BitwardenClipboardManager,
    private val generatorRepository: GeneratorRepository,
    private val authRepository: AuthRepository,
    private val policyManager: PolicyManager,
    private val reviewPromptManager: ReviewPromptManager,
) : BaseViewModel<GeneratorState, GeneratorEvent, GeneratorAction>(
    initialState = savedStateHandle[KEY_STATE] ?: run {
        val generatorMode = GeneratorArgs(savedStateHandle).type
        GeneratorState(
            generatedText = NO_GENERATED_TEXT,
            selectedType = when (generatorMode) {
                is GeneratorMode.Modal.Username -> {
                    GeneratorState.MainType.Username(
                        selectedType = generatorRepository
                            .getUsernameGenerationOptions()
                            .usernameType,
                    )
                }

                GeneratorMode.Modal.Password -> {
                    generatorRepository.getPasscodeGenerationOptions().passcodeType
                }

                GeneratorMode.Default -> GeneratorState.MainType.Password()
            },
            generatorMode = generatorMode,
            currentEmailAddress = requireNotNull(
                authRepository.userStateFlow.value?.activeAccount?.email,
            ),
            isUnderPolicy = policyManager
                .getActivePolicies<PolicyInformation.PasswordGenerator>()
                .any(),
            website = (generatorMode as? GeneratorMode.Modal.Username)?.website,
        )
    },
) {

    //region Initialization and Overrides

    private var generateTextJob: Job = Job().apply { complete() }

    init {
        stateFlow.onEach { savedStateHandle[KEY_STATE] = it }.launchIn(viewModelScope)
        loadOptions()
        policyManager
            .getActivePoliciesFlow<PolicyInformation.PasswordGenerator>()
            .map { GeneratorAction.Internal.PasswordGeneratorPolicyReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: GeneratorAction) {
        when (action) {
            is GeneratorAction.PasswordHistoryClick -> handlePasswordHistoryClick()
            is GeneratorAction.CloseClick -> handleCloseClick()
            is GeneratorAction.SelectClick -> handleSelectClick()
            is GeneratorAction.RegenerateClick -> handleRegenerationClick()
            is GeneratorAction.CopyClick -> handleCopyClick()
            is GeneratorAction.MainTypeOptionSelect -> handleMainTypeOptionSelect(action)
            is GeneratorAction.MainType -> handleMainTypeAction(action)
            is GeneratorAction.Internal -> handleInternalAction(action)
            GeneratorAction.LifecycleResume -> handleOnResumed()
        }
    }

    private fun handleOnResumed() {
        // when the screen resumes we need to refresh the options for the current option from
        // disk in the event they were changed while the screen was in the foreground.
        loadOptions(shouldUseStorageOptions = true)
    }

    @Suppress("MaxLineLength")
    private fun handleMainTypeAction(action: GeneratorAction.MainType) {
        when (action) {
            is GeneratorAction.MainType.Password -> {
                handlePasswordSpecificAction(action)
            }

            is GeneratorAction.MainType.Passphrase -> {
                handlePassphraseSpecificAction(action)
            }

            is GeneratorAction.MainType.Username.UsernameTypeOptionSelect -> {
                handleUsernameTypeOptionSelect(action)
            }

            is GeneratorAction.MainType.Username.UsernameType.TooltipClick -> {
                handleTooltipClick()
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

            is GeneratorAction.MainType.Username.UsernameType.ForwardedEmailAlias.ForwardEmail -> {
                handleForwardEmailSpecificAction(action)
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

    private fun handleInternalAction(action: GeneratorAction.Internal) {
        when (action) {
            is GeneratorAction.Internal.UpdateGeneratedPasswordResult -> {
                handleUpdateGeneratedPasswordResult(action)
            }

            is GeneratorAction.Internal.UpdateGeneratedPassphraseResult -> {
                handleUpdateGeneratedPassphraseResult(action)
            }

            is GeneratorAction.Internal.UpdateGeneratedPlusAddressedUsernameResult -> {
                handleUpdatePlusAddressedGeneratedUsernameResult(action)
            }

            is GeneratorAction.Internal.UpdateGeneratedCatchAllUsernameResult -> {
                handleUpdateCatchAllGeneratedUsernameResult(action)
            }

            is GeneratorAction.Internal.UpdateGeneratedRandomWordUsernameResult -> {
                handleUpdateRandomWordGeneratedUsernameResult(action)
            }

            is GeneratorAction.Internal.UpdateGeneratedForwardedServiceUsernameResult -> {
                handleUpdateForwardedServiceGeneratedUsernameResult(action)
            }

            is GeneratorAction.Internal.PasswordGeneratorPolicyReceive -> {
                handlePasswordGeneratorPolicyReceive(action)
            }
        }
    }

    //endregion Initialization and Overrides

    //region Top Level Handlers

    private fun handlePasswordHistoryClick() {
        sendEvent(GeneratorEvent.NavigateToPasswordHistory)
    }

    private fun handleCloseClick() {
        sendEvent(GeneratorEvent.NavigateBack)
    }

    private fun handleSelectClick() {
        when (state.selectedType) {
            is GeneratorState.MainType.Passphrase,
            is GeneratorState.MainType.Password,
                -> {
                generatorRepository.emitGeneratorResult(
                    GeneratorResult.Password(state.generatedText),
                )
            }

            is GeneratorState.MainType.Username -> {
                generatorRepository.emitGeneratorResult(
                    GeneratorResult.Username(state.generatedText),
                )
            }
        }
        sendEvent(GeneratorEvent.NavigateBack)
    }

    //endregion Top Level Handlers

    //region Generation Handlers

    private fun loadOptions(shouldUseStorageOptions: Boolean = false) {
        when (val selectedType = state.selectedType) {
            is GeneratorState.MainType.Passphrase,
            is GeneratorState.MainType.Password,
                -> {
                val mainType = if (shouldUseStorageOptions) {
                    generatorRepository
                        .getPasscodeGenerationOptions()
                        ?.passcodeType
                        ?: selectedType
                } else {
                    selectedType
                }
                loadPasscodeOptions(selectedType = mainType)
            }

            is GeneratorState.MainType.Username -> {
                val mainType = if (shouldUseStorageOptions) {
                    generatorRepository
                        .getUsernameGenerationOptions()
                        ?.usernameType
                        ?.let { GeneratorState.MainType.Username(it) }
                        ?: selectedType
                } else {
                    selectedType
                }
                loadUsernameOptions(
                    selectedType = mainType,
                    forceRegeneration = mainType.selectedType !is ForwardedEmailAlias,
                )
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    private fun loadPasscodeOptions(selectedType: GeneratorState.MainType) {
        if (selectedType is GeneratorState.MainType.Username) {
            // This can only handle passwords and passphrases
            return
        }
        val options = generatorRepository
            .getPasscodeGenerationOptions()
            ?: generatePasscodeDefaultOptions()

        val policy = policyManager
            .getActivePolicies<PolicyInformation.PasswordGenerator>()
            .toStrictestPolicy()

        val passwordType = when (policy.overridePasswordType.toPasscodePolicyOverride()) {
            GeneratorState.PasscodePolicyOverride.PASSWORD -> {
                mutableStateFlow.update {
                    it.copy(
                        passcodePolicyOverride = GeneratorState.PasscodePolicyOverride.PASSWORD,
                    )
                }
                GeneratorState.MainType.Password()
            }

            GeneratorState.PasscodePolicyOverride.PASSPHRASE -> {
                mutableStateFlow.update {
                    it.copy(
                        passcodePolicyOverride = GeneratorState.PasscodePolicyOverride.PASSPHRASE,
                    )
                }
                GeneratorState.MainType.Passphrase()
            }

            null -> {
                mutableStateFlow.update { it.copy(passcodePolicyOverride = null) }
                selectedType
            }
        }

        when (passwordType) {
            is GeneratorState.MainType.Passphrase -> {
                val minNumWords = policy
                    .minNumberWords
                    ?: GeneratorState.MainType.Passphrase.PASSPHRASE_MIN_NUMBER_OF_WORDS
                updateGeneratorMainType {
                    GeneratorState.MainType.Passphrase(
                        numWords = max(options.numWords, minNumWords),
                        minNumWords = minNumWords,
                        wordSeparator = options.wordSeparator.toCharArray().firstOrNull(),
                        capitalize = options.allowCapitalize || policy.capitalize == true,
                        capitalizeEnabled = policy.capitalize != true,
                        includeNumber = options.allowIncludeNumber || policy.includeNumber == true,
                        includeNumberEnabled = policy.includeNumber != true,
                    )
                }
            }

            is GeneratorState.MainType.Password -> {
                val minLength = policy
                    .minLength
                    ?: GeneratorState.MainType.Password.PASSWORD_LENGTH_SLIDER_MIN
                updateGeneratorMainType {
                    GeneratorState.MainType.Password(
                        length = max(options.length, minLength),
                        minLength = minLength,
                        useCapitals = options.hasUppercase || policy.useUpper == true,
                        capitalsEnabled = policy.useUpper != true,
                        useLowercase = options.hasLowercase || policy.useLower == true,
                        lowercaseEnabled = policy.useLower != true,
                        useNumbers = options.hasNumbers || policy.useNumbers == true,
                        numbersEnabled = policy.useNumbers != true,
                        useSpecialChars = options.allowSpecial || policy.useSpecial == true,
                        specialCharsEnabled = policy.useSpecial != true,
                        minNumbers = max(options.minNumber, policy.minNumbers ?: 0),
                        minNumbersAllowed = policy.minNumbers ?: 0,
                        minSpecial = max(options.minSpecial, policy.minSpecial ?: 0),
                        minSpecialAllowed = policy.minSpecial ?: 0,
                        avoidAmbiguousChars = options.allowAmbiguousChar,
                    )
                }
            }

            is GeneratorState.MainType.Username -> Unit
        }
    }

    private fun loadUsernameOptions(
        selectedType: GeneratorState.MainType.Username,
        forceRegeneration: Boolean = false,
    ) {
        val options = generatorRepository.getUsernameGenerationOptions()
        val updatedSelectedType = when (val type = selectedType.selectedType) {
            is PlusAddressedEmail -> {
                val emailToUse = options
                    ?.plusAddressedEmail
                    ?.orNullIfBlank()
                    ?: state.currentEmailAddress

                GeneratorState.MainType.Username(
                    selectedType = PlusAddressedEmail(email = emailToUse),
                )
            }

            is CatchAllEmail -> {
                val catchAllEmail = CatchAllEmail(
                    domainName = options?.catchAllEmailDomain ?: type.domainName,
                )
                GeneratorState.MainType.Username(selectedType = catchAllEmail)
            }

            is RandomWord -> {
                val randomWord = RandomWord(
                    capitalize = options?.capitalizeRandomWordUsername ?: type.capitalize,
                    includeNumber = options?.includeNumberRandomWordUsername ?: type.includeNumber,
                )
                GeneratorState.MainType.Username(selectedType = randomWord)
            }

            is ForwardedEmailAlias -> {
                val mappedServiceType = options
                    ?.serviceType
                    ?.toServiceType(options)
                    ?: type.selectedServiceType

                GeneratorState.MainType.Username(
                    selectedType = ForwardedEmailAlias(
                        selectedServiceType = mappedServiceType,
                        obfuscatedText = "",
                    ),
                )
            }
        }

        updateGeneratorMainType(forceRegeneration = forceRegeneration) { updatedSelectedType }
    }

    private fun savePasswordOptionsToDisk(password: GeneratorState.MainType.Password) {
        val options = generatorRepository
            .getPasscodeGenerationOptions() ?: generatePasscodeDefaultOptions()
        val newOptions = options.copy(
            type = PasscodeGenerationOptions.PasscodeType.PASSWORD,
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

    private fun savePassphraseOptionsToDisk(passphrase: GeneratorState.MainType.Passphrase) {
        val options = generatorRepository
            .getPasscodeGenerationOptions() ?: generatePasscodeDefaultOptions()
        val newOptions = options.copy(
            type = PasscodeGenerationOptions.PasscodeType.PASSPHRASE,
            numWords = passphrase.numWords,
            wordSeparator = passphrase.wordSeparator?.toString().orEmpty(),
            allowCapitalize = passphrase.capitalize,
            allowIncludeNumber = passphrase.includeNumber,
        )
        generatorRepository.savePasscodeGenerationOptions(newOptions)
    }

    private fun savePlusAddressedEmailOptionsToDisk(plusAddressedEmail: PlusAddressedEmail) {
        val options = generatorRepository.getUsernameGenerationOptions()
            ?: generateUsernameDefaultOptions()
        val newOptions = options.copy(
            type = UsernameGenerationOptions.UsernameType.PLUS_ADDRESSED_EMAIL,
            plusAddressedEmail = plusAddressedEmail.email,
        )

        generatorRepository.saveUsernameGenerationOptions(newOptions)
    }

    private fun saveCatchAllEmailOptionsToDisk(catchAllEmail: CatchAllEmail) {
        val options = generatorRepository
            .getUsernameGenerationOptions() ?: generateUsernameDefaultOptions()
        val newOptions = options.copy(
            type = UsernameGenerationOptions.UsernameType.CATCH_ALL_EMAIL,
            catchAllEmailDomain = catchAllEmail.domainName,
        )
        generatorRepository.saveUsernameGenerationOptions(newOptions)
    }

    private fun saveRandomWordOptionsToDisk(randomWord: RandomWord) {
        val options = generatorRepository
            .getUsernameGenerationOptions() ?: generateUsernameDefaultOptions()
        val newOptions = options.copy(
            type = UsernameGenerationOptions.UsernameType.RANDOM_WORD,
            capitalizeRandomWordUsername = randomWord.capitalize,
            includeNumberRandomWordUsername = randomWord.includeNumber,
        )
        generatorRepository.saveUsernameGenerationOptions(newOptions)
    }

    private fun saveForwardedEmailAliasServiceTypeToDisk(forwardedEmailAlias: ForwardedEmailAlias) {
        val options =
            generatorRepository.getUsernameGenerationOptions() ?: generateUsernameDefaultOptions()
        val newOptions = when (forwardedEmailAlias.selectedServiceType) {
            is AddyIo -> options.copy(
                type = UsernameGenerationOptions.UsernameType.FORWARDED_EMAIL_ALIAS,
                serviceType = UsernameGenerationOptions.ForwardedEmailServiceType.ANON_ADDY,
                anonAddyApiAccessToken = forwardedEmailAlias.selectedServiceType.apiAccessToken,
                anonAddyDomainName = forwardedEmailAlias.selectedServiceType.domainName,
            )

            is DuckDuckGo -> options.copy(
                type = UsernameGenerationOptions.UsernameType.FORWARDED_EMAIL_ALIAS,
                serviceType = UsernameGenerationOptions.ForwardedEmailServiceType.DUCK_DUCK_GO,
                duckDuckGoApiKey = forwardedEmailAlias.selectedServiceType.apiKey,
            )

            is FastMail -> options.copy(
                type = UsernameGenerationOptions.UsernameType.FORWARDED_EMAIL_ALIAS,
                serviceType = UsernameGenerationOptions.ForwardedEmailServiceType.FASTMAIL,
                fastMailApiKey = forwardedEmailAlias.selectedServiceType.apiKey,
            )

            is FirefoxRelay -> options.copy(
                type = UsernameGenerationOptions.UsernameType.FORWARDED_EMAIL_ALIAS,
                serviceType = UsernameGenerationOptions.ForwardedEmailServiceType.FIREFOX_RELAY,
                firefoxRelayApiAccessToken = forwardedEmailAlias.selectedServiceType.apiAccessToken,
            )

            is SimpleLogin -> options.copy(
                type = UsernameGenerationOptions.UsernameType.FORWARDED_EMAIL_ALIAS,
                serviceType = UsernameGenerationOptions.ForwardedEmailServiceType.SIMPLE_LOGIN,
                simpleLoginApiKey = forwardedEmailAlias.selectedServiceType.apiKey,
            )

            is ForwardEmail -> options.copy(
                type = UsernameGenerationOptions.UsernameType.FORWARDED_EMAIL_ALIAS,
                serviceType = UsernameGenerationOptions.ForwardedEmailServiceType.FORWARD_EMAIL,
                forwardEmailApiAccessToken = forwardedEmailAlias.selectedServiceType.apiKey,
                forwardEmailDomainName = forwardedEmailAlias.selectedServiceType.domainName,
            )

            else -> options.copy(
                type = UsernameGenerationOptions.UsernameType.FORWARDED_EMAIL_ALIAS,
                serviceType = UsernameGenerationOptions.ForwardedEmailServiceType.NONE,
            )
        }

        generatorRepository.saveUsernameGenerationOptions(newOptions)
    }

    private fun generatePasscodeDefaultOptions(): PasscodeGenerationOptions {
        val defaultPassword = GeneratorState.MainType.Password()
        val defaultPassphrase = GeneratorState.MainType.Passphrase()

        return PasscodeGenerationOptions(
            type = PasscodeGenerationOptions.PasscodeType.PASSWORD,
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

    private fun generateUsernameDefaultOptions(): UsernameGenerationOptions {
        return UsernameGenerationOptions(
            type = UsernameGenerationOptions.UsernameType.PLUS_ADDRESSED_EMAIL,
            serviceType = UsernameGenerationOptions.ForwardedEmailServiceType.NONE,
            capitalizeRandomWordUsername = false,
            includeNumberRandomWordUsername = false,
            plusAddressedEmail = "",
            catchAllEmailDomain = "",
            firefoxRelayApiAccessToken = "",
            simpleLoginApiKey = "",
            duckDuckGoApiKey = "",
            fastMailApiKey = "",
            anonAddyApiAccessToken = "",
            anonAddyDomainName = "",
            forwardEmailApiAccessToken = "",
            forwardEmailDomainName = "",
            emailWebsite = "",
        )
    }

    private suspend fun generatePassword(password: GeneratorState.MainType.Password) {
        val request = PasswordGeneratorRequest(
            lowercase = password.useLowercase,
            uppercase = password.useCapitals,
            numbers = password.useNumbers,
            special = password.useSpecialChars,
            length = max(password.computedMinimumLength, password.length).toUByte(),
            avoidAmbiguous = password.avoidAmbiguousChars,
            minLowercase = null,
            minUppercase = null,
            minNumber = password.minNumbers.toUByte(),
            minSpecial = password.minSpecial.toUByte(),
        )
        val shouldSave = !password.isUserInteracting

        val result = generatorRepository.generatePassword(request, shouldSave)
        sendAction(GeneratorAction.Internal.UpdateGeneratedPasswordResult(result))
    }

    private suspend fun generatePassphrase(passphrase: GeneratorState.MainType.Passphrase) {
        val request = PassphraseGeneratorRequest(
            numWords = passphrase.numWords.toUByte(),
            wordSeparator = passphrase.wordSeparator?.toString() ?: " ",
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
        updateGeneratorMainType(forceRegeneration = true) { state.selectedType }
    }

    private fun handleCopyClick() {
        reviewPromptManager.registerGeneratedResultAction()
        clipboardManager.setText(text = state.generatedText)
    }

    private fun handleTooltipClick() {
        sendEvent(GeneratorEvent.NavigateToTooltip)
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

    private fun handleUpdatePlusAddressedGeneratedUsernameResult(
        action: GeneratorAction.Internal.UpdateGeneratedPlusAddressedUsernameResult,
    ) {
        when (val result = action.result) {
            is GeneratedPlusAddressedUsernameResult.Success -> {
                mutableStateFlow.update {
                    it.copy(generatedText = result.generatedEmailAddress)
                }
            }

            GeneratedPlusAddressedUsernameResult.InvalidRequest -> {
                sendEvent(GeneratorEvent.ShowSnackbar(R.string.an_error_has_occurred.asText()))
            }
        }
    }

    private fun handleUpdateCatchAllGeneratedUsernameResult(
        action: GeneratorAction.Internal.UpdateGeneratedCatchAllUsernameResult,
    ) {
        when (val result = action.result) {
            is GeneratedCatchAllUsernameResult.Success -> {
                mutableStateFlow.update {
                    it.copy(generatedText = result.generatedEmailAddress)
                }
            }

            GeneratedCatchAllUsernameResult.InvalidRequest -> {
                sendEvent(GeneratorEvent.ShowSnackbar(R.string.an_error_has_occurred.asText()))
            }
        }
    }

    private fun handleUpdateRandomWordGeneratedUsernameResult(
        action: GeneratorAction.Internal.UpdateGeneratedRandomWordUsernameResult,
    ) {
        when (val result = action.result) {
            is GeneratedRandomWordUsernameResult.Success -> {
                mutableStateFlow.update {
                    it.copy(generatedText = result.generatedUsername)
                }
            }

            GeneratedRandomWordUsernameResult.InvalidRequest -> {
                sendEvent(GeneratorEvent.ShowSnackbar(R.string.an_error_has_occurred.asText()))
            }
        }
    }

    private fun handleUpdateForwardedServiceGeneratedUsernameResult(
        action: GeneratorAction.Internal.UpdateGeneratedForwardedServiceUsernameResult,
    ) {
        when (val result = action.result) {
            is GeneratedForwardedServiceUsernameResult.Success -> {
                mutableStateFlow.update {
                    it.copy(generatedText = result.generatedEmailAddress)
                }
            }

            is GeneratedForwardedServiceUsernameResult.InvalidRequest -> {
                sendEvent(
                    GeneratorEvent.ShowSnackbar(
                        message = result.message?.asText()
                            ?: R.string.an_error_has_occurred.asText(),
                    ),
                )
            }
        }
    }

    private fun handlePasswordGeneratorPolicyReceive(
        action: GeneratorAction.Internal.PasswordGeneratorPolicyReceive,
    ) {
        mutableStateFlow.update { it.copy(isUnderPolicy = action.policies.any()) }
        loadOptions()
    }

    //endregion Generated Field Handlers

    //region Main Type Option Handlers

    private fun handleMainTypeOptionSelect(action: GeneratorAction.MainTypeOptionSelect) {
        when (action.mainTypeOption) {
            GeneratorState.MainTypeOption.PASSWORD -> {
                loadPasscodeOptions(selectedType = GeneratorState.MainType.Password())
            }

            GeneratorState.MainTypeOption.PASSPHRASE -> {
                loadPasscodeOptions(selectedType = GeneratorState.MainType.Passphrase())
            }

            GeneratorState.MainTypeOption.USERNAME -> {
                val type = generatorRepository.getUsernameGenerationOptions().usernameType
                loadUsernameOptions(
                    selectedType = GeneratorState.MainType.Username(selectedType = type),
                    forceRegeneration = true,
                )
            }
        }
    }

    //endregion Main Type Option Handlers

    //region Password Specific Handlers

    private fun handlePasswordSpecificAction(action: GeneratorAction.MainType.Password) {
        when (action) {
            is GeneratorAction.MainType.Password.SliderLengthChange -> {
                handlePasswordLengthSliderChange(action)
            }

            is GeneratorAction.MainType.Password.ToggleCapitalLettersChange -> {
                handleToggleCapitalLetters(action)
            }

            is GeneratorAction.MainType.Password.ToggleLowercaseLettersChange -> {
                handleToggleLowercaseLetters(action)
            }

            is GeneratorAction.MainType.Password.ToggleNumbersChange -> {
                handleToggleNumbers(action)
            }

            is GeneratorAction.MainType.Password.ToggleSpecialCharactersChange -> {
                handleToggleSpecialChars(action)
            }

            is GeneratorAction.MainType.Password.MinNumbersCounterChange -> {
                handleMinNumbersChange(action)
            }

            is GeneratorAction.MainType.Password.MinSpecialCharactersChange -> {
                handleMinSpecialChange(action)
            }

            is GeneratorAction.MainType.Password.ToggleAvoidAmbiguousCharactersChange -> {
                handleToggleAmbiguousChars(action)
            }
        }
    }

    private fun handlePasswordLengthSliderChange(
        action: GeneratorAction.MainType.Password.SliderLengthChange,
    ) {
        val adjustedLength = action.length

        updatePasswordType { currentPasswordType ->
            currentPasswordType.copy(
                length = max(adjustedLength, currentPasswordType.computedMinimumLength),
                isUserInteracting = action.isUserInteracting,
            )
        }
    }

    private fun handleToggleCapitalLetters(
        action: GeneratorAction.MainType.Password.ToggleCapitalLettersChange,
    ) {
        updatePasswordType { currentPasswordType ->
            currentPasswordType.copy(
                useCapitals = action.useCapitals,
            )
        }
        updatePasswordLength()
    }

    private fun handleToggleLowercaseLetters(
        action: GeneratorAction.MainType.Password.ToggleLowercaseLettersChange,
    ) {
        updatePasswordType { currentPasswordType ->
            currentPasswordType.copy(
                useLowercase = action.useLowercase,
            )
        }
        updatePasswordLength()
    }

    private fun handleToggleNumbers(
        action: GeneratorAction.MainType.Password.ToggleNumbersChange,
    ) {
        updatePasswordType { currentPasswordType ->
            currentPasswordType.copy(
                useNumbers = action.useNumbers,
            )
        }
        updatePasswordLength()
    }

    private fun handleToggleSpecialChars(
        action: GeneratorAction.MainType.Password.ToggleSpecialCharactersChange,
    ) {
        updatePasswordType { currentPasswordType ->
            currentPasswordType.copy(
                useSpecialChars = action.useSpecialChars,
            )
        }
        updatePasswordLength()
    }

    private fun handleMinNumbersChange(
        action: GeneratorAction.MainType.Password.MinNumbersCounterChange,
    ) {
        updatePasswordType { currentPasswordType ->
            currentPasswordType.copy(
                minNumbers = action.minNumbers,
            )
        }
        updatePasswordLength()
    }

    private fun handleMinSpecialChange(
        action: GeneratorAction.MainType.Password.MinSpecialCharactersChange,
    ) {
        updatePasswordType { currentPasswordType ->
            currentPasswordType.copy(
                minSpecial = action.minSpecial,
            )
        }
        updatePasswordLength()
    }

    private fun handleToggleAmbiguousChars(
        action: GeneratorAction.MainType.Password.ToggleAvoidAmbiguousCharactersChange,
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
        action: GeneratorAction.MainType.Passphrase,
    ) {
        when (action) {
            is GeneratorAction.MainType.Passphrase.NumWordsCounterChange -> {
                handleNumWordsCounterChange(action)
            }

            is GeneratorAction.MainType.Passphrase.ToggleCapitalizeChange -> {
                handlePassphraseToggleCapitalizeChange(action)
            }

            is GeneratorAction.MainType.Passphrase.ToggleIncludeNumberChange -> {
                handlePassphraseToggleIncludeNumberChange(action)
            }

            is GeneratorAction.MainType.Passphrase.WordSeparatorTextChange -> {
                handleWordSeparatorTextInputChange(action)
            }
        }
    }

    private fun handlePassphraseToggleCapitalizeChange(
        action: GeneratorAction.MainType.Passphrase.ToggleCapitalizeChange,
    ) {
        updatePassphraseType { currentPassphraseType ->
            currentPassphraseType.copy(
                capitalize = action.capitalize,
            )
        }
    }

    private fun handlePassphraseToggleIncludeNumberChange(
        action: GeneratorAction.MainType.Passphrase.ToggleIncludeNumberChange,
    ) {
        updatePassphraseType { currentPassphraseType ->
            currentPassphraseType.copy(
                includeNumber = action.includeNumber,
            )
        }
    }

    private fun handleNumWordsCounterChange(
        action: GeneratorAction.MainType.Passphrase.NumWordsCounterChange,
    ) {
        updatePassphraseType { passphraseType ->
            passphraseType.copy(numWords = action.numWords)
        }
    }

    private fun handleWordSeparatorTextInputChange(
        action: GeneratorAction.MainType.Passphrase.WordSeparatorTextChange,
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
            GeneratorState.MainType.Username.UsernameTypeOption.PLUS_ADDRESSED_EMAIL -> {
                loadUsernameOptions(
                    selectedType = GeneratorState.MainType.Username(
                        selectedType = PlusAddressedEmail(),
                    ),
                    forceRegeneration = true,
                )
            }

            GeneratorState.MainType.Username.UsernameTypeOption.CATCH_ALL_EMAIL -> {
                loadUsernameOptions(
                    selectedType = GeneratorState.MainType.Username(
                        selectedType = CatchAllEmail(),
                    ),
                    forceRegeneration = true,
                )
            }

            // We do not force regeneration here since the API can fail if the data is entered
            // incorrectly. This will only be generated when the user clicks the regenerate button.
            GeneratorState.MainType.Username.UsernameTypeOption.FORWARDED_EMAIL_ALIAS -> {
                loadUsernameOptions(
                    selectedType = GeneratorState.MainType.Username(
                        selectedType = ForwardedEmailAlias(),
                    ),
                    forceRegeneration = false,
                )
            }

            GeneratorState.MainType.Username.UsernameTypeOption.RANDOM_WORD -> loadUsernameOptions(
                selectedType = GeneratorState.MainType.Username(selectedType = RandomWord()),
                forceRegeneration = true,
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
        val options = generatorRepository.getUsernameGenerationOptions()
            ?: generateUsernameDefaultOptions()
        when (action.serviceTypeOption) {
            ForwardedEmailAlias.ServiceTypeOption.ADDY_IO -> updateForwardedEmailAliasType {
                ForwardedEmailAlias(
                    selectedServiceType = AddyIo(
                        apiAccessToken = options.anonAddyApiAccessToken.orEmpty(),
                        domainName = options.anonAddyDomainName.orEmpty(),
                    ),
                )
            }

            ForwardedEmailAlias.ServiceTypeOption.DUCK_DUCK_GO -> updateForwardedEmailAliasType {
                ForwardedEmailAlias(
                    selectedServiceType = DuckDuckGo(
                        apiKey = options.duckDuckGoApiKey.orEmpty(),
                    ),
                )
            }

            ForwardedEmailAlias.ServiceTypeOption.FAST_MAIL -> updateForwardedEmailAliasType {
                ForwardedEmailAlias(
                    selectedServiceType = FastMail(
                        apiKey = options.fastMailApiKey.orEmpty(),
                    ),
                )
            }

            ForwardedEmailAlias.ServiceTypeOption.FIREFOX_RELAY -> updateForwardedEmailAliasType {
                ForwardedEmailAlias(
                    selectedServiceType = FirefoxRelay(
                        apiAccessToken = options.firefoxRelayApiAccessToken.orEmpty(),
                    ),
                )
            }

            ForwardedEmailAlias.ServiceTypeOption.FORWARD_EMAIL -> updateForwardedEmailAliasType {
                ForwardedEmailAlias(
                    selectedServiceType = ForwardEmail(
                        apiKey = options.forwardEmailApiAccessToken.orEmpty(),
                        domainName = options.forwardEmailDomainName.orEmpty(),
                    ),
                )
            }

            ForwardedEmailAlias.ServiceTypeOption.SIMPLE_LOGIN -> updateForwardedEmailAliasType {
                ForwardedEmailAlias(
                    selectedServiceType = SimpleLogin(
                        apiKey = options.simpleLoginApiKey.orEmpty(),
                    ),
                )
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

    //region ForwardEmail Email Specific Handlers

    private fun handleForwardEmailSpecificAction(
        action: GeneratorAction
        .MainType
        .Username
        .UsernameType
        .ForwardedEmailAlias
        .ForwardEmail,
    ) {
        when (action) {
            is GeneratorAction
            .MainType
            .Username
            .UsernameType
            .ForwardedEmailAlias
            .ForwardEmail
            .ApiKeyTextChange,
                -> {
                handleForwardEmailApiKeyTextChange(action)
            }

            is GeneratorAction
            .MainType
            .Username
            .UsernameType
            .ForwardedEmailAlias
            .ForwardEmail
            .DomainNameTextChange,
                -> {
                handleForwardEmailDomainNameTextChange(action)
            }
        }
    }

    private fun handleForwardEmailApiKeyTextChange(
        action: GeneratorAction
        .MainType
        .Username
        .UsernameType
        .ForwardedEmailAlias
        .ForwardEmail
        .ApiKeyTextChange,
    ) {
        updateForwardEmailServiceType { forwardEmailServiceType ->
            val newApiKey = action.apiKey
            forwardEmailServiceType.copy(apiKey = newApiKey)
        }
    }

    private fun handleForwardEmailDomainNameTextChange(
        action: GeneratorAction
        .MainType
        .Username
        .UsernameType
        .ForwardedEmailAlias
        .ForwardEmail
        .DomainNameTextChange,
    ) {
        updateForwardEmailServiceType { forwardEmailServiceType ->
            val newDomainName = action.domainName
            forwardEmailServiceType.copy(domainName = newDomainName)
        }
    }

    //endregion ForwardEmail Email Specific Handlers

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
        forceRegeneration: Boolean = false,
        crossinline block: (GeneratorState.MainType) -> GeneratorState.MainType?,
    ) {
        val currentSelectedType = state.selectedType
        val updatedMainType = block(currentSelectedType) ?: return
        mutableStateFlow.update { it.copy(selectedType = updatedMainType) }

        generateTextJob.cancel()
        generateTextJob = viewModelScope.launch {
            when (updatedMainType) {
                is GeneratorState.MainType.Passphrase -> {
                    savePassphraseOptionsToDisk(updatedMainType)
                    generatePassphrase(updatedMainType)
                }

                is GeneratorState.MainType.Password -> {
                    savePasswordOptionsToDisk(updatedMainType)
                    generatePassword(updatedMainType)
                }

                is GeneratorState.MainType.Username -> {
                    when (val selectedType = updatedMainType.selectedType) {
                        is ForwardedEmailAlias -> {
                            saveForwardedEmailAliasServiceTypeToDisk(selectedType)
                            if (forceRegeneration) {
                                generateForwardedEmailAlias(selectedType)
                            } else {
                                mutableStateFlow.update {
                                    it.copy(generatedText = NO_GENERATED_TEXT)
                                }
                            }
                        }

                        is CatchAllEmail -> {
                            saveCatchAllEmailOptionsToDisk(selectedType)
                            if (forceRegeneration) {
                                generateCatchAllEmail(selectedType)
                            } else {
                                mutableStateFlow.update {
                                    it.copy(generatedText = NO_GENERATED_TEXT)
                                }
                            }
                        }

                        is PlusAddressedEmail -> {
                            savePlusAddressedEmailOptionsToDisk(selectedType)
                            if (forceRegeneration) {
                                generatePlusAddressedEmail(selectedType)
                            } else {
                                mutableStateFlow.update {
                                    it.copy(generatedText = NO_GENERATED_TEXT)
                                }
                            }
                        }

                        is RandomWord -> {
                            saveRandomWordOptionsToDisk(selectedType)
                            generateRandomWordUsername(selectedType)
                        }
                    }
                }
            }
        }
    }

    private suspend fun generateForwardedEmailAlias(alias: ForwardedEmailAlias) {
        val request = alias.selectedServiceType?.toUsernameGeneratorRequest(state.website) ?: run {
            mutableStateFlow.update { it.copy(generatedText = NO_GENERATED_TEXT) }
            return
        }
        val result = generatorRepository.generateForwardedServiceUsername(request)
        sendAction(GeneratorAction.Internal.UpdateGeneratedForwardedServiceUsernameResult(result))
    }

    private suspend fun generatePlusAddressedEmail(plusAddressedEmail: PlusAddressedEmail) {
        val result = generatorRepository.generatePlusAddressedEmail(
            UsernameGeneratorRequest.Subaddress(
                type = AppendType.Random,
                email = plusAddressedEmail.email,
            ),
        )
        sendAction(GeneratorAction.Internal.UpdateGeneratedPlusAddressedUsernameResult(result))
    }

    private suspend fun generateCatchAllEmail(catchAllEmail: CatchAllEmail) {
        val domainName = catchAllEmail.domainName.orNullIfBlank() ?: run {
            mutableStateFlow.update { it.copy(generatedText = NO_GENERATED_TEXT) }
            return
        }
        val result = generatorRepository.generateCatchAllEmail(
            UsernameGeneratorRequest.Catchall(
                type = AppendType.Random,
                domain = domainName,
            ),
        )
        sendAction(GeneratorAction.Internal.UpdateGeneratedCatchAllUsernameResult(result))
    }

    private suspend fun generateRandomWordUsername(randomWord: RandomWord) {
        val result = generatorRepository.generateRandomWordUsername(
            UsernameGeneratorRequest.Word(
                capitalize = randomWord.capitalize,
                includeNumber = randomWord.includeNumber,
            ),
        )
        sendAction(GeneratorAction.Internal.UpdateGeneratedRandomWordUsernameResult(result))
    }

    /**
     * Updates the length property of the [GeneratorState.MainType.Password] to reflect the new
     * minimum.
     */
    private fun updatePasswordLength() {
        updatePasswordType { currentPasswordType ->
            currentPasswordType.copy(
                length = max(
                    currentPasswordType.length,
                    currentPasswordType.computedMinimumLength,
                ),
            )
        }
    }

    private inline fun updatePasswordType(
        crossinline block: (GeneratorState.MainType.Password) -> GeneratorState.MainType.Password,
    ) {
        updateGeneratorMainType { currentSelectedType ->
            if (currentSelectedType !is GeneratorState.MainType.Password) {
                return@updateGeneratorMainType currentSelectedType
            }
            currentSelectedType.let(block).enforceAtLeastOneToggleOn()
        }
    }

    private inline fun updatePassphraseType(
        crossinline block: (
            GeneratorState.MainType.Passphrase,
        ) -> GeneratorState.MainType.Passphrase,
    ) {
        updateGeneratorMainType { currentSelectedType ->
            if (currentSelectedType !is GeneratorState.MainType.Passphrase) {
                return@updateGeneratorMainType currentSelectedType
            }
            block(currentSelectedType)
        }
    }

    private inline fun updateGeneratorMainTypeUsername(
        crossinline block: (GeneratorState.MainType.Username) -> GeneratorState.MainType.Username,
    ) {
        updateGeneratorMainType {
            if (it !is GeneratorState.MainType.Username) null else block(it)
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

    private inline fun updateForwardEmailServiceType(
        crossinline block: (ForwardEmail) -> ForwardEmail,
    ) {
        updateGeneratorMainTypeUsername { currentUsernameType ->
            if (currentUsernameType.selectedType !is ForwardedEmailAlias) {
                return@updateGeneratorMainTypeUsername currentUsernameType
            }

            val currentServiceType = currentUsernameType.selectedType.selectedServiceType
            if (currentServiceType !is ForwardEmail) {
                return@updateGeneratorMainTypeUsername currentUsernameType
            }

            currentUsernameType.copy(
                selectedType = ForwardedEmailAlias(
                    selectedServiceType = block(currentServiceType),
                    obfuscatedText = currentUsernameType.selectedType.obfuscatedText,
                ),
            )
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
}

/**
 * Represents the state of the generator, maintaining the generated text and the
 * selected type along with the options for the type of generation (PASSWORD, USERNAME).
 *
 * @param generatedText The text that is generated based on the selected options.
 * @param selectedType The currently selected main type for generating text.
 * @param currentEmailAddress The email address for the current user.
 */
@Parcelize
data class GeneratorState(
    val generatedText: String,
    val selectedType: MainType,
    val generatorMode: GeneratorMode = GeneratorMode.Default,
    val currentEmailAddress: String,
    val isUnderPolicy: Boolean = false,
    val website: String? = null,
    var passcodePolicyOverride: PasscodePolicyOverride? = null,
) : Parcelable {

    /**
     * Provides a list of available main types for the generator based on the [GeneratorMode].
     */
    val typeOptions: List<MainTypeOption>
        get() = when (generatorMode) {
            GeneratorMode.Default -> MainTypeOption.entries.toList()
            GeneratorMode.Modal.Password -> MainTypeOption
                .entries
                .filter { it != MainTypeOption.USERNAME }

            is GeneratorMode.Modal.Username -> emptyList()
        }

    /**
     * Enum representing the main type options for the generator, such as PASSWORD PASSPHRASE, and
     * USERNAME.
     *
     * @property labelRes The resource ID of the string that represents the label of each type.
     * @property testTag The string used as the test tag for the option.
     */
    enum class MainTypeOption(
        val labelRes: Int,
        val testTag: String,
    ) {
        PASSWORD(labelRes = R.string.password, testTag = "password_option"),
        PASSPHRASE(labelRes = R.string.passphrase, testTag = "passphrase_option"),
        USERNAME(labelRes = R.string.username, testTag = "username_option"),
    }

    /**
     * Enum representing the passcode types for the generator, such as PASSWORD and PASSPHRASE.
     */
    enum class PasscodePolicyOverride {
        PASSWORD,
        PASSPHRASE,
    }

    /**
     * A sealed class representing the main types that can be selected in the generator,
     * encapsulating the different configurations and properties each main type has.
     */
    @Parcelize
    sealed class MainType : Parcelable {

        /**
         * Indicates the type this is.
         */
        abstract val mainTypeOption: MainTypeOption

        /**
         * Represents a standard PASSWORD type, with configurable options for
         * length, character types, and requirements.
         *
         * @property length The length of the generated password.
         * @property minLength The number available on the low end of the slider.
         * @property maxLength The number available on the high end of the slider.
         * @property useCapitals Whether to include capital letters.
         * @property capitalsEnabled Whether capitals are enabled for this password.
         * @property useLowercase Whether to include lowercase letters.
         * @property lowercaseEnabled Whether lowercase letters are enabled.
         * @property useNumbers Whether to include numbers.
         * @property numbersEnabled Whether numbers are enabled for this password.
         * @property useSpecialChars Whether to include special characters.
         * @property specialCharsEnabled Whether special characters are enabled.
         * @property minNumbers The minimum number of numeric characters.
         * @property minNumbersAllowed The minimum number of numbers permitted.
         * @property maxNumbersAllowed The maximum number of numbers permitted.
         * @property minSpecial The minimum number of special characters.
         * @property minSpecialAllowed The minimum number of special characters permitted.
         * @property maxSpecialAllowed The maximum number of special characters permitted.
         * @property avoidAmbiguousChars Whether to avoid characters that look similar.
         * @property ambiguousCharsEnabled Whether to allow ambiguous characters.
         * @property isUserInteracting Indicates whether the user is currently interacting
         * with a control. This flag can be used to prevent unnecessary updates or
         * processing during continuous interaction.
         */
        @Parcelize
        data class Password(
            val length: Int = DEFAULT_PASSWORD_LENGTH,
            val minLength: Int = PASSWORD_LENGTH_SLIDER_MIN,
            val maxLength: Int = PASSWORD_LENGTH_SLIDER_MAX,
            val useCapitals: Boolean = true,
            val capitalsEnabled: Boolean = true,
            val useLowercase: Boolean = true,
            val lowercaseEnabled: Boolean = true,
            val useNumbers: Boolean = true,
            val numbersEnabled: Boolean = true,
            val useSpecialChars: Boolean = false,
            val specialCharsEnabled: Boolean = true,
            val minNumbers: Int = MIN_NUMBERS,
            val minNumbersAllowed: Int = PASSWORD_COUNTER_MIN,
            val maxNumbersAllowed: Int = PASSWORD_COUNTER_MAX,
            val minSpecial: Int = MIN_SPECIAL,
            val minSpecialAllowed: Int = PASSWORD_COUNTER_MIN,
            val maxSpecialAllowed: Int = PASSWORD_COUNTER_MAX,
            val avoidAmbiguousChars: Boolean = false,
            val ambiguousCharsEnabled: Boolean = true,
            val isUserInteracting: Boolean = false,
        ) : MainType() {
            override val mainTypeOption: MainTypeOption get() = MainTypeOption.PASSWORD

            /**
             * The computed minimum length for the generated Password
             * based on what characters must be included.
             */
            val computedMinimumLength: Int
                get() {
                    val minLowercase = if (useLowercase) 1 else 0
                    val minUppercase = if (useCapitals) 1 else 0
                    val minimumNumbers = if (useNumbers) max(1, minNumbers) else 0
                    val minimumSpecial = if (useSpecialChars) max(1, minSpecial) else 0
                    return max(
                        minLength,
                        minLowercase + minUppercase + minimumNumbers + minimumSpecial,
                    )
                }

            @Suppress("UndocumentedPublicClass")
            companion object {
                private const val DEFAULT_PASSWORD_LENGTH: Int = 14
                private const val MIN_NUMBERS: Int = 1
                private const val MIN_SPECIAL: Int = 1

                const val PASSWORD_LENGTH_SLIDER_MIN: Int = 5
                const val PASSWORD_LENGTH_SLIDER_MAX: Int = 128
                const val PASSWORD_COUNTER_MIN: Int = 0
                const val PASSWORD_COUNTER_MAX: Int = 9
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
            val minNumWords: Int = PASSPHRASE_MIN_NUMBER_OF_WORDS,
            val maxNumWords: Int = PASSPHRASE_MAX_NUMBER_OF_WORDS,
            val wordSeparator: Char? = DEFAULT_PASSPHRASE_SEPARATOR,
            val capitalize: Boolean = false,
            val capitalizeEnabled: Boolean = true,
            val includeNumber: Boolean = false,
            val includeNumberEnabled: Boolean = true,
        ) : MainType() {
            override val mainTypeOption: MainTypeOption get() = MainTypeOption.PASSPHRASE

            @Suppress("UndocumentedPublicClass")
            companion object {
                private const val DEFAULT_PASSPHRASE_SEPARATOR: Char = '-'
                private const val DEFAULT_NUM_WORDS: Int = 3

                const val PASSPHRASE_MIN_NUMBER_OF_WORDS: Int = 3
                const val PASSPHRASE_MAX_NUMBER_OF_WORDS: Int = 20
            }
        }

        /**
         * Represents the USERNAME main type, holding the configuration
         * and properties for generating usernames.
         */
        @Parcelize
        data class Username(
            val selectedType: UsernameType = PlusAddressedEmail(),
        ) : MainType() {
            override val mainTypeOption: MainTypeOption get() = MainTypeOption.USERNAME

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
                    val email: String = "",
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
                        FORWARD_EMAIL(R.string.forward_email),
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
                            val baseUrl: String = "https://app.addy.io",
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
                         * Represents the ForwardEmail service type, with configurable options for
                         * api key and domain name.
                         *
                         * @property apiKey The api key used for generation.
                         * @property domainName The domain name used for generation.
                         */
                        @Parcelize
                        data class ForwardEmail(
                            val apiKey: String = "",
                            val domainName: String = "",
                        ) : ServiceType(), Parcelable {
                            override val displayStringResId: Int
                                get() = ServiceTypeOption.FORWARD_EMAIL.labelRes
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
     * Indicates the UI has been entered a resumed lifecycle state.
     */
    data object LifecycleResume : GeneratorAction()

    /**
     * Indicates that the overflow option for password history has been clicked.
     */
    data object PasswordHistoryClick : GeneratorAction()

    /**
     * Indicates the user has selected a generated string from the modal generator
     */
    data object SelectClick : GeneratorAction()

    /**
     * Indicates the user has clicked the close button.
     */
    data object CloseClick : GeneratorAction()

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
         * Represents actions specifically related to passwords, a subtype of passcode.
         */
        sealed class Password : MainType() {
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
            data class ToggleAvoidAmbiguousCharactersChange(
                val avoidAmbiguousChars: Boolean,
            ) : Password()
        }

        /**
         * Represents actions specifically related to passphrases, a subtype of passcode.
         */
        sealed class Passphrase : MainType() {

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
                 * Represents the action to learn more.
                 */
                data object TooltipClick : UsernameType()

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
                     * Represents actions specifically related to the ForwardEmail service.
                     */
                    sealed class ForwardEmail : ForwardedEmailAlias() {

                        /**
                         * Fired when the api key input text is changed.
                         *
                         * @property apiKey The new api key text.
                         */
                        data class ApiKeyTextChange(val apiKey: String) : ForwardEmail()

                        /**
                         * Fires when the domain name input text is changed.
                         *
                         * @property domainName The new domain name text.
                         */
                        data class DomainNameTextChange(val domainName: String) : ForwardEmail()
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
         * Indicates that updated policies have been received.
         */
        data class PasswordGeneratorPolicyReceive(
            val policies: List<PolicyInformation.PasswordGenerator>,
        ) : Internal()

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
        data class UpdateGeneratedPlusAddressedUsernameResult(
            val result: GeneratedPlusAddressedUsernameResult,
        ) : Internal()

        /**
         * Indicates a generated text update is received.
         */
        data class UpdateGeneratedCatchAllUsernameResult(
            val result: GeneratedCatchAllUsernameResult,
        ) : Internal()

        /**
         * Indicates a generated text update is received.
         */
        data class UpdateGeneratedRandomWordUsernameResult(
            val result: GeneratedRandomWordUsernameResult,
        ) : Internal()

        /**
         * Indicates a generated text update is received.
         */
        data class UpdateGeneratedForwardedServiceUsernameResult(
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
     * Navigate back to previous screen.
     */
    data object NavigateBack : GeneratorEvent()

    /**
     * Navigate back to learn more screen.
     */
    data object NavigateToTooltip : GeneratorEvent()

    /**
     * Displays the message in a snackbar.
     */
    data class ShowSnackbar(
        val message: Text,
    ) : GeneratorEvent()
}

@Suppress("ComplexCondition", "MaxLineLength")
private fun GeneratorState.MainType.Password.enforceAtLeastOneToggleOn(): GeneratorState.MainType.Password =
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

private val PasscodeGenerationOptions?.passcodeType: GeneratorState.MainType
    get() = when (this?.type) {
        PasscodeGenerationOptions.PasscodeType.PASSWORD -> GeneratorState.MainType.Password()
        PasscodeGenerationOptions.PasscodeType.PASSPHRASE -> GeneratorState.MainType.Passphrase()
        else -> GeneratorState.MainType.Password()
    }

private val UsernameGenerationOptions?.usernameType: GeneratorState.MainType.Username.UsernameType
    get() = when (this?.type) {
        UsernameGenerationOptions.UsernameType.PLUS_ADDRESSED_EMAIL -> PlusAddressedEmail()
        UsernameGenerationOptions.UsernameType.CATCH_ALL_EMAIL -> CatchAllEmail()
        UsernameGenerationOptions.UsernameType.FORWARDED_EMAIL_ALIAS -> ForwardedEmailAlias()
        UsernameGenerationOptions.UsernameType.RANDOM_WORD -> RandomWord()
        else -> PlusAddressedEmail()
    }

private fun String?.toPasscodePolicyOverride(): GeneratorState.PasscodePolicyOverride? =
    if (this.isNullOrBlank()) {
        null
    } else {
        when (this) {
            PolicyInformation.PasswordGenerator.TYPE_PASSPHRASE -> {
                GeneratorState.PasscodePolicyOverride.PASSPHRASE
            }

            else -> GeneratorState.PasscodePolicyOverride.PASSWORD
        }
    }
