package com.x8bit.bitwarden.data.platform.manager.restriction

import android.annotation.SuppressLint
import android.content.Context
import android.content.RestrictionsManager
import android.os.Bundle
import com.x8bit.bitwarden.data.auth.datasource.disk.model.EnvironmentUrlDataJson
import com.x8bit.bitwarden.data.platform.base.FakeDispatcherManager
import com.x8bit.bitwarden.data.platform.manager.model.AppForegroundState
import com.x8bit.bitwarden.data.platform.manager.util.FakeAppStateManager
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.platform.repository.util.FakeEnvironmentRepository
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@SuppressLint("UnspecifiedRegisterReceiverFlag")
class RestrictionManagerTest {

    private val context = mockk<Context> {
        every { registerReceiver(any(), any()) } returns null
        every { unregisterReceiver(any()) } just runs
    }
    private val fakeAppStateManager = FakeAppStateManager()
    private val fakeDispatcherManager = FakeDispatcherManager().apply {
        setMain(unconfined)
    }
    private val fakeEnvironmentRepository = FakeEnvironmentRepository()
    private val restrictionsManager = mockk<RestrictionsManager>()

    private val restrictionManager: RestrictionManager = RestrictionManagerImpl(
        appStateManager = fakeAppStateManager,
        dispatcherManager = fakeDispatcherManager,
        context = context,
        environmentRepository = fakeEnvironmentRepository,
        restrictionsManager = restrictionsManager,
    )

    @AfterEach
    fun tearDown() {
        fakeDispatcherManager.resetMain()
    }

    @Test
    fun `on app foreground with a null bundle should register receiver and do nothing else`() {
        every { restrictionsManager.applicationRestrictions } returns null

        fakeAppStateManager.appForegroundState = AppForegroundState.FOREGROUNDED

        verify(exactly = 1) {
            context.registerReceiver(any(), any())
        }
        assertEquals(Environment.Us, fakeEnvironmentRepository.environment)
    }

