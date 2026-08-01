package com.x8bit.bitwarden.data.platform.manager

import com.bitwarden.data.datasource.disk.model.EnvironmentUrlDataJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.CustomHeadersDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.FakeEnvironmentDiskSource
import com.x8bit.bitwarden.data.vault.repository.model.createMockAccountJson
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CustomHeadersManagerTest {

    private val fakeAuthDiskSource = FakeAuthDiskSource()
    private val fakeEnvironmentDiskSource = FakeEnvironmentDiskSource()
    private val customHeadersDiskSource: CustomHeadersDiskSource = mockk {
        every { storeCustomHeaders(id = any(), headers = any()) } just runs
    }

    private val customHeadersManager: CustomHeadersManager = CustomHeadersManagerImpl(
        authDiskSource = fakeAuthDiskSource,
        customHeadersDiskSource = customHeadersDiskSource,
        environmentDiskSource = fakeEnvironmentDiskSource,
    )

    @Test
    fun `getCustomHeaders by url should return empty map when there is no environment data`() {
        fakeEnvironmentDiskSource.preAuthEnvironmentUrlData = null

        val result = customHeadersManager.getCustomHeaders(url = SELF_HOSTED_ICON_URL)

        assertEquals(emptyMap<String, String>(), result)
        verify(exactly = 0) { customHeadersDiskSource.getCustomHeaders(id = any()) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getCustomHeaders by url should return empty map when the environment has no custom headers id`() {
        fakeEnvironmentDiskSource.preAuthEnvironmentUrlData = SELF_HOSTED_ENVIRONMENT.copy(
            customHeadersId = null,
        )

        val result = customHeadersManager.getCustomHeaders(url = SELF_HOSTED_ICON_URL)

        assertEquals(emptyMap<String, String>(), result)
        verify(exactly = 0) { customHeadersDiskSource.getCustomHeaders(id = any()) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getCustomHeaders by url should return empty map when the url host is not an environment host`() {
        fakeEnvironmentDiskSource.preAuthEnvironmentUrlData = SELF_HOSTED_ENVIRONMENT

        val result = customHeadersManager.getCustomHeaders(
            url = "https://api.pwnedpasswords.com/range/12345",
        )

        assertEquals(emptyMap<String, String>(), result)
        verify(exactly = 0) { customHeadersDiskSource.getCustomHeaders(id = any()) }
    }

    @Test
    fun `getCustomHeaders by url should return empty map when the url is malformed`() {
        fakeEnvironmentDiskSource.preAuthEnvironmentUrlData = SELF_HOSTED_ENVIRONMENT

        val result = customHeadersManager.getCustomHeaders(url = "not a valid url")

        assertEquals(emptyMap<String, String>(), result)
        verify(exactly = 0) { customHeadersDiskSource.getCustomHeaders(id = any()) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getCustomHeaders by url should return empty map when the url scheme does not match the environment`() {
        fakeEnvironmentDiskSource.preAuthEnvironmentUrlData = SELF_HOSTED_ENVIRONMENT

        val result = customHeadersManager.getCustomHeaders(
            url = SELF_HOSTED_ICON_URL.replace("https://", "http://"),
        )

        assertEquals(emptyMap<String, String>(), result)
        verify(exactly = 0) { customHeadersDiskSource.getCustomHeaders(id = any()) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getCustomHeaders by url should return the stored headers for an environment host url`() {
        fakeEnvironmentDiskSource.preAuthEnvironmentUrlData = SELF_HOSTED_ENVIRONMENT
        every {
            customHeadersDiskSource.getCustomHeaders(id = CUSTOM_HEADERS_ID)
        } returns CUSTOM_HEADERS

        val result = customHeadersManager.getCustomHeaders(url = SELF_HOSTED_ICON_URL)

        assertEquals(CUSTOM_HEADERS, result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getCustomHeaders by url should return empty map when no headers are stored for the id`() {
        fakeEnvironmentDiskSource.preAuthEnvironmentUrlData = SELF_HOSTED_ENVIRONMENT
        every { customHeadersDiskSource.getCustomHeaders(id = CUSTOM_HEADERS_ID) } returns null

        val result = customHeadersManager.getCustomHeaders(url = SELF_HOSTED_ICON_URL)

        assertEquals(emptyMap<String, String>(), result)
    }

    @Test
    fun `getStoredCustomHeaders should return the stored headers for the id`() {
        every {
            customHeadersDiskSource.getCustomHeaders(id = CUSTOM_HEADERS_ID)
        } returns CUSTOM_HEADERS

        assertEquals(
            CUSTOM_HEADERS,
            customHeadersManager.getStoredCustomHeaders(id = CUSTOM_HEADERS_ID),
        )
    }

    @Test
    fun `getStoredCustomHeaders should return null when no headers are stored for the id`() {
        every { customHeadersDiskSource.getCustomHeaders(id = "unknownId") } returns null

        assertNull(customHeadersManager.getStoredCustomHeaders(id = "unknownId"))
    }

    @Test
    fun `saveCustomHeaders should store the headers under a fresh id and return it`() {
        val idSlot = slot<String>()
        every {
            customHeadersDiskSource.storeCustomHeaders(
                id = capture(idSlot),
                headers = CUSTOM_HEADERS,
            )
        } just runs

        val result = customHeadersManager.saveCustomHeaders(headers = CUSTOM_HEADERS)

        assertEquals(idSlot.captured, result)
        verify(exactly = 1) {
            customHeadersDiskSource.storeCustomHeaders(id = result, headers = CUSTOM_HEADERS)
        }
    }

    @Test
    fun `removeCustomHeaders should delete the headers when nothing references the id`() {
        fakeEnvironmentDiskSource.preAuthEnvironmentUrlData = SELF_HOSTED_ENVIRONMENT.copy(
            customHeadersId = null,
        )
        fakeAuthDiskSource.userState = null

        customHeadersManager.removeCustomHeaders(id = CUSTOM_HEADERS_ID)

        verify(exactly = 1) {
            customHeadersDiskSource.storeCustomHeaders(id = CUSTOM_HEADERS_ID, headers = null)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `removeCustomHeaders should not delete the headers when the pre-auth environment references the id`() {
        fakeEnvironmentDiskSource.preAuthEnvironmentUrlData = SELF_HOSTED_ENVIRONMENT
        fakeAuthDiskSource.userState = null

        customHeadersManager.removeCustomHeaders(id = CUSTOM_HEADERS_ID)

        verify(exactly = 0) {
            customHeadersDiskSource.storeCustomHeaders(id = any(), headers = null)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `removeCustomHeaders should not delete the headers when an account environment references the id`() {
        fakeEnvironmentDiskSource.preAuthEnvironmentUrlData = SELF_HOSTED_ENVIRONMENT.copy(
            customHeadersId = null,
        )
        fakeAuthDiskSource.userState = UserStateJson(
            activeUserId = USER_ID_1,
            accounts = mapOf(USER_ID_1 to ACCOUNT_1_WITH_CUSTOM_HEADERS),
        )

        customHeadersManager.removeCustomHeaders(id = CUSTOM_HEADERS_ID)

        verify(exactly = 0) {
            customHeadersDiskSource.storeCustomHeaders(id = any(), headers = null)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `removeCustomHeadersForUser should delete the headers when no other environment references the id`() {
        fakeEnvironmentDiskSource.preAuthEnvironmentUrlData = SELF_HOSTED_ENVIRONMENT.copy(
            customHeadersId = null,
        )
        fakeAuthDiskSource.userState = UserStateJson(
            activeUserId = USER_ID_1,
            accounts = mapOf(
                USER_ID_1 to ACCOUNT_1_WITH_CUSTOM_HEADERS,
                USER_ID_2 to createMockAccountJson(number = 2),
            ),
        )

        customHeadersManager.removeCustomHeadersForUser(userId = USER_ID_1)

        verify(exactly = 1) {
            customHeadersDiskSource.storeCustomHeaders(id = CUSTOM_HEADERS_ID, headers = null)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `removeCustomHeadersForUser should keep the headers when the pre-auth environment references the id`() {
        fakeEnvironmentDiskSource.preAuthEnvironmentUrlData = SELF_HOSTED_ENVIRONMENT
        fakeAuthDiskSource.userState = UserStateJson(
            activeUserId = USER_ID_1,
            accounts = mapOf(USER_ID_1 to ACCOUNT_1_WITH_CUSTOM_HEADERS),
        )

        customHeadersManager.removeCustomHeadersForUser(userId = USER_ID_1)

        verify(exactly = 0) {
            customHeadersDiskSource.storeCustomHeaders(id = any(), headers = null)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `removeCustomHeadersForUser should keep the headers when another account references the id`() {
        fakeEnvironmentDiskSource.preAuthEnvironmentUrlData = SELF_HOSTED_ENVIRONMENT.copy(
            customHeadersId = null,
        )
        fakeAuthDiskSource.userState = UserStateJson(
            activeUserId = USER_ID_1,
            accounts = mapOf(
                USER_ID_1 to ACCOUNT_1_WITH_CUSTOM_HEADERS,
                USER_ID_2 to createMockAccountJson(
                    number = 2,
                    settings = AccountJson.Settings(
                        environmentUrlData = SELF_HOSTED_ENVIRONMENT,
                    ),
                ),
            ),
        )

        customHeadersManager.removeCustomHeadersForUser(userId = USER_ID_1)

        verify(exactly = 0) {
            customHeadersDiskSource.storeCustomHeaders(id = any(), headers = null)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `removeCustomHeadersForUser should do nothing when the account has no custom headers id`() {
        fakeEnvironmentDiskSource.preAuthEnvironmentUrlData = SELF_HOSTED_ENVIRONMENT
        fakeAuthDiskSource.userState = UserStateJson(
            activeUserId = USER_ID_1,
            accounts = mapOf(USER_ID_1 to createMockAccountJson(number = 1)),
        )

        customHeadersManager.removeCustomHeadersForUser(userId = USER_ID_1)

        verify(exactly = 0) {
            customHeadersDiskSource.storeCustomHeaders(id = any(), headers = any())
        }
    }
}

private const val CUSTOM_HEADERS_ID = "mockCustomHeadersId"
private const val USER_ID_1 = "mockId-1"
private const val USER_ID_2 = "mockId-2"
private const val SELF_HOSTED_ICON_URL =
    "https://vault.example.com/icons/bitwarden.com/icon.png"
private val CUSTOM_HEADERS = mapOf(
    "CF-Access-Client-Id" to "clientId",
    "CF-Access-Client-Secret" to "clientSecret",
)
private val SELF_HOSTED_ENVIRONMENT = EnvironmentUrlDataJson(
    base = "https://vault.example.com",
    customHeadersId = CUSTOM_HEADERS_ID,
)
private val ACCOUNT_1_WITH_CUSTOM_HEADERS = createMockAccountJson(
    number = 1,
    settings = AccountJson.Settings(
        environmentUrlData = SELF_HOSTED_ENVIRONMENT,
    ),
)
