package com.x8bit.bitwarden.data.auth.manager.onboarding

import com.bitwarden.core.data.manager.biometrics.BiometricsSupportManager
import com.bitwarden.core.data.manager.model.FlagKey
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.autofill.manager.browser.BrowserThirdPartyAutofillEnabledManager
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.Test

class OnboardingManagerTest {

    private val fakeAuthDiskSource = FakeAuthDiskSource().apply {
        userState = USER_STATE
    }
    private val biometricsSupportManager: BiometricsSupportManager = mockk {
        every { isBiometricsSupported(isDeviceCredentialAllowed = false) } returns false
    }
    private var isDefaultBrowserAvailableAndDisabled: Boolean = false
    private val browserThirdPartyAutofillEnabledManager: BrowserThirdPartyAutofillEnabledManager =
        mockk {
            every { browserThirdPartyAutofillStatus } answers {
                mockk {
                    every {
                        isDefaultBrowserAvailableAndDisabled
                    } returns this@OnboardingManagerTest.isDefaultBrowserAvailableAndDisabled
                }
            }
        }
    private val mutableIsAutofillEnabledStateFlow = MutableStateFlow(true)
    private val settingsRepository: SettingsRepository = mockk {
        every { isAutofillEnabledStateFlow } returns mutableIsAutofillEnabledStateFlow
    }
    private val featureFlagManager: FeatureFlagManager = mockk {
        every { getFeatureFlag(key = FlagKey.UpdatedOnboardingFlow) } returns true
    }

    private val onboardingManager: OnboardingManager = OnboardingManagerImpl(
        authDiskSource = fakeAuthDiskSource,
        biometricsSupportManager = biometricsSupportManager,
        browserThirdPartyAutofillEnabledManager = browserThirdPartyAutofillEnabledManager,
        settingsRepository = settingsRepository,
        featureFlagManager = featureFlagManager,
    )

    //region initializeOnboardingFlow

    @Test
    fun `initializeOnboardingFlow with UpdatedOnboardingFlow disabled should not set a status`() {
        every {
            featureFlagManager.getFeatureFlag(key = FlagKey.UpdatedOnboardingFlow)
        } returns false

        onboardingManager.initializeOnboardingFlow(userId = USER_ID)

        fakeAuthDiskSource.assertOnboardingStatus(userId = USER_ID, onboardingStatus = null)
        verify(exactly = 0) {
            biometricsSupportManager.isBiometricsSupported(isDeviceCredentialAllowed = any())
        }
    }

