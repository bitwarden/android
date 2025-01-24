package com.x8bit.bitwarden.ui.tools.feature.generator

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.bitwarden.generators.PasswordGeneratorRequest
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.ReviewPromptManager
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedCatchAllUsernameResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedForwardedServiceUsernameResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedPassphraseResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedPasswordResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedRandomWordUsernameResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratorResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.PasscodeGenerationOptions
import com.x8bit.bitwarden.data.tools.generator.repository.model.UsernameGenerationOptions
import com.x8bit.bitwarden.data.tools.generator.repository.util.FakeGeneratorRepository
import com.x8bit.bitwarden.data.vault.datasource.network.model.PolicyTypeJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockPolicy
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias.ServiceType
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias.ServiceTypeOption
import com.x8bit.bitwarden.ui.tools.feature.generator.model.GeneratorMode
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@Suppress("LargeClass")
class GeneratorViewModelTest : BaseViewModelTest() {

    private val initialPasscodeState = createPasswordState()
    private val initialPasscodeSavedStateHandle =
        createSavedStateHandleWithState(initialPasscodeState)

    private val initialUsernameModeState = createUsernameModeState()

    private val initialPassphraseState = createPassphraseState()
    private val passphraseSavedStateHandle = createSavedStateHandleWithState(initialPassphraseState)

    private val initialUsernameState = createPlusAddressedEmailState()
    private val usernameSavedStateHandle = createSavedStateHandleWithState(initialUsernameState)

    private val initialForwardedEmailAliasState = createForwardedEmailAliasState()
    private val forwardedEmailAliasSavedStateHandle =
        createSavedStateHandleWithState(initialForwardedEmailAliasState)

    private val initialAddyIoState = createAddyIoState()
    private val addyIoSavedStateHandle = createSavedStateHandleWithState(initialAddyIoState)

    private val initialDuckDuckGoState = createDuckDuckGoState()
    private val duckDuckGoSavedStateHandle = createSavedStateHandleWithState(initialDuckDuckGoState)

    private val initialFastMailState = createFastMailState()
    private val fastMailSavedStateHandle = createSavedStateHandleWithState(initialFastMailState)

    private val initialFirefoxRelay = createFirefoxRelayState()
    private val firefoxRelaySavedStateHandle = createSavedStateHandleWithState(initialFirefoxRelay)

    private val initialSimpleLogin = createSimpleLoginState()
    private val simpleLoginSavedStateHandle = createSavedStateHandleWithState(initialSimpleLogin)

    private val initialCatchAllEmailState = createCatchAllEmailState()
    private val catchAllEmailSavedStateHandle =
        createSavedStateHandleWithState(initialCatchAllEmailState)

    private val initialRandomWordState = createRandomWordState()
    private val randomWordSavedStateHandle = createSavedStateHandleWithState(initialRandomWordState)

    private val mutableUserStateFlow = MutableStateFlow<UserState?>(DEFAULT_USER_STATE)
    private val authRepository = mockk<AuthRepository> {
        every { userStateFlow } returns mutableUserStateFlow
    }

    private val clipboardManager: BitwardenClipboardManager = mockk()
    private val fakeGeneratorRepository = FakeGeneratorRepository().apply {
        setMockGeneratePasswordResult(
            GeneratedPasswordResult.Success("defaultPassword"),
        )
    }

    private val mutablePolicyFlow = bufferedMutableSharedFlow<List<SyncResponseJson.Policy>>()
    private val policyManager: PolicyManager = mockk {
        every { getActivePolicies(PolicyTypeJson.PASSWORD_GENERATOR) } returns emptyList()
        every { getActivePoliciesFlow(PolicyTypeJson.PASSWORD_GENERATOR) } returns mutablePolicyFlow
    }

    private val reviewPromptManager: ReviewPromptManager = mockk {
        every { registerGeneratedResultAction() } just runs
    }

    @Test
    fun `initial state should be correct when there is no saved state`() {
        val viewModel = createViewModel(state = null)
        assertEquals(initialPasscodeState, viewModel.stateFlow.value)
    }

    @Test
    fun `initial state should be correct when there is a saved state`() {
        val viewModel = createViewModel(state = initialPasscodeState)
        assertEquals(initialPasscodeState, viewModel.stateFlow.value)
    }

    @Test
    fun `initial state should be correct for username modal`() {
        val usernameGenerationOptions = UsernameGenerationOptions(
            type = UsernameGenerationOptions.UsernameType.RANDOM_WORD,
        )
        fakeGeneratorRepository.saveUsernameGenerationOptions(usernameGenerationOptions)
        val expected = GeneratorState(
            generatedText = "randomWord",
            selectedType = GeneratorState.MainType.Username(
                selectedType = GeneratorState.MainType.Username.UsernameType.RandomWord(
                    capitalize = false,
                    includeNumber = false,
                ),
            ),
            generatorMode = GeneratorMode.Modal.Username(website = ""),
            currentEmailAddress = "currentEmail",
            isUnderPolicy = false,
            website = "",
        )

        val viewModel = createViewModel(
            state = null,
            type = "username_generator",
            website = "",
        )
        assertEquals(expected, viewModel.stateFlow.value)
    }

    @Test
    fun `initial state should be correct for passcode modal`() {
        val passcodeGenerationOptions = PasscodeGenerationOptions(
            type = PasscodeGenerationOptions.PasscodeType.PASSPHRASE,
            length = 14,
            allowAmbiguousChar = true,
            hasNumbers = true,
            minNumber = 1,
            hasUppercase = true,
            hasLowercase = true,
            allowSpecial = false,
            minSpecial = 1,
            numWords = 3,
            wordSeparator = "-",
            allowCapitalize = false,
            allowIncludeNumber = false,
        )
        fakeGeneratorRepository.savePasscodeGenerationOptions(passcodeGenerationOptions)
        val expected = GeneratorState(
            generatedText = "updatedPassphrase",
            selectedType = GeneratorState.MainType.Passphrase(),
            generatorMode = GeneratorMode.Modal.Password,
            currentEmailAddress = "currentEmail",
            isUnderPolicy = false,
            website = null,
        )

        val viewModel = createViewModel(
            state = null,
            type = "password_generator",
        )
        assertEquals(expected, viewModel.stateFlow.value)
    }

