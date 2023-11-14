package com.x8bit.bitwarden.data.generator.repository

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
import com.x8bit.bitwarden.data.generator.datasource.disk.GeneratorDiskSource
import com.x8bit.bitwarden.data.generator.datasource.sdk.GeneratorSdkSource
import com.x8bit.bitwarden.data.generator.repository.model.GeneratedPasswordResult
import com.x8bit.bitwarden.data.generator.repository.model.PasswordGenerationOptions
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
    fun `getPasswordGenerationOptions should return options when available`() = runTest {
        val userId = "activeUserId"
        val expectedOptions = PasswordGenerationOptions(
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
        )

        coEvery { authDiskSource.userState } returns USER_STATE

        coEvery {
            generatorDiskSource.getPasswordGenerationOptions(userId)
        } returns expectedOptions

        val result = repository.getPasswordGenerationOptions()

        assertEquals(expectedOptions, result)
        coVerify { generatorDiskSource.getPasswordGenerationOptions(userId) }
    }

    @Test
    fun `getPasswordGenerationOptions should return null when there is no active user`() = runTest {
        coEvery { authDiskSource.userState } returns null

        val result = repository.getPasswordGenerationOptions()

        assertNull(result)
        coVerify(exactly = 0) { generatorDiskSource.getPasswordGenerationOptions(any()) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getPasswordGenerationOptions should return null when no data is stored for active user`() = runTest {
        val userId = "activeUserId"
        coEvery { authDiskSource.userState } returns USER_STATE
        coEvery { generatorDiskSource.getPasswordGenerationOptions(userId) } returns null

        val result = repository.getPasswordGenerationOptions()

        assertNull(result)
        coVerify { generatorDiskSource.getPasswordGenerationOptions(userId) }
    }

    @Test
    fun `savePasswordGenerationOptions should store options correctly`() = runTest {
        val userId = "activeUserId"
        val optionsToSave = PasswordGenerationOptions(
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
        )

        coEvery { authDiskSource.userState } returns USER_STATE

        coEvery {
            generatorDiskSource.storePasswordGenerationOptions(userId, optionsToSave)
        } just Runs

        repository.savePasswordGenerationOptions(optionsToSave)

        coVerify { generatorDiskSource.storePasswordGenerationOptions(userId, optionsToSave) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `savePasswordGenerationOptions should not store options when there is no active user`() = runTest {
        val optionsToSave = PasswordGenerationOptions(
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
        )

        coEvery { authDiskSource.userState } returns null

        repository.savePasswordGenerationOptions(optionsToSave)

        coVerify(exactly = 0) { generatorDiskSource.storePasswordGenerationOptions(any(), any()) }
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
