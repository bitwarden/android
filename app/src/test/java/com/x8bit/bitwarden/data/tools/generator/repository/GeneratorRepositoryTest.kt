package com.x8bit.bitwarden.data.tools.generator.repository

import app.cash.turbine.test
import com.bitwarden.core.AppendType
import com.bitwarden.core.ForwarderServiceType
import com.bitwarden.core.PassphraseGeneratorRequest
import com.bitwarden.core.PasswordGeneratorRequest
import com.bitwarden.core.PasswordHistory
import com.bitwarden.core.PasswordHistoryView
import com.bitwarden.core.UsernameGeneratorRequest
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.EnvironmentUrlDataJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.ForcePasswordResetReason
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.KdfTypeJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.KeyConnectorUserDecryptionOptionsJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.TrustedDeviceUserDecryptionOptionsJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.UserDecryptionOptionsJson
import com.x8bit.bitwarden.data.platform.base.FakeDispatcherManager
import com.x8bit.bitwarden.data.platform.repository.model.LocalDataState
import com.x8bit.bitwarden.data.tools.generator.datasource.disk.GeneratorDiskSource
import com.x8bit.bitwarden.data.tools.generator.datasource.disk.PasswordHistoryDiskSource
import com.x8bit.bitwarden.data.tools.generator.datasource.disk.entity.PasswordHistoryEntity
import com.x8bit.bitwarden.data.tools.generator.datasource.disk.entity.toPasswordHistoryEntity
import com.x8bit.bitwarden.data.tools.generator.datasource.sdk.GeneratorSdkSource
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedCatchAllUsernameResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedForwardedServiceUsernameResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedPassphraseResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedPasswordResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedPlusAddressedUsernameResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedRandomWordUsernameResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.PasscodeGenerationOptions
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class GeneratorRepositoryTest {

    private val mutableUserStateFlow = MutableStateFlow<UserStateJson?>(null)

    private val generatorSdkSource: GeneratorSdkSource = mockk()
    private val generatorDiskSource: GeneratorDiskSource = mockk()
    private val authDiskSource: AuthDiskSource = mockk {
        every { userStateFlow } returns mutableUserStateFlow
        every { userState } returns null
    }
    private val passwordHistoryDiskSource: PasswordHistoryDiskSource = mockk()
    private val vaultSdkSource: VaultSdkSource = mockk()
    private val dispatcherManager = FakeDispatcherManager()

    private val repository = GeneratorRepositoryImpl(
        generatorSdkSource = generatorSdkSource,
        generatorDiskSource = generatorDiskSource,
        authDiskSource = authDiskSource,
        passwordHistoryDiskSource = passwordHistoryDiskSource,
        vaultSdkSource = vaultSdkSource,
        dispatcherManager = dispatcherManager,
    )

    @AfterEach
    fun tearDown() {
        unmockkStatic(Instant::class)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `generatePassword should emit Success result and store the generated password when shouldSave is true`() =
        runTest {
            val fixedInstant = Instant.parse("2021-01-01T00:00:00Z")

            mockkStatic(Instant::class)
            every { Instant.now() } returns fixedInstant

            val userId = "testUserId"
            val request = PasswordGeneratorRequest(
                lowercase = true,
                uppercase = true,
                numbers = true,
                special = true,
                length = 12.toUByte(),
                avoidAmbiguous = false,
                minLowercase = null,
                minUppercase = null,
                minNumber = null,
                minSpecial = null,
            )
            val generatedPassword = "GeneratedPassword123!"
            val encryptedPasswordHistory =
                PasswordHistory(password = generatedPassword, lastUsedDate = Instant.now())

            coEvery { authDiskSource.userState?.activeUserId } returns userId

            coEvery { generatorSdkSource.generatePassword(request) } returns
                Result.success(generatedPassword)

            coEvery { vaultSdkSource.encryptPasswordHistory(any(), any()) } returns
                Result.success(encryptedPasswordHistory)

            coEvery { passwordHistoryDiskSource.insertPasswordHistory(any()) } just runs

            val result = repository.generatePassword(request, true)

            assertEquals(
                generatedPassword,
                (result as GeneratedPasswordResult.Success).generatedString,
            )
            coVerify { generatorSdkSource.generatePassword(request) }

            coVerify {
                passwordHistoryDiskSource.insertPasswordHistory(
                    encryptedPasswordHistory.toPasswordHistoryEntity(userId),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `generatePassword should emit Success result but not store the generated password when shouldSave is false`() =
        runTest {
            val fixedInstant = Instant.parse("2021-01-01T00:00:00Z")

            mockkStatic(Instant::class)
            every { Instant.now() } returns fixedInstant

            val userId = "testUserId"
            val request = PasswordGeneratorRequest(
                lowercase = true,
                uppercase = true,
                numbers = true,
                special = true,
                length = 12.toUByte(),
                avoidAmbiguous = false,
                minLowercase = null,
                minUppercase = null,
                minNumber = null,
                minSpecial = null,
            )
            val generatedPassword = "GeneratedPassword123!"
            val encryptedPasswordHistory =
                PasswordHistory(password = generatedPassword, lastUsedDate = Instant.now())

            coEvery { authDiskSource.userState?.activeUserId } returns userId

            coEvery { generatorSdkSource.generatePassword(request) } returns
                Result.success(generatedPassword)

            coEvery { vaultSdkSource.encryptPasswordHistory(any(), any()) } returns
                Result.success(encryptedPasswordHistory)

            coEvery { passwordHistoryDiskSource.insertPasswordHistory(any()) } just runs

            val result = repository.generatePassword(request, false)

            assertEquals(
                generatedPassword,
                (result as GeneratedPasswordResult.Success).generatedString,
            )
            coVerify { generatorSdkSource.generatePassword(request) }

            coVerify(exactly = 0) {
                passwordHistoryDiskSource.insertPasswordHistory(any())
            }
        }

    @Test
    fun `generatePassword should emit InvalidRequest result when SDK throws exception`() = runTest {
        val request = PasswordGeneratorRequest(
            lowercase = true,
            uppercase = true,
            numbers = true,
            special = true,
            length = 12.toUByte(),
            avoidAmbiguous = false,
            minLowercase = null,
            minUppercase = null,
            minNumber = null,
            minSpecial = null,
        )
        val exception = RuntimeException("An error occurred")
        coEvery { generatorSdkSource.generatePassword(request) } returns Result.failure(exception)

        val result = repository.generatePassword(request, true)

        assertTrue(result is GeneratedPasswordResult.InvalidRequest)
        coVerify { generatorSdkSource.generatePassword(request) }
    }

    @Test
    fun `generatePassphrase should emit Success result and store the generated passphrase`() =
        runTest {
            val fixedInstant = Instant.parse("2021-01-01T00:00:00Z")
            mockkStatic(Instant::class)
            every { Instant.now() } returns fixedInstant

            val userId = "testUserId"
            val request = PassphraseGeneratorRequest(
                numWords = 5.toUByte(),
                capitalize = true,
                includeNumber = true,
                wordSeparator = "-",
            )
            val generatedPassphrase = "Generated-Passphrase-123"
            val encryptedPasswordHistory =
                PasswordHistory(password = generatedPassphrase, lastUsedDate = Instant.now())

            coEvery { authDiskSource.userState?.activeUserId } returns userId

            coEvery { generatorSdkSource.generatePassphrase(request) } returns
                Result.success(generatedPassphrase)

            coEvery { vaultSdkSource.encryptPasswordHistory(any(), any()) } returns
                Result.success(encryptedPasswordHistory)

            coEvery { passwordHistoryDiskSource.insertPasswordHistory(any()) } just runs

            val result = repository.generatePassphrase(request)

            assertEquals(
                generatedPassphrase,
                (result as GeneratedPassphraseResult.Success).generatedString,
            )
            coVerify { generatorSdkSource.generatePassphrase(request) }
            coVerify { vaultSdkSource.encryptPasswordHistory(any(), any()) }
            coVerify {
                passwordHistoryDiskSource.insertPasswordHistory(
                    encryptedPasswordHistory.toPasswordHistoryEntity(userId),
                )
            }
        }

    @Test
    fun `generatePassphrase should emit InvalidRequest result when SDK throws exception`() =
        runTest {
            val request = PassphraseGeneratorRequest(
                numWords = 5.toUByte(),
                capitalize = true,
                includeNumber = true,
                wordSeparator = '-'.toString(),
            )
            val exception = RuntimeException("An error occurred")
            coEvery { generatorSdkSource.generatePassphrase(request) } returns Result.failure(
                exception,
            )

            val result = repository.generatePassphrase(request)

            assertTrue(result is GeneratedPassphraseResult.InvalidRequest)
            coVerify { generatorSdkSource.generatePassphrase(request) }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `generatePlusAddressedEmail should return Success with generated email when SDK call is successful`() = runTest {
        val userId = "testUserId"
        val request = UsernameGeneratorRequest.Subaddress(
            type = AppendType.Random,
            email = "user@example.com",
        )
        val generatedEmail = "user+generated@example.com"

        coEvery { authDiskSource.userState?.activeUserId } returns userId
        coEvery { generatorSdkSource.generatePlusAddressedEmail(request) } returns
            Result.success(generatedEmail)

        val result = repository.generatePlusAddressedEmail(request)

        assertEquals(
            generatedEmail,
            (result as GeneratedPlusAddressedUsernameResult.Success).generatedEmailAddress,
        )
        coVerify { generatorSdkSource.generatePlusAddressedEmail(request) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `generatePlusAddressedEmail should return InvalidRequest on SDK failure`() = runTest {
        val request = UsernameGeneratorRequest.Subaddress(
            type = AppendType.Random,
            email = "user@example.com",
        )
        val exception = RuntimeException("An error occurred")
        coEvery {
            generatorSdkSource.generatePlusAddressedEmail(request)
        } returns Result.failure(exception)

        val result = repository.generatePlusAddressedEmail(request)

        assertTrue(result is GeneratedPlusAddressedUsernameResult.InvalidRequest)
        coVerify { generatorSdkSource.generatePlusAddressedEmail(request) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `generateCatchAllEmail should return Success with generated email when SDK call is successful`() = runTest {
        val userId = "testUserId"
        val request = UsernameGeneratorRequest.Catchall(
            type = AppendType.Random,
            domain = "domain",
        )
        val generatedEmail = "user@domain"

        coEvery { generatorSdkSource.generateCatchAllEmail(request) } returns
            Result.success(generatedEmail)

        val result = repository.generateCatchAllEmail(request)

        assertEquals(
            generatedEmail,
            (result as GeneratedCatchAllUsernameResult.Success).generatedEmailAddress,
        )
        coVerify { generatorSdkSource.generateCatchAllEmail(request) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `generateCatchAllEmail should return InvalidRequest on SDK failure`() = runTest {
        val request = UsernameGeneratorRequest.Catchall(
            type = AppendType.Random,
            domain = "user@domain",
        )
        val exception = RuntimeException("An error occurred")
        coEvery {
            generatorSdkSource.generateCatchAllEmail(request)
        } returns Result.failure(exception)

        val result = repository.generateCatchAllEmail(request)

        assertTrue(result is GeneratedCatchAllUsernameResult.InvalidRequest)
        coVerify { generatorSdkSource.generateCatchAllEmail(request) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `generateRandomWord should return Success with generated email when SDK call is successful`() = runTest {
        val userId = "testUserId"
        val request = UsernameGeneratorRequest.Word(
            capitalize = false,
            includeNumber = false,
        )
        val generatedEmail = "user"

        coEvery { generatorSdkSource.generateRandomWord(request) } returns
            Result.success(generatedEmail)

        val result = repository.generateRandomWordUsername(request)

        assertEquals(
            generatedEmail,
            (result as GeneratedRandomWordUsernameResult.Success).generatedUsername,
        )
        coVerify { generatorSdkSource.generateRandomWord(request) }
    }

    @Test
    fun `generateRandomWord should return InvalidRequest on SDK failure`() = runTest {
        val request = UsernameGeneratorRequest.Word(
            capitalize = false,
            includeNumber = false,
        )
        val exception = RuntimeException("An error occurred")
        coEvery {
            generatorSdkSource.generateRandomWord(request)
        } returns Result.failure(exception)

        val result = repository.generateRandomWordUsername(request)

        assertTrue(result is GeneratedRandomWordUsernameResult.InvalidRequest)
        coVerify { generatorSdkSource.generateRandomWord(request) }
    }

    @Test
    fun `generateForwardedService should emit Success result and store the generated email`() =
        runTest {
            val userId = "testUserId"
            val request = UsernameGeneratorRequest.Forwarded(
                service = ForwarderServiceType.DuckDuckGo(
                    token = "testToken",
                ),
                website = null,
            )

            val generatedEmail = "generated@email.com"

            coEvery { authDiskSource.userState?.activeUserId } returns userId
            coEvery { generatorSdkSource.generateForwardedServiceEmail(request) } returns
                Result.success(generatedEmail)

            val result = repository.generateForwardedServiceUsername(request)

            assertEquals(
                generatedEmail,
                (result as GeneratedForwardedServiceUsernameResult.Success).generatedEmailAddress,
            )
            coVerify { generatorSdkSource.generateForwardedServiceEmail(request) }
        }

    @Test
    fun `generateForwardedService should emit InvalidRequest result when SDK throws exception`() =
        runTest {
            val request = UsernameGeneratorRequest.Forwarded(
                service = ForwarderServiceType.DuckDuckGo(token = "testToken"),
                website = null,
            )
            val exception = RuntimeException("An error occurred")
            coEvery {
                generatorSdkSource.generateForwardedServiceEmail(request)
            } returns Result.failure(exception)

            val result = repository.generateForwardedServiceUsername(request)

            assertTrue(result is GeneratedForwardedServiceUsernameResult.InvalidRequest)
            coVerify { generatorSdkSource.generateForwardedServiceEmail(request) }
        }

    @Test
    fun `getPasscodeGenerationOptions should return options when available`() = runTest {
        val userId = "activeUserId"
        val expectedOptions = PasscodeGenerationOptions(
            length = 14,
            allowAmbiguousChar = false,
            hasNumbers = true,
            minNumber = 0,
            hasUppercase = true,
            minUppercase = null,
            hasLowercase = false,
            minLowercase = null,
            allowSpecial = false,
            minSpecial = 1,
            allowCapitalize = false,
            allowIncludeNumber = false,
            wordSeparator = "-",
            numWords = 3,
        )

        coEvery { authDiskSource.userState } returns USER_STATE

        coEvery {
            generatorDiskSource.getPasscodeGenerationOptions(userId)
        } returns expectedOptions

        val result = repository.getPasscodeGenerationOptions()

        assertEquals(expectedOptions, result)
        coVerify { generatorDiskSource.getPasscodeGenerationOptions(userId) }
    }

    @Test
    fun `getPasscodeGenerationOptions should return null when there is no active user`() = runTest {
        coEvery { authDiskSource.userState } returns null

        val result = repository.getPasscodeGenerationOptions()

        assertNull(result)
        coVerify(exactly = 0) { generatorDiskSource.getPasscodeGenerationOptions(any()) }
    }

    @Test
    fun `getPasscodeGenerationOptions should return null when no data is stored for active user`() =
        runTest {
            val userId = "activeUserId"
            coEvery { authDiskSource.userState } returns USER_STATE
            coEvery { generatorDiskSource.getPasscodeGenerationOptions(userId) } returns null

            val result = repository.getPasscodeGenerationOptions()

            assertNull(result)
            coVerify { generatorDiskSource.getPasscodeGenerationOptions(userId) }
        }

    @Test
    fun `savePasscodeGenerationOptions should store options correctly`() = runTest {
        val userId = "activeUserId"
        val optionsToSave = PasscodeGenerationOptions(
            length = 14,
            allowAmbiguousChar = false,
            hasNumbers = true,
            minNumber = 0,
            hasUppercase = true,
            minUppercase = null,
            hasLowercase = false,
            minLowercase = null,
            allowSpecial = false,
            minSpecial = 1,
            allowCapitalize = false,
            allowIncludeNumber = false,
            wordSeparator = "-",
            numWords = 3,
        )

        coEvery { authDiskSource.userState } returns USER_STATE

        coEvery {
            generatorDiskSource.storePasscodeGenerationOptions(userId, optionsToSave)
        } just runs

        repository.savePasscodeGenerationOptions(optionsToSave)

        coVerify { generatorDiskSource.storePasscodeGenerationOptions(userId, optionsToSave) }
    }

    @Test
    fun `storePasswordHistory should call encrypt and insert functions`() = runTest {
        val testUserId = "testUserId"
        val passwordHistoryView = PasswordHistoryView(
            password = "decryptedPassword",
            lastUsedDate = Instant.parse("2021-01-01T00:00:00Z"),
        )
        val encryptedPasswordHistory = PasswordHistory(
            password = "encryptedPassword",
            lastUsedDate = Instant.parse("2021-01-01T00:00:00Z"),
        )
        val expectedPasswordHistoryEntity = encryptedPasswordHistory
            .toPasswordHistoryEntity(testUserId)

        coEvery { authDiskSource.userState?.activeUserId } returns testUserId

        coEvery {
            vaultSdkSource.encryptPasswordHistory(
                userId = testUserId,
                passwordHistory = passwordHistoryView,
            )
        } returns
            Result.success(encryptedPasswordHistory)

        coEvery {
            passwordHistoryDiskSource.insertPasswordHistory(expectedPasswordHistoryEntity)
        } just runs

        repository.storePasswordHistory(passwordHistoryView)

        coVerify {
            vaultSdkSource.encryptPasswordHistory(
                userId = testUserId,
                passwordHistory = passwordHistoryView,
            )
        }
        coVerify { passwordHistoryDiskSource.insertPasswordHistory(expectedPasswordHistoryEntity) }
    }

    @Test
    fun `passwordHistoryStateFlow should emit correct states based on password history updates`() =
        runTest {
            val encryptedPasswordHistoryEntities = listOf(
                PasswordHistoryEntity(
                    userId = USER_STATE.activeUserId,
                    encryptedPassword = "encryptedPassword2",
                    generatedDateTimeMs = Instant.parse("2021-01-02T00:00:00Z").toEpochMilli(),
                ),
                PasswordHistoryEntity(
                    userId = USER_STATE.activeUserId,
                    encryptedPassword = "encryptedPassword1",
                    generatedDateTimeMs = Instant.parse("2021-01-01T00:00:00Z").toEpochMilli(),
                ),
            )

            val decryptedPasswordHistoryList = listOf(
                PasswordHistoryView(
                    password = "password2",
                    lastUsedDate = Instant.parse("2021-01-02T00:00:00Z"),
                ),
                PasswordHistoryView(
                    password = "password1",
                    lastUsedDate = Instant.parse("2021-01-01T00:00:00Z"),
                ),
            )

            coEvery {
                passwordHistoryDiskSource.getPasswordHistoriesForUser(USER_STATE.activeUserId)
            } returns flowOf(encryptedPasswordHistoryEntities)

            coEvery {
                vaultSdkSource.decryptPasswordHistoryList(any(), any())
            } returns Result.success(decryptedPasswordHistoryList)

            val historyFlow = repository.passwordHistoryStateFlow

            historyFlow.test {
                assertEquals(LocalDataState.Loading, awaitItem())
                mutableUserStateFlow.value = USER_STATE
                assertEquals(LocalDataState.Loaded(decryptedPasswordHistoryList), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            coVerify {
                passwordHistoryDiskSource.getPasswordHistoriesForUser(USER_STATE.activeUserId)
            }

            coVerify { vaultSdkSource.decryptPasswordHistoryList(any(), any()) }
        }

    @Test
    fun `clearPasswordHistory should call clearAllPasswords function`() = runTest {
        val testUserId = "testUserId"
        coEvery { authDiskSource.userState?.activeUserId } returns testUserId
        coEvery { passwordHistoryDiskSource.clearPasswordHistories(testUserId) } just runs

        repository.clearPasswordHistory()

        coVerify { passwordHistoryDiskSource.clearPasswordHistories(testUserId) }
    }

    @Test
    fun `savePasscodeGenerationOptions should not store options when there is no active user`() =
        runTest {
            val optionsToSave = PasscodeGenerationOptions(
                length = 14,
                allowAmbiguousChar = false,
                hasNumbers = true,
                minNumber = 0,
                hasUppercase = true,
                minUppercase = null,
                hasLowercase = false,
                minLowercase = null,
                allowSpecial = false,
                minSpecial = 1,
                allowCapitalize = false,
                allowIncludeNumber = false,
                wordSeparator = "-",
                numWords = 3,
            )

            coEvery { authDiskSource.userState } returns null

            repository.savePasscodeGenerationOptions(optionsToSave)

            coVerify(exactly = 0) {
                generatorDiskSource.storePasscodeGenerationOptions(any(), any())
            }
        }

    private val USER_STATE = UserStateJson(
        activeUserId = "activeUserId",
        accounts = mapOf(
            "activeUserId" to AccountJson(
                profile = AccountJson.Profile(
                    userId = "activeUserId",
                    email = "email",
                    isEmailVerified = true,
                    name = "name",
                    stamp = "stamp",
                    organizationId = "organizationId",
                    avatarColorHex = "avatarColorHex",
                    hasPremium = true,
                    forcePasswordResetReason = ForcePasswordResetReason.ADMIN_FORCE_PASSWORD_RESET,
                    kdfType = KdfTypeJson.ARGON2_ID,
                    kdfIterations = 600000,
                    kdfMemory = 16,
                    kdfParallelism = 4,
                    userDecryptionOptions = UserDecryptionOptionsJson(
                        hasMasterPassword = true,
                        trustedDeviceUserDecryptionOptions = TrustedDeviceUserDecryptionOptionsJson(
                            encryptedPrivateKey = "encryptedPrivateKey",
                            encryptedUserKey = "encryptedUserKey",
                            hasAdminApproval = true,
                            hasLoginApprovingDevice = true,
                            hasManageResetPasswordPermission = true,
                        ),
                        keyConnectorUserDecryptionOptions = KeyConnectorUserDecryptionOptionsJson(
                            keyConnectorUrl = "keyConnectorUrl",
                        ),
                    ),
                ),
                tokens = AccountJson.Tokens(
                    accessToken = "accessToken",
                    refreshToken = "refreshToken",
                ),
                settings = AccountJson.Settings(
                    environmentUrlData = EnvironmentUrlDataJson(
                        base = "base",
                        api = "api",
                        identity = "identity",
                        icon = "icon",
                        notifications = "notifications",
                        webVault = "webVault",
                        events = "events",
                    ),
                ),
            ),
        ),
    )
}
