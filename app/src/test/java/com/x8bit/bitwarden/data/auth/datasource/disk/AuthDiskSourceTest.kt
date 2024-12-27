package com.x8bit.bitwarden.data.auth.datasource.disk

import androidx.core.content.edit
import app.cash.turbine.test
import com.bitwarden.authenticatorbridge.util.generateSecretKey
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountTokensJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.EnvironmentUrlDataJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.ForcePasswordResetReason
import com.x8bit.bitwarden.data.auth.datasource.disk.model.NewDeviceNoticeDisplayStatus
import com.x8bit.bitwarden.data.auth.datasource.disk.model.NewDeviceNoticeState
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.datasource.disk.model.PendingAuthRequestJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.KdfTypeJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.KeyConnectorUserDecryptionOptionsJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.TrustedDeviceUserDecryptionOptionsJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.UserDecryptionOptionsJson
import com.x8bit.bitwarden.data.platform.base.FakeSharedPreferences
import com.x8bit.bitwarden.data.platform.datasource.disk.legacy.LegacySecureStorageMigrator
import com.x8bit.bitwarden.data.platform.datasource.network.di.PlatformNetworkModule
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockOrganization
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockPolicy
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.encodeToJsonElement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

@Suppress("LargeClass")
class AuthDiskSourceTest {
    private val fakeEncryptedSharedPreferences = FakeSharedPreferences()
    private val fakeSharedPreferences = FakeSharedPreferences()
    private val legacySecureStorageMigrator = mockk<LegacySecureStorageMigrator> {
        every { migrateIfNecessary() } just runs
    }

    private val json = PlatformNetworkModule.providesJson()

    private val authDiskSource = AuthDiskSourceImpl(
        encryptedSharedPreferences = fakeEncryptedSharedPreferences,
        sharedPreferences = fakeSharedPreferences,
        legacySecureStorageMigrator = legacySecureStorageMigrator,
        json = json,
    )

    @Test
    fun `initialization should kick off a legacy migration if necessary`() {
        verify(exactly = 1) { legacySecureStorageMigrator.migrateIfNecessary() }
    }

    @Test
    fun `uniqueAppId should generate a new ID and update SharedPreferences if none exists`() {
        val rememberedUniqueAppIdKey = "bwPreferencesStorage:appId"

        // Assert that the SharedPreferences are empty
        assertNull(fakeSharedPreferences.getString(rememberedUniqueAppIdKey, null))

        // Generate a new uniqueAppId and retrieve it
        val newId = authDiskSource.uniqueAppId

        // Ensure that the SharedPreferences were updated
        assertEquals(
            newId,
            fakeSharedPreferences.getString(rememberedUniqueAppIdKey, null),
        )
    }

    @Test
    fun `uniqueAppId should not generate a new ID if one exists`() {
        val rememberedUniqueAppIdKey = "bwPreferencesStorage:appId"
        val testId = "testId"

        // Update preferences to hold test value
        fakeSharedPreferences.edit { putString(rememberedUniqueAppIdKey, testId) }

        assertEquals(testId, authDiskSource.uniqueAppId)
    }

    @Test
    fun `rememberedEmailAddress should pull from and update SharedPreferences`() {
        val rememberedEmailKey = "bwPreferencesStorage:rememberedEmail"

        // Shared preferences and the repository start with the same value.
        assertNull(authDiskSource.rememberedEmailAddress)
        assertNull(fakeSharedPreferences.getString(rememberedEmailKey, null))

        // Updating the repository updates shared preferences
        authDiskSource.rememberedEmailAddress = "remembered@gmail.com"
        assertEquals(
            "remembered@gmail.com",
            fakeSharedPreferences.getString(rememberedEmailKey, null),
        )

        // Update SharedPreferences updates the repository
        fakeSharedPreferences.edit { putString(rememberedEmailKey, null) }
        assertNull(authDiskSource.rememberedEmailAddress)
    }

    @Test
    fun `rememberedOrgIdentifier should pull from and update SharedPreferences`() {
        val rememberedOrgIdentifierKey = "bwPreferencesStorage:rememberedOrgIdentifier"

        // Shared preferences and the disk source start with the same value.
        assertNull(authDiskSource.rememberedOrgIdentifier)
        assertNull(fakeSharedPreferences.getString(rememberedOrgIdentifierKey, null))

        // Updating the disk source updates shared preferences
        authDiskSource.rememberedOrgIdentifier = "Bitwarden"
        assertEquals(
            "Bitwarden",
            fakeSharedPreferences.getString(rememberedOrgIdentifierKey, null),
        )

        // Update SharedPreferences updates the disk source
        fakeSharedPreferences.edit { putString(rememberedOrgIdentifierKey, null) }
        assertNull(authDiskSource.rememberedOrgIdentifier)
    }

    @Test
    fun `shouldUseKeyConnector should pull from and update SharedPreferences`() {
        val userId = "userId"
        val shouldUseKeyConnectorKey = "bwPreferencesStorage:usesKeyConnector_$userId"

        // Shared preferences and the disk source start with the same value.
        assertNull(authDiskSource.getShouldUseKeyConnector(userId = userId))
        assertFalse(fakeSharedPreferences.getBoolean(shouldUseKeyConnectorKey, false))

        // Updating the disk source updates shared preferences
        authDiskSource.storeShouldUseKeyConnector(userId = userId, shouldUseKeyConnector = true)
        assertTrue(fakeSharedPreferences.getBoolean(shouldUseKeyConnectorKey, false))

        // Update SharedPreferences updates the disk source
        fakeSharedPreferences.edit { putBoolean(shouldUseKeyConnectorKey, false) }
        assertFalse(authDiskSource.getShouldUseKeyConnector(userId = userId) ?: true)
    }

    @Test
    fun `getShouldUseKeyConnectorFlow should react to changes in getShouldUseKeyConnector`() =
        runTest {
            val mockUserId = "mockUserId"
            authDiskSource.getShouldUseKeyConnectorFlow(userId = mockUserId).test {
                assertNull(authDiskSource.getShouldUseKeyConnector(userId = mockUserId))
                assertNull(awaitItem())
                authDiskSource.storeShouldUseKeyConnector(
                    userId = mockUserId,
                    shouldUseKeyConnector = true,
                )
                assertEquals(true, awaitItem())
                authDiskSource.storeShouldUseKeyConnector(
                    userId = mockUserId,
                    shouldUseKeyConnector = false,
                )
                assertEquals(false, awaitItem())
            }
        }

