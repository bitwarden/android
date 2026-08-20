package com.x8bit.bitwarden.data.platform.manager.policy

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
import com.x8bit.bitwarden.data.auth.repository.model.PolicyInformation
import com.x8bit.bitwarden.data.auth.repository.util.toSdkParams
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockPolicyView
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class PasswordPolicyManagerTest {

    private val fakeAuthDiskSource = FakeAuthDiskSource()
    private val authSdkSource: AuthSdkSource = mockk()
    private val policyManager: PolicyManager = mockk {
        every { getActivePolicies(type = PolicyType.MASTER_PASSWORD) } returns emptyList()
    }

    private val passwordPolicyManager: PasswordPolicyManager = PasswordPolicyManagerImpl(
        authDiskSource = fakeAuthDiskSource,
        authSdkSource = authSdkSource,
        policyManager = policyManager,
    )

    @Test
    fun `passwordPolicies should return the active master password policies`() {
        setPolicies(
            createPolicyJson(minLength = 12, requireUpper = true),
            createPolicyJson(minComplexity = 3, enforceOnLogin = false),
        )

        assertEquals(
            listOf(
                PolicyInformation.MasterPassword(
                    minLength = 12,
                    minComplexity = null,
                    requireUpper = true,
                    requireLower = false,
                    requireNumbers = false,
                    requireSpecial = false,
                    enforceOnLogin = true,
                ),
                PolicyInformation.MasterPassword(
                    minLength = 0,
                    minComplexity = 3,
                    requireUpper = false,
                    requireLower = false,
                    requireNumbers = false,
                    requireSpecial = false,
                    enforceOnLogin = false,
                ),
            ),
            passwordPolicyManager.passwordPolicies,
        )
    }

    @Test
    fun `passwordResetReason should pull from the user's profile in AuthDiskSource`() {
        fakeAuthDiskSource.userState = UserStateJson(
            activeUserId = USER_ID_1,
            accounts = mapOf(
                USER_ID_1 to ACCOUNT_1.copy(
                    profile = PROFILE_1.copy(
                        forcePasswordResetReason = ForcePasswordResetReason
                            .WEAK_MASTER_PASSWORD_ON_LOGIN,
                    ),
                ),
            ),
        )

        assertEquals(
            ForcePasswordResetReason.WEAK_MASTER_PASSWORD_ON_LOGIN,
            passwordPolicyManager.passwordResetReason,
        )
    }

    @Test
    fun `passwordResetReason should return null when there is no active user`() {
        assertNull(passwordPolicyManager.passwordResetReason)
    }

    @Test
    fun `getPasswordStrength returns expected results for various strength levels`() {
        PasswordStrength.entries.forEach { strength ->
            every {
                authSdkSource.passwordStrength(email = any(), password = strength.name)
            } returns strength.asSuccess()

            assertEquals(
                PasswordStrengthResult.Success(strength),
                passwordPolicyManager.getPasswordStrength(
                    email = EMAIL,
                    password = strength.name,
                ),
            )
        }
    }

    @Test
    fun `getPasswordStrength failure should return Error`() {
        val error = Throwable("Fail")
        every {
            authSdkSource.passwordStrength(email = EMAIL, password = PASSWORD)
        } returns error.asFailure()

        assertEquals(
            PasswordStrengthResult.Error(error = error),
            passwordPolicyManager.getPasswordStrength(email = EMAIL, password = PASSWORD),
        )
    }

    @Test
    fun `getPasswordStrength with a null email should use the active account email`() {
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
        every {
            authSdkSource.passwordStrength(email = EMAIL, password = PASSWORD)
        } returns PasswordStrength.LEVEL_3.asSuccess()

        assertEquals(
            PasswordStrengthResult.Success(PasswordStrength.LEVEL_3),
            passwordPolicyManager.getPasswordStrength(password = PASSWORD),
        )
        verify(exactly = 1) {
            authSdkSource.passwordStrength(email = EMAIL, password = PASSWORD)
        }
    }

    @Test
    fun `getPasswordStrength with a null email and no active account should use an empty email`() {
        every {
            authSdkSource.passwordStrength(email = "", password = PASSWORD)
        } returns PasswordStrength.LEVEL_3.asSuccess()

        assertEquals(
            PasswordStrengthResult.Success(PasswordStrength.LEVEL_3),
            passwordPolicyManager.getPasswordStrength(password = PASSWORD),
        )
        verify(exactly = 1) {
            authSdkSource.passwordStrength(email = "", password = PASSWORD)
        }
    }

    @Test
    fun `validatePasswordAgainstPolicies should return true when there are no active policies`() {
        assertTrue(passwordPolicyManager.validatePasswordAgainstPolicies(password = "123"))
    }

    @Test
    fun `validatePasswordAgainstPolicies validates password against policy requirements`() {
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1

        setPolicies(createPolicyJson(minLength = 10))
        assertFalse(passwordPolicyManager.validatePasswordAgainstPolicies(password = "123"))

        val password = "simple"
        coEvery {
            authSdkSource.passwordStrength(email = EMAIL, password = password)
        } returns PasswordStrength.LEVEL_0.asSuccess()
        setPolicies(createPolicyJson(minComplexity = 10))
        assertFalse(passwordPolicyManager.validatePasswordAgainstPolicies(password = password))

        setPolicies(createPolicyJson(requireUpper = true))
        assertFalse(passwordPolicyManager.validatePasswordAgainstPolicies(password = "lower"))

        setPolicies(createPolicyJson(requireLower = true))
        assertFalse(passwordPolicyManager.validatePasswordAgainstPolicies(password = "UPPER"))

        setPolicies(createPolicyJson(requireNumbers = true))
        assertFalse(passwordPolicyManager.validatePasswordAgainstPolicies(password = "letters"))

        setPolicies(createPolicyJson(requireSpecial = true))
        assertFalse(passwordPolicyManager.validatePasswordAgainstPolicies(password = "letters"))
    }

    @Suppress("MaxLineLength")
    @Test
    fun `validatePasswordAgainstPolicies should return true when the password satisfies every requirement`() {
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
        val password = "Str0ng!Password"
        every {
            authSdkSource.passwordStrength(email = EMAIL, password = password)
        } returns PasswordStrength.LEVEL_4.asSuccess()
        setPolicies(
            createPolicyJson(
                minLength = 10,
                minComplexity = 3,
                requireUpper = true,
                requireLower = true,
                requireNumbers = true,
                requireSpecial = true,
            ),
        )

        assertTrue(passwordPolicyManager.validatePasswordAgainstPolicies(password = password))
    }

    @Test
    fun `validatePasswordAgainstPolicies should ignore a minLength of zero`() {
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
        setPolicies(createPolicyJson(minLength = 0))

        assertTrue(passwordPolicyManager.validatePasswordAgainstPolicies(password = "a"))
        verify(exactly = 0) {
            authSdkSource.passwordStrength(email = any(), password = any())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `validatePasswordAgainstPolicies should ignore a minComplexity of zero even for the weakest password`() {
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
        // Note that the strength is still requested from the SDK before the zero check happens.
        every {
            authSdkSource.passwordStrength(email = EMAIL, password = "a")
        } returns PasswordStrength.LEVEL_0.asSuccess()
        setPolicies(createPolicyJson(minComplexity = 0))

        assertTrue(passwordPolicyManager.validatePasswordAgainstPolicies(password = "a"))
    }

    @Suppress("MaxLineLength")
    @Test
    fun `validatePasswordAgainstPolicies with isCreation false should ignore policies not enforced on login`() {
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
        setPolicies(
            createPolicyJson(minLength = 100, enforceOnLogin = false),
            createPolicyJson(minLength = 100, enforceOnLogin = null),
        )

        assertTrue(
            passwordPolicyManager.validatePasswordAgainstPolicies(
                password = "123",
                isCreation = false,
            ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `validatePasswordAgainstPolicies with isCreation false should enforce policies enforced on login`() {
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
        setPolicies(createPolicyJson(minLength = 100, enforceOnLogin = true))

        assertFalse(
            passwordPolicyManager.validatePasswordAgainstPolicies(
                password = "123",
                isCreation = false,
            ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `validatePasswordAgainstPolicies with isCreation true should enforce policies not enforced on login`() {
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
        setPolicies(createPolicyJson(minLength = 100, enforceOnLogin = false))

        assertFalse(
            passwordPolicyManager.validatePasswordAgainstPolicies(
                password = "123",
                isCreation = true,
            ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `validatePasswordAgainstPolicies with a minComplexity policy and no active account should skip the complexity check`() {
        setPolicies(createPolicyJson(minComplexity = 4))

        assertTrue(passwordPolicyManager.validatePasswordAgainstPolicies(password = "123"))
        verify(exactly = 0) {
            authSdkSource.passwordStrength(email = any(), password = any())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `validatePasswordAgainstPolicies with a minComplexity policy should skip the complexity check when the strength lookup fails`() {
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
        every {
            authSdkSource.passwordStrength(email = EMAIL, password = "123")
        } returns Throwable("Fail").asFailure()
        setPolicies(createPolicyJson(minComplexity = 4))

        assertTrue(passwordPolicyManager.validatePasswordAgainstPolicies(password = "123"))
    }

    @Test
    fun `passwordPassesPolicy should return true when enforceOnLogin is null or false`() {
        listOf(null, false).forEach { enforceOnLogin ->
            assertTrue(
                passwordPolicyManager.passwordPassesPolicy(
                    password = "123",
                    policyInfo = PolicyInformation.MasterPassword(
                        minLength = 100,
                        minComplexity = null,
                        requireUpper = null,
                        requireLower = null,
                        requireNumbers = null,
                        requireSpecial = null,
                        enforceOnLogin = enforceOnLogin,
                    ),
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `passwordPassesPolicy should return false when enforced on login and the password fails the policy`() {
        assertFalse(
            passwordPolicyManager.passwordPassesPolicy(
                password = "123",
                policyInfo = PolicyInformation.MasterPassword(
                    minLength = 100,
                    minComplexity = null,
                    requireUpper = null,
                    requireLower = null,
                    requireNumbers = null,
                    requireSpecial = null,
                    enforceOnLogin = true,
                ),
            ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `passwordPassesPolicy should return true when enforced on login and the password passes the policy`() {
        assertTrue(
            passwordPolicyManager.passwordPassesPolicy(
                password = "Str0ng!Password",
                policyInfo = PolicyInformation.MasterPassword(
                    minLength = 10,
                    minComplexity = null,
                    requireUpper = true,
                    requireLower = true,
                    requireNumbers = true,
                    requireSpecial = true,
                    enforceOnLogin = true,
                ),
            ),
        )
    }

    /**
     * Sets the active master password policies to the given [policyDataJson] payloads, each of
     * which should be created via [createPolicyJson].
     */
    private fun setPolicies(vararg policyDataJson: String) {
        every {
            policyManager.getActivePolicies(type = PolicyType.MASTER_PASSWORD)
        } returns policyDataJson.map { data ->
            createMockPolicyView(
                type = PolicyType.MASTER_PASSWORD,
                enabled = true,
                data = data,
            )
        }
    }
}

/**
 * Builds the `data` payload of a master password [PolicyView].
 */
@Suppress("LongParameterList")
private fun createPolicyJson(
    minLength: Int = 0,
    minComplexity: Int? = null,
    requireUpper: Boolean = false,
    requireLower: Boolean = false,
    requireNumbers: Boolean = false,
    requireSpecial: Boolean = false,
    enforceOnLogin: Boolean? = true,
): String =
    """
    {
      "minLength":$minLength,
      "minComplexity":$minComplexity,
      "requireUpper":$requireUpper,
      "requireLower":$requireLower,
      "requireNumbers":$requireNumbers,
      "requireSpecial":$requireSpecial,
      "enforceOnLogin":$enforceOnLogin
    }
    """

private const val EMAIL = "test@bitwarden.com"
private const val PASSWORD = "password"
private const val USER_ID_1 = "2a135b23-e1fb-42c9-bec3-573857bc8181"
private const val ENCRYPTED_USER_KEY = "encryptedUserKey"

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