    @Test
    fun `initializeOnboardingFlow with existing onboarding status should not change the status`() {
        every {
            biometricsSupportManager.isBiometricsSupported(isDeviceCredentialAllowed = false)
        } returns true
        fakeAuthDiskSource.storeOnboardingStatus(
            userId = USER_ID,
            onboardingStatus = OnboardingStatus.AUTOFILL_SETUP,
        )

        onboardingManager.initializeOnboardingFlow(userId = USER_ID)

        fakeAuthDiskSource.assertOnboardingStatus(
            userId = USER_ID,
            onboardingStatus = OnboardingStatus.AUTOFILL_SETUP,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `initializeOnboardingFlow when biometrics is supported but not setup should set status to ACCOUNT_LOCK_SETUP`() {
        every {
            biometricsSupportManager.isBiometricsSupported(isDeviceCredentialAllowed = false)
        } returns true
        mutableIsAutofillEnabledStateFlow.value = false
        isDefaultBrowserAvailableAndDisabled = true

        onboardingManager.initializeOnboardingFlow(userId = USER_ID)

        fakeAuthDiskSource.assertOnboardingStatus(
            userId = USER_ID,
            onboardingStatus = OnboardingStatus.ACCOUNT_LOCK_SETUP,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `initializeOnboardingFlow when biometrics is setup and autofill is disabled should set status to AUTOFILL_SETUP`() {
        every {
            biometricsSupportManager.isBiometricsSupported(isDeviceCredentialAllowed = false)
        } returns true
        fakeAuthDiskSource.storeUserBiometricUnlockKey(
            userId = USER_ID,
            biometricsKey = "biometricsKey",
        )
        mutableIsAutofillEnabledStateFlow.value = false
        isDefaultBrowserAvailableAndDisabled = true

        onboardingManager.initializeOnboardingFlow(userId = USER_ID)

        fakeAuthDiskSource.assertOnboardingStatus(
            userId = USER_ID,
            onboardingStatus = OnboardingStatus.AUTOFILL_SETUP,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `initializeOnboardingFlow when biometrics is not supported and autofill is disabled should set status to AUTOFILL_SETUP`() {
        mutableIsAutofillEnabledStateFlow.value = false

        onboardingManager.initializeOnboardingFlow(userId = USER_ID)

        fakeAuthDiskSource.assertOnboardingStatus(
            userId = USER_ID,
            onboardingStatus = OnboardingStatus.AUTOFILL_SETUP,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `initializeOnboardingFlow when autofill is enabled and browser autofill is disabled should set status to BROWSER_AUTOFILL_SETUP`() {
        isDefaultBrowserAvailableAndDisabled = true

        onboardingManager.initializeOnboardingFlow(userId = USER_ID)

        fakeAuthDiskSource.assertOnboardingStatus(
            userId = USER_ID,
            onboardingStatus = OnboardingStatus.BROWSER_AUTOFILL_SETUP,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `initializeOnboardingFlow when everything is already configured should set status to COMPLETE`() {
        onboardingManager.initializeOnboardingFlow(userId = USER_ID)

        fakeAuthDiskSource.assertOnboardingStatus(
            userId = USER_ID,
            onboardingStatus = OnboardingStatus.COMPLETE,
        )
    }

    //endregion initializeOnboardingFlow

    //region goToNextStep

    @Test
    fun `goToNextStep with no active user should not change any status`() {
        fakeAuthDiskSource.storeOnboardingStatus(
            userId = USER_ID,
            onboardingStatus = OnboardingStatus.FINAL_STEP,
        )
        fakeAuthDiskSource.userState = null

        onboardingManager.goToNextStep()

        fakeAuthDiskSource.assertOnboardingStatus(
            userId = USER_ID,
            onboardingStatus = OnboardingStatus.FINAL_STEP,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `goToNextStep from ACCOUNT_LOCK_SETUP when autofill is disabled should set status to AUTOFILL_SETUP`() {
        fakeAuthDiskSource.storeOnboardingStatus(
            userId = USER_ID,
            onboardingStatus = OnboardingStatus.ACCOUNT_LOCK_SETUP,
        )
        mutableIsAutofillEnabledStateFlow.value = false
        isDefaultBrowserAvailableAndDisabled = true

        onboardingManager.goToNextStep()

        fakeAuthDiskSource.assertOnboardingStatus(
            userId = USER_ID,
            onboardingStatus = OnboardingStatus.AUTOFILL_SETUP,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `goToNextStep from ACCOUNT_LOCK_SETUP when autofill is enabled and browser autofill is disabled should set status to BROWSER_AUTOFILL_SETUP`() {
        fakeAuthDiskSource.storeOnboardingStatus(
            userId = USER_ID,
            onboardingStatus = OnboardingStatus.ACCOUNT_LOCK_SETUP,
        )
        isDefaultBrowserAvailableAndDisabled = true

        onboardingManager.goToNextStep()

        fakeAuthDiskSource.assertOnboardingStatus(
            userId = USER_ID,
            onboardingStatus = OnboardingStatus.BROWSER_AUTOFILL_SETUP,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `goToNextStep from ACCOUNT_LOCK_SETUP when autofill and browser autofill are enabled should set status to FINAL_STEP`() {
        fakeAuthDiskSource.storeOnboardingStatus(
            userId = USER_ID,
            onboardingStatus = OnboardingStatus.ACCOUNT_LOCK_SETUP,
        )

        onboardingManager.goToNextStep()

        fakeAuthDiskSource.assertOnboardingStatus(
            userId = USER_ID,
            onboardingStatus = OnboardingStatus.FINAL_STEP,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `goToNextStep from AUTOFILL_SETUP when autofill is enabled and browser autofill is disabled should set status to BROWSER_AUTOFILL_SETUP`() {
        fakeAuthDiskSource.storeOnboardingStatus(
            userId = USER_ID,
            onboardingStatus = OnboardingStatus.AUTOFILL_SETUP,
        )
        isDefaultBrowserAvailableAndDisabled = true

        onboardingManager.goToNextStep()

        fakeAuthDiskSource.assertOnboardingStatus(
            userId = USER_ID,
            onboardingStatus = OnboardingStatus.BROWSER_AUTOFILL_SETUP,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `goToNextStep from AUTOFILL_SETUP when autofill is disabled should set status to FINAL_STEP`() {
        fakeAuthDiskSource.storeOnboardingStatus(
            userId = USER_ID,
            onboardingStatus = OnboardingStatus.AUTOFILL_SETUP,
        )
        mutableIsAutofillEnabledStateFlow.value = false
        isDefaultBrowserAvailableAndDisabled = true

        onboardingManager.goToNextStep()

        fakeAuthDiskSource.assertOnboardingStatus(
            userId = USER_ID,
            onboardingStatus = OnboardingStatus.FINAL_STEP,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `goToNextStep from AUTOFILL_SETUP when autofill and browser autofill are enabled should set status to FINAL_STEP`() {
        fakeAuthDiskSource.storeOnboardingStatus(
            userId = USER_ID,
            onboardingStatus = OnboardingStatus.AUTOFILL_SETUP,
        )

        onboardingManager.goToNextStep()

        fakeAuthDiskSource.assertOnboardingStatus(
            userId = USER_ID,
            onboardingStatus = OnboardingStatus.FINAL_STEP,
        )
    }

    @Test
    fun `goToNextStep from BROWSER_AUTOFILL_SETUP should set status to FINAL_STEP`() {
        fakeAuthDiskSource.storeOnboardingStatus(
            userId = USER_ID,
            onboardingStatus = OnboardingStatus.BROWSER_AUTOFILL_SETUP,
        )
        mutableIsAutofillEnabledStateFlow.value = false
        isDefaultBrowserAvailableAndDisabled = true

        onboardingManager.goToNextStep()

        fakeAuthDiskSource.assertOnboardingStatus(
            userId = USER_ID,
            onboardingStatus = OnboardingStatus.FINAL_STEP,
        )
    }

    @Test
    fun `goToNextStep from FINAL_STEP should set status to COMPLETE`() {
        fakeAuthDiskSource.storeOnboardingStatus(
            userId = USER_ID,
            onboardingStatus = OnboardingStatus.FINAL_STEP,
        )

        onboardingManager.goToNextStep()

        fakeAuthDiskSource.assertOnboardingStatus(
            userId = USER_ID,
            onboardingStatus = OnboardingStatus.COMPLETE,
        )
    }

    @Test
    fun `goToNextStep from COMPLETE should leave status as COMPLETE`() {
        fakeAuthDiskSource.storeOnboardingStatus(
            userId = USER_ID,
            onboardingStatus = OnboardingStatus.COMPLETE,
        )
        mutableIsAutofillEnabledStateFlow.value = false

        onboardingManager.goToNextStep()

        fakeAuthDiskSource.assertOnboardingStatus(
            userId = USER_ID,
            onboardingStatus = OnboardingStatus.COMPLETE,
        )
    }

    @Test
    fun `goToNextStep with no existing status should set the initial onboarding status`() {
        every {
            biometricsSupportManager.isBiometricsSupported(isDeviceCredentialAllowed = false)
        } returns true

        onboardingManager.goToNextStep()

        fakeAuthDiskSource.assertOnboardingStatus(
            userId = USER_ID,
            onboardingStatus = OnboardingStatus.ACCOUNT_LOCK_SETUP,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `goToNextStep with no existing status and UpdatedOnboardingFlow disabled should still set the initial onboarding status`() {
        every {
            featureFlagManager.getFeatureFlag(key = FlagKey.UpdatedOnboardingFlow)
        } returns false
        mutableIsAutofillEnabledStateFlow.value = false

        onboardingManager.goToNextStep()

        fakeAuthDiskSource.assertOnboardingStatus(
            userId = USER_ID,
            onboardingStatus = OnboardingStatus.AUTOFILL_SETUP,
        )
        verify(exactly = 0) {
            featureFlagManager.getFeatureFlag(key = FlagKey.UpdatedOnboardingFlow)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `goToNextStep with no existing status when everything is already configured should set status to COMPLETE`() {
        onboardingManager.goToNextStep()

        fakeAuthDiskSource.assertOnboardingStatus(
            userId = USER_ID,
            onboardingStatus = OnboardingStatus.COMPLETE,
        )
    }

    //endregion goToNextStep

    //region setOnboardingStatus

    @Test
    fun `setOnboardingStatus should save the onboarding status for the active user`() {
        onboardingManager.setOnboardingStatus(status = OnboardingStatus.ACCOUNT_LOCK_SETUP)

        fakeAuthDiskSource.assertOnboardingStatus(
            userId = USER_ID,
            onboardingStatus = OnboardingStatus.ACCOUNT_LOCK_SETUP,
        )
    }

    @Test
    fun `setOnboardingStatus with no active user should not change any status`() {
        fakeAuthDiskSource.storeOnboardingStatus(
            userId = USER_ID,
            onboardingStatus = OnboardingStatus.ACCOUNT_LOCK_SETUP,
        )
        fakeAuthDiskSource.userState = null

        onboardingManager.setOnboardingStatus(status = OnboardingStatus.COMPLETE)

        fakeAuthDiskSource.assertOnboardingStatus(
            userId = USER_ID,
            onboardingStatus = OnboardingStatus.ACCOUNT_LOCK_SETUP,
        )
    }

    //endregion setOnboardingStatus
}

private const val USER_ID: String = "userId"

private val USER_STATE: UserStateJson = mockk {
    every { activeUserId } returns USER_ID
}
