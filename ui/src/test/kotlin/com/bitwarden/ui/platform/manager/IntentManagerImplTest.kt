package com.bitwarden.ui.platform.manager

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.browser.auth.AuthTabIntent
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import com.bitwarden.core.data.manager.BuildInfoManager
import com.bitwarden.ui.platform.manager.intent.model.AuthTabData
import io.mockk.anyConstructed
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock

@Config(sdk = [Config.NEWEST_SDK])
@RunWith(RobolectricTestRunner::class)
class IntentManagerImplTest {

    private val activity: Activity = mockk(relaxed = true)
    private val launcher: ActivityResultLauncher<Intent> = mockk(relaxed = true)
    private val authTabIntent: AuthTabIntent = mockk(relaxed = true)
    private val customTabsIntent: CustomTabsIntent = mockk(relaxed = true)

    private val intentManager = IntentManagerImpl(
        activity = activity,
        clock = Clock.systemUTC(),
        buildInfoManager = mockk<BuildInfoManager>(),
    )

    @Before
    fun setup() {
        mockkStatic(CustomTabsClient::class)
        mockkConstructor(AuthTabIntent.Builder::class)
        mockkConstructor(CustomTabsIntent.Builder::class)
        every {
            CustomTabsClient.getPackageName(activity, null)
        } returns PROVIDER_PACKAGE_NAME
        every {
            anyConstructed<AuthTabIntent.Builder>().build()
        } returns authTabIntent
        every {
            anyConstructed<CustomTabsIntent.Builder>().build()
        } returns customTabsIntent
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `startAuthTab with supported provider and HttpsScheme launches Auth Tab`() {
        val uri = Uri.parse(CONNECTOR_URL)
        val authTabData = AuthTabData.HttpsScheme(
            host = CALLBACK_HOST,
            path = CALLBACK_PATH,
        )
        every {
            CustomTabsClient.isAuthTabSupported(activity, PROVIDER_PACKAGE_NAME)
        } returns true

        intentManager.startAuthTab(uri, authTabData, launcher)

        verify(exactly = 1) {
            authTabIntent.launch(launcher, uri, CALLBACK_HOST, "\\$CALLBACK_PATH")
        }
        verify(exactly = 0) { activity.startActivity(any<Intent>()) }
    }

    @Test
    fun `startAuthTab with supported provider and CustomScheme launches Auth Tab`() {
        val uri = Uri.parse(CONNECTOR_URL)
        val authTabData = AuthTabData.CustomScheme(callbackUrl = CUSTOM_CALLBACK_URL)
        every {
            CustomTabsClient.isAuthTabSupported(activity, PROVIDER_PACKAGE_NAME)
        } returns true

        intentManager.startAuthTab(uri, authTabData, launcher)

        verify(exactly = 1) {
            authTabIntent.launch(launcher, uri, authTabData.callbackScheme)
        }
        verify(exactly = 0) { activity.startActivity(any<Intent>()) }
    }

    @Test
    fun `startAuthTab with unsupported provider and HttpsScheme launches browser intent`() {
        val uri = Uri.parse(CONNECTOR_URL_WITH_UPPERCASE_SCHEME)
        val startedIntent = mutableListOf<Intent>()
        every { activity.startActivity(capture(startedIntent)) } just runs
        every {
            CustomTabsClient.isAuthTabSupported(activity, PROVIDER_PACKAGE_NAME)
        } returns false

        intentManager.startAuthTab(
            uri = uri,
            authTabData = AuthTabData.HttpsScheme(
                host = CALLBACK_HOST,
                path = CALLBACK_PATH,
            ),
            launcher = launcher,
        )

        assertEquals(Intent.ACTION_VIEW, startedIntent.single().action)
        assertEquals(Uri.parse(CONNECTOR_URL), startedIntent.single().data)
        verify(exactly = 0) { customTabsIntent.launchUrl(any(), any()) }
        verify(exactly = 0) { launcher.launch(any()) }
    }

    @Test
    fun `startAuthTab with unsupported provider and CustomScheme launches Custom Tab`() {
        val uri = Uri.parse(CONNECTOR_URL)
        every {
            CustomTabsClient.isAuthTabSupported(activity, PROVIDER_PACKAGE_NAME)
        } returns false

        intentManager.startAuthTab(
            uri = uri,
            authTabData = AuthTabData.CustomScheme(callbackUrl = CUSTOM_CALLBACK_URL),
            launcher = launcher,
        )

        verify(exactly = 1) { customTabsIntent.launchUrl(activity, uri) }
        verify(exactly = 0) { activity.startActivity(any<Intent>()) }
        verify(exactly = 0) { launcher.launch(any()) }
    }

    @Test
    fun `startAuthTab with unsupported provider and unavailable browser does not crash`() {
        every { activity.startActivity(any<Intent>()) } throws ActivityNotFoundException()
        every {
            CustomTabsClient.isAuthTabSupported(activity, PROVIDER_PACKAGE_NAME)
        } returns false

        intentManager.startAuthTab(
            uri = Uri.parse(CONNECTOR_URL),
            authTabData = AuthTabData.HttpsScheme(
                host = CALLBACK_HOST,
                path = CALLBACK_PATH,
            ),
            launcher = launcher,
        )

        verify(exactly = 0) { launcher.launch(any()) }
    }
}

private const val PROVIDER_PACKAGE_NAME: String = "com.example.browser"
private const val CONNECTOR_URL: String = "https://vault.bitwarden.com/webauthn-connector.html"
private const val CONNECTOR_URL_WITH_UPPERCASE_SCHEME: String =
    "HTTPS://vault.bitwarden.com/webauthn-connector.html"
private const val CALLBACK_HOST: String = "bitwarden.com"
private const val CALLBACK_PATH: String = "webauthn-callback"
private const val CUSTOM_CALLBACK_URL: String = "bitwarden://webauthn-callback"
