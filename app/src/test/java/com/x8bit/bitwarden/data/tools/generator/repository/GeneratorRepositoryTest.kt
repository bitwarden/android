package com.x8bit.bitwarden.data.tools.generator.repository

import com.bitwarden.core.PassphraseGeneratorRequest
import com.bitwarden.core.PasswordGeneratorRequest
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.EnvironmentUrlDataJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.ForcePasswordResetReason
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.KdfTypeJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.KeyConnectorUserDecryptionOptionsJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.TrustedDeviceUserDecryptionOptionsJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.UserDecryptionOptionsJson
import com.x8bit.bitwarden.data.tools.generator.datasource.disk.GeneratorDiskSource
import com.x8bit.bitwarden.data.tools.generator.datasource.sdk.GeneratorSdkSource
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedPassphraseResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedPasswordResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.PasscodeGenerationOptions
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GeneratorRepositoryTest {

    private val generatorSdkSource: GeneratorSdkSource = mockk()
    private val generatorDiskSource: GeneratorDiskSource = mockk()
    private val authDiskSource: AuthDiskSource = mockk()

    private val repository = GeneratorRepositoryImpl(
        generatorSdkSource = generatorSdkSource,
        generatorDiskSource = generatorDiskSource,
        authDiskSource = authDiskSource,
    )

    @BeforeEach
    fun setUp() {
        clearMocks(generatorSdkSource)
    }

    @Test
    fun `generatePassword should emit Success result with the generated password`() = runTest {
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
        val expectedResult = "GeneratedPassword123!"
        coEvery {
            generatorSdkSource.generatePassword(request)
        } returns Result.success(expectedResult)

        val result = repository.generatePassword(request)

        assertEquals(expectedResult, (result as GeneratedPasswordResult.Success).generatedString)
        coVerify { generatorSdkSource.generatePassword(request) }
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

        val result = repository.generatePassword(request)

        assertTrue(result is GeneratedPasswordResult.InvalidRequest)
        coVerify { generatorSdkSource.generatePassword(request) }
    }

    @Test
    fun `generatePassphrase should emit Success result with the generated passphrase`() = runTest {
        val request = PassphraseGeneratorRequest(
            numWords = 5.toUByte(),
            capitalize = true,
            includeNumber = true,
            wordSeparator = '-'.toString(),
        )
        val expectedResult = "Generated-Passphrase-123!"
        coEvery {
            generatorSdkSource.generatePassphrase(request)
        } returns Result.success(expectedResult)

        val result = repository.generatePassphrase(request)

        assertEquals(expectedResult, (result as GeneratedPassphraseResult.Success).generatedString)
        coVerify { generatorSdkSource.generatePassphrase(request) }
    }

    @Suppress("MaxLineLength")
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

    @Suppress("MaxLineLength")
    @Test
    fun `getPasscodeGenerationOptions should return null when no data is stored for active user`() = runTest {
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
        } just Runs

        repository.savePasscodeGenerationOptions(optionsToSave)

        coVerify { generatorDiskSource.storePasscodeGenerationOptions(userId, optionsToSave) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `savePasscodeGenerationOptions should not store options when there is no active user`() = runTest {
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

        coVerify(exactly = 0) { generatorDiskSource.storePasscodeGenerationOptions(any(), any()) }
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
