package com.x8bit.bitwarden.data.autofill.builder

import android.os.Build
import android.service.autofill.FillRequest
import android.service.autofill.SaveInfo
import android.view.autofill.AutofillId
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo
import com.x8bit.bitwarden.data.autofill.model.AutofillPartition
import com.x8bit.bitwarden.data.autofill.model.AutofillView
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.util.mockBuilder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SaveInfoBuilderTest {
    private lateinit var saveInfoBuilder: SaveInfoBuilder

    private val settingsRepository: SettingsRepository = mockk()

    private val autofillAppInfo: AutofillAppInfo = mockk()
    private val fillRequest: FillRequest = mockk()
    private val autofillIdOptional: AutofillId = mockk()
    private val autofillViewDataOptional = AutofillView.Data(
        autofillId = autofillIdOptional,
        isFocused = true,
        textValue = null,
    )
    private val autofillIdValid: AutofillId = mockk()
    private val autofillViewDataValid = AutofillView.Data(
        autofillId = autofillIdValid,
        isFocused = true,
        textValue = null,
    )
    private val autofillPartitionCard: AutofillPartition.Card = AutofillPartition.Card(
        views = listOf(
            AutofillView.Card.Number(
                data = autofillViewDataValid,
            ),
            AutofillView.Card.SecurityCode(
                data = autofillViewDataOptional,
            ),
        ),
    )
    private val autofillPartitionLogin: AutofillPartition.Login = AutofillPartition.Login(
        views = listOf(
            AutofillView.Login.Password(
                data = autofillViewDataValid,
            ),
            AutofillView.Login.Username(
                data = autofillViewDataOptional,
            ),
        ),
    )
    private val saveInfo: SaveInfo = mockk()

    @BeforeEach
    fun setup() {
        mockkConstructor(SaveInfo.Builder::class)
        saveInfoBuilder = SaveInfoBuilderImpl(
            settingsRepository = settingsRepository,
        )
        every { anyConstructed<SaveInfo.Builder>().build() } returns saveInfo
    }

    @AfterEach
    fun teardown() {
        unmockkConstructor(SaveInfo.Builder::class)
    }

    @Test
    fun `build should return null if autofill disabled`() {
        // Setup
        every { settingsRepository.isAutofillSavePromptDisabled } returns true

        // Test
        val actual = saveInfoBuilder.build(
            autofillAppInfo = autofillAppInfo,
            autofillPartition = autofillPartitionCard,
            fillRequest = fillRequest,
            packageName = PACKAGE_NAME,
        )

        // Verify
        assertNull(actual)
    }

    @Test
    fun `build should return null if autofill enabled and can't perform autofill`() {
        // Setup
        every { settingsRepository.isAutofillSavePromptDisabled } returns false

        // Test
        val actual = saveInfoBuilder.build(
            autofillAppInfo = autofillAppInfo,
            autofillPartition = AUTOFILL_PARTITION_LOGIN_EMPTY,
            fillRequest = fillRequest,
            packageName = PACKAGE_NAME,
        )

        // Verify
        assertNull(actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `build should return null if autofill possible but flags indicate compat mode and is login`() {
        // Setup
        every { settingsRepository.isAutofillSavePromptDisabled } returns false
        every { fillRequest.flags } returns FillRequest.FLAG_COMPATIBILITY_MODE_REQUEST
        every { autofillAppInfo.sdkInt } returns Build.VERSION_CODES.TIRAMISU

        // Test
        val actual = saveInfoBuilder.build(
            autofillAppInfo = autofillAppInfo,
            autofillPartition = autofillPartitionLogin,
            fillRequest = fillRequest,
            packageName = PACKAGE_NAME,
        )

        // Verify
        assertNull(actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `build should return null if autofill possible but package name is in compat list and is login`() {
        // Setup
        every { settingsRepository.isAutofillSavePromptDisabled } returns false
        every { autofillAppInfo.sdkInt } returns Build.VERSION_CODES.P

        // Test
        COMPAT_BROWSERS
            .forEach { packageName ->
                val actual = saveInfoBuilder.build(
                    autofillAppInfo = autofillAppInfo,
                    autofillPartition = autofillPartitionLogin,
                    fillRequest = fillRequest,
                    packageName = packageName,
                )

                // Verify
                assertNull(actual)
            }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `build should return SaveInfo with flag set if autofill possible, flags indicate compat mode, and is card`() {
        // Setup
        every { settingsRepository.isAutofillSavePromptDisabled } returns false
        every { fillRequest.flags } returns FillRequest.FLAG_COMPATIBILITY_MODE_REQUEST
        every { autofillAppInfo.sdkInt } returns Build.VERSION_CODES.TIRAMISU
        mockBuilder<SaveInfo.Builder> {
            it.setOptionalIds(arrayOf(autofillIdOptional))
        }
        mockBuilder<SaveInfo.Builder> {
            it.setFlags(SaveInfo.FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE)
        }

        // Test
        val actual = saveInfoBuilder.build(
            autofillAppInfo = autofillAppInfo,
            autofillPartition = autofillPartitionCard,
            fillRequest = fillRequest,
            packageName = PACKAGE_NAME,
        )

        // Verify
        assertEquals(saveInfo, actual)
        verify(exactly = 1) {
            anyConstructed<SaveInfo.Builder>().setOptionalIds(arrayOf(autofillIdOptional))
            anyConstructed<SaveInfo.Builder>().setFlags(SaveInfo.FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `build should return SaveInfo if autofill possible, packageName is not in compat list, and is login`() {
        // Setup
        every { settingsRepository.isAutofillSavePromptDisabled } returns false
        every { autofillAppInfo.sdkInt } returns Build.VERSION_CODES.P
        mockBuilder<SaveInfo.Builder> {
            it.setOptionalIds(arrayOf(autofillIdOptional))
        }

        // Test
        val actual = saveInfoBuilder.build(
            autofillAppInfo = autofillAppInfo,
            autofillPartition = autofillPartitionLogin,
            fillRequest = fillRequest,
            packageName = PACKAGE_NAME,
        )

        // Verify
        assertEquals(saveInfo, actual)
        verify(exactly = 1) {
            anyConstructed<SaveInfo.Builder>().setOptionalIds(arrayOf(autofillIdOptional))
        }
    }
}

private const val PACKAGE_NAME: String = "com.google"
private val AUTOFILL_PARTITION_LOGIN_EMPTY: AutofillPartition.Login = AutofillPartition.Login(
    views = listOf(),
)
private val COMPAT_BROWSERS: List<String> = listOf(
    "alook.browser",
    "alook.browser.google",
    "app.vanadium.browser",
    "com.amazon.cloud9",
    "com.android.browser",
    "com.android.chrome",
    "com.android.htmlviewer",
    "com.avast.android.secure.browser",
    "com.avg.android.secure.browser",
    "com.brave.browser",
    "com.brave.browser_beta",
    "com.brave.browser_default",
    "com.brave.browser_dev",
    "com.brave.browser_nightly",
    "com.chrome.beta",
    "com.chrome.canary",
    "com.chrome.dev",
    "com.cookiegames.smartcookie",
    "com.cookiejarapps.android.smartcookieweb",
    "com.ecosia.android",
    "com.google.android.apps.chrome",
    "com.google.android.apps.chrome_dev",
    "com.google.android.captiveportallogin",
    "com.iode.firefox",
    "com.jamal2367.styx",
    "com.kiwibrowser.browser",
    "com.kiwibrowser.browser.dev",
    "com.lemurbrowser.exts",
    "com.microsoft.emmx",
    "com.microsoft.emmx.beta",
    "com.microsoft.emmx.canary",
    "com.microsoft.emmx.dev",
    "com.mmbox.browser",
    "com.mmbox.xbrowser",
    "com.mycompany.app.soulbrowser",
    "com.naver.whale",
    "com.neeva.app",
    "com.opera.browser",
    "com.opera.browser.beta",
    "com.opera.gx",
    "com.opera.mini.native",
    "com.opera.mini.native.beta",
    "com.opera.touch",
    "com.qflair.browserq",
    "com.qwant.liberty",
    "com.rainsee.create",
    "com.sec.android.app.sbrowser",
    "com.sec.android.app.sbrowser.beta",
    "com.stoutner.privacybrowser.free",
    "com.stoutner.privacybrowser.standard",
    "com.vivaldi.browser",
    "com.vivaldi.browser.snapshot",
    "com.vivaldi.browser.sopranos",
    "com.yandex.browser",
    "com.yjllq.internet",
    "com.yjllq.kito",
    "com.yujian.ResideMenuDemo",
    "com.z28j.feel",
    "idm.internet.download.manager",
    "idm.internet.download.manager.adm.lite",
    "idm.internet.download.manager.plus",
    "io.github.forkmaintainers.iceraven",
    "mark.via",
    "mark.via.gp",
    "net.dezor.browser",
    "net.slions.fulguris.full.download",
    "net.slions.fulguris.full.download.debug",
    "net.slions.fulguris.full.playstore",
    "net.slions.fulguris.full.playstore.debug",
    "org.adblockplus.browser",
    "org.adblockplus.browser.beta",
    "org.bromite.bromite",
    "org.bromite.chromium",
    "org.chromium.chrome",
    "org.codeaurora.swe.browser",
    "org.cromite.cromite",
    "org.gnu.icecat",
    "org.mozilla.fenix",
    "org.mozilla.fenix.nightly",
    "org.mozilla.fennec_aurora",
    "org.mozilla.fennec_fdroid",
    "org.mozilla.firefox",
    "org.mozilla.firefox_beta",
    "org.mozilla.reference.browser",
    "org.mozilla.rocket",
    "org.torproject.torbrowser",
    "org.torproject.torbrowser_alpha",
    "org.ungoogled.chromium.extensions.stable",
    "org.ungoogled.chromium.stable",
    "us.spotco.fennec_dos",
)