    @Test
    fun `tdeLoginComplete should pull from and update SharedPreferences`() {
        val userId = "userId"
        val isTdeLoginComplete = "bwPreferencesStorage:tdeLoginComplete_$userId"

        // Shared preferences and the disk source start with the same value.
        assertNull(authDiskSource.getIsTdeLoginComplete(userId = userId))
        assertFalse(fakeSharedPreferences.getBoolean(isTdeLoginComplete, false))

        // Updating the disk source updates shared preferences
        authDiskSource.storeIsTdeLoginComplete(userId = userId, isTdeLoginComplete = true)
        assertTrue(fakeSharedPreferences.getBoolean(isTdeLoginComplete, false))

        // Update SharedPreferences updates the disk source
        fakeSharedPreferences.edit { putBoolean(isTdeLoginComplete, false) }
        assertFalse(authDiskSource.getIsTdeLoginComplete(userId = userId) ?: true)
    }

    @Test
    fun `shouldTrustDevice should pull from and update SharedPreferences`() {
        val userId = "userId"
        val shouldTrustDeviceKey = "bwPreferencesStorage:shouldTrustDevice_$userId"

        // Shared preferences and the disk source start with the same value.
        assertNull(authDiskSource.getShouldTrustDevice(userId = userId))
        assertFalse(fakeSharedPreferences.getBoolean(shouldTrustDeviceKey, false))

        // Updating the disk source updates shared preferences
        authDiskSource.storeShouldTrustDevice(userId = userId, shouldTrustDevice = true)
        assertTrue(fakeSharedPreferences.getBoolean(shouldTrustDeviceKey, false))

        // Update SharedPreferences updates the disk source
        fakeSharedPreferences.edit { putBoolean(shouldTrustDeviceKey, false) }
        assertFalse(authDiskSource.getShouldTrustDevice(userId = userId) ?: true)
    }

    @Test
    fun `userState should pull from and update SharedPreferences`() {
        val userStateKey = "bwPreferencesStorage:state"

        // Shared preferences and the repository start with the same value.
        assertNull(authDiskSource.userState)
        assertNull(fakeSharedPreferences.getString(userStateKey, null))

        // Updating the repository updates shared preferences
        authDiskSource.userState = USER_STATE
        assertEquals(
            json.parseToJsonElement(
                USER_STATE_JSON,
            ),
            json.parseToJsonElement(
                fakeSharedPreferences.getString(userStateKey, null)!!,
            ),
        )

        // Update SharedPreferences updates the repository
        fakeSharedPreferences.edit { putString(userStateKey, null) }
        assertNull(authDiskSource.userState)
    }

    @Test
    fun `userStateFlow should react to changes in userState`() = runTest {
        authDiskSource.userStateFlow.test {
            // The initial values of the Flow and the property are in sync
            assertNull(authDiskSource.userState)
            assertNull(awaitItem())

            // Extra emission from migration logic
            assertNull(awaitItem())

            // Updating the repository updates shared preferences
            authDiskSource.userState = USER_STATE
            assertEquals(USER_STATE, awaitItem())
        }
    }

    @Test
    fun `clearData should clear all necessary data for the given user`() {
        val userId = "userId"

        val pendingAuthRequestJson = PendingAuthRequestJson(
            requestId = "12345",
            requestPrivateKey = "67890",
            requestFingerprint = "fingerprint",
            requestAccessCode = "accessCode",
        )
        authDiskSource.storePendingAuthRequest(
            userId = userId,
            pendingAuthRequest = pendingAuthRequestJson,
        )
        authDiskSource.storeShouldUseKeyConnector(userId = userId, shouldUseKeyConnector = true)
        val shouldTrustDevice = true
        authDiskSource.storeShouldTrustDevice(
            userId = userId,
            shouldTrustDevice = shouldTrustDevice,
        )
        authDiskSource.storeIsTdeLoginComplete(userId = userId, isTdeLoginComplete = true)
        val deviceKey = "deviceKey"
        authDiskSource.storeDeviceKey(userId = userId, deviceKey = deviceKey)
        authDiskSource.storeUserBiometricUnlockKey(
            userId = userId,
            biometricsKey = "1234-9876-0192",
        )
        authDiskSource.storePinProtectedUserKey(
            userId = userId,
            pinProtectedUserKey = "pinProtectedUserKey",
        )
        authDiskSource.storeInvalidUnlockAttempts(
            userId = userId,
            invalidUnlockAttempts = 1,
        )
        authDiskSource.storeUserKey(userId = userId, userKey = "userKey")
        authDiskSource.storeUserAutoUnlockKey(
            userId = userId,
            userAutoUnlockKey = "userAutoUnlockKey",
        )
        authDiskSource.storePrivateKey(userId = userId, privateKey = "privateKey")
        authDiskSource.storeOrganizationKeys(
            userId = userId,
            organizationKeys = mapOf("organizationId" to "key"),
        )
        authDiskSource.storeOrganizations(
            userId = userId,
            organizations = listOf(createMockOrganization(1)),
        )
        authDiskSource.storePolicies(
            userId = userId,
            policies = listOf(createMockPolicy()),
        )
        authDiskSource.storeAccountTokens(
            userId = userId,
            accountTokens = AccountTokensJson(
                accessToken = "accessToken",
                refreshToken = "refreshToken",
            ),
        )
        authDiskSource.storeEncryptedPin(userId = userId, encryptedPin = "encryptedPin")
        authDiskSource.storeMasterPasswordHash(userId = userId, passwordHash = "passwordHash")
        authDiskSource.storeAuthenticatorSyncUnlockKey(
            userId = userId,
            authenticatorSyncUnlockKey = "authenticatorSyncUnlockKey",
        )

        authDiskSource.storeOnboardingStatus(
            userId = userId,
            onboardingStatus = OnboardingStatus.AUTOFILL_SETUP,
        )

        authDiskSource.clearData(userId = userId)

        // We do not clear these even when you call clear storage
        assertEquals(pendingAuthRequestJson, authDiskSource.getPendingAuthRequest(userId = userId))
        assertEquals(deviceKey, authDiskSource.getDeviceKey(userId = userId))
        assertEquals(shouldTrustDevice, authDiskSource.getShouldTrustDevice(userId = userId))
        assertEquals(
            OnboardingStatus.AUTOFILL_SETUP,
            authDiskSource.getOnboardingStatus(userId = userId),
        )

        // These should be cleared
        assertNull(authDiskSource.getUserBiometricUnlockKey(userId = userId))
        assertNull(authDiskSource.getPinProtectedUserKey(userId = userId))
        assertNull(authDiskSource.getInvalidUnlockAttempts(userId = userId))
        assertNull(authDiskSource.getUserKey(userId = userId))
        assertNull(authDiskSource.getUserAutoUnlockKey(userId = userId))
        assertNull(authDiskSource.getPrivateKey(userId = userId))
        assertNull(authDiskSource.getOrganizationKeys(userId = userId))
        assertNull(authDiskSource.getOrganizations(userId = userId))
        assertNull(authDiskSource.getPolicies(userId = userId))
        assertNull(authDiskSource.getAccountTokens(userId = userId))
        assertNull(authDiskSource.getEncryptedPin(userId = userId))
        assertNull(authDiskSource.getMasterPasswordHash(userId = userId))
        assertNull(authDiskSource.getShouldUseKeyConnector(userId = userId))
        assertNull(authDiskSource.getIsTdeLoginComplete(userId = userId))
        assertNull(authDiskSource.getAuthenticatorSyncUnlockKey(userId = userId))
        assertNull(authDiskSource.getShowImportLogins(userId = userId))
    }