    @Test
    fun `on app foreground with an empty bundle should register receiver and do nothing else`() {
        every { restrictionsManager.applicationRestrictions } returns mockBundle()

        fakeAppStateManager.appForegroundState = AppForegroundState.FOREGROUNDED

        verify(exactly = 1) {
            context.registerReceiver(any(), any())
        }
        assertEquals(Environment.Us, fakeEnvironmentRepository.environment)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on app foreground with unknown bundle data should register receiver and do nothing else`() {
        every {
            restrictionsManager.applicationRestrictions
        } returns mockBundle("key" to "unknown")

        fakeAppStateManager.appForegroundState = AppForegroundState.FOREGROUNDED

        verify(exactly = 1) {
            context.registerReceiver(any(), any())
        }
        assertEquals(Environment.Us, fakeEnvironmentRepository.environment)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on app foreground with baseEnvironmentUrl bundle data matching the current US environment should register receiver and set the environment to US`() {
        every {
            restrictionsManager.applicationRestrictions
        } returns mockBundle("baseEnvironmentUrl" to "https://vault.bitwarden.com")

        fakeAppStateManager.appForegroundState = AppForegroundState.FOREGROUNDED

        verify(exactly = 1) {
            context.registerReceiver(any(), any())
        }
        assertEquals(Environment.Us, fakeEnvironmentRepository.environment)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on app foreground with baseEnvironmentUrl bundle data not matching the current US environment should register receiver and set the environment to self-hosted`() {
        val baseUrl = "https://other.bitwarden.com"
        every {
            restrictionsManager.applicationRestrictions
        } returns mockBundle("baseEnvironmentUrl" to baseUrl)

        fakeAppStateManager.appForegroundState = AppForegroundState.FOREGROUNDED

        verify(exactly = 1) {
            context.registerReceiver(any(), any())
        }
        assertEquals(
            Environment.SelfHosted(
                environmentUrlData = Environment.Us.environmentUrlData.copy(base = baseUrl),
            ),
            fakeEnvironmentRepository.environment,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on app foreground with baseEnvironmentUrl bundle data matching the current EU environment should register receiver and set the environment to EU`() {
        fakeEnvironmentRepository.environment = Environment.Eu
        every {
            restrictionsManager.applicationRestrictions
        } returns mockBundle("baseEnvironmentUrl" to "https://vault.bitwarden.eu")

        fakeAppStateManager.appForegroundState = AppForegroundState.FOREGROUNDED

        verify(exactly = 1) {
            context.registerReceiver(any(), any())
        }
        assertEquals(Environment.Eu, fakeEnvironmentRepository.environment)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on app foreground with baseEnvironmentUrl bundle data not matching the current EU environment should register receiver and set the environment to self-hosted`() {
        val baseUrl = "https://other.bitwarden.eu"
        fakeEnvironmentRepository.environment = Environment.Eu
        every {
            restrictionsManager.applicationRestrictions
        } returns mockBundle("baseEnvironmentUrl" to baseUrl)

        fakeAppStateManager.appForegroundState = AppForegroundState.FOREGROUNDED

        verify(exactly = 1) {
            context.registerReceiver(any(), any())
        }
        assertEquals(
            Environment.SelfHosted(
                environmentUrlData = Environment.Eu.environmentUrlData.copy(base = baseUrl),
            ),
            fakeEnvironmentRepository.environment,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on app foreground with baseEnvironmentUrl bundle data matching the current self-hosted environment should register receiver and set the environment to self-hosted`() {
        val baseUrl = "https://vault.qa.bitwarden.pw"
        val environment = Environment.SelfHosted(
            environmentUrlData = EnvironmentUrlDataJson(base = baseUrl),
        )
        fakeEnvironmentRepository.environment = environment
        every {
            restrictionsManager.applicationRestrictions
        } returns mockBundle("baseEnvironmentUrl" to baseUrl)

        fakeAppStateManager.appForegroundState = AppForegroundState.FOREGROUNDED

        verify(exactly = 1) {
            context.registerReceiver(any(), any())
        }
        assertEquals(environment, fakeEnvironmentRepository.environment)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on app foreground with baseEnvironmentUrl bundle data not matching the current self-hosted environment should register receiver and set the environment to self-hosted`() {
        val baseUrl = "https://other.qa.bitwarden.pw"
        val environment = Environment.SelfHosted(
            environmentUrlData = EnvironmentUrlDataJson(base = "https://vault.qa.bitwarden.pw"),
        )
        fakeEnvironmentRepository.environment = environment
        every {
            restrictionsManager.applicationRestrictions
        } returns mockBundle("baseEnvironmentUrl" to baseUrl)

        fakeAppStateManager.appForegroundState = AppForegroundState.FOREGROUNDED

        verify(exactly = 1) {
            context.registerReceiver(any(), any())
        }
        assertEquals(
            Environment.SelfHosted(
                environmentUrlData = environment.environmentUrlData.copy(base = baseUrl),
            ),
            fakeEnvironmentRepository.environment,
        )
    }

    @Test
    fun `on app background when not foregrounded should do nothing`() {
        fakeAppStateManager.appForegroundState = AppForegroundState.BACKGROUNDED

        verify(exactly = 0) {
            context.unregisterReceiver(any())
            restrictionsManager.applicationRestrictions
        }
    }

    @Test
    fun `on app background after foreground should unregister receiver`() {
        every { restrictionsManager.applicationRestrictions } returns null
        fakeAppStateManager.appForegroundState = AppForegroundState.FOREGROUNDED
        clearMocks(context, restrictionsManager, answers = false)

        fakeAppStateManager.appForegroundState = AppForegroundState.BACKGROUNDED

        verify(exactly = 1) {
            context.unregisterReceiver(any())
        }
        verify(exactly = 0) {
            restrictionsManager.applicationRestrictions
        }
    }
}

/**
 * Helper method for constructing a simple mock bundle.
 */
private fun mockBundle(vararg pairs: Pair<String, String>): Bundle =
    mockk<Bundle> {
        every { isEmpty } returns pairs.isEmpty()
        every { getString(any()) } returns null
        pairs.forEach { (key, value) ->
            every { getString(key) } returns value
        }
    }
