package com.x8bit.bitwarden.ui.platform.manager.keychain

import android.app.Activity
import android.net.Uri
import android.security.KeyChain
import android.security.KeyChainAliasCallback
import androidx.core.net.toUri
import com.x8bit.bitwarden.ui.platform.manager.keychain.model.PrivateKeyAliasSelectionResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf

class KeyChainManagerTest {

    private val mockActivity = mockk<Activity>()
    private val keyChainManager = KeyChainManagerImpl(activity = mockActivity)

    @BeforeEach
    fun setUp() {
        mockkStatic(KeyChain::class)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(KeyChain::class)
    }

    @Test
    fun `choosePrivateKeyAlias should return Success with alias when key is selected`() = runTest {
        setupMockUri()
        val systemCallbackCaptor = slot<KeyChainAliasCallback>()
        every {
            KeyChain.choosePrivateKeyAlias(
                /* activity = */ mockActivity,
                /* response = */ capture(systemCallbackCaptor),
                /* keyTypes = */ null,
                /* issuers = */ null,
                /* uri = */ null,
                /* alias = */ null,
            )
        } answers {
            systemCallbackCaptor.captured.alias("mockAlias")
        }

        val result = keyChainManager.choosePrivateKeyAlias(currentServerUrl = null)

        assertEquals(
            PrivateKeyAliasSelectionResult.Success("mockAlias"),
            result,
        )
    }

    @Test
    fun `choosePrivateKeyAlias should return Error when IllegalArgumentException is thrown`() =
        runTest {
            setupMockUri()
            every {
                KeyChain.choosePrivateKeyAlias(
                    /* activity = */ mockActivity,
                    /* response = */ any(),
                    /* keyTypes = */ null,
                    /* issuers = */ null,
                    /* uri = */ null,
                    /* alias = */ null,
                )
            } throws IllegalArgumentException()

            val result = keyChainManager.choosePrivateKeyAlias(currentServerUrl = null)

            assertInstanceOf<PrivateKeyAliasSelectionResult.Error>(result)
        }

    @Test
    fun `choosePrivateKeyAlias should pass currentServerUrl to system KeyChain`() = runTest {
        setupMockUri()
        val systemCallbackCaptor = slot<KeyChainAliasCallback>()
        every {
            KeyChain.choosePrivateKeyAlias(
                /* activity = */ mockActivity,
                /* response = */ capture(systemCallbackCaptor),
                /* keyTypes = */ null,
                /* issuers = */ null,
                /* uri = */ "www.mockuri.com".toUri(),
                /* alias = */ null,
            )
        } answers {
            systemCallbackCaptor.captured.alias("mockAlias")
        }

        val result = keyChainManager.choosePrivateKeyAlias(currentServerUrl = "www.mockuri.com")

        assertInstanceOf<PrivateKeyAliasSelectionResult.Success>(result)
            .also { assertEquals("mockAlias", it.alias) }
    }

    @Test
    fun `choosePrivateKeyAlias should return Success with null alias when no key is selected`() =
        runTest {
            setupMockUri()
            val systemCallbackCaptor = slot<KeyChainAliasCallback>()
            every {
                KeyChain.choosePrivateKeyAlias(
                    /* activity = */ mockActivity,
                    /* response = */ capture(systemCallbackCaptor),
                    /* keyTypes = */ null,
                    /* issuers = */ null,
                    /* uri = */ null,
                    /* alias = */ null,
                )
            } answers {
                systemCallbackCaptor.captured.alias(null)
            }

            val result = keyChainManager.choosePrivateKeyAlias(currentServerUrl = null)

            assertInstanceOf<PrivateKeyAliasSelectionResult.Success>(result)
                .also { assertNull(it.alias) }
        }

    private fun setupMockUri() {
        mockkStatic(Uri::class)
        val uriMock = mockk<Uri>()
        every { Uri.parse(any()) } returns uriMock
        every { uriMock.host } returns "www.mockuri.com"
    }
}
