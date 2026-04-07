package com.x8bit.bitwarden.data.platform.manager.restriction

import android.content.RestrictionsManager
import android.os.Bundle
import com.bitwarden.data.datasource.disk.model.EnvironmentUrlDataJson
import com.bitwarden.data.repository.model.Environment
import com.x8bit.bitwarden.data.platform.repository.util.FakeEnvironmentRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RestrictionManagerTest {

    private val fakeEnvironmentRepository = FakeEnvironmentRepository()
    private val restrictionsManager = mockk<RestrictionsManager>()

    private val restrictionManager: RestrictionManager = RestrictionManagerImpl(
        environmentRepository = fakeEnvironmentRepository,
        restrictionsManager = restrictionsManager,
    )

    @Test
    fun `initialize with a null bundle should do nothing`() {
        every { restrictionsManager.applicationRestrictions } returns null

        restrictionManager.initialize()

        verify(exactly = 1) {
            restrictionsManager.applicationRestrictions
        }
        assertEquals(Environment.Us, fakeEnvironmentRepository.environment)
    }

    @Test
    fun `initialize with an empty bundle should do nothing`() {
        every { restrictionsManager.applicationRestrictions } returns mockBundle()

        restrictionManager.initialize()

        verify(exactly = 1) {
            restrictionsManager.applicationRestrictions
        }
        assertEquals(Environment.Us, fakeEnvironmentRepository.environment)
    }

    @Test
    fun `initialize with unknown bundle data should do nothing`() {
        every {
            restrictionsManager.applicationRestrictions
        } returns mockBundle("key" to "unknown")

        restrictionManager.initialize()

        verify(exactly = 1) {
            restrictionsManager.applicationRestrictions
        }
        assertEquals(Environment.Us, fakeEnvironmentRepository.environment)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `initialize with baseEnvironmentUrl bundle data matching the current US environment should set the environment to US`() {
        every {
            restrictionsManager.applicationRestrictions
        } returns mockBundle("baseEnvironmentUrl" to "https://vault.bitwarden.com")

        restrictionManager.initialize()

        verify(exactly = 1) {
            restrictionsManager.applicationRestrictions
        }
        assertEquals(Environment.Us, fakeEnvironmentRepository.environment)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `initialize with baseEnvironmentUrl bundle data not matching the current US environment should set the environment to self-hosted`() {
        val baseUrl = "https://other.bitwarden.com"
        every {
            restrictionsManager.applicationRestrictions
        } returns mockBundle("baseEnvironmentUrl" to baseUrl)

        restrictionManager.initialize()

        verify(exactly = 1) {
            restrictionsManager.applicationRestrictions
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
    fun `initialize with baseEnvironmentUrl bundle data matching the current EU environment should set the environment to EU`() {
        fakeEnvironmentRepository.environment = Environment.Eu
        every {
            restrictionsManager.applicationRestrictions
        } returns mockBundle("baseEnvironmentUrl" to "https://vault.bitwarden.eu")

        restrictionManager.initialize()

        verify(exactly = 1) {
            restrictionsManager.applicationRestrictions
        }
        assertEquals(Environment.Eu, fakeEnvironmentRepository.environment)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `initialize with baseEnvironmentUrl bundle data not matching the current EU environment should set the environment to self-hosted`() {
        val baseUrl = "https://other.bitwarden.eu"
        fakeEnvironmentRepository.environment = Environment.Eu
        every {
            restrictionsManager.applicationRestrictions
        } returns mockBundle("baseEnvironmentUrl" to baseUrl)

        restrictionManager.initialize()

        verify(exactly = 1) {
            restrictionsManager.applicationRestrictions
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
    fun `initialize with baseEnvironmentUrl bundle data matching the current self-hosted environment should set the environment to self-hosted`() {
        val baseUrl = "https://vault.qa.bitwarden.pw"
        val environment = Environment.SelfHosted(
            environmentUrlData = EnvironmentUrlDataJson(base = baseUrl),
        )
        fakeEnvironmentRepository.environment = environment
        every {
            restrictionsManager.applicationRestrictions
        } returns mockBundle("baseEnvironmentUrl" to baseUrl)

        restrictionManager.initialize()

        verify(exactly = 1) {
            restrictionsManager.applicationRestrictions
        }
        assertEquals(environment, fakeEnvironmentRepository.environment)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `initialize with baseEnvironmentUrl bundle data not matching the current self-hosted environment should set the environment to self-hosted`() {
        val baseUrl = "https://other.qa.bitwarden.pw"
        val environment = Environment.SelfHosted(
            environmentUrlData = EnvironmentUrlDataJson(base = "https://vault.qa.bitwarden.pw"),
        )
        fakeEnvironmentRepository.environment = environment
        every {
            restrictionsManager.applicationRestrictions
        } returns mockBundle("baseEnvironmentUrl" to baseUrl)

        restrictionManager.initialize()

        verify(exactly = 1) {
            restrictionsManager.applicationRestrictions
        }
        assertEquals(
            Environment.SelfHosted(
                environmentUrlData = environment.environmentUrlData.copy(base = baseUrl),
            ),
            fakeEnvironmentRepository.environment,
        )
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
