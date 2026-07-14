package com.x8bit.bitwarden.data.platform.manager.policy

import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.core.data.manager.encryption.EncryptionManager
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.data.datasource.disk.model.EnvironmentUrlDataJson
import com.bitwarden.network.model.KdfTypeJson
import com.bitwarden.network.model.MasterPasswordUnlockDataJson
import com.bitwarden.network.model.UserDecryptionOptionsJson
import com.bitwarden.policies.PolicyType
import com.bitwarden.policies.PolicyView
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.ForcePasswordResetReason
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.sdk.AuthSdkSource
import com.x8bit.bitwarden.data.auth.datasource.sdk.model.PasswordStrength
import com.x8bit.bitwarden.data.auth.datasource.sdk.util.toKdfRequestModel
import com.x8bit.bitwarden.data.auth.repository.model.PasswordStrengthResult
import com.x8bit.bitwarden.data.auth.repository.util.toSdkParams
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockPolicyView
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant

class PasswordPolicyManagerTest {

    private val fakeAuthDiskSource = FakeAuthDiskSource()
    private val authSdkSource: AuthSdkSource = mockk()
    private val mutableActivePolicyFlow = bufferedMutableSharedFlow<List<PolicyView>>()
    private val policyManager: PolicyManager = mockk {
        every {
            getActivePoliciesFlow(type = PolicyType.MASTER_PASSWORD)
        } returns mutableActivePolicyFlow
    }
    private val encryptionManager: EncryptionManager = mockk {
        every {
            encrypt(alias = "PasswordPolicyManager", bytes = any())
        } returns DEFAULT_ENCRYPTED_BYTE_ARRAY.asSuccess()
        every {
            decrypt(alias = "PasswordPolicyManager", bytes = any())
        } returns DEFAULT_DECRYPTED_BYTE_ARRAY.asSuccess()
    }

    private val passwordPolicyManager: PasswordPolicyManager = PasswordPolicyManagerImpl(
        authDiskSource = fakeAuthDiskSource,
        authSdkSource = authSdkSource,
        policyManager = policyManager,
        encryptionManager = encryptionManager,
        dispatcherManager = FakeDispatcherManager(),
    )

    @Test
    fun `validatePasswordAgainstPolicy validates password against policy requirements`() = runTest {
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1

        // A helper method to set a policy with the given parameters.
        fun setPolicy(
            minLength: Int = 0,
            minComplexity: Int? = null,
            requireUpper: Boolean = false,
            requireLower: Boolean = false,
            requireNumbers: Boolean = false,
            requireSpecial: Boolean = false,
        ) {
            every {
                policyManager.getActivePolicies(type = PolicyType.MASTER_PASSWORD)
            } returns listOf(
                createMockPolicyView(
                    type = PolicyType.MASTER_PASSWORD,
                    enabled = true,
                    data = """
                      {
                        "minLength":$minLength,
                        "minComplexity":$minComplexity,
                        "requireUpper":$requireUpper,
                        "requireLower":$requireLower,
                        "requireNumbers":$requireNumbers,
                        "requireSpecial":$requireSpecial,
                        "enforceOnLogin":true
                      }
                    """,
                ),
            )
        }

        setPolicy(minLength = 10)
        assertFalse(passwordPolicyManager.validatePasswordAgainstPolicies(password = "123"))

        val password = "simple"
        coEvery {
            authSdkSource.passwordStrength(
                email = SINGLE_USER_STATE_1.activeAccount.profile.email,
                password = password,
            )
        } returns PasswordStrength.LEVEL_0.asSuccess()
        setPolicy(minComplexity = 10)
        assertFalse(passwordPolicyManager.validatePasswordAgainstPolicies(password = password))

        setPolicy(requireUpper = true)
        assertFalse(passwordPolicyManager.validatePasswordAgainstPolicies(password = "lower"))

        setPolicy(requireLower = true)
        assertFalse(passwordPolicyManager.validatePasswordAgainstPolicies(password = "UPPER"))

        setPolicy(requireNumbers = true)
        assertFalse(passwordPolicyManager.validatePasswordAgainstPolicies(password = "letters"))

        setPolicy(requireSpecial = true)
        assertFalse(passwordPolicyManager.validatePasswordAgainstPolicies(password = "letters"))
    }

