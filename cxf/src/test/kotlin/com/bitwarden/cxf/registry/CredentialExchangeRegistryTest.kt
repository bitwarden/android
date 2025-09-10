package com.bitwarden.cxf.registry

import android.content.Context
import android.graphics.Bitmap
import androidx.credentials.providerevents.ProviderEventsManager
import com.bitwarden.cxf.registry.model.RegistrationRequest
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CredentialExchangeRegistryTest {

    private val mockContext = mockk<Context>(relaxed = true)
    private val mockProviderEventsManager = mockk<ProviderEventsManager>()
    private val registry = CredentialExchangeRegistryImpl(
        context = mockContext,
        providerEventsManager = mockProviderEventsManager,
    )

    @Test
    fun `register should return true when registration is successful`() = runTest {
        val request = RegistrationRequest(
            appName = "Test App",
            bitmap = mockk<Bitmap>(relaxed = true),
            credentialTypes = emptySet(),
        )

        coEvery {
            mockProviderEventsManager.registerExport(any())
        } returns true

        assertTrue(
            registry.register(
                registrationRequest = request,
            ),
        )
    }

    @Test
    fun `register should return false when registration fails`() = runTest {
        val request = RegistrationRequest(
            appName = "Test App",
            bitmap = mockk<Bitmap>(relaxed = true),
            credentialTypes = emptySet(),
        )
        coEvery {
            mockProviderEventsManager.registerExport(any())
        } returns false

        assertFalse(
            registry.register(
                registrationRequest = request,
            ),
        )
    }

    @Test
    fun `unregister should return true when unregistration is successful`() = runTest {
        coEvery {
            mockProviderEventsManager.registerExport(any())
        } returns true
        assertTrue(registry.unregister())
    }

    @Test
    fun `unregister should return false when unregistration fails`() = runTest {
        coEvery {
            mockProviderEventsManager.registerExport(any())
        } returns false
        assertFalse(registry.unregister())
    }
}