    @Test
    fun `getInvalidUnlockAttempts should pull from SharedPreferences`() {
        val lastActiveTimeBaseKey = "bwPreferencesStorage:invalidUnlockAttempts"
        val mockUserId = "mockUserId"
        val mockInvalidUnlockAttempts = 4
        fakeSharedPreferences
            .edit {
                putInt(
                    "${lastActiveTimeBaseKey}_$mockUserId",
                    mockInvalidUnlockAttempts,
                )
            }
        val actual = authDiskSource.getInvalidUnlockAttempts(userId = mockUserId)
        assertEquals(
            mockInvalidUnlockAttempts,
            actual,
        )
    }

    @Test
    fun `storeInvalidUnlockAttempts for non-null values should update SharedPreferences`() {
        val invalidUnlockAttemptsBaseKey = "bwPreferencesStorage:invalidUnlockAttempts"
        val mockUserId = "mockUserId"
        val mockInvalidUnlockAttempts = 4
        authDiskSource.storeInvalidUnlockAttempts(
            userId = mockUserId,
            invalidUnlockAttempts = mockInvalidUnlockAttempts,
        )
        val actual = fakeSharedPreferences
            .getInt(
                "${invalidUnlockAttemptsBaseKey}_$mockUserId",
                0,
            )
        assertEquals(
            mockInvalidUnlockAttempts,
            actual,
        )
    }

    @Test
    fun `storeInvalidUnlockAttempts for null values should clear SharedPreferences`() {
        val invalidUnlockAttemptsBaseKey = "bwPreferencesStorage:invalidUnlockAttempts"
        val mockUserId = "mockUserId"
        val mockInvalidUnlockAttempts = 4
        val invalidUnlockAttemptsKey = "${invalidUnlockAttemptsBaseKey}_$mockUserId"
        fakeSharedPreferences
            .edit {
                putInt(invalidUnlockAttemptsKey, mockInvalidUnlockAttempts)
            }
        assertTrue(fakeSharedPreferences.contains(invalidUnlockAttemptsKey))
        authDiskSource.storeInvalidUnlockAttempts(
            userId = mockUserId,
            invalidUnlockAttempts = null,
        )
        assertFalse(fakeSharedPreferences.contains(invalidUnlockAttemptsKey))
    }

    @Test
    fun `getUserKey should pull from SharedPreferences`() {
        val userKeyBaseKey = "bwPreferencesStorage:masterKeyEncryptedUserKey"
        val mockUserId = "mockUserId"
        val mockUserKey = "mockUserKey"
        fakeSharedPreferences
            .edit {
                putString(
                    "${userKeyBaseKey}_$mockUserId",
                    mockUserKey,
                )
            }
        val actual = authDiskSource.getUserKey(userId = mockUserId)
        assertEquals(
            mockUserKey,
            actual,
        )
    }

    @Test
    fun `storeUserKey should update SharedPreferences`() {
        val userKeyBaseKey = "bwPreferencesStorage:masterKeyEncryptedUserKey"
        val mockUserId = "mockUserId"
        val mockUserKey = "mockUserKey"
        authDiskSource.storeUserKey(
            userId = mockUserId,
            userKey = mockUserKey,
        )
        val actual = fakeSharedPreferences
            .getString(
                "${userKeyBaseKey}_$mockUserId",
                null,
            )
        assertEquals(
            mockUserKey,
            actual,
        )
    }

    @Test
    fun `getPrivateKey should pull from SharedPreferences`() {
        val privateKeyBaseKey = "bwPreferencesStorage:encPrivateKey"
        val mockUserId = "mockUserId"
        val mockPrivateKey = "mockPrivateKey"
        fakeSharedPreferences
            .edit {
                putString(
                    "${privateKeyBaseKey}_$mockUserId",
                    mockPrivateKey,
                )
            }
        val actual = authDiskSource.getPrivateKey(userId = mockUserId)
        assertEquals(
            mockPrivateKey,
            actual,
        )
    }

    @Test
    fun `storePrivateKey should update SharedPreferences`() {
        val privateKeyBaseKey = "bwPreferencesStorage:encPrivateKey"
        val mockUserId = "mockUserId"
        val mockPrivateKey = "mockPrivateKey"
        authDiskSource.storePrivateKey(
            userId = mockUserId,
            privateKey = mockPrivateKey,
        )
        val actual = fakeSharedPreferences.getString(
            "${privateKeyBaseKey}_$mockUserId",
            null,
        )
        assertEquals(
            mockPrivateKey,
            actual,
        )
    }