    @Test
    fun `passwordResetReason should pull from the user's profile in AuthDiskSource`() = runTest {
        val updatedProfile = PROFILE_1.copy(
            forcePasswordResetReason = ForcePasswordResetReason.WEAK_MASTER_PASSWORD_ON_LOGIN,
        )
        fakeAuthDiskSource.userState = UserStateJson(
            activeUserId = USER_ID_1,
            accounts = mapOf(
                USER_ID_1 to ACCOUNT_1.copy(
                    profile = updatedProfile,
                ),
            ),
        )
        assertEquals(
            ForcePasswordResetReason.WEAK_MASTER_PASSWORD_ON_LOGIN,
            passwordPolicyManager.passwordResetReason,
        )
    }

    @Test
    fun `getPasswordStrength returns expected results for various strength levels`() = runTest {
        coEvery {
            authSdkSource.passwordStrength(email = any(), password = eq("level_0"))
        } returns PasswordStrength.LEVEL_0.asSuccess()

        coEvery {
            authSdkSource.passwordStrength(email = any(), password = eq("level_1"))
        } returns PasswordStrength.LEVEL_1.asSuccess()

        coEvery {
            authSdkSource.passwordStrength(email = any(), password = eq("level_2"))
        } returns PasswordStrength.LEVEL_2.asSuccess()

        coEvery {
            authSdkSource.passwordStrength(email = any(), password = eq("level_3"))
        } returns PasswordStrength.LEVEL_3.asSuccess()

        coEvery {
            authSdkSource.passwordStrength(email = any(), password = eq("level_4"))
        } returns PasswordStrength.LEVEL_4.asSuccess()

        assertEquals(
            PasswordStrengthResult.Success(PasswordStrength.LEVEL_0),
            passwordPolicyManager.getPasswordStrength(email = EMAIL, password = "level_0"),
        )

        assertEquals(
            PasswordStrengthResult.Success(PasswordStrength.LEVEL_1),
            passwordPolicyManager.getPasswordStrength(email = EMAIL, password = "level_1"),
        )

        assertEquals(
            PasswordStrengthResult.Success(PasswordStrength.LEVEL_2),
            passwordPolicyManager.getPasswordStrength(email = EMAIL, password = "level_2"),
        )

        assertEquals(
            PasswordStrengthResult.Success(PasswordStrength.LEVEL_3),
            passwordPolicyManager.getPasswordStrength(email = EMAIL, password = "level_3"),
        )

        assertEquals(
            PasswordStrengthResult.Success(PasswordStrength.LEVEL_4),
            passwordPolicyManager.getPasswordStrength(email = EMAIL, password = "level_4"),
        )
    }

    @Test
    fun `storePasswordToCheck encrypts the password using the EncryptionManager`() = runTest {
        val password = "somePassword"
        passwordPolicyManager.storePasswordToCheck(userId = USER_ID_1, password = password)

        verify(exactly = 1) {
            encryptionManager.encrypt(
                alias = "PasswordPolicyManager",
                bytes = match { it.contentEquals(password.encodeToByteArray()) },
            )
        }
    }

    @Test
    fun `storePasswordToCheck does not store the password when encryption fails`() = runTest {
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
        val password = "somePassword"
        every {
            encryptionManager.encrypt(
                alias = "PasswordPolicyManager",
                bytes = password.encodeToByteArray(),
            )
        } returns Throwable("Fail").asFailure()

        passwordPolicyManager.storePasswordToCheck(userId = USER_ID_1, password = password)
        mutableActivePolicyFlow.tryEmit(listOf(createMasterPasswordPolicyView(minLength = 10)))

        // With nothing stored, there is no encrypted password to decrypt.
        verify(exactly = 0) {
            encryptionManager.decrypt(alias = "PasswordPolicyManager", bytes = any())
        }
        assertNull(fakeAuthDiskSource.userState?.activeAccount?.profile?.forcePasswordResetReason)
    }

