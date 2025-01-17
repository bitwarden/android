package com.x8bit.bitwarden.ui.platform.feature.settings.about

import android.os.Build
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.BuildConfig
import com.x8bit.bitwarden.data.platform.manager.LogsManager
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.repository.util.FakeEnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.util.baseWebVaultUrlOrDefault
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.concat
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.Year
import java.time.ZoneId

class AboutViewModelTest : BaseViewModelTest() {

    private val environmentRepository = FakeEnvironmentRepository()
    private val clipboardManager: BitwardenClipboardManager = mockk()
    private val logsManager: LogsManager = mockk {
        every { isEnabled } returns false
        every { isEnabled = any() } just runs
    }

    @Test
    fun `on BackClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AboutAction.BackClick)
            assertEquals(AboutEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `on GiveFeedbackClick should emit NavigateToFeedbackForm`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AboutAction.GiveFeedbackClick)
            assertEquals(AboutEvent.NavigateToFeedbackForm, awaitItem())
        }
    }

    @Test
    fun `on HelpCenterClick should emit NavigateToHelpCenter`() = runTest {
        val viewModel = createViewModel(DEFAULT_ABOUT_STATE)
        viewModel.eventFlow.test {
            viewModel.trySendAction(AboutAction.HelpCenterClick)
            assertEquals(AboutEvent.NavigateToHelpCenter, awaitItem())
        }
    }

    @Test
    fun `on PrivacyPolicyClick should emit NavigateToPrivacyPolicy`() = runTest {
        val viewModel = createViewModel(DEFAULT_ABOUT_STATE)
        viewModel.eventFlow.test {
            viewModel.trySendAction(AboutAction.PrivacyPolicyClick)
            assertEquals(AboutEvent.NavigateToPrivacyPolicy, awaitItem())
        }
    }

    @Test
    fun `on LearnAboutOrganizationsClick should emit NavigateToLearnAboutOrganizations`() =
        runTest {
            val viewModel = createViewModel(DEFAULT_ABOUT_STATE)
            viewModel.eventFlow.test {
                viewModel.trySendAction(AboutAction.LearnAboutOrganizationsClick)
                assertEquals(AboutEvent.NavigateToLearnAboutOrganizations, awaitItem())
            }
        }

    @Test
    fun `on RateAppClick should emit NavigateToRateApp`() = runTest {
        val viewModel = createViewModel(DEFAULT_ABOUT_STATE)
        viewModel.eventFlow.test {
            viewModel.trySendAction(AboutAction.RateAppClick)
            assertEquals(AboutEvent.NavigateToRateApp, awaitItem())
        }
    }

    @Test
    fun `on SubmitCrashLogsClick should update isSubmitCrashLogsEnabled to true`() = runTest {
        val viewModel = createViewModel(DEFAULT_ABOUT_STATE)
        assertFalse(viewModel.stateFlow.value.isSubmitCrashLogsEnabled)
        viewModel.trySendAction(AboutAction.SubmitCrashLogsClick(true))
        assertTrue(viewModel.stateFlow.value.isSubmitCrashLogsEnabled)
    }

    @Test
    fun `on SubmitCrashLogsClick should update LogsManager isEnabled`() = runTest {
        val viewModel = createViewModel(DEFAULT_ABOUT_STATE)

        viewModel.trySendAction(AboutAction.SubmitCrashLogsClick(true))

        coVerify(exactly = 1) { logsManager.isEnabled = true }
    }

    @Test
    fun `on VersionClick should call setText on the ClipboardManager with specific Text`() {
        val versionName = BuildConfig.VERSION_NAME
        val versionCode = BuildConfig.VERSION_CODE

        val deviceBrandModel = "\uD83D\uDCF1 ${Build.BRAND} ${Build.MODEL}"
        val osInfo = "\uD83E\uDD16 ${Build.VERSION.RELEASE}@${Build.VERSION.SDK_INT}"
        val buildInfo = "\uD83D\uDCE6 dev"
        val ciInfo = BuildConfig.CI_INFO

        val expectedText = "© Bitwarden Inc. 2015-"
            .asText()
            .concat(Year.now(fixedClock).value.toString().asText())
            .concat("\n\n".asText())
            .concat("Version: $versionName ($versionCode)".asText())
            .concat("\n".asText())
            .concat("$deviceBrandModel $osInfo $buildInfo".asText())
            .concat(
                "\n$ciInfo"
                    .takeUnless { ciInfo.isEmpty() }
                    .orEmpty()
                    .asText(),
            )

        every { clipboardManager.setText(expectedText, true, null) } just runs

        val viewModel = createViewModel(DEFAULT_ABOUT_STATE)
        viewModel.trySendAction(AboutAction.VersionClick)

        verify(exactly = 1) {
            clipboardManager.setText(expectedText, ofType(Boolean::class), isNull())
        }
    }

    @Test
    fun `on WebVaultClick should emit NavigateToWebVault`() = runTest {
        val viewModel = createViewModel(DEFAULT_ABOUT_STATE)
        viewModel.eventFlow.test {
            viewModel.trySendAction(AboutAction.WebVaultClick)
            assertEquals(
                AboutEvent.NavigateToWebVault(
                    vaultUrl = environmentRepository
                        .environment
                        .environmentUrlData
                        .baseWebVaultUrlOrDefault,
                ),
                awaitItem(),
            )
        }
    }

    private fun createViewModel(
        state: AboutState? = null,
    ): AboutViewModel = AboutViewModel(
        savedStateHandle = SavedStateHandle().apply { set("state", state) },
        clipboardManager = clipboardManager,
        clock = fixedClock,
        environmentRepository = environmentRepository,
        logsManager = logsManager,
    )
}

private val fixedClock = Clock.fixed(
    Instant.parse("2024-01-25T10:15:30.00Z"),
    ZoneId.systemDefault(),
)
private val DEFAULT_ABOUT_STATE: AboutState = AboutState(
    version = "Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})".asText(),
    isSubmitCrashLogsEnabled = false,
    copyrightInfo = "© Bitwarden Inc. 2015-"
        .asText()
        .concat(Year.now(fixedClock).value.toString().asText()),
    shouldShowCrashLogsButton = true,
)