    @Test
    fun `getTwoFactorToken should pull from SharedPreferences`() {
        val twoFactorTokenBaseKey = "bwPreferencesStorage:twoFactorToken"
        val mockEmail = "mockUserId"
        val mockTwoFactorToken = "immaLilToken123"
        fakeSharedPreferences
            .edit {
                putString(
                    "${twoFactorTokenBaseKey}_$mockEmail",
                    mockTwoFactorToken,
                )
            }
        val actual = authDiskSource.getTwoFactorToken(email = mockEmail)
        assertEquals(
            mockTwoFactorToken,
            actual,
        )
    }

    @Test
    fun `storeTwoFactorToken should update SharedPreferences`() {
        val twoFactorTokenBaseKey = "bwPreferencesStorage:twoFactorToken"
        val mockEmail = "mockUserId"
        val mockTwoFactorToken = "immaLilToken123"
        authDiskSource.storeTwoFactorToken(
            email = mockEmail,
            twoFactorToken = mockTwoFactorToken,
        )
        val actual = fakeSharedPreferences.getString(
            "${twoFactorTokenBaseKey}_$mockEmail",
            null,
        )
        assertEquals(
            mockTwoFactorToken,
            actual,
        )
    }

    @Test
    fun `getUserAutoUnlockKey should pull from SharedPreferences`() {
        val userAutoUnlockKeyBaseKey = "bwSecureStorage:userKeyAutoUnlock"
        val mockUserId = "mockUserId"
        val mockUserAutoUnlockKey = "mockUserAutoUnlockKey"
        fakeEncryptedSharedPreferences
            .edit {
                putString(
                    "${userAutoUnlockKeyBaseKey}_$mockUserId",
                    mockUserAutoUnlockKey,
                )
            }
        val actual = authDiskSource.getUserAutoUnlockKey(userId = mockUserId)
        assertEquals(
            mockUserAutoUnlockKey,
            actual,
        )
    }

    @Test
    fun `storeUserAutoUnlockKey should update SharedPreferences`() {
        val userAutoUnlockKeyBaseKey = "bwSecureStorage:userKeyAutoUnlock"
        val mockUserId = "mockUserId"
        val mockUserAutoUnlockKey = "mockUserAutoUnlockKey"
        authDiskSource.storeUserAutoUnlockKey(
            userId = mockUserId,
            userAutoUnlockKey = mockUserAutoUnlockKey,
        )
        val actual = fakeEncryptedSharedPreferences
            .getString(
                "${userAutoUnlockKeyBaseKey}_$mockUserId",
                null,
            )
        assertEquals(
            mockUserAutoUnlockKey,
            actual,
        )
    }

    @Test
    fun `getDeviceKey should pull from SharedPreferences`() {
        val deviceKeyBaseKey = "bwSecureStorage:deviceKey"
        val mockUserId = "mockUserId"
        val deviceKeyKey = "${deviceKeyBaseKey}_$mockUserId"
        val devicesKey = "1234"
        fakeEncryptedSharedPreferences.edit { putString(deviceKeyKey, devicesKey) }
        val actual = authDiskSource.getDeviceKey(userId = mockUserId)
        assertEquals(devicesKey, actual)
    }

    @Test
    fun `storeDeviceKey for non-null values should update SharedPreferences`() {
        val deviceKeyBaseKey = "bwSecureStorage:deviceKey"
        val mockUserId = "mockUserId"
        val deviceKeyKey = "${deviceKeyBaseKey}_$mockUserId"
        val devicesKey = "1234"
        authDiskSource.storeDeviceKey(userId = mockUserId, deviceKey = devicesKey)
        val actual = fakeEncryptedSharedPreferences.getString(
            key = deviceKeyKey,
            defaultValue = null,
        )
        assertEquals(devicesKey, actual)
    }

    @Test
    fun `storeDeviceKey for null values should clear SharedPreferences`() {
        val deviceKeyBaseKey = "bwSecureStorage:deviceKey"
        val mockUserId = "mockUserId"
        val deviceKeyKey = "${deviceKeyBaseKey}_$mockUserId"
        val deviceKey = "1234"
        fakeEncryptedSharedPreferences.edit { putString(deviceKeyKey, deviceKey) }
        authDiskSource.storeDeviceKey(userId = mockUserId, deviceKey = null)
        assertFalse(fakeEncryptedSharedPreferences.contains(deviceKeyKey))
    }

    @Test
    fun `getPendingAuthRequest should pull from SharedPreferences`() {
        val pendingAdminAuthRequestBaseKey = "bwSecureStorage:pendingAdminAuthRequest"
        val mockUserId = "mockUserId"
        val pendingAdminAuthRequestKey = "${pendingAdminAuthRequestBaseKey}_$mockUserId"
        fakeEncryptedSharedPreferences.edit {
            putString(
                pendingAdminAuthRequestKey,
                """
                {
                  "id": "12345",
                  "privateKey": "67890",
                  "fingerprint": "fingerprint",
                  "accessCode": "accessCode"
                }
                """,
            )
        }
        val actual = authDiskSource.getPendingAuthRequest(userId = mockUserId)
        assertEquals(
            PendingAuthRequestJson(
                requestId = "12345",
                requestPrivateKey = "67890",
                requestFingerprint = "fingerprint",
                requestAccessCode = "accessCode",
            ),
            actual,
        )
    }

    @Test
    fun `storePendingAuthRequest for non-null values should update SharedPreferences`() {
        val pendingAdminAuthRequestKeyBaseKey = "bwSecureStorage:pendingAdminAuthRequest"
        val mockUserId = "mockUserId"
        val pendingAuthRequestKey = "${pendingAdminAuthRequestKeyBaseKey}_$mockUserId"
        val pendingAdminAuthRequest = PendingAuthRequestJson(
            requestId = "12345",
            requestPrivateKey = "67890",
            requestFingerprint = "fingerprint",
            requestAccessCode = "accessCode",
        )
        authDiskSource.storePendingAuthRequest(
            userId = mockUserId,
            pendingAuthRequest = pendingAdminAuthRequest,
        )
        val actual = fakeEncryptedSharedPreferences.getString(
            key = pendingAuthRequestKey,
            defaultValue = null,
        )
        assertEquals(
            json.parseToJsonElement(
                """
                {
                  "id": "12345",
                  "privateKey": "67890",
                  "fingerprint": "fingerprint",
                  "accessCode": "accessCode"
                }
                """
                    .trimIndent(),
            ),
            json.parseToJsonElement(requireNotNull(actual)),
        )
    }