    @Test
    fun `activePolicyFlow changes should update state`() = runTest {
        val payload = mapOf(
            "overridePasswordType" to JsonNull,
            "minLength" to JsonPrimitive(10),
            "useUpper" to JsonPrimitive(true),
            "useNumbers" to JsonPrimitive(true),
            "useSpecial" to JsonPrimitive(true),
            "minNumbers" to JsonPrimitive(3),
            "minSpecial" to JsonPrimitive(3),
            "minNumberWords" to JsonPrimitive(5),
            "capitalize" to JsonPrimitive(true),
            "includeNumber" to JsonPrimitive(true),
            "useLower" to JsonPrimitive(true),
        )
        val policies = listOf(
            SyncResponseJson.Policy(
                organizationId = "organizationId",
                id = "id",
                type = PolicyTypeJson.PASSWORD_GENERATOR,
                isEnabled = true,
                data = JsonObject(payload),
            ),
        )
        val viewModel = createViewModel(state = initialPasscodeState)

        viewModel.stateFlow.test {
            assertEquals(initialPasscodeState, awaitItem())
            every {
                policyManager.getActivePolicies(type = PolicyTypeJson.PASSWORD_GENERATOR)
            } returns policies
            mutablePolicyFlow.tryEmit(value = policies)
            assertEquals(
                initialPasscodeState.copy(
                    selectedType = GeneratorState.MainType.Password(
                        length = 14,
                        minLength = 10,
                        maxLength = 128,
                        useCapitals = true,
                        capitalsEnabled = false,
                        useLowercase = true,
                        lowercaseEnabled = false,
                        useNumbers = true,
                        numbersEnabled = false,
                        useSpecialChars = true,
                        specialCharsEnabled = false,
                        minNumbers = 3,
                        minNumbersAllowed = 3,
                        maxNumbersAllowed = 9,
                        minSpecial = 3,
                        minSpecialAllowed = 3,
                        maxSpecialAllowed = 9,
                        avoidAmbiguousChars = false,
                        ambiguousCharsEnabled = true,
                        isUserInteracting = false,
                    ),
                    isUnderPolicy = true,
                ),
                awaitItem(),
            )
            every {
                policyManager.getActivePolicies(type = PolicyTypeJson.PASSWORD_GENERATOR)
            } returns emptyList()
            mutablePolicyFlow.tryEmit(value = emptyList())
            assertEquals(
                initialPasscodeState.copy(
                    selectedType = GeneratorState.MainType.Password(
                        length = 14,
                        minLength = 5,
                        maxLength = 128,
                        useCapitals = true,
                        capitalsEnabled = true,
                        useLowercase = true,
                        lowercaseEnabled = true,
                        useNumbers = true,
                        numbersEnabled = true,
                        useSpecialChars = true,
                        specialCharsEnabled = true,
                        minNumbers = 3,
                        minNumbersAllowed = 0,
                        maxNumbersAllowed = 9,
                        minSpecial = 3,
                        minSpecialAllowed = 0,
                        maxSpecialAllowed = 9,
                        avoidAmbiguousChars = false,
                        ambiguousCharsEnabled = true,
                        isUserInteracting = false,
                    ),
                    isUnderPolicy = false,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `CloseClick should emit NavigateBack event`() = runTest {
        val viewModel = createViewModel()
        viewModel.trySendAction(GeneratorAction.CloseClick)

        viewModel.eventFlow.test {
            val event = awaitItem()
            assertEquals(GeneratorEvent.NavigateBack, event)
        }
    }

    @Test
    fun `SelectClick should emit the NavigateBack event with GeneratorResult`() = runTest {
        turbineScope {
            val viewModel = createViewModel(state = initialUsernameModeState)
            val eventTurbine = viewModel
                .eventFlow
                .testIn(backgroundScope)
            val generatorResultTurbine = fakeGeneratorRepository
                .generatorResultFlow
                .testIn(backgroundScope)

            viewModel.trySendAction(GeneratorAction.SelectClick)

            assertEquals(GeneratorEvent.NavigateBack, eventTurbine.awaitItem())
            assertEquals(
                GeneratorResult.Username(username = "email+abcd1234@address.com"),
                generatorResultTurbine.awaitItem(),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `RegenerateClick action for password state updates generatedText and saves password generation options on successful password generation`() =
        runTest {
            val updatedGeneratedPassword = "updatedPassword"

            val viewModel = createViewModel()
            val initialState = viewModel.stateFlow.value

            val updatedPasswordOptions = PasscodeGenerationOptions(
                type = PasscodeGenerationOptions.PasscodeType.PASSWORD,
                length = 14,
                allowAmbiguousChar = false,
                hasNumbers = true,
                minNumber = 1,
                hasUppercase = true,
                minUppercase = null,
                hasLowercase = true,
                minLowercase = null,
                allowSpecial = false,
                minSpecial = 1,
                allowCapitalize = false,
                allowIncludeNumber = false,
                wordSeparator = "-",
                numWords = 3,
            )

            fakeGeneratorRepository.setMockGeneratePasswordResult(
                GeneratedPasswordResult.Success(updatedGeneratedPassword),
            )

            viewModel.trySendAction(GeneratorAction.RegenerateClick)

            val expectedState = initialState.copy(generatedText = updatedGeneratedPassword)
            assertEquals(expectedState, viewModel.stateFlow.value)

            assertEquals(
                updatedPasswordOptions,
                fakeGeneratorRepository.getPasscodeGenerationOptions(),
            )

            fakeGeneratorRepository.assertEqualsStoredRequest(
                PasswordGeneratorRequest(
                    lowercase = true,
                    uppercase = true,
                    numbers = true,
                    special = false,
                    length = 14.toUByte(),
                    avoidAmbiguous = false,
                    minLowercase = null,
                    minUppercase = null,
                    minNumber = 1.toUByte(),
                    minSpecial = 1.toUByte(),
                ),
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `RegenerateClick action for password state sends ShowSnackbar event on password generation failure`() =
        runTest {
            val viewModel = createViewModel()

            fakeGeneratorRepository.setMockGeneratePasswordResult(
                GeneratedPasswordResult.InvalidRequest,
            )

            viewModel.trySendAction(GeneratorAction.RegenerateClick)

            viewModel.eventFlow.test {
                assertEquals(
                    GeneratorEvent.ShowSnackbar(R.string.an_error_has_occurred.asText()),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `RegenerateClick action for passphrase state updates generatedText and saves passphrase generation options on successful passphrase generation`() =
        runTest {
            val updatedGeneratedPassphrase = "updatedPassphrase"

            val viewModel = createViewModel(initialPassphraseState)
            val initialState = viewModel.stateFlow.value

            val updatedPassphraseOptions = PasscodeGenerationOptions(
                type = PasscodeGenerationOptions.PasscodeType.PASSPHRASE,
                length = 14,
                allowAmbiguousChar = false,
                hasNumbers = true,
                minNumber = 1,
                hasUppercase = true,
                minUppercase = null,
                hasLowercase = true,
                minLowercase = null,
                allowSpecial = false,
                minSpecial = 1,
                allowCapitalize = false,
                allowIncludeNumber = false,
                wordSeparator = "-",
                numWords = 3,
            )

            fakeGeneratorRepository.setMockGeneratePassphraseResult(
                GeneratedPassphraseResult.Success(updatedGeneratedPassphrase),
            )

            viewModel.trySendAction(GeneratorAction.RegenerateClick)

            val expectedState = initialState.copy(generatedText = updatedGeneratedPassphrase)
            assertEquals(expectedState, viewModel.stateFlow.value)

            assertEquals(
                updatedPassphraseOptions,
                fakeGeneratorRepository.getPasscodeGenerationOptions(),
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `RegenerateClick action for passphrase state sends ShowSnackbar event on passphrase generation failure`() =
        runTest {
            val viewModel = createViewModel(initialPassphraseState)

            fakeGeneratorRepository.setMockGeneratePassphraseResult(
                GeneratedPassphraseResult.InvalidRequest,
            )

            viewModel.trySendAction(GeneratorAction.RegenerateClick)

            viewModel.eventFlow.test {
                assertEquals(
                    GeneratorEvent.ShowSnackbar(R.string.an_error_has_occurred.asText()),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `RegenerateClick for plus addressed email state should update the plus addressed email correctly`() =
        runTest {
            val viewModel = createViewModel(usernameSavedStateHandle)

            fakeGeneratorRepository.setMockGeneratePasswordResult(
                GeneratedPasswordResult.Success("DifferentUsername"),
            )

            viewModel.trySendAction(GeneratorAction.RegenerateClick)

            val expectedState =
                initialPasscodeState.copy(
                    generatedText = "email+abcd1234@address.com",
                    selectedType = GeneratorState.MainType.Username(
                        GeneratorState.MainType.Username.UsernameType.PlusAddressedEmail(
                            email = "currentEmail",
                        ),
                    ),
                )

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

    @Test
    fun `RegenerateClick for catch all email state should update the catch all email correctly`() =
        runTest {
            val viewModel = createViewModel(catchAllEmailSavedStateHandle)

            fakeGeneratorRepository.setMockCatchAllResult(
                GeneratedCatchAllUsernameResult.Success("DifferentUsername"),
            )

            viewModel.trySendAction(GeneratorAction.RegenerateClick)

            val expectedState =
                initialCatchAllEmailState.copy(
                    generatedText = "DifferentUsername",
                    selectedType = GeneratorState.MainType.Username(
                        GeneratorState.MainType.Username.UsernameType.CatchAllEmail(
                            domainName = "defaultDomain",
                        ),
                    ),
                )

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `RegenerateClick for random word username state should update the random word username correctly`() =
        runTest {
            val viewModel = createViewModel(randomWordSavedStateHandle)

            fakeGeneratorRepository.setMockRandomWordResult(
                GeneratedRandomWordUsernameResult.Success("DifferentUsername"),
            )

            viewModel.trySendAction(GeneratorAction.RegenerateClick)

            val expectedState =
                initialCatchAllEmailState.copy(
                    generatedText = "DifferentUsername",
                    selectedType = GeneratorState.MainType.Username(
                        GeneratorState.MainType.Username.UsernameType.RandomWord(),
                    ),
                )

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

    @Test
    fun `CopyClick should call setText on ClipboardManager and register generator action`() {
        val viewModel = createViewModel()
        every { clipboardManager.setText(viewModel.stateFlow.value.generatedText) } just runs

        viewModel.trySendAction(GeneratorAction.CopyClick)

        verify(exactly = 1) {
            clipboardManager.setText(text = viewModel.stateFlow.value.generatedText)
            reviewPromptManager.registerGeneratedResultAction()
        }
    }

    @Test
    fun `Policy should overwrite password options if stricter`() {
        val policy = createMockPolicy(
            number = 1,
            type = PolicyTypeJson.PASSWORD_GENERATOR,
            isEnabled = true,
            data = JsonObject(
                mapOf(
                    "overridePasswordType" to JsonNull,
                    "minLength" to JsonPrimitive(10),
                    "useUpper" to JsonPrimitive(true),
                    "useNumbers" to JsonPrimitive(true),
                    "useSpecial" to JsonPrimitive(true),
                    "minNumbers" to JsonPrimitive(3),
                    "minSpecial" to JsonPrimitive(3),
                    "minNumberWords" to JsonPrimitive(5),
                    "capitalize" to JsonPrimitive(true),
                    "includeNumber" to JsonPrimitive(true),
                    "useLower" to JsonPrimitive(true),
                ),
            ),
        )
        every {
            policyManager.getActivePolicies(any())
        } returns listOf(policy)

        val viewModel = createViewModel()
        assertEquals(
            initialPasscodeState.copy(
                selectedType = GeneratorState.MainType.Password(
                    length = 14,
                    minLength = 10,
                    useCapitals = true,
                    capitalsEnabled = false,
                    useLowercase = true,
                    lowercaseEnabled = false,
                    useNumbers = true,
                    numbersEnabled = false,
                    useSpecialChars = true,
                    specialCharsEnabled = false,
                    minNumbers = 3,
                    minNumbersAllowed = 3,
                    minSpecial = 3,
                    minSpecialAllowed = 3,
                    avoidAmbiguousChars = false,
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `Policy should overwrite passphrase options if stricter`() {
        val policy = createMockPolicy(
            number = 1,
            type = PolicyTypeJson.PASSWORD_GENERATOR,
            isEnabled = true,
            data = JsonObject(
                mapOf(
                    "overridePasswordType" to JsonNull,
                    "minLength" to JsonPrimitive(10),
                    "useUpper" to JsonPrimitive(true),
                    "useNumbers" to JsonPrimitive(true),
                    "useSpecial" to JsonPrimitive(true),
                    "minNumbers" to JsonPrimitive(3),
                    "minSpecial" to JsonPrimitive(3),
                    "minNumberWords" to JsonPrimitive(5),
                    "capitalize" to JsonPrimitive(true),
                    "includeNumber" to JsonPrimitive(true),
                    "useLower" to JsonPrimitive(true),
                ),
            ),
        )
        every {
            policyManager.getActivePolicies(any())
        } returns listOf(policy)

        val viewModel = createViewModel()
        val action = GeneratorAction.MainTypeOptionSelect(
            mainTypeOption = GeneratorState.MainTypeOption.PASSPHRASE,
        )

        viewModel.trySendAction(action)

        assertEquals(
            initialPasscodeState.copy(
                generatedText = "updatedPassphrase",
                selectedType = GeneratorState.MainType.Passphrase(
                    numWords = 5,
                    minNumWords = 5,
                    maxNumWords = 20,
                    capitalize = true,
                    capitalizeEnabled = false,
                    includeNumber = true,
                    includeNumberEnabled = false,
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `Policy should overwrite passwordType if has overridePasswordType`() {
        val policy = createMockPolicy(
            number = 1,
            type = PolicyTypeJson.PASSWORD_GENERATOR,
            isEnabled = true,
            data = JsonObject(
                mapOf(
                    "overridePasswordType" to JsonPrimitive("passphrase"),
                ),
            ),
        )
        every {
            policyManager.getActivePolicies(any())
        } returns listOf(policy)

        val viewModel = createViewModel()

        assertEquals(
            initialPasscodeState.copy(
                generatedText = "updatedPassphrase",
                selectedType = GeneratorState.MainType.Passphrase(),
                passcodePolicyOverride = GeneratorState.PasscodePolicyOverride.PASSPHRASE,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `Policy should should prioritize password if multiple have OverridePasswordType`() {
        val policies = listOf(
            createMockPolicy(
                number = 1,
                type = PolicyTypeJson.PASSWORD_GENERATOR,
                isEnabled = true,
                data = JsonObject(
                    mapOf(
                        "overridePasswordType" to JsonPrimitive("passphrase"),
                    ),
                ),
            ),
            createMockPolicy(
                number = 1,
                type = PolicyTypeJson.PASSWORD_GENERATOR,
                isEnabled = true,
                data = JsonObject(
                    mapOf(
                        "overridePasswordType" to JsonPrimitive("password"),
                    ),
                ),
            ),
        )
        every {
            policyManager.getActivePolicies(any())
        } returns policies

        val viewModel = createViewModel()

        assertEquals(
            initialPasscodeState.copy(
                generatedText = "defaultPassword",
                selectedType = GeneratorState.MainType.Password(),
                passcodePolicyOverride = GeneratorState.PasscodePolicyOverride.PASSWORD,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `MainTypeOptionSelect PASSWORD should switch to Passcode`() = runTest {
        val viewModel = createViewModel()
        fakeGeneratorRepository.setMockGeneratePasswordResult(
            GeneratedPasswordResult.Success("updatedText"),
        )

        val action = GeneratorAction.MainTypeOptionSelect(GeneratorState.MainTypeOption.PASSWORD)

        viewModel.trySendAction(action)

        val expectedState = initialPasscodeState.copy(
            selectedType = GeneratorState.MainType.Password(),
            generatedText = "updatedText",
        )

        assertEquals(expectedState, viewModel.stateFlow.value)
    }

    @Test
    fun `MainTypeOptionSelect USERNAME should switch to Username`() = runTest {
        val viewModel = createViewModel()
        fakeGeneratorRepository.setMockGeneratePasswordResult(
            GeneratedPasswordResult.Success("updatedText"),
        )

        val action = GeneratorAction.MainTypeOptionSelect(GeneratorState.MainTypeOption.USERNAME)

        viewModel.trySendAction(action)

        val expectedState =
            initialPasscodeState.copy(
                generatedText = "email+abcd1234@address.com",
                selectedType = GeneratorState.MainType.Username(
                    GeneratorState.MainType.Username.UsernameType.PlusAddressedEmail(
                        email = "currentEmail",
                    ),
                ),
            )

        assertEquals(expectedState, viewModel.stateFlow.value)
    }

    @Test
    fun `UsernameTypeOptionSelect PLUS_ADDRESSED_EMAIL should switch to PlusAddressedEmail type`() =
        runTest {
            val viewModel = createViewModel(initialUsernameState)

            viewModel.trySendAction(
                GeneratorAction.MainType.Username.UsernameTypeOptionSelect(
                    usernameTypeOption = GeneratorState
                        .MainType
                        .Username
                        .UsernameTypeOption
                        .PLUS_ADDRESSED_EMAIL,
                ),
            )

            val expectedState = initialUsernameState.copy(
                generatedText = "email+abcd1234@address.com",
                selectedType = GeneratorState.MainType.Username(
                    selectedType = GeneratorState
                        .MainType
                        .Username
                        .UsernameType
                        .PlusAddressedEmail(
                            email = "currentEmail",
                        ),
                ),
            )

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

    @Test
    fun `UsernameTypeOptionSelect CATCH_ALL_EMAIL should switch to CatchAllEmail type`() = runTest {
        val viewModel = createViewModel(initialUsernameState)

        viewModel.trySendAction(
            GeneratorAction.MainType.Username.UsernameTypeOptionSelect(
                usernameTypeOption = GeneratorState
                    .MainType
                    .Username
                    .UsernameTypeOption
                    .CATCH_ALL_EMAIL,
            ),
        )

        val expectedState = initialUsernameState.copy(
            generatedText = "-",
            selectedType = GeneratorState.MainType.Username(
                selectedType = GeneratorState.MainType.Username.UsernameType.CatchAllEmail(),
            ),
        )

        assertEquals(expectedState, viewModel.stateFlow.value)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `UsernameTypeOptionSelect FORWARDED_EMAIL_ALIAS should switch to ForwardedEmailAlias type`() =
        runTest {
            val viewModel = createViewModel(initialUsernameState)

            viewModel.trySendAction(
                GeneratorAction.MainType.Username.UsernameTypeOptionSelect(
                    usernameTypeOption = GeneratorState
                        .MainType
                        .Username
                        .UsernameTypeOption
                        .FORWARDED_EMAIL_ALIAS,
                ),
            )

            val expectedState = initialUsernameState.copy(
                generatedText = "-",
                selectedType = GeneratorState.MainType.Username(
                    selectedType = GeneratorState
                        .MainType
                        .Username
                        .UsernameType
                        .ForwardedEmailAlias(),
                ),
            )

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

    @Test
    fun `UsernameTypeOptionSelect RANDOM_WORD should switch to RandomWord type`() = runTest {
        val viewModel = createViewModel(initialUsernameState)

        viewModel.trySendAction(
            GeneratorAction.MainType.Username.UsernameTypeOptionSelect(
                usernameTypeOption = GeneratorState
                    .MainType
                    .Username
                    .UsernameTypeOption
                    .RANDOM_WORD,
            ),
        )

        val expectedState = initialUsernameState.copy(
            generatedText = "randomWord",
            selectedType = GeneratorState.MainType.Username(
                selectedType = GeneratorState.MainType.Username.UsernameType.RandomWord(),
            ),
        )

        assertEquals(expectedState, viewModel.stateFlow.value)
    }

    @Test
    fun `TooltipClick should emit NavigateToTooltip event`() = runTest {
        val viewModel = createViewModel(initialUsernameState)

        viewModel.trySendAction(GeneratorAction.MainType.Username.UsernameType.TooltipClick)

        viewModel.eventFlow.test {
            val event = awaitItem()
            assertEquals(GeneratorEvent.NavigateToTooltip, event)
        }
    }

    @Test
    fun `LifecycleResumedAction should use storage options derived state over VM state`() {
        val initialState = initialUsernameState.copy(
            selectedType = GeneratorState.MainType.Username(
                selectedType = GeneratorState.MainType.Username.UsernameType.PlusAddressedEmail(
                    email = "currentEmail",
                ),
            ),
        )
        val viewModel = createViewModel(initialState)
        fakeGeneratorRepository.saveUsernameGenerationOptions(
            UsernameGenerationOptions(
                type = UsernameGenerationOptions.UsernameType.RANDOM_WORD,
            ),
        )
        val expectedState = initialState.copy(
            selectedType = GeneratorState.MainType.Username(
                selectedType = GeneratorState.MainType.Username.UsernameType.RandomWord(),
            ),
            generatedText = "randomWord",
        )
        viewModel.trySendAction(GeneratorAction.LifecycleResume)
        assertEquals(
            expectedState,
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `LifecycleResumedAction should use passcode storage options derived state over VM state`() {
        val initialState = initialPasscodeState
        val viewModel = createViewModel(initialState)
        fakeGeneratorRepository.savePasscodeGenerationOptions(
            PasscodeGenerationOptions(
                type = PasscodeGenerationOptions.PasscodeType.PASSPHRASE,
                length = 14,
                allowAmbiguousChar = false,
                hasNumbers = false,
                minNumber = 3,
                hasUppercase = false,
                minUppercase = null,
                hasLowercase = false,
                minLowercase = null,
                allowSpecial = false,
                minSpecial = 0,
                numWords = 3,
                wordSeparator = "-",
                allowCapitalize = false,
                allowIncludeNumber = false,
            ),
        )
        val expectedState = initialState.copy(
            selectedType = GeneratorState.MainType.Passphrase(
                numWords = 3,
                minNumWords = 3,
                maxNumWords = 20,
                wordSeparator = '-',
                capitalize = false,
                capitalizeEnabled = true,
                includeNumber = false,
                includeNumberEnabled = true,
            ),
            generatedText = "updatedPassphrase",
        )
        viewModel.trySendAction(GeneratorAction.LifecycleResume)
        assertEquals(
            expectedState,
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `No loadOptions with default arguments should use VM state options derived state over VM state`() =
        runTest {
            val initialState = initialUsernameState.copy(
                selectedType = GeneratorState.MainType.Username(
                    selectedType = GeneratorState.MainType.Username.UsernameType.PlusAddressedEmail(
                        email = "currentEmail",
                    ),
                ),
            )
            val viewModel = createViewModel(initialState)
            // the state is updated via the call to `loadOptions()` in the init block
            viewModel.stateFlow.test {
                assertEquals(
                    initialState.copy(generatedText = "email+abcd1234@address.com"),
                    awaitItem(),
                )
                // Setting the repository options to RANDOM_WORD to show this does NOT get used.
                fakeGeneratorRepository.saveUsernameGenerationOptions(
                    UsernameGenerationOptions(
                        type = UsernameGenerationOptions.UsernameType.RANDOM_WORD,
                    ),
                )
                // When this action is handled there will be another call to `loadOptions()`
                // since we are using the default arguments with `shouldUseStorageOptions` set to
                // false we should not expect a state update.
                viewModel.trySendAction(
                    GeneratorAction.Internal.PasswordGeneratorPolicyReceive(policies = emptyList()),
                )
                expectNoEvents()
            }
        }

    @Nested
    inner class PasswordActions {
        private val defaultPasswordState = createPasswordState()
        private lateinit var viewModel: GeneratorViewModel

        @BeforeEach
        fun setup() {
            fakeGeneratorRepository.setMockGeneratePasswordResult(
                GeneratedPasswordResult.Success("defaultPassword"),
            )
            viewModel = createViewModel(initialPasscodeSavedStateHandle)
        }

        @Suppress("MaxLineLength")
        @Test
        fun `SliderLengthChange should update password length correctly to new value and generate text`() =
            runTest {
                val updatedGeneratedPassword = "updatedPassword"
                fakeGeneratorRepository.setMockGeneratePasswordResult(
                    GeneratedPasswordResult.Success(updatedGeneratedPassword),
                )

                val newLength = 16

                viewModel.trySendAction(
                    GeneratorAction.MainType.Password.SliderLengthChange(
                        length = newLength,
                        isUserInteracting = false,
                    ),
                )

                val expectedState = defaultPasswordState.copy(
                    generatedText = updatedGeneratedPassword,
                    selectedType = GeneratorState.MainType.Password(length = newLength),
                )

                assertEquals(expectedState, viewModel.stateFlow.value)
            }

        @Suppress("MaxLineLength")
        @Test
        fun `ToggleCapitalLettersChange should update useCapital correctly, update length, and generate text`() =
            runTest {
                val updatedGeneratedPassword = "updatedPassword"
                fakeGeneratorRepository.setMockGeneratePasswordResult(
                    GeneratedPasswordResult.Success(updatedGeneratedPassword),
                )

                // Update the length to something small enough that it updates on capitals toggle.
                viewModel.trySendAction(
                    GeneratorAction.MainType.Password.SliderLengthChange(
                        length = 5,
                        isUserInteracting = false,
                    ),
                )

                // Set useCapitals to false initially to ensure length change is reflected.
                viewModel.trySendAction(
                    GeneratorAction.MainType.Password.ToggleCapitalLettersChange(
                        useCapitals = false,
                    ),
                )

                // Verify useCapitals is reflected and initial length is correct.
                val expectedState1 = defaultPasswordState.copy(
                    generatedText = updatedGeneratedPassword,
                    selectedType = GeneratorState.MainType.Password(
                        length = 5,
                        useCapitals = false,
                    ),
                )
                assertEquals(expectedState1, viewModel.stateFlow.value)

                // Update minNumbers so that length will exceed 5 when useCapitals is enabled.
                viewModel.trySendAction(
                    GeneratorAction.MainType.Password.MinNumbersCounterChange(
                        minNumbers = 4,
                    ),
                )

                // Verify length is still 5, minNumbers is updated, and useCapital is still false.
                val expectedState2 = defaultPasswordState.copy(
                    generatedText = updatedGeneratedPassword,
                    selectedType = GeneratorState.MainType.Password(
                        length = 5, // 0 uppercase + 1 lowercase + 4 numbers + 0 special chars
                        minNumbers = 4,
                        useCapitals = false,
                        useNumbers = true,
                    ),
                )
                assertEquals(expectedState2, viewModel.stateFlow.value)

                // Enable useCapitals.
                viewModel.trySendAction(
                    GeneratorAction.MainType.Password.ToggleCapitalLettersChange(
                        useCapitals = true,
                    ),
                )

                // Verify this has caused length to increase.
                val expectedState3 = defaultPasswordState.copy(
                    generatedText = updatedGeneratedPassword,
                    selectedType = GeneratorState.MainType.Password(
                        length = 6, // 1 uppercase + 1 lowercase + 4 numbers + 0 special chars
                        minNumbers = 4,
                        useCapitals = true,
                    ),
                )
                assertEquals(expectedState3, viewModel.stateFlow.value)
            }

        @Suppress("MaxLineLength")
        @Test
        fun `ToggleLowercaseLettersChange should update useLowercase correctly, update length, and generate text`() =
            runTest {
                val updatedGeneratedPassword = "updatedPassword"
                fakeGeneratorRepository.setMockGeneratePasswordResult(
                    GeneratedPasswordResult.Success(updatedGeneratedPassword),
                )

                // Update the length to something small enough that it updates on lowercase toggle.
                viewModel.trySendAction(
                    GeneratorAction.MainType.Password.SliderLengthChange(
                        length = 5,
                        isUserInteracting = false,
                    ),
                )

                // Set useLowercase to false initially to ensure length change is reflected.
                viewModel.trySendAction(
                    GeneratorAction.MainType.Password.ToggleLowercaseLettersChange(
                        useLowercase = false,
                    ),
                )

                // Verify useLowercase is reflected and initial length is correct.
                val expectedState1 = defaultPasswordState.copy(
                    generatedText = updatedGeneratedPassword,
                    selectedType = GeneratorState.MainType.Password(
                        length = 5,
                        useLowercase = false,
                    ),
                )
                assertEquals(expectedState1, viewModel.stateFlow.value)

                // Update minNumbers so that length will exceed 5 when useLowercase is enabled.
                viewModel.trySendAction(
                    GeneratorAction.MainType.Password.MinNumbersCounterChange(
                        minNumbers = 4,
                    ),
                )

                // Verify length is still 5, minNumbers is updated, and useLowercase is still false.
                val expectedState2 = defaultPasswordState.copy(
                    generatedText = updatedGeneratedPassword,
                    selectedType = GeneratorState.MainType.Password(
                        length = 5, // 1 uppercase + 0 lowercase + 4 numbers + 0 special chars
                        minNumbers = 4,
                        useLowercase = false,
                        useNumbers = true,
                    ),
                )
                assertEquals(expectedState2, viewModel.stateFlow.value)

                // Enable useLowercase.
                viewModel.trySendAction(
                    GeneratorAction.MainType.Password.ToggleLowercaseLettersChange(
                        useLowercase = true,
                    ),
                )

                // Verify this has caused length to increase.
                val expectedState3 = defaultPasswordState.copy(
                    generatedText = updatedGeneratedPassword,
                    selectedType = GeneratorState.MainType.Password(
                        length = 6, // 1 uppercase + 1 lowercase + 4 numbers + 0 special chars
                        minNumbers = 4,
                        useLowercase = true,
                    ),
                )
                assertEquals(expectedState3, viewModel.stateFlow.value)
            }

        @Test
        fun `ToggleNumbersChange should update useNumbers correctly and generate text`() = runTest {
            val updatedGeneratedPassword = "updatedPassword"
            fakeGeneratorRepository.setMockGeneratePasswordResult(
                GeneratedPasswordResult.Success(updatedGeneratedPassword),
            )

            val useNumbers = true

            viewModel.trySendAction(
                GeneratorAction.MainType.Password.ToggleNumbersChange(
                    useNumbers = useNumbers,
                ),
            )

            val expectedState = defaultPasswordState.copy(
                generatedText = updatedGeneratedPassword,
                selectedType = GeneratorState.MainType.Password(
                    useNumbers = useNumbers,
                ),
            )

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Suppress("MaxLineLength")
        @Test
        fun `ToggleSpecialCharactersChange should update useSpecialChars correctly and generate text`() =
            runTest {
                val updatedGeneratedPassword = "updatedPassword"
                fakeGeneratorRepository.setMockGeneratePasswordResult(
                    GeneratedPasswordResult.Success(updatedGeneratedPassword),
                )

                val useSpecialChars = true

                viewModel.trySendAction(
                    GeneratorAction.MainType.Password.ToggleSpecialCharactersChange(
                        useSpecialChars = useSpecialChars,
                    ),
                )

                val expectedState = defaultPasswordState.copy(
                    generatedText = updatedGeneratedPassword,
                    selectedType = GeneratorState.MainType.Password(
                        useSpecialChars = useSpecialChars,
                    ),
                )

                assertEquals(expectedState, viewModel.stateFlow.value)
            }

        @Test
        fun `MinNumbersCounterChange should update minNumbers, update length, and generate text`() =
            runTest {
                val updatedGeneratedPassword = "updatedPassword"
                fakeGeneratorRepository.setMockGeneratePasswordResult(
                    GeneratedPasswordResult.Success(updatedGeneratedPassword),
                )

                // Toggle numbers to false initially so we can verify length changes appropriately.
                viewModel.trySendAction(
                    GeneratorAction.MainType.Password.ToggleNumbersChange(
                        useNumbers = false,
                    ),
                )

                // Update the length to something small enough that it's updated on numbers toggle.
                viewModel.trySendAction(
                    GeneratorAction.MainType.Password.SliderLengthChange(
                        length = 5,
                        isUserInteracting = false,
                    ),
                )

                val minNumbers = 5
                viewModel.trySendAction(
                    GeneratorAction.MainType.Password.MinNumbersCounterChange(
                        minNumbers = minNumbers,
                    ),
                )

                val expectedState1 = defaultPasswordState.copy(
                    generatedText = updatedGeneratedPassword,
                    selectedType = GeneratorState.MainType.Password(
                        length = 5, // the current slider value, which the min doesn't exceed
                        minNumbers = minNumbers,
                        useNumbers = false,
                    ),
                )
                assertEquals(expectedState1, viewModel.stateFlow.value)

                // Toggle numbers to true so we can verify length changes appropriately.
                viewModel.trySendAction(
                    GeneratorAction.MainType.Password.ToggleNumbersChange(
                        useNumbers = true,
                    ),
                )

                val expectedState2 = defaultPasswordState.copy(
                    generatedText = updatedGeneratedPassword,
                    selectedType = GeneratorState.MainType.Password(
                        length = 7, // 1 uppercase + 1 lowercase + 5 numbers + 0 special
                        minNumbers = minNumbers,
                        useNumbers = true,
                    ),
                )
                assertEquals(expectedState2, viewModel.stateFlow.value)
            }

        @Suppress("MaxLineLength")
        @Test
        fun `MinSpecialCharactersChange should update minSpecial, update length, and generate text`() =
            runTest {
                val updatedGeneratedPassword = "updatedPassword"
                fakeGeneratorRepository.setMockGeneratePasswordResult(
                    GeneratedPasswordResult.Success(updatedGeneratedPassword),
                )

                // Update the length to something small enough that it's updated on toggle.
                viewModel.trySendAction(
                    GeneratorAction.MainType.Password.SliderLengthChange(
                        length = 5,
                        isUserInteracting = false,
                    ),
                )

                val minSpecial = 5

                viewModel.trySendAction(
                    GeneratorAction.MainType.Password.MinSpecialCharactersChange(
                        minSpecial = minSpecial,
                    ),
                )

                // Length should still be 5 because special characters are not enabled.
                val expectedState1 = defaultPasswordState.copy(
                    generatedText = updatedGeneratedPassword,
                    selectedType = GeneratorState.MainType.Password(
                        length = 5,
                        minSpecial = minSpecial,
                        useSpecialChars = false,
                    ),
                )
                assertEquals(expectedState1, viewModel.stateFlow.value)

                viewModel.trySendAction(
                    GeneratorAction.MainType.Password.ToggleSpecialCharactersChange(
                        useSpecialChars = true,
                    ),
                )

                // Length should update to 7 because special characters are now enabled.
                val expectedState2 = defaultPasswordState.copy(
                    generatedText = updatedGeneratedPassword,
                    selectedType = GeneratorState.MainType.Password(
                        length = 8, // 1 uppercase + 1 lowercase + 1 number + 5 special chars
                        minSpecial = minSpecial,
                        useSpecialChars = true,
                    ),
                )
                assertEquals(expectedState2, viewModel.stateFlow.value)
            }

        @Suppress("MaxLineLength")
        @Test
        fun `ToggleAvoidAmbigousCharactersChange should update avoidAmbiguousChars correctly and generate text`() =
            runTest {
                val updatedGeneratedPassword = "updatedPassword"
                fakeGeneratorRepository.setMockGeneratePasswordResult(
                    GeneratedPasswordResult.Success(updatedGeneratedPassword),
                )

                val avoidAmbiguousChars = true

                viewModel.trySendAction(
                    GeneratorAction.MainType.Password.ToggleAvoidAmbiguousCharactersChange(
                        avoidAmbiguousChars = avoidAmbiguousChars,
                    ),
                )

                val expectedState = defaultPasswordState.copy(
                    generatedText = updatedGeneratedPassword,
                    selectedType = GeneratorState.MainType.Password(
                        avoidAmbiguousChars = avoidAmbiguousChars,
                    ),
                )

                assertEquals(expectedState, viewModel.stateFlow.value)
            }

        @Test
        fun `Turning off all toggles should automatically turn on useLowercase`() = runTest {
            val updatedGeneratedPassword = "updatedPassword"
            fakeGeneratorRepository.setMockGeneratePasswordResult(
                GeneratedPasswordResult.Success(updatedGeneratedPassword),
            )

            // Initially turn on all toggles
            viewModel.trySendAction(
                GeneratorAction.MainType.Password.ToggleCapitalLettersChange(
                    useCapitals = true,
                ),
            )
            viewModel.trySendAction(
                GeneratorAction.MainType.Password.ToggleLowercaseLettersChange(
                    useLowercase = true,
                ),
            )
            viewModel.trySendAction(
                GeneratorAction.MainType.Password.ToggleNumbersChange(
                    useNumbers = true,
                ),
            )
            viewModel.trySendAction(
                GeneratorAction.MainType.Password.ToggleSpecialCharactersChange(
                    useSpecialChars = true,
                ),
            )

            // Attempt to turn off all toggles
            viewModel.trySendAction(
                GeneratorAction.MainType.Password.ToggleCapitalLettersChange(
                    useCapitals = false,
                ),
            )
            viewModel.trySendAction(
                GeneratorAction.MainType.Password.ToggleLowercaseLettersChange(
                    useLowercase = false,
                ),
            )
            viewModel.trySendAction(
                GeneratorAction.MainType.Password.ToggleNumbersChange(
                    useNumbers = false,
                ),
            )

            // Check the state with only one toggle (useSpecialChars) left on
            val intermediatePasswordState = GeneratorState.MainType.Password(
                useCapitals = false,
                useLowercase = false,
                useNumbers = false,
                useSpecialChars = true,
            )

            val intermediateState = defaultPasswordState.copy(
                generatedText = updatedGeneratedPassword,
                selectedType = intermediatePasswordState,
            )

            assertEquals(intermediateState, viewModel.stateFlow.value)

            viewModel.trySendAction(
                GeneratorAction.MainType.Password.ToggleSpecialCharactersChange(
                    useSpecialChars = false,
                ),
            )

            // Check if useLowercase is turned on automatically
            val expectedState = intermediateState.copy(
                selectedType = intermediatePasswordState.copy(
                    useLowercase = true,
                    useSpecialChars = false,
                ),
            )

            assertEquals(expectedState, viewModel.stateFlow.value)
        }
    }

    @Nested
    inner class PassphraseActions {

        private val defaultPassphraseState = createPassphraseState()
        private lateinit var viewModel: GeneratorViewModel

        @BeforeEach
        fun setup() {
            fakeGeneratorRepository.setMockGeneratePasswordResult(
                GeneratedPasswordResult.Success("defaultPassphrase"),
            )
            viewModel = createViewModel(passphraseSavedStateHandle)
        }

        @Test
        fun `NumWordsCounterChange should update the numWords property correctly`() =
            runTest {
                val updatedGeneratedPassphrase = "updatedPassword"
                fakeGeneratorRepository.setMockGeneratePassphraseResult(
                    GeneratedPassphraseResult.Success(updatedGeneratedPassphrase),
                )

                val newNumWords = 4
                viewModel.trySendAction(
                    GeneratorAction.MainType.Passphrase.NumWordsCounterChange(
                        numWords = newNumWords,
                    ),
                )

                val expectedState = defaultPassphraseState.copy(
                    generatedText = updatedGeneratedPassphrase,
                    selectedType = GeneratorState.MainType.Passphrase(
                        numWords = newNumWords,
                    ),
                )

                assertEquals(expectedState, viewModel.stateFlow.value)
            }

        @Test
        fun `WordSeparatorTextChange should update wordSeparator correctly to new value`() =
            runTest {
                val updatedGeneratedPassphrase = "updatedPassword"
                fakeGeneratorRepository.setMockGeneratePassphraseResult(
                    GeneratedPassphraseResult.Success(updatedGeneratedPassphrase),
                )

                val newWordSeparatorChar = '_'

                viewModel.trySendAction(
                    GeneratorAction.MainType.Passphrase.WordSeparatorTextChange(
                        wordSeparator = newWordSeparatorChar,
                    ),
                )

                val expectedState = defaultPassphraseState.copy(
                    generatedText = updatedGeneratedPassphrase,
                    selectedType = GeneratorState.MainType.Passphrase(
                        wordSeparator = newWordSeparatorChar,
                    ),
                )

                assertEquals(expectedState, viewModel.stateFlow.value)
            }

        @Test
        fun `ToggleIncludeNumberChange should update the includeNumber property correctly`() =
            runTest {
                val updatedGeneratedPassphrase = "updatedPassword"
                fakeGeneratorRepository.setMockGeneratePassphraseResult(
                    GeneratedPassphraseResult.Success(updatedGeneratedPassphrase),
                )

                viewModel.trySendAction(
                    GeneratorAction.MainType.Passphrase.ToggleIncludeNumberChange(
                        includeNumber = true,
                    ),
                )

                val expectedState = defaultPassphraseState.copy(
                    generatedText = updatedGeneratedPassphrase,
                    selectedType = GeneratorState.MainType.Passphrase(
                        includeNumber = true,
                    ),
                )

                assertEquals(expectedState, viewModel.stateFlow.value)
            }

        @Test
        fun `ToggleCapitalizeChange should update the capitalize property correctly`() =
            runTest {
                val updatedGeneratedPassphrase = "updatedPassword"
                fakeGeneratorRepository.setMockGeneratePassphraseResult(
                    GeneratedPassphraseResult.Success(updatedGeneratedPassphrase),
                )

                viewModel.trySendAction(
                    GeneratorAction.MainType.Passphrase.ToggleCapitalizeChange(
                        capitalize = true,
                    ),
                )

                val expectedState = defaultPassphraseState.copy(
                    generatedText = updatedGeneratedPassphrase,
                    selectedType = GeneratorState.MainType.Passphrase(
                        capitalize = true,
                    ),
                )

                assertEquals(expectedState, viewModel.stateFlow.value)
            }
    }

    @Nested
    inner class ForwardedEmailAliasActions {
        private val defaultForwardedEmailAliasState = createForwardedEmailAliasState()
        private lateinit var viewModel: GeneratorViewModel

        @BeforeEach
        fun setup() {
            viewModel = createViewModel(forwardedEmailAliasSavedStateHandle)
        }

        @Test
        fun `ServiceTypeOptionSelect should update service type correctly`() = runTest {
            fakeGeneratorRepository.setMockGenerateForwardedServiceResult(
                GeneratedForwardedServiceUsernameResult.Success(
                    generatedEmailAddress = "defaultForwardedEmailAlias",
                ),
            )

            ServiceTypeOption
                .entries
                .forEach {
                    val action = GeneratorAction
                        .MainType
                        .Username
                        .UsernameType
                        .ForwardedEmailAlias
                        .ServiceTypeOptionSelect(serviceTypeOption = it)

                    viewModel.trySendAction(action)

                    val serviceType = createMockForwardedEmailAliasGeneratorState(it)

                    val expectedState = defaultForwardedEmailAliasState.copy(
                        generatedText = "-",
                        selectedType = GeneratorState.MainType.Username(
                            selectedType = GeneratorState
                                .MainType
                                .Username
                                .UsernameType
                                .ForwardedEmailAlias(
                                    selectedServiceType = serviceType,
                                ),
                        ),
                    )

                    assertEquals(expectedState, viewModel.stateFlow.value)
                }
        }

        @Suppress("MaxLineLength")
        private fun createMockForwardedEmailAliasGeneratorState(
            serviceTypeOption: ServiceTypeOption,
        ): ServiceType = when (serviceTypeOption) {
            ServiceTypeOption.ADDY_IO -> {
                ServiceType.AddyIo()
            }

            ServiceTypeOption.DUCK_DUCK_GO -> {
                ServiceType.DuckDuckGo()
            }

            ServiceTypeOption.FAST_MAIL -> {
                ServiceType.FastMail()
            }

            ServiceTypeOption.FIREFOX_RELAY -> {
                ServiceType.FirefoxRelay()
            }

            ServiceTypeOption.FORWARD_EMAIL -> {
                ServiceType.ForwardEmail()
            }

            ServiceTypeOption.SIMPLE_LOGIN -> {
                ServiceType.SimpleLogin()
            }
        }
    }

    @Nested
    inner class AddyIoActions {
        private val defaultAddyIoState = createAddyIoState()
        private lateinit var viewModel: GeneratorViewModel

        @BeforeEach
        fun setup() {
            viewModel = createViewModel(addyIoSavedStateHandle)
        }

        @Test
        fun `AccessTokenTextChange should update access token text correctly`() = runTest {
            val newAccessToken = "newAccessToken"
            val action = GeneratorAction
                .MainType
                .Username
                .UsernameType
                .ForwardedEmailAlias
                .AddyIo
                .AccessTokenTextChange(
                    accessToken = newAccessToken,
                )

            fakeGeneratorRepository.setMockGenerateForwardedServiceResult(
                GeneratedForwardedServiceUsernameResult.Success(
                    generatedEmailAddress = "defaultAddyIo",
                ),
            )

            viewModel.trySendAction(action)

            val expectedState = defaultAddyIoState.copy(
                generatedText = "-",
                selectedType = GeneratorState.MainType.Username(
                    GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias(
                        selectedServiceType = ServiceType
                            .AddyIo(
                                apiAccessToken = newAccessToken,
                            ),
                    ),
                ),
            )
            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `DomainTextChange should update the domain text correctly`() = runTest {
            val newDomainName = "newDomain"
            val action = GeneratorAction
                .MainType
                .Username
                .UsernameType
                .ForwardedEmailAlias
                .AddyIo
                .DomainTextChange(
                    domain = newDomainName,
                )

            fakeGeneratorRepository.setMockGenerateForwardedServiceResult(
                GeneratedForwardedServiceUsernameResult.Success(
                    generatedEmailAddress = "defaultAddyIo",
                ),
            )

            viewModel.trySendAction(action)

            val expectedState = defaultAddyIoState.copy(
                generatedText = "-",
                selectedType = GeneratorState.MainType.Username(
                    GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias(
                        selectedServiceType = ServiceType
                            .AddyIo(
                                domainName = newDomainName,
                            ),
                    ),
                ),
            )

            assertEquals(expectedState, viewModel.stateFlow.value)
        }
    }

    @Nested
    inner class DuckDuckGoActions {
        private val defaultDuckDuckGoState = createDuckDuckGoState()
        private lateinit var viewModel: GeneratorViewModel

        @BeforeEach
        fun setup() {
            viewModel = createViewModel(duckDuckGoSavedStateHandle)
        }

        @Test
        fun `ApiKeyTextChange should update api key text correctly`() = runTest {
            val newApiKey = "newApiKey"
            val action = GeneratorAction
                .MainType
                .Username
                .UsernameType
                .ForwardedEmailAlias
                .DuckDuckGo.ApiKeyTextChange(
                    apiKey = newApiKey,
                )

            fakeGeneratorRepository.setMockGenerateForwardedServiceResult(
                GeneratedForwardedServiceUsernameResult.Success(
                    generatedEmailAddress = "defaultDuckDuckGo",
                ),
            )

            viewModel.trySendAction(action)

            val expectedState = defaultDuckDuckGoState.copy(
                generatedText = "-",
                selectedType = GeneratorState.MainType.Username(
                    GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias(
                        selectedServiceType = ServiceType
                            .DuckDuckGo(
                                apiKey = newApiKey,
                            ),
                    ),
                ),
            )

            assertEquals(expectedState, viewModel.stateFlow.value)
        }
    }

    @Nested
    inner class FastMailActions {
        private val defaultFastMailState = createFastMailState()
        private lateinit var viewModel: GeneratorViewModel

        @BeforeEach
        fun setup() {
            viewModel = createViewModel(fastMailSavedStateHandle)
        }

        @Test
        fun `ApiKeyTextChange should update api key text correctly`() = runTest {
            val newApiKey = "newApiKey"
            val action = GeneratorAction
                .MainType
                .Username
                .UsernameType
                .ForwardedEmailAlias
                .FastMail.ApiKeyTextChange(
                    apiKey = newApiKey,
                )

            fakeGeneratorRepository.setMockGenerateForwardedServiceResult(
                GeneratedForwardedServiceUsernameResult.Success(
                    generatedEmailAddress = "defaultFastMail",
                ),
            )

            viewModel.trySendAction(action)

            val expectedState = defaultFastMailState.copy(
                generatedText = "-",
                selectedType = GeneratorState.MainType.Username(
                    GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias(
                        selectedServiceType = ServiceType
                            .FastMail(
                                apiKey = newApiKey,
                            ),
                    ),
                ),
            )

            assertEquals(expectedState, viewModel.stateFlow.value)
        }
    }

    @Nested
    inner class FirefoxRelayActions {
        private val defaultFirefoxRelayState = createFirefoxRelayState()
        private lateinit var viewModel: GeneratorViewModel

        @BeforeEach
        fun setup() {
            viewModel = createViewModel(firefoxRelaySavedStateHandle)
        }

        @Test
        fun `AccessTokenTextChange should update access token text correctly`() = runTest {
            val newAccessToken = "newAccessToken"
            val action = GeneratorAction
                .MainType
                .Username
                .UsernameType
                .ForwardedEmailAlias
                .FirefoxRelay
                .AccessTokenTextChange(
                    accessToken = newAccessToken,
                )

            fakeGeneratorRepository.setMockGenerateForwardedServiceResult(
                GeneratedForwardedServiceUsernameResult.Success(
                    generatedEmailAddress = "defaultFirefoxRelay",
                ),
            )

            viewModel.trySendAction(action)

            val expectedState = defaultFirefoxRelayState.copy(
                generatedText = "-",
                selectedType = GeneratorState.MainType.Username(
                    GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias(
                        selectedServiceType = ServiceType
                            .FirefoxRelay(
                                apiAccessToken = newAccessToken,
                            ),
                    ),
                ),
            )

            assertEquals(expectedState, viewModel.stateFlow.value)
        }
    }

    @Nested
    inner class ForwardEmailActions {
        private val defaultForwardEmailState = createForwardEmailState()
        private lateinit var viewModel: GeneratorViewModel

        @BeforeEach
        fun setUp() {
            viewModel = createViewModel(defaultForwardEmailState)
        }

        @Test
        fun `ApiKeyTextChange should update api key text correctly`() {
            val newApiKey = "newApiKey"
            val action = GeneratorAction
                .MainType
                .Username
                .UsernameType
                .ForwardedEmailAlias
                .ForwardEmail
                .ApiKeyTextChange(
                    apiKey = newApiKey,
                )

            fakeGeneratorRepository.setMockGenerateForwardedServiceResult(
                GeneratedForwardedServiceUsernameResult.Success(
                    generatedEmailAddress = "defaultForwardEmail",
                ),
            )

            viewModel.trySendAction(action)

            val expectedState = defaultForwardEmailState.copy(
                generatedText = "-",
                selectedType = GeneratorState.MainType.Username(
                    GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias(
                        selectedServiceType = ServiceType
                            .ForwardEmail(
                                apiKey = newApiKey,
                            ),
                    ),
                ),
            )

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `DomainNameTextChange should update domain name text correctly`() {
            val newDomainName = "newDomainName"
            val action = GeneratorAction
                .MainType
                .Username
                .UsernameType
                .ForwardedEmailAlias
                .ForwardEmail
                .DomainNameTextChange(
                    domainName = newDomainName,
                )

            fakeGeneratorRepository.setMockGenerateForwardedServiceResult(
                GeneratedForwardedServiceUsernameResult.Success(
                    generatedEmailAddress = "defaultForwardEmail",
                ),
            )

            viewModel.trySendAction(action)

            val expectedState = defaultForwardEmailState.copy(
                generatedText = "-",
                selectedType = GeneratorState.MainType.Username(
                    GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias(
                        selectedServiceType = ServiceType
                            .ForwardEmail(
                                domainName = newDomainName,
                            ),
                    ),
                ),
            )

            assertEquals(expectedState, viewModel.stateFlow.value)
        }
    }

    @Nested
    inner class SimpleLoginActions {
        private val defaultSimpleLoginState = createSimpleLoginState()
        private lateinit var viewModel: GeneratorViewModel

        @BeforeEach
        fun setup() {
            viewModel = createViewModel(simpleLoginSavedStateHandle)
        }

        @Test
        fun `ApiKeyTextChange should update api key text correctly`() = runTest {
            val newApiKey = "newApiKey"
            val action = GeneratorAction
                .MainType
                .Username
                .UsernameType
                .ForwardedEmailAlias
                .SimpleLogin
                .ApiKeyTextChange(
                    apiKey = newApiKey,
                )

            fakeGeneratorRepository.setMockGenerateForwardedServiceResult(
                GeneratedForwardedServiceUsernameResult.Success(
                    generatedEmailAddress = "defaultSimpleLogin",
                ),
            )

            viewModel.trySendAction(action)

            val expectedState = defaultSimpleLoginState.copy(
                generatedText = "-",
                selectedType = GeneratorState.MainType.Username(
                    GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias(
                        selectedServiceType = ServiceType
                            .SimpleLogin(
                                apiKey = newApiKey,
                            ),
                    ),
                ),
            )

            assertEquals(expectedState, viewModel.stateFlow.value)
        }
    }

    @Nested
    inner class PlusAddressedEmailActions {
        private val defaultPlusAddressedEmailState = createPlusAddressedEmailState()
        private lateinit var viewModel: GeneratorViewModel

        @BeforeEach
        fun setup() {
            viewModel = createViewModel(usernameSavedStateHandle)
        }

        @Test
        fun `EmailTextChange should update email correctly`() = runTest {
            val newEmail = "test@example.com"
            viewModel.trySendAction(
                GeneratorAction
                    .MainType
                    .Username
                    .UsernameType
                    .PlusAddressedEmail
                    .EmailTextChange(
                        email = newEmail,
                    ),
            )

            val expectedState = defaultPlusAddressedEmailState.copy(
                generatedText = "-",
                selectedType = GeneratorState.MainType.Username(
                    selectedType = GeneratorState
                        .MainType
                        .Username
                        .UsernameType
                        .PlusAddressedEmail(
                            email = newEmail,
                        ),
                ),
            )

            assertEquals(expectedState, viewModel.stateFlow.value)
        }
    }

    @Nested
    inner class CatchAllEmailActions {
        private val defaultCatchAllEmailState = createCatchAllEmailState()
        private lateinit var viewModel: GeneratorViewModel

        @BeforeEach
        fun setup() {
            viewModel = createViewModel(catchAllEmailSavedStateHandle)
        }

        @Suppress("MaxLineLength")
        @Test
        fun `DomainTextChange should update domain correctly`() =
            runTest {
                val newDomain = "test.com"
                viewModel.trySendAction(
                    GeneratorAction
                        .MainType
                        .Username
                        .UsernameType
                        .CatchAllEmail
                        .DomainTextChange(
                            domain = newDomain,
                        ),
                )

                val expectedState = defaultCatchAllEmailState.copy(
                    generatedText = "-",
                    selectedType = GeneratorState.MainType.Username(
                        selectedType = GeneratorState
                            .MainType
                            .Username
                            .UsernameType
                            .CatchAllEmail(
                                domainName = newDomain,
                            ),
                    ),
                )

                assertEquals(expectedState, viewModel.stateFlow.value)
            }
    }

    @Nested
    inner class RandomWordActions {
        private val defaultRandomWordState = createRandomWordState()
        private lateinit var viewModel: GeneratorViewModel

        @BeforeEach
        fun setup() {
            viewModel = createViewModel(randomWordSavedStateHandle)
        }

        @Suppress("MaxLineLength")
        @Test
        fun `ToggleCapitalizeChange should update the capitalize property correctly`() = runTest {
            viewModel.trySendAction(
                GeneratorAction
                    .MainType
                    .Username
                    .UsernameType
                    .RandomWord
                    .ToggleCapitalizeChange(
                        capitalize = true,
                    ),
            )

            val expectedState = defaultRandomWordState.copy(
                generatedText = "randomWord",
                selectedType = GeneratorState.MainType.Username(
                    GeneratorState.MainType.Username.UsernameType.RandomWord(
                        capitalize = true,
                    ),
                ),
            )

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `ToggleIncludeNumberChange should update the includeNumber property correctly`() =
            runTest {
                viewModel.trySendAction(
                    GeneratorAction
                        .MainType
                        .Username
                        .UsernameType
                        .RandomWord
                        .ToggleIncludeNumberChange(
                            includeNumber = true,
                        ),
                )

                val expectedState = defaultRandomWordState.copy(
                    generatedText = "randomWord",
                    selectedType = GeneratorState.MainType.Username(
                        GeneratorState.MainType.Username.UsernameType.RandomWord(
                            includeNumber = true,
                        ),
                    ),
                )

                assertEquals(expectedState, viewModel.stateFlow.value)
            }
    }

    @Test
    fun `Password minimumLength should be at least as long as the sum of the minimums`() {
        val password = GeneratorState.MainType.Password(
            length = 14,
            minLength = 10,
            useCapitals = true,
            capitalsEnabled = false,
            useLowercase = true,
            lowercaseEnabled = false,
            useNumbers = true,
            numbersEnabled = false,
            useSpecialChars = true,
            specialCharsEnabled = false,
            minNumbers = 9,
            minNumbersAllowed = 3,
            minSpecial = 9,
            minSpecialAllowed = 3,
            avoidAmbiguousChars = false,
        )
        // 9 numbers + 9 special + 1 lowercase + 1 uppercase
        assertEquals(20, password.computedMinimumLength)
    }

    @Test
    fun `Password minimumLength should use minLength if higher than sum of the minimums`() {
        val password = GeneratorState.MainType.Password(
            length = 14,
            minLength = 10,
            useCapitals = true,
            capitalsEnabled = false,
            useLowercase = true,
            lowercaseEnabled = false,
            useNumbers = true,
            numbersEnabled = false,
            useSpecialChars = true,
            specialCharsEnabled = false,
            minNumbers = 1,
            minNumbersAllowed = 3,
            minSpecial = 1,
            minSpecialAllowed = 3,
            avoidAmbiguousChars = false,
        )
        assertEquals(10, password.computedMinimumLength)
    }
    //region Helper Functions

    @Suppress("LongParameterList")
    private fun createPasswordState(
        generatedText: String = "defaultPassword",
        length: Int = 14,
        useCapitals: Boolean = true,
        useLowercase: Boolean = true,
        useNumbers: Boolean = true,
        useSpecialChars: Boolean = false,
        minNumbers: Int = 1,
        minSpecial: Int = 1,
        avoidAmbiguousChars: Boolean = false,
    ): GeneratorState =
        GeneratorState(
            generatedText = generatedText,
            selectedType = GeneratorState.MainType.Password(
                length = length,
                useCapitals = useCapitals,
                useLowercase = useLowercase,
                useNumbers = useNumbers,
                useSpecialChars = useSpecialChars,
                minNumbers = minNumbers,
                minSpecial = minSpecial,
                avoidAmbiguousChars = avoidAmbiguousChars,
            ),
            currentEmailAddress = "currentEmail",
        )

    private fun createPassphraseState(
        generatedText: String = "defaultPassphrase",
        numWords: Int = 3,
        wordSeparator: Char = '-',
        capitalize: Boolean = false,
        includeNumber: Boolean = false,
    ): GeneratorState =
        GeneratorState(
            generatedText = generatedText,
            selectedType = GeneratorState.MainType.Passphrase(
                numWords = numWords,
                wordSeparator = wordSeparator,
                capitalize = capitalize,
                includeNumber = includeNumber,
            ),
            currentEmailAddress = "currentEmail",
        )

    private fun createUsernameModeState(
        generatedText: String = "username",
        email: String = "currentEmail",
    ): GeneratorState =
        GeneratorState(
            generatedText = generatedText,
            generatorMode = GeneratorMode.Modal.Username(website = null),
            selectedType = GeneratorState.MainType.Username(
                GeneratorState.MainType.Username.UsernameType.PlusAddressedEmail(
                    email = email,
                ),
            ),
            currentEmailAddress = "currentEmail",
        )

    private fun createForwardedEmailAliasState(
        generatedText: String = "defaultForwardedEmailAlias",
        obfuscatedText: String = "defaultObfuscatedText",
    ): GeneratorState =
        GeneratorState(
            generatedText = generatedText,
            selectedType = GeneratorState.MainType.Username(
                GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias(
                    selectedServiceType = null,
                    obfuscatedText = obfuscatedText,
                ),
            ),
            currentEmailAddress = "currentEmail",
        )

    private fun createAddyIoState(
        generatedText: String = "defaultAddyIo",
    ): GeneratorState =
        GeneratorState(
            generatedText = generatedText,
            selectedType = GeneratorState.MainType.Username(
                GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias(
                    selectedServiceType = ServiceType
                        .AddyIo(),
                ),
            ),
            currentEmailAddress = "currentEmail",
        )

    private fun createDuckDuckGoState(
        generatedText: String = "defaultDuckDuckGo",
    ): GeneratorState =
        GeneratorState(
            generatedText = generatedText,
            selectedType = GeneratorState.MainType.Username(
                GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias(
                    selectedServiceType = ServiceType
                        .DuckDuckGo(),
                ),
            ),
            currentEmailAddress = "currentEmail",
        )

    private fun createFastMailState(
        generatedText: String = "defaultFastMail",
    ): GeneratorState =
        GeneratorState(
            generatedText = generatedText,
            selectedType = GeneratorState.MainType.Username(
                GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias(
                    selectedServiceType = ServiceType
                        .FastMail(),
                ),
            ),
            currentEmailAddress = "currentEmail",
        )

    private fun createFirefoxRelayState(
        generatedText: String = "defaultFirefoxRelay",
    ): GeneratorState =
        GeneratorState(
            generatedText = generatedText,
            selectedType = GeneratorState.MainType.Username(
                GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias(
                    selectedServiceType = ServiceType
                        .FirefoxRelay(),
                ),
            ),
            currentEmailAddress = "currentEmail",
        )

    private fun createForwardEmailState(
        generatedText: String = "defaultForwardEmail",
    ): GeneratorState =
        GeneratorState(
            generatedText = generatedText,
            selectedType = GeneratorState.MainType.Username(
                GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias(
                    selectedServiceType = ServiceType
                        .ForwardEmail(),
                ),
            ),
            currentEmailAddress = "currentEmail",
        )

    private fun createSimpleLoginState(
        generatedText: String = "defaultSimpleLogin",
    ): GeneratorState =
        GeneratorState(
            generatedText = generatedText,
            selectedType = GeneratorState.MainType.Username(
                GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias(
                    selectedServiceType = ServiceType
                        .SimpleLogin(),
                ),
            ),
            currentEmailAddress = "currentEmail",
        )

    private fun createPlusAddressedEmailState(
        generatedText: String = "defaultPlusAddressedEmail",
        email: String = "currentEmail",
    ): GeneratorState =
        GeneratorState(
            generatedText = generatedText,
            selectedType = GeneratorState.MainType.Username(
                GeneratorState.MainType.Username.UsernameType.PlusAddressedEmail(
                    email = email,
                ),
            ),
            currentEmailAddress = "currentEmail",
        )

    private fun createCatchAllEmailState(
        generatedText: String = "defaultCatchAllEmail",
        domain: String = "defaultDomain",
    ): GeneratorState =
        GeneratorState(
            generatedText = generatedText,
            selectedType = GeneratorState.MainType.Username(
                GeneratorState.MainType.Username.UsernameType.CatchAllEmail(
                    domainName = domain,
                ),
            ),
            currentEmailAddress = "currentEmail",
        )

    private fun createRandomWordState(
        generatedText: String = "defaultRandomWord",
        capitalize: Boolean = false,
        includeNumber: Boolean = false,
    ): GeneratorState =
        GeneratorState(
            generatedText = generatedText,
            selectedType = GeneratorState.MainType.Username(
                GeneratorState.MainType.Username.UsernameType.RandomWord(
                    capitalize = capitalize,
                    includeNumber = includeNumber,
                ),
            ),
            currentEmailAddress = "currentEmail",
        )

    private fun createSavedStateHandleWithState(state: GeneratorState) =
        SavedStateHandle().apply {
            set("state", state)
        }

    private fun createViewModel(
        savedStateHandle: SavedStateHandle,
    ): GeneratorViewModel = GeneratorViewModel(
        savedStateHandle = savedStateHandle,
        clipboardManager = clipboardManager,
        generatorRepository = fakeGeneratorRepository,
        authRepository = authRepository,
        policyManager = policyManager,
        reviewPromptManager = reviewPromptManager,
    )

    private fun createViewModel(
        state: GeneratorState? = initialPasscodeState,
        type: String? = null,
        website: String? = null,
    ): GeneratorViewModel = createViewModel(
        savedStateHandle = SavedStateHandle().apply {
            set("state", state)
            set("generator_mode_type", type)
            set("generator_website", website)
        },
    )

    //endregion Helper Functions
}

private val DEFAULT_USER_STATE = UserState(
    activeUserId = "activeUserId",
    accounts = listOf(
        UserState.Account(
            userId = "activeUserId",
            name = "Active User",
            email = "currentEmail",
            environment = Environment.Us,
            avatarColorHex = "#aa00aa",
            isPremium = true,
            isLoggedIn = true,
            isVaultUnlocked = true,
            needsPasswordReset = false,
            isBiometricsEnabled = false,
            organizations = emptyList(),
            needsMasterPassword = false,
            trustedDevice = null,
            hasMasterPassword = true,
            isUsingKeyConnector = false,
            onboardingStatus = OnboardingStatus.COMPLETE,
            firstTimeState = FirstTimeState(showImportLoginsCard = true),
        ),
    ),
)
