package com.x8bit.bitwarden.data.platform.datasource.disk

import androidx.core.content.edit
import com.bitwarden.core.util.getBinaryLongFromZoneDateTime
import com.bitwarden.core.util.getZoneDateTimeFromBinaryLong
import com.bitwarden.data.datasource.disk.base.FakeSharedPreferences
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class PushDiskSourceTest {
    private val fakeSharedPreferences = FakeSharedPreferences()

    private val pushDiskSource = PushDiskSourceImpl(
        sharedPreferences = fakeSharedPreferences,
    )

    @Test
    fun `registeredPushToken should pull from and update SharedPreferences`() {
        val registeredPushTokenKey = "bwPreferencesStorage:pushRegisteredToken"

        // Shared preferences and the repository start with the same value.
        assertNull(pushDiskSource.registeredPushToken)
        assertNull(fakeSharedPreferences.getString(registeredPushTokenKey, null))

        // Updating the repository updates shared preferences
        pushDiskSource.registeredPushToken = "abcd"
        assertEquals(
            "abcd",
            fakeSharedPreferences.getString(registeredPushTokenKey, null),
        )

        // Update SharedPreferences updates the repository
        fakeSharedPreferences.edit { putString(registeredPushTokenKey, null) }
        assertNull(pushDiskSource.registeredPushToken)
    }

    @Test
    fun `clearData should clear all necessary data for the given user`() {
        val userId = "userId"
        pushDiskSource.storeCurrentPushToken(
            userId = userId,
            pushToken = "pushToken",
        )
        pushDiskSource.storeLastPushTokenRegistrationDate(
            userId = userId,
            registrationDate = ZonedDateTime.parse("2023-10-27T12:00:00Z"),
        )

        pushDiskSource.clearData(userId = userId)

        assertNull(pushDiskSource.getCurrentPushToken(userId = userId))
        assertNull(pushDiskSource.getLastPushTokenRegistrationDate(userId = userId))
    }

    @Test
    fun `getCurrentPushToken should pull from SharedPreferences`() {
        val currentPushTokenBaseKey = "bwPreferencesStorage:pushCurrentToken"
        val mockUserId = "mockUserId"
        val mockCurrentPushToken = "abcd"
        fakeSharedPreferences
            .edit {
                putString(
                    "${currentPushTokenBaseKey}_$mockUserId",
                    mockCurrentPushToken,
                )
            }
        val actual = pushDiskSource.getCurrentPushToken(userId = mockUserId)
        assertEquals(
            mockCurrentPushToken,
            actual,
        )
    }

    @Test
    fun `storeCurrentPushToken should update SharedPreferences`() {
        val currentPushTokenBaseKey = "bwPreferencesStorage:pushCurrentToken"
        val mockUserId = "mockUserId"
        val mockCurrentPushToken = "abcd"
        pushDiskSource.storeCurrentPushToken(
            userId = mockUserId,
            pushToken = mockCurrentPushToken,
        )
        val actual = fakeSharedPreferences
            .getString(
                "${currentPushTokenBaseKey}_$mockUserId",
                null,
            )
        assertEquals(
            mockCurrentPushToken,
            actual,
        )
    }

    @Test
    fun `getLastPushTokenRegistrationDate should pull from SharedPreferences`() {
        val lastPushTokenBaseKey = "bwPreferencesStorage:pushLastRegistrationDate"
        val mockUserId = "mockUserId"
        val mockLastPushTokenRegistration = ZonedDateTime.parse("2024-01-06T22:27:45.904314Z")
        fakeSharedPreferences
            .edit {
                putLong(
                    "${lastPushTokenBaseKey}_$mockUserId",
                    getBinaryLongFromZoneDateTime(mockLastPushTokenRegistration),
                )
            }
        val actual = pushDiskSource.getLastPushTokenRegistrationDate(userId = mockUserId)!!
        assertEquals(
            mockLastPushTokenRegistration,
            actual,
        )
    }

    @Test
    fun `storeLastPushTokenRegistrationDate for non-null values should update SharedPreferences`() {
        val lastPushTokenBaseKey = "bwPreferencesStorage:pushLastRegistrationDate"
        val mockUserId = "mockUserId"
        val mockLastPushTokenRegistration = ZonedDateTime.parse("2024-01-06T22:27:45.904314Z")
        pushDiskSource.storeLastPushTokenRegistrationDate(
            userId = mockUserId,
            registrationDate = mockLastPushTokenRegistration,
        )
        val actual = fakeSharedPreferences
            .getLong(
                "${lastPushTokenBaseKey}_$mockUserId",
                0,
            )
        assertEquals(
            mockLastPushTokenRegistration,
            getZoneDateTimeFromBinaryLong(actual),
        )
    }

    @Test
    fun `storeLastPushTokenRegistrationDate for null values should clear SharedPreferences`() {
        val lastPushTokenBaseKey = "bwPreferencesStorage:pushLastRegistrationDate"
        val mockUserId = "mockUserId"
        val mockLastPushTokenRegistration = ZonedDateTime.parse("2023-10-27T12:00:00Z")
        val lastPushTokenKey = "${lastPushTokenBaseKey}_$mockUserId"
        fakeSharedPreferences.edit {
            putLong(lastPushTokenKey, mockLastPushTokenRegistration.toEpochSecond())
        }
        assertTrue(fakeSharedPreferences.contains(lastPushTokenKey))
        pushDiskSource.storeLastPushTokenRegistrationDate(
            userId = mockUserId,
            registrationDate = null,
        )
        assertFalse(fakeSharedPreferences.contains(lastPushTokenKey))
    }
}