    @Test
    fun `getUserBiometricUnlockKey should pull from SharedPreferences`() {
        val biometricsKeyBaseKey = "bwSecureStorage:userKeyBiometricUnlock"
        val mockUserId = "mockUserId"
        val biometricsKeyKey = "${biometricsKeyBaseKey}_$mockUserId"
        val biometricsKey = "1234"
        fakeEncryptedSharedPreferences.edit {
            putString(biometricsKeyKey, biometricsKey)
        }
        val actual = authDiskSource.getUserBiometricUnlockKey(userId = mockUserId)
        assertEquals(biometricsKey, actual)
    }

    @Test
    fun `storeUserBiometricUnlockKey for non-null values should update SharedPreferences`() {
        val biometricsKeyBaseKey = "bwSecureStorage:userKeyBiometricUnlock"
        val mockUserId = "mockUserId"
        val biometricsKeyKey = "${biometricsKeyBaseKey}_$mockUserId"
        val biometricsKey = "1234"
        authDiskSource.storeUserBiometricUnlockKey(
            userId = mockUserId,
            biometricsKey = biometricsKey,
        )
        val actual = fakeEncryptedSharedPreferences.getString(
            key = biometricsKeyKey,
            defaultValue = null,
        )
        assertEquals(biometricsKey, actual)
    }

    @Test
    fun `storeUserBiometricUnlockKey for null values should clear SharedPreferences`() {
        val biometricsKeyBaseKey = "bwSecureStorage:userKeyBiometricUnlock"
        val mockUserId = "mockUserId"
        val biometricsKeyKey = "${biometricsKeyBaseKey}_$mockUserId"
        val biometricsKey = "1234"
        fakeEncryptedSharedPreferences.edit {
            putString(biometricsKeyKey, biometricsKey)
        }
        authDiskSource.storeUserBiometricUnlockKey(
            userId = mockUserId,
            biometricsKey = null,
        )
        assertFalse(fakeEncryptedSharedPreferences.contains(biometricsKeyKey))
    }

    @Suppress("MaxLineLength")
    @Test
    fun `storeUserBiometricUnlockKey should update the resulting flow from getUserBiometicUnlockKeyFlow`() =
        runTest {
            val topSecretKey = "topsecret"
            val mockUserId = "mockUserId"
            authDiskSource.getUserBiometicUnlockKeyFlow(mockUserId).test {
                assertNull(awaitItem())
                authDiskSource.storeUserBiometricUnlockKey(
                    userId = mockUserId,
                    biometricsKey = topSecretKey,
                )
                assertEquals(topSecretKey, awaitItem())
            }
        }

    @Test
    fun `getPinProtectedUserKey should pull from SharedPreferences`() {
        val pinProtectedUserKeyBaseKey = "bwPreferencesStorage:pinKeyEncryptedUserKey"
        val mockUserId = "mockUserId"
        val mockPinProtectedUserKey = "mockPinProtectedUserKey"
        fakeSharedPreferences
            .edit {
                putString(
                    "${pinProtectedUserKeyBaseKey}_$mockUserId",
                    mockPinProtectedUserKey,
                )
            }
        val actual = authDiskSource.getPinProtectedUserKey(userId = mockUserId)
        assertEquals(
            mockPinProtectedUserKey,
            actual,
        )
    }

    @Test
    fun `storePinProtectedUserKey should update SharedPreferences`() {
        val pinProtectedUserKeyBaseKey = "bwPreferencesStorage:pinKeyEncryptedUserKey"
        val mockUserId = "mockUserId"
        val mockPinProtectedUserKey = "mockPinProtectedUserKey"
        authDiskSource.storePinProtectedUserKey(
            userId = mockUserId,
            pinProtectedUserKey = mockPinProtectedUserKey,
        )
        val actual = fakeSharedPreferences
            .getString(
                "${pinProtectedUserKeyBaseKey}_$mockUserId",
                null,
            )
        assertEquals(
            mockPinProtectedUserKey,
            actual,
        )
    }

    @Test
    fun `storePinProtectedUserKey should update result flow from getPinProtectedUserKeyFlow`() =
        runTest {
            val topSecretKey = "topsecret"
            val mockUserId = "mockUserId"
            authDiskSource.getPinProtectedUserKeyFlow(mockUserId).test {
                assertNull(awaitItem())
                authDiskSource.storePinProtectedUserKey(
                    userId = mockUserId,
                    pinProtectedUserKey = topSecretKey,
                )
                assertEquals(topSecretKey, awaitItem())
            }
        }

    @Test
    fun `getEncryptedPin should pull from SharedPreferences`() {
        val encryptedPinBaseKey = "bwPreferencesStorage:protectedPin"
        val mockUserId = "mockUserId"
        val mockEncryptedPin = "mockEncryptedPin"
        fakeSharedPreferences
            .edit {
                putString(
                    "${encryptedPinBaseKey}_$mockUserId",
                    mockEncryptedPin,
                )
            }
        val actual = authDiskSource.getEncryptedPin(userId = mockUserId)
        assertEquals(
            mockEncryptedPin,
            actual,
        )
    }

    @Test
    fun `storeEncryptedPin should update SharedPreferences`() {
        val encryptedPinBaseKey = "bwPreferencesStorage:protectedPin"
        val mockUserId = "mockUserId"
        val mockEncryptedPin = "mockUserAutoUnlockKey"
        authDiskSource.storeEncryptedPin(
            userId = mockUserId,
            encryptedPin = mockEncryptedPin,
        )
        val actual = fakeSharedPreferences
            .getString(
                "${encryptedPinBaseKey}_$mockUserId",
                null,
            )
        assertEquals(
            mockEncryptedPin,
            actual,
        )
    }

