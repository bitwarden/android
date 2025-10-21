package com.x8bit.bitwarden.data.platform.manager

import androidx.credentials.providerevents.exception.RegisterExportUnknownErrorException
import androidx.credentials.providerevents.transfer.CredentialTypes
import androidx.credentials.providerevents.transfer.RegisterExportResponse
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.cxf.registry.CredentialExchangeRegistry
import com.bitwarden.cxf.registry.model.RegistrationRequest
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.manager.model.RegisterExportResult
import com.x8bit.bitwarden.data.platform.manager.model.UnregisterExportResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CredentialExchangeRegistryManagerImplTest {

    private val credentialExchangeRegistry: CredentialExchangeRegistry = mockk {
        coEvery { register(any()) } returns RegisterExportResponse().asSuccess()
        coEvery { unregister() } returns RegisterExportResponse().asSuccess()
    }
    private val settingsDiskSource: SettingsDiskSource = mockk {
        every { getAppRegisteredForExport() } returns false
        every { storeAppRegisteredForExport(any()) } just runs
    }

    private val registryManager: CredentialExchangeRegistryManager =
        CredentialExchangeRegistryManagerImpl(
            credentialExchangeRegistry = credentialExchangeRegistry,
            settingsDiskSource = settingsDiskSource,
        )

    @Suppress("MaxLineLength")
    @Test
    fun `register should store app registered for export and return Success when registration is successful`() =
        runTest {
            val result = registryManager.register()

            coVerify {
                credentialExchangeRegistry.register(
                    registrationRequest = RegistrationRequest(
                        appNameRes = R.string.app_name,
                        credentialTypes = setOf(
                            CredentialTypes.CREDENTIAL_TYPE_BASIC_AUTH,
                            CredentialTypes.CREDENTIAL_TYPE_PUBLIC_KEY,
                            CredentialTypes.CREDENTIAL_TYPE_ADDRESS,
                            CredentialTypes.CREDENTIAL_TYPE_API_KEY,
                            CredentialTypes.CREDENTIAL_TYPE_CREDIT_CARD,
                            CredentialTypes.CREDENTIAL_TYPE_CUSTOM_FIELDS,
                            CredentialTypes.CREDENTIAL_TYPE_DRIVERS_LICENSE,
                            CredentialTypes.CREDENTIAL_TYPE_IDENTITY_DOCUMENT,
                            CredentialTypes.CREDENTIAL_TYPE_NOTE,
                            CredentialTypes.CREDENTIAL_TYPE_PASSPORT,
                            CredentialTypes.CREDENTIAL_TYPE_PERSON_NAME,
                            CredentialTypes.CREDENTIAL_TYPE_SSH_KEY,
                            CredentialTypes.CREDENTIAL_TYPE_TOTP,
                            CredentialTypes.CREDENTIAL_TYPE_WIFI,
                        ),
                        iconResId = BitwardenDrawable.logo_bitwarden_icon,
                    ),
                )
                settingsDiskSource.storeAppRegisteredForExport(true)
            }

            assertEquals(RegisterExportResult.Success, result)
        }

    @Test
    fun `register should return Failure when registration fails`() =
        runTest {
            coEvery {
                credentialExchangeRegistry.register(any())
            } returns RegisterExportUnknownErrorException().asFailure()

            val result = registryManager.register()

            verify(exactly = 0) {
                settingsDiskSource.storeAppRegisteredForExport(any())
            }
            assertTrue(result is RegisterExportResult.Failure)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `unregister should store app registered for export and return Success when unregistration is successful`() =
        runTest {
            val result = registryManager.unregister()

            coVerify {
                credentialExchangeRegistry.unregister()
                settingsDiskSource.storeAppRegisteredForExport(false)
            }

            assertEquals(UnregisterExportResult.Success, result)
        }

    @Test
    fun `unregister should return Failure when unregistration fails`() = runTest {
        coEvery {
            credentialExchangeRegistry.unregister()
        } returns RegisterExportUnknownErrorException().asFailure()

        val result = registryManager.unregister()

        verify(exactly = 0) {
            settingsDiskSource.storeAppRegisteredForExport(any())
        }
        assertTrue(result is UnregisterExportResult.Failure)
    }
}
