package com.x8bit.bitwarden.data.platform.repository

import app.cash.turbine.test
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.EnvironmentUrlDataJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.platform.base.FakeDispatcherManager
import com.x8bit.bitwarden.data.platform.datasource.disk.FakeEnvironmentDiskSource
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.platform.repository.util.toEnvironmentUrls
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EnvironmentRepositoryTest {

    private val dispatcherManager: DispatcherManager = FakeDispatcherManager()

    private val fakeEnvironmentDiskSource = FakeEnvironmentDiskSource()
    private val fakeAuthDiskSource = FakeAuthDiskSource()

    private val repository = EnvironmentRepositoryImpl(
        environmentDiskSource = fakeEnvironmentDiskSource,
        authDiskSource = fakeAuthDiskSource,
        dispatcherManager = dispatcherManager,
    )

    @BeforeEach
    fun setUp() {
        mockkStatic(EnvironmentUrlDataJson::toEnvironmentUrls)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(EnvironmentUrlDataJson::toEnvironmentUrls)
    }

    @Test
    fun `changes to the active user should update the environment if necessary`() {
        assertEquals(
            Environment.Us,
            repository.environment,
        )
        assertEquals(
            null,
            fakeEnvironmentDiskSource.preAuthEnvironmentUrlData,
        )

        // Updating the environment for the active user to a non-null value triggers an update
        // in the saved environment.
        fakeAuthDiskSource.userState = getMockUserState(
            environmentForActiveUser = EnvironmentUrlDataJson.DEFAULT_EU,
        )
        assertEquals(
            Environment.Eu,
            repository.environment,
        )
        assertEquals(
            EnvironmentUrlDataJson.DEFAULT_EU,
            fakeEnvironmentDiskSource.preAuthEnvironmentUrlData,
        )

        // Updating the environment for the active user to a null value leaves the current
        // environment unchanged.
        fakeAuthDiskSource.userState = getMockUserState(
            environmentForActiveUser = null,
        )
        assertEquals(
            Environment.Eu,
            repository.environment,
        )
        assertEquals(
            EnvironmentUrlDataJson.DEFAULT_EU,
            fakeEnvironmentDiskSource.preAuthEnvironmentUrlData,
        )
    }

    @Test
    fun `environment should pull from and update EnvironmentDiskSource`() {
        val environmentUrlDataJson = mockk<EnvironmentUrlDataJson>()
        val environment = mockk<Environment> {
            every { environmentUrlData } returns environmentUrlDataJson
        }
        every { environmentUrlDataJson.toEnvironmentUrls() } returns environment

        // The repository exposes a non-null default value when the disk source is empty
        assertNull(fakeEnvironmentDiskSource.preAuthEnvironmentUrlData)
        assertEquals(
            Environment.Us,
            repository.environment,
        )

        // Updating the repository updates the disk source
        repository.environment = environment
        assertEquals(
            environmentUrlDataJson,
            fakeEnvironmentDiskSource.preAuthEnvironmentUrlData,
        )

        // Updating the disk source updates the repository
        fakeEnvironmentDiskSource.preAuthEnvironmentUrlData = null
        assertEquals(
            Environment.Us,
            repository.environment,
        )
        fakeEnvironmentDiskSource.preAuthEnvironmentUrlData = environmentUrlDataJson
        assertEquals(
            environment,
            repository.environment,
        )
    }

    @Test
    fun `environmentStateFow should react to changes in environment`() = runTest {
        val environmentUrlDataJson = mockk<EnvironmentUrlDataJson>()
        val environment = mockk<Environment> {
            every { environmentUrlData } returns environmentUrlDataJson
        }
        every { environmentUrlDataJson.toEnvironmentUrls() } returns environment

        repository.environmentStateFlow.test {
            // The initial values of the Flow and the property are in sync
            assertEquals(
                Environment.Us,
                repository.environment,
            )
            assertEquals(
                Environment.Us,
                awaitItem(),
            )

            // Updating the property causes a flow emissions
            repository.environment = environment
            assertEquals(environment, awaitItem())
        }
    }

    @Test
    fun `loadEnvironmentForEmail should update the environment`() = runTest {
        val environmentUrlDataJson = mockk<EnvironmentUrlDataJson>()
        val environment = mockk<Environment> {
            every { environmentUrlData } returns environmentUrlDataJson
        }
        every { environmentUrlDataJson.toEnvironmentUrls() } returns environment

        fakeEnvironmentDiskSource.storePreAuthEnvironmentUrlDataForEmail(
            userEmail = EMAIL,
            environmentUrlDataJson,
        )

        repository.loadEnvironmentForEmail(userEmail = EMAIL)

        assertEquals(
            environment,
            repository.environment,
        )
    }

    @Test
    fun `loadEnvironmentForEmail returns false on fail`() = runTest {
        val result = repository.loadEnvironmentForEmail(userEmail = EMAIL)
        assertFalse(result)
    }

    @Test
    fun `saveCurrentEnvironmentForEmail should save the environment`() = runTest {
        val environmentUrlDataJson = mockk<EnvironmentUrlDataJson>()
        val environment = mockk<Environment> {
            every { environmentUrlData } returns environmentUrlDataJson
        }
        every { environmentUrlDataJson.toEnvironmentUrls() } returns environment

        repository.environment = Environment.Eu
        fakeEnvironmentDiskSource.storePreAuthEnvironmentUrlDataForEmail(
            userEmail = EMAIL,
            environmentUrlDataJson,
        )

        repository.saveCurrentEnvironmentForEmail(userEmail = EMAIL)

        assertEquals(
            Environment.Eu.environmentUrlData,
            fakeEnvironmentDiskSource
                .getPreAuthEnvironmentUrlDataForEmail(
                    userEmail = EMAIL,
                ),
        )
    }

    private fun getMockUserState(
        environmentForActiveUser: EnvironmentUrlDataJson?,
    ): UserStateJson =
        UserStateJson(
            activeUserId = "activeUserId",
            accounts = mapOf(
                "activeUserId" to AccountJson(
                    profile = mockk(),
                    tokens = mockk(),
                    settings = AccountJson.Settings(
                        environmentUrlData = environmentForActiveUser,
                    ),
                ),
            ),
        )
}

private const val EMAIL = "email@example.com"