    @Test
    fun `getOrganizationKeys should pull from SharedPreferences`() {
        val organizationKeysBaseKey = "bwPreferencesStorage:encOrgKeys"
        val mockUserId = "mockUserId"
        val mockOrganizationKeys = mapOf(
            "organizationId1" to "organizationKey1",
            "organizationId2" to "organizationKey2",
        )
        fakeSharedPreferences
            .edit {
                putString(
                    "${organizationKeysBaseKey}_$mockUserId",
                    """
                    {
                      "organizationId1": "organizationKey1",
                      "organizationId2": "organizationKey2"
                    }
                    """
                        .trimIndent(),
                )
            }
        val actual = authDiskSource.getOrganizationKeys(userId = mockUserId)
        assertEquals(
            mockOrganizationKeys,
            actual,
        )
    }

    @Test
    fun `putOrganizationKeys should update SharedPreferences`() {
        val organizationKeysBaseKey = "bwPreferencesStorage:encOrgKeys"
        val mockUserId = "mockUserId"
        val mockOrganizationKeys = mapOf(
            "organizationId1" to "organizationKey1",
            "organizationId2" to "organizationKey2",
        )
        authDiskSource.storeOrganizationKeys(
            userId = mockUserId,
            organizationKeys = mockOrganizationKeys,
        )
        val actual = fakeSharedPreferences.getString(
            "${organizationKeysBaseKey}_$mockUserId",
            null,
        )
        assertEquals(
            json.parseToJsonElement(
                """
                {
                  "organizationId1": "organizationKey1",
                  "organizationId2": "organizationKey2"
                }
                """
                    .trimIndent(),
            ),
            json.parseToJsonElement(requireNotNull(actual)),
        )
    }

    @Test
    fun `getOrganizations should pull from SharedPreferences`() {
        val organizationsBaseKey = "bwPreferencesStorage:organizations"
        val mockUserId = "mockUserId"
        val mockOrganizations = listOf(
            createMockOrganization(0),
            createMockOrganization(1),
        )
        val mockOrganizationsMap = mockOrganizations.associateBy { it.id }
        fakeSharedPreferences
            .edit {
                putString(
                    "${organizationsBaseKey}_$mockUserId",
                    json.encodeToString(mockOrganizationsMap),
                )
            }
        val actual = authDiskSource.getOrganizations(userId = mockUserId)
        assertEquals(
            mockOrganizations,
            actual,
        )
    }

    @Test
    fun `getOrganizationsFlow should react to changes in getOrganizations`() = runTest {
        val mockUserId = "mockUserId"
        val mockOrganizations = listOf(
            createMockOrganization(0),
            createMockOrganization(1),
        )
        authDiskSource.getOrganizationsFlow(userId = mockUserId).test {
            // The initial values of the Flow and the property are in sync
            assertNull(authDiskSource.getOrganizations(userId = mockUserId))
            assertNull(awaitItem())

            // Updating the repository updates shared preferences
            authDiskSource.storeOrganizations(
                userId = mockUserId,
                organizations = mockOrganizations,
            )
            assertEquals(mockOrganizations, awaitItem())
        }
    }

    @Test
    fun `storeOrganizations should update SharedPreferences`() {
        val organizationsBaseKey = "bwPreferencesStorage:organizations"
        val mockUserId = "mockUserId"
        val mockOrganizations = listOf(
            createMockOrganization(0),
            createMockOrganization(1),
        )
        val mockOrganizationsMap = mockOrganizations.associateBy { it.id }
        authDiskSource.storeOrganizations(
            userId = mockUserId,
            organizations = mockOrganizations,
        )
        val actual = fakeSharedPreferences.getString(
            "${organizationsBaseKey}_$mockUserId",
            null,
        )
        assertEquals(
            json.encodeToJsonElement(mockOrganizationsMap),
            json.parseToJsonElement(requireNotNull(actual)),
        )
    }

    @Test
    fun `getMasterPasswordHash should pull from SharedPreferences`() {
        val passwordHashBaseKey = "bwPreferencesStorage:keyHash"
        val mockUserId = "mockUserId"
        val mockPasswordHash = "mockPasswordHash"
        fakeSharedPreferences
            .edit {
                putString(
                    "${passwordHashBaseKey}_$mockUserId",
                    mockPasswordHash,
                )
            }
        val actual = authDiskSource.getMasterPasswordHash(userId = mockUserId)
        assertEquals(
            mockPasswordHash,
            actual,
        )
    }

    @Test
    fun `storeMasterPasswordHash should update SharedPreferences`() {
        val passwordHashBaseKey = "bwPreferencesStorage:keyHash"
        val mockUserId = "mockUserId"
        val mockPasswordHash = "mockPasswordHash"
        authDiskSource.storeMasterPasswordHash(
            userId = mockUserId,
            passwordHash = mockPasswordHash,
        )
        val actual = fakeSharedPreferences
            .getString(
                "${passwordHashBaseKey}_$mockUserId",
                null,
            )
        assertEquals(
            mockPasswordHash,
            actual,
        )
    }

    @Test
    fun `getPolicies should pull from SharedPreferences`() {
        val policiesBaseKey = "bwPreferencesStorage:policies"
        val mockUserId = "mockUserId"
        val mockPolicies = listOf(
            createMockPolicy(number = 0),
            createMockPolicy(number = 1),
        )
        val mockPoliciesMap = mockPolicies.associateBy { it.id }
        fakeSharedPreferences
            .edit {
                putString(
                    "${policiesBaseKey}_$mockUserId",
                    json.encodeToString(mockPoliciesMap),
                )
            }
        val actual = authDiskSource.getPolicies(userId = mockUserId)
        assertEquals(
            mockPolicies,
            actual,
        )
    }

    @Test
    fun `getPoliciesFlow should react to changes in getOrganizations`() = runTest {
        val mockUserId = "mockUserId"
        val mockPolicies = listOf(
            createMockPolicy(number = 0),
            createMockPolicy(number = 1),
        )
        authDiskSource.getPoliciesFlow(userId = mockUserId).test {
            // The initial values of the Flow and the property are in sync
            assertNull(authDiskSource.getPolicies(userId = mockUserId))
            assertNull(awaitItem())

            // Updating the repository updates shared preferences
            authDiskSource.storePolicies(
                userId = mockUserId,
                policies = mockPolicies,
            )
            assertEquals(mockPolicies, awaitItem())
        }
    }