    @Test
    fun `policy emission decrypts the stored password and sets reset reason when it fails`() =
        runTest {
            fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
            val password = "123"
            every {
                encryptionManager.decrypt(alias = "PasswordPolicyManager", bytes = any())
            } returns password.encodeToByteArray().asSuccess()

            passwordPolicyManager.storePasswordToCheck(userId = USER_ID_1, password = password)
            mutableActivePolicyFlow.tryEmit(listOf(createMasterPasswordPolicyView(minLength = 10)))

            verify(exactly = 1) {
                encryptionManager.decrypt(
                    alias = "PasswordPolicyManager",
                    bytes = match { it.contentEquals(DEFAULT_ENCRYPTED_BYTE_ARRAY) },
                )
            }
            assertEquals(
                ForcePasswordResetReason.WEAK_MASTER_PASSWORD_ON_LOGIN,
                fakeAuthDiskSource.userState?.activeAccount?.profile?.forcePasswordResetReason,
            )
        }

    @Test
    fun `policy emission decrypts the stored password and clears reset reason when it passes`() =
        runTest {
            fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
            val password = "aValidPassword"
            every {
                encryptionManager.decrypt(alias = "PasswordPolicyManager", bytes = any())
            } returns password.encodeToByteArray().asSuccess()

            passwordPolicyManager.storePasswordToCheck(userId = USER_ID_1, password = password)
            mutableActivePolicyFlow.tryEmit(listOf(createMasterPasswordPolicyView(minLength = 5)))

            verify(exactly = 1) {
                encryptionManager.decrypt(
                    alias = "PasswordPolicyManager",
                    bytes = match { it.contentEquals(DEFAULT_ENCRYPTED_BYTE_ARRAY) },
                )
            }
            assertNull(
                fakeAuthDiskSource.userState?.activeAccount?.profile?.forcePasswordResetReason,
            )
        }

    @Test
    fun `policy emission does not set a reset reason when decryption fails`() = runTest {
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
        every {
            encryptionManager.decrypt(alias = "PasswordPolicyManager", bytes = any())
        } returns Throwable("Fail").asFailure()

        passwordPolicyManager.storePasswordToCheck(userId = USER_ID_1, password = "123")
        mutableActivePolicyFlow.tryEmit(listOf(createMasterPasswordPolicyView(minLength = 10)))

        verify(exactly = 1) {
            encryptionManager.decrypt(
                alias = "PasswordPolicyManager",
                bytes = match { it.contentEquals(DEFAULT_ENCRYPTED_BYTE_ARRAY) },
            )
        }
        assertNull(fakeAuthDiskSource.userState?.activeAccount?.profile?.forcePasswordResetReason)
    }

    @Test
    fun `policy emission does not decrypt when a reset reason is already set`() = runTest {
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1.copy(
            accounts = mapOf(
                USER_ID_1 to ACCOUNT_1.copy(
                    profile = PROFILE_1.copy(
                        forcePasswordResetReason =
                            ForcePasswordResetReason.WEAK_MASTER_PASSWORD_ON_LOGIN,
                    ),
                ),
            ),
        )

        passwordPolicyManager.storePasswordToCheck(userId = USER_ID_1, password = "123")
        mutableActivePolicyFlow.tryEmit(listOf(createMasterPasswordPolicyView(minLength = 10)))

        // The user is already being forced to reset, so no re-check is performed.
        verify(exactly = 0) {
            encryptionManager.decrypt(alias = "PasswordPolicyManager", bytes = any())
        }
    }

