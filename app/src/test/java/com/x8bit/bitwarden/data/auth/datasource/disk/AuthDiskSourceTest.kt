package com.x8bit.bitwarden.data.auth.datasource.disk

import app.cash.turbine.test
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.EnvironmentUrlDataJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.ForcePasswordResetReason
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.KdfTypeJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.KeyConnectorUserDecryptionOptionsJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.TrustedDeviceUserDecryptionOptionsJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.UserDecryptionOptionsJson
import com.x8bit.bitwarden.data.platform.base.FakeSharedPreferences
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AuthDiskSourceTest {
    private val fakeSharedPreferences = FakeSharedPreferences()

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val authDiskSource = AuthDiskSourceImpl(
        sharedPreferences = fakeSharedPreferences,
        json = json,
    )

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
        fakeSharedPreferences.edit().putString(rememberedUniqueAppIdKey, testId).apply()

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
        fakeSharedPreferences.edit().putString(rememberedEmailKey, null).apply()
        assertNull(authDiskSource.rememberedEmailAddress)
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
        fakeSharedPreferences.edit().putString(userStateKey, null).apply()
        assertNull(authDiskSource.userState)
    }

    @Test
    fun `userStateFlow should react to changes in userState`() = runTest {
        authDiskSource.userStateFlow.test {
            // The initial values of the Flow and the property are in sync
            assertNull(authDiskSource.userState)
            assertNull(awaitItem())

            // Updating the repository updates shared preferences
            authDiskSource.userState = USER_STATE
            assertEquals(USER_STATE, awaitItem())
        }
    }

    @Test
    fun `getUserKey should pull from SharedPreferences`() {
        val mockUserId = "mockUserId"
        val mockUserKey = "mockUserKey"
        fakeSharedPreferences
            .edit()
            .putString(
                "masterKeyEncryptedUserKey_$mockUserId",
                mockUserKey,
            )
            .apply()
        val actual = authDiskSource.getUserKey(userId = mockUserId)
        assertEquals(
            mockUserKey,
            actual,
        )
    }

    @Test
    fun `storeUserKey should update SharedPreferences`() {
        val mockUserId = "mockUserId"
        val mockUserKey = "mockUserKey"
        authDiskSource.storeUserKey(
            userId = mockUserId,
            userKey = mockUserKey,
        )
        val actual = fakeSharedPreferences
            .getString(
                "masterKeyEncryptedUserKey_$mockUserId",
                null,
            )
        assertEquals(
            mockUserKey,
            actual,
        )
    }

    @Test
    fun `getPrivateKey should pull from SharedPreferences`() {
        val mockUserId = "mockUserId"
        val mockPrivateKey = "mockPrivateKey"
        fakeSharedPreferences
            .edit()
            .putString(
                "encPrivateKey_$mockUserId",
                mockPrivateKey,
            )
            .apply()
        val actual = authDiskSource.getPrivateKey(userId = mockUserId)
        assertEquals(
            mockPrivateKey,
            actual,
        )
    }

    @Test
    fun `storePrivateKey should update SharedPreferences`() {
        val mockUserId = "mockUserId"
        val mockPrivateKey = "mockPrivateKey"
        authDiskSource.storePrivateKey(
            userId = mockUserId,
            privateKey = mockPrivateKey,
        )
        val actual = fakeSharedPreferences.getString(
            "encPrivateKey_$mockUserId",
            null,
        )
        assertEquals(
            mockPrivateKey,
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
            "accountDecryptionOptions": {
              "HasMasterPassword": true,
              "TrustedDeviceOption": {
                "EncryptedPrivateKey": "encryptedPrivateKey",
                "EncryptedUserKey": "encryptedUserKey",
                "HasAdminApproval": true,
                "HasLoginApprovingDevice": true,
                "HasManageResetPasswordPermission": true
              },
              "KeyConnectorOption": {
                "KeyConnectorUrl": "keyConnectorUrl"
              }
            }
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
              "icon": "icon",
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