    @Test
    fun `storePolicies should update SharedPreferences`() {
        val policiesBaseKey = "bwPreferencesStorage:policies"
        val mockUserId = "mockUserId"
        val mockPolicies = listOf(
            createMockPolicy(number = 0),
            createMockPolicy(number = 1),
        )
        val mockPoliciesMap = mockPolicies.associateBy { it.id }
        authDiskSource.storePolicies(
            userId = mockUserId,
            policies = mockPolicies,
        )
        val actual = fakeSharedPreferences.getString(
            "${policiesBaseKey}_$mockUserId",
            null,
        )
        assertEquals(
            json.encodeToJsonElement(mockPoliciesMap),
            json.parseToJsonElement(requireNotNull(actual)),
        )
    }

    @Test
    fun `getAccountTokens should pull from SharedPreferences`() {
        val baseKey = "bwSecureStorage:accountTokens"
        val mockUserId = "mockUserId"
        val accountTokens = AccountTokensJson(
            accessToken = "accessToken",
            refreshToken = "refreshToken",
        )
        fakeEncryptedSharedPreferences.edit {
            putString("${baseKey}_$mockUserId", json.encodeToString(accountTokens))
        }
        val actual = authDiskSource.getAccountTokens(userId = mockUserId)
        assertEquals(accountTokens, actual)
    }

    @Test
    fun `getAccountTokensFlow should react to changes from storeAccountTokens`() = runTest {
        val mockUserId = "mockUserId"
        val accountTokens = AccountTokensJson(
            accessToken = "accessToken",
            refreshToken = "refreshToken",
        )
        authDiskSource.getAccountTokensFlow(userId = mockUserId).test {
            // The initial values of the Flow and the property are in sync
            assertNull(authDiskSource.getAccountTokens(userId = mockUserId))
            assertNull(awaitItem())

            // Updating the repository updates shared preferences
            authDiskSource.storeAccountTokens(
                userId = mockUserId,
                accountTokens = accountTokens,
            )
            assertEquals(accountTokens, awaitItem())

            // clear the repository clears shared preferences
            authDiskSource.storeAccountTokens(
                userId = mockUserId,
                accountTokens = null,
            )
            assertNull(awaitItem())
        }
    }

    @Test
    fun `storeAccountTokens should update SharedPreferences`() {
        val baseKey = "bwSecureStorage:accountTokens"
        val mockUserId = "mockUserId"
        val accountTokens = AccountTokensJson(
            accessToken = "accessToken",
            refreshToken = "refreshToken",
        )
        authDiskSource.storeAccountTokens(
            userId = mockUserId,
            accountTokens = accountTokens,
        )
        val actual = fakeEncryptedSharedPreferences.getString(
            key = "${baseKey}_$mockUserId",
            defaultValue = null,
        )
        assertEquals(
            json.encodeToJsonElement(accountTokens),
            json.parseToJsonElement(requireNotNull(actual)),
        )
    }

    @Test
    fun `getAuthenticatorSyncUnlockKey should pull from SharedPreferences`() {
        val authenticatorSyncUnlockKey = "bwSecureStorage:authenticatorSyncUnlock"
        val mockUserId = "mockUserId"
        val mockAuthenticatorSyncUnlockKey = "mockAuthSyncUnlockKey"
        fakeEncryptedSharedPreferences
            .edit {
                putString(
                    "${authenticatorSyncUnlockKey}_$mockUserId",
                    mockAuthenticatorSyncUnlockKey,
                )
            }
        val actual = authDiskSource.getAuthenticatorSyncUnlockKey(userId = mockUserId)
        assertEquals(
            mockAuthenticatorSyncUnlockKey,
            actual,
        )
    }

    @Test
    fun `storeAuthenticatorSyncUnlockKey should update SharedPreferences`() {
        val authenticatorSyncUnlockKey = "bwSecureStorage:authenticatorSyncUnlock"
        val mockUserId = "mockUserId"
        val mockAuthenticatorSyncUnlockKey = "mockAuthSyncUnlockKey"
        authDiskSource.storeAuthenticatorSyncUnlockKey(
            userId = mockUserId,
            authenticatorSyncUnlockKey = mockAuthenticatorSyncUnlockKey,
        )

        val actual = fakeEncryptedSharedPreferences.getString(
            key = "${authenticatorSyncUnlockKey}_$mockUserId",
            defaultValue = null,
        )
        assertEquals(
            mockAuthenticatorSyncUnlockKey,
            actual,
        )
    }

    @Test
    fun `getOnboardingStatus should pull from SharedPreferences`() {
        val onboardingStatusBaseKey = "bwPreferencesStorage:onboardingStatus"
        val mockUserId = "mockUserId"
        val expectedStatus = OnboardingStatus.AUTOFILL_SETUP
        fakeSharedPreferences.edit {
            putString(
                "${onboardingStatusBaseKey}_$mockUserId",
                json.encodeToString(expectedStatus),
            )
        }
        val actual = authDiskSource.getOnboardingStatus(userId = mockUserId)
        assertEquals(
            expectedStatus,
            actual,
        )
    }

    @Test
    fun `storeOnboardingStatus should update SharedPreferences`() {
        val onboardingStatusBaseKey = "bwPreferencesStorage:onboardingStatus"
        val mockUserId = "mockUserId"
        val mockOnboardingStatus = OnboardingStatus.AUTOFILL_SETUP
        authDiskSource.storeOnboardingStatus(mockUserId, mockOnboardingStatus)

        val actual = fakeSharedPreferences.getString(
            "${onboardingStatusBaseKey}_$mockUserId",
            null,
        )
        assertEquals(
            json.encodeToString(mockOnboardingStatus),
            actual,
        )
    }

    @Test
    fun `getOnboardingStatusFlow should react to changes from storeOnboardingStatus`() = runTest {
        val userId = "userId"
        authDiskSource.getOnboardingStatusFlow(userId).test {
            // The initial values of the Flow and the property are in sync
            assertNull(awaitItem())

            // Updating the repository updates shared preferences
            authDiskSource.storeOnboardingStatus(userId, OnboardingStatus.AUTOFILL_SETUP)
            assertEquals(OnboardingStatus.AUTOFILL_SETUP, awaitItem())
        }
    }