    @Test
    fun `removePasswordToCheck prevents the stored password from being decrypted`() = runTest {
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1

        passwordPolicyManager.storePasswordToCheck(userId = USER_ID_1, password = "123")
        passwordPolicyManager.removePasswordToCheck(userId = USER_ID_1)
        mutableActivePolicyFlow.tryEmit(listOf(createMasterPasswordPolicyView(minLength = 10)))

        verify(exactly = 0) {
            encryptionManager.decrypt(alias = "PasswordPolicyManager", bytes = any())
        }
        assertNull(fakeAuthDiskSource.userState?.activeAccount?.profile?.forcePasswordResetReason)
    }

    @Test
    fun `switching users removes the stored password before it can be decrypted`() = runTest {
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
        passwordPolicyManager.storePasswordToCheck(userId = USER_ID_1, password = "123")

        // Switch away to a different active user and then back to the original user.
        fakeAuthDiskSource.userState = UserStateJson(
            activeUserId = USER_ID_2,
            accounts = mapOf(USER_ID_2 to ACCOUNT_1),
        )
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1

        mutableActivePolicyFlow.tryEmit(listOf(createMasterPasswordPolicyView(minLength = 10)))

        // The password was purged when the previous active user changed away.
        verify(exactly = 0) {
            encryptionManager.decrypt(alias = "PasswordPolicyManager", bytes = any())
        }
    }
}

private const val EMAIL = "test@bitwarden.com"
private const val USER_ID_1 = "2a135b23-e1fb-42c9-bec3-573857bc8181"
private const val USER_ID_2 = "b9d09b0a-4c3f-4b3d-9c2f-1b2d3e4f5a6b"
private const val ENCRYPTED_USER_KEY = "encryptedUserKey"

/**
 * Creates a [PolicyView] with a master password policy enforcing the given [minLength] on login.
 */
private fun createMasterPasswordPolicyView(minLength: Int): PolicyView =
    createMockPolicyView(
        type = PolicyType.MASTER_PASSWORD,
        enabled = true,
        data = """
          {
            "minLength":$minLength,
            "minComplexity":null,
            "requireUpper":false,
            "requireLower":false,
            "requireNumbers":false,
            "requireSpecial":false,
            "enforceOnLogin":true
          }
        """,
    )

private val BASE_PROFILE_1 = AccountJson.Profile(
    userId = USER_ID_1,
    email = EMAIL,
    isEmailVerified = true,
    name = "Bitwarden Tester",
    hasPremiumPersonally = false,
    hasPremiumFromOrganization = null,
    stamp = null,
    organizationId = null,
    avatarColorHex = null,
    forcePasswordResetReason = null,
    kdfType = KdfTypeJson.ARGON2_ID,
    kdfIterations = 600000,
    kdfMemory = 16,
    kdfParallelism = 4,
    userDecryptionOptions = null,
    isTwoFactorEnabled = false,
    creationDate = Instant.parse("2024-09-13T01:00:00.00Z"),
)

private val PROFILE_1 = BASE_PROFILE_1.copy(
    userDecryptionOptions = UserDecryptionOptionsJson(
        hasMasterPassword = true,
        trustedDeviceUserDecryptionOptions = null,
        keyConnectorUserDecryptionOptions = null,
        masterPasswordUnlock = MasterPasswordUnlockDataJson(
            kdf = BASE_PROFILE_1.toSdkParams().toKdfRequestModel(),
            masterKeyWrappedUserKey = ENCRYPTED_USER_KEY,
            salt = EMAIL,
        ),
    ),
)
private val ACCOUNT_1 = AccountJson(
    profile = PROFILE_1,
    settings = AccountJson.Settings(
        environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
    ),
)

private val SINGLE_USER_STATE_1 = UserStateJson(
    activeUserId = USER_ID_1,
    accounts = mapOf(
        USER_ID_1 to ACCOUNT_1,
    ),
)

private val DEFAULT_ENCRYPTED_BYTE_ARRAY: ByteArray = ByteArray(4) { (0x80 + it).toByte() }
private val DEFAULT_DECRYPTED_BYTE_ARRAY: ByteArray = ByteArray(4) { (0xFF + it).toByte() }
