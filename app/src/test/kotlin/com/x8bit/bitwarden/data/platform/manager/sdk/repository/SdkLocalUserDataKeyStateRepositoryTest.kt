package com.x8bit.bitwarden.data.platform.manager.sdk.repository

import com.bitwarden.core.LocalUserDataKeyState
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SdkLocalUserDataKeyStateRepositoryTest {

    private val fakeAuthDiskSource = FakeAuthDiskSource()

    private val repository = SdkLocalUserDataKeyStateRepository(
        authDiskSource = fakeAuthDiskSource,
    )

    @Test
    fun `get should return null when no key is stored for the given id`() = runTest {
        assertNull(repository.get(id = USER_ID))
    }

    @Test
    fun `get should return LocalUserDataKeyState when key is stored for the given id`() = runTest {
        fakeAuthDiskSource.storeLocalUserDataKey(userId = USER_ID, wrappedKey = WRAPPED_KEY)

        assertEquals(
            LocalUserDataKeyState(wrappedKey = WRAPPED_KEY),
            repository.get(id = USER_ID),
        )
    }

    @Test
    fun `has should return false when no key is stored for the given id`() = runTest {
        assertFalse(repository.has(id = USER_ID))
    }

    @Test
    fun `has should return true when a key is stored for the given id`() = runTest {
        fakeAuthDiskSource.storeLocalUserDataKey(userId = USER_ID, wrappedKey = WRAPPED_KEY)

        assertTrue(repository.has(id = USER_ID))
    }

    @Test
    fun `list should return empty list when userState is null`() = runTest {
        fakeAuthDiskSource.userState = null

        assertEquals(emptyList<LocalUserDataKeyState>(), repository.list())
    }

    @Test
    fun `list should return empty list when no keys are stored for any account`() = runTest {
        fakeAuthDiskSource.userState = UserStateJson(
            activeUserId = USER_ID,
            accounts = mapOf(USER_ID to mockk()),
        )

        assertEquals(emptyList<LocalUserDataKeyState>(), repository.list())
    }

    @Test
    fun `list should return LocalUserDataKeyState for each account that has a stored key`() =
        runTest {
            fakeAuthDiskSource.userState = UserStateJson(
                activeUserId = USER_ID,
                accounts = mapOf(
                    USER_ID to mockk(),
                    USER_ID_2 to mockk(),
                ),
            )
            fakeAuthDiskSource.storeLocalUserDataKey(userId = USER_ID, wrappedKey = WRAPPED_KEY)
            fakeAuthDiskSource.storeLocalUserDataKey(userId = USER_ID_2, wrappedKey = WRAPPED_KEY_2)

            assertEquals(
                listOf(
                    LocalUserDataKeyState(wrappedKey = WRAPPED_KEY),
                    LocalUserDataKeyState(wrappedKey = WRAPPED_KEY_2),
                ),
                repository.list(),
            )
        }

    @Test
    fun `list should omit accounts that have no stored key`() = runTest {
        fakeAuthDiskSource.userState = UserStateJson(
            activeUserId = USER_ID,
            accounts = mapOf(USER_ID to mockk(), USER_ID_2 to mockk()),
        )
        fakeAuthDiskSource.storeLocalUserDataKey(userId = USER_ID, wrappedKey = WRAPPED_KEY)

        assertEquals(
            listOf(LocalUserDataKeyState(wrappedKey = WRAPPED_KEY)),
            repository.list(),
        )
    }

    @Test
    fun `remove should clear the stored key for the given id`() = runTest {
        fakeAuthDiskSource.storeLocalUserDataKey(userId = USER_ID, wrappedKey = WRAPPED_KEY)

        repository.remove(id = USER_ID)

        assertNull(fakeAuthDiskSource.getLocalUserDataKey(userId = USER_ID))
    }

    @Test
    fun `removeAll should clear the stored key for all accounts`() = runTest {
        fakeAuthDiskSource.userState = UserStateJson(
            activeUserId = USER_ID,
            accounts = mapOf(
                USER_ID to mockk(),
                USER_ID_2 to mockk(),
            ),
        )
        fakeAuthDiskSource.storeLocalUserDataKey(userId = USER_ID, wrappedKey = WRAPPED_KEY)
        fakeAuthDiskSource.storeLocalUserDataKey(userId = USER_ID_2, wrappedKey = WRAPPED_KEY_2)

        repository.removeAll()

        assertNull(fakeAuthDiskSource.getLocalUserDataKey(userId = USER_ID))
        assertNull(fakeAuthDiskSource.getLocalUserDataKey(userId = USER_ID_2))
    }

    @Test
    fun `removeAll should do nothing when userState is null`() = runTest {
        fakeAuthDiskSource.userState = null

        repository.removeAll()
    }

    @Test
    fun `removeBulk should clear the stored key for each given id`() = runTest {
        fakeAuthDiskSource.storeLocalUserDataKey(userId = USER_ID, wrappedKey = WRAPPED_KEY)
        fakeAuthDiskSource.storeLocalUserDataKey(userId = USER_ID_2, wrappedKey = WRAPPED_KEY_2)

        repository.removeBulk(keys = listOf(USER_ID, USER_ID_2))

        assertNull(fakeAuthDiskSource.getLocalUserDataKey(userId = USER_ID))
        assertNull(fakeAuthDiskSource.getLocalUserDataKey(userId = USER_ID_2))
    }

    @Test
    fun `set should store the wrapped key for the given id`() = runTest {
        repository.set(id = USER_ID, value = LocalUserDataKeyState(wrappedKey = WRAPPED_KEY))

        assertEquals(WRAPPED_KEY, fakeAuthDiskSource.getLocalUserDataKey(userId = USER_ID))
    }

    @Test
    fun `setBulk should store the wrapped key for each given id`() = runTest {
        repository.setBulk(
            values = mapOf(
                USER_ID to LocalUserDataKeyState(wrappedKey = WRAPPED_KEY),
                USER_ID_2 to LocalUserDataKeyState(wrappedKey = WRAPPED_KEY_2),
            ),
        )

        assertEquals(WRAPPED_KEY, fakeAuthDiskSource.getLocalUserDataKey(userId = USER_ID))
        assertEquals(WRAPPED_KEY_2, fakeAuthDiskSource.getLocalUserDataKey(userId = USER_ID_2))
    }
}

private const val USER_ID: String = "userId"
private const val USER_ID_2: String = "userId2"
private const val WRAPPED_KEY: String = "wrappedKey"
private const val WRAPPED_KEY_2: String = "wrappedKey2"