    @Test
    fun `authenticatorSyncSymmetricKey should store and update from EncryptedSharedPreferences`() {
        val sharedPrefsKey = "bwSecureStorage:authenticatorSyncSymmetric"

        // Shared preferences and the repository start with the same value:
        assertNull(authDiskSource.authenticatorSyncSymmetricKey)
        assertNull(fakeEncryptedSharedPreferences.getString(sharedPrefsKey, null))

        // Updating the repository updates shared preferences:
        val symmetricKey = generateSecretKey().getOrThrow().encoded
        authDiskSource.authenticatorSyncSymmetricKey = symmetricKey
        assertEquals(
            symmetricKey.toString(Charsets.ISO_8859_1),
            fakeEncryptedSharedPreferences.getString(sharedPrefsKey, null),
        )

        // Retrieving the key from repository should give same byte array despite String conversion:
        assertTrue(authDiskSource.authenticatorSyncSymmetricKey.contentEquals(symmetricKey))
    }

    @Test
    fun `getShowImportLogins should pull from SharedPreferences`() {
        val showImportLoginsBaseKey = "bwPreferencesStorage:showImportLogins"
        val mockUserId = "mockUserId"
        fakeSharedPreferences.edit {
            putBoolean("${showImportLoginsBaseKey}_$mockUserId", true)
        }
        val actual = authDiskSource.getShowImportLogins(userId = mockUserId) ?: false
        assertTrue(actual)
    }

    @Test
    fun `storeShowImportLogins should update SharedPreferences`() {
        val showImportLoginsBaseKey = "bwPreferencesStorage:showImportLogins"
        val mockUserId = "mockUserId"
        authDiskSource.storeShowImportLogins(
            userId = mockUserId,
            showImportLogins = true,
        )
        val actual = fakeSharedPreferences.getBoolean(
            "${showImportLoginsBaseKey}_$mockUserId",
            false,
        )
        assertTrue(actual)
    }

    @Test
    fun `getShowImportLoginsFlow should react to changes from storeShowImportLogins`() = runTest {
        val mockUserId = "mockUserId"
        authDiskSource.getShowImportLoginsFlow(userId = mockUserId).test {
            // The initial values of the Flow and the property are in sync
            assertNull(awaitItem())
            authDiskSource.storeShowImportLogins(userId = mockUserId, true)
            assertTrue(awaitItem() ?: false)
        }
    }

    @Test
    fun `getNewDeviceNoticeState should pull from SharedPreferences`() {
        val storeKey = "bwPreferencesStorage:newDeviceNoticeState"
        val mockUserId = "mockUserId"
        val expectedState = NewDeviceNoticeState(
            displayStatus = NewDeviceNoticeDisplayStatus.HAS_SEEN,
            lastSeenDate = ZonedDateTime.parse("2024-12-25T01:00:00.00Z"),
        )
        fakeSharedPreferences.edit {
            putString(
                "${storeKey}_$mockUserId",
                json.encodeToString(expectedState),
            )
        }
        val actual = authDiskSource.getNewDeviceNoticeState(userId = mockUserId)
        assertEquals(
            expectedState,
            actual,
        )
    }

    @Test
    fun `getNewDeviceNoticeState should pull default from SharedPreferences if no user is found`() {
        val mockUserId = "mockUserId"
        val defaultState = NewDeviceNoticeState(
            displayStatus = NewDeviceNoticeDisplayStatus.HAS_NOT_SEEN,
            lastSeenDate = null,
        )
        val actual = authDiskSource.getNewDeviceNoticeState(userId = mockUserId)
        assertEquals(
            defaultState,
            actual,
        )
    }

    @Test
    fun `setNewDeviceNoticeState should update SharedPreferences`() {
        val storeKey = "bwPreferencesStorage:newDeviceNoticeState"
        val mockUserId = "mockUserId"
        val mockStatus = NewDeviceNoticeState(
            displayStatus = NewDeviceNoticeDisplayStatus.HAS_SEEN,
            lastSeenDate = ZonedDateTime.parse("2024-12-25T01:00:00.00Z"),
        )
        authDiskSource.storeNewDeviceNoticeState(
            userId = mockUserId,
            mockStatus,
        )

        val actual = fakeSharedPreferences.getString(
            "${storeKey}_$mockUserId",
            null,
        )
        assertEquals(
            json.encodeToString(mockStatus),
            actual,
        )
    }
}

private const val USER_STATE_JSON = """
    {
      "activeUserId": "activeUserId",
      "accounts": {
        "activeUserId": {
          "profile": {
            "userId": "activeUserId",
            "email": "email",
            "emailVerified": true,
            "isTwoFactorEnabled": false,
            "name": "name",
            "stamp": "stamp",
            "orgIdentifier": "organizationId",
            "avatarColor": "avatarColorHex",
            "hasPremiumPersonally": true,
            "forcePasswordResetReason": "adminForcePasswordReset",
            "kdfType": 1,
            "kdfIterations": 600000,
            "kdfMemory": 16,
            "kdfParallelism": 4,
            "userDecryptionOptions": {
              "hasMasterPassword": true,
              "trustedDeviceOption": {
                "encryptedPrivateKey": "encryptedPrivateKey",
                "encryptedUserKey": "encryptedUserKey",
                "hasAdminApproval": true,
                "hasLoginApprovingDevice": true,
                "hasManageResetPasswordPermission": true
              },
              "keyConnectorOption": {
                "keyConnectorUrl": "keyConnectorUrl"
              }
            },
            "creationDate": "2024-09-13T01:00:00.000Z"
          },
          "tokens": {
            "accessToken": "accessToken",
            "refreshToken": "refreshToken"
          },
          "settings": {
            "environmentUrls": {
              "base": "base",
              "api": "api",
              "identity": "identity",
              "icons": "icon",
              "notifications": "notifications",
              "webVault": "webVault",
              "events": "events"
            }
          }
        }
      }
    }
"""

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
                isTwoFactorEnabled = false,
                creationDate = ZonedDateTime.parse("2024-09-13T01:00:00.00Z"),
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
            tokens = AccountTokensJson(
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
