package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilityEnabledManager
import com.x8bit.bitwarden.data.autofill.manager.AutofillEnabledManager
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.ui.platform.util.orZero

private const val ADD_ACTION_REQUIREMENT = 3
private const val COPY_ACTION_REQUIREMENT = 3
private const val CREATE_ACTION_REQUIREMENT = 3

/**
 * Default implementation of [ReviewPromptManager].
 */
class ReviewPromptManagerImpl(
    private val authDiskSource: AuthDiskSource,
    private val settingsDiskSource: SettingsDiskSource,
    private val autofillEnabledManager: AutofillEnabledManager,
    private val accessibilityEnabledManager: AccessibilityEnabledManager,
) : ReviewPromptManager {

    override fun registerAddCipherAction() {
        authDiskSource.userState?.activeUserId ?: return
        if (isMinimumAddActionsMet()) return
        val currentValue = settingsDiskSource.getAddCipherActionCount().orZero()
        settingsDiskSource.storeAddCipherActionCount(
            count = currentValue + 1,
        )
    }

    override fun registerGeneratedResultAction() {
        authDiskSource.userState?.activeUserId ?: return
        if (isMinimumCopyActionsMet()) return
        val currentValue = settingsDiskSource
            .getGeneratedResultActionCount()
            .orZero()
        settingsDiskSource.storeGeneratedResultActionCount(
            count = currentValue + 1,
        )
    }

    override fun registerCreateSendAction() {
        authDiskSource.userState?.activeUserId ?: return
        if (isMinimumCreateActionsMet()) return
        val currentValue = settingsDiskSource.getCreateSendActionCount().orZero()
        settingsDiskSource.storeCreateSendActionCount(
            count = currentValue + 1,
        )
    }

    override fun shouldPromptForAppReview(): Boolean {
        authDiskSource.userState?.activeUserId ?: return false
        val autofillEnabled = autofillEnabledManager.isAutofillEnabledStateFlow.value
        val accessibilityEnabled = accessibilityEnabledManager.isAccessibilityEnabledStateFlow.value
        val minAddActionsMet = isMinimumAddActionsMet()
        val minCopyActionsMet = isMinimumCopyActionsMet()
        val minCreateActionsMet = isMinimumCreateActionsMet()
        return (autofillEnabled || accessibilityEnabled) &&
            (minAddActionsMet || minCopyActionsMet || minCreateActionsMet)
    }

    private fun isMinimumAddActionsMet(): Boolean =
        settingsDiskSource.getAddCipherActionCount().orZero() >= ADD_ACTION_REQUIREMENT

    private fun isMinimumCopyActionsMet(): Boolean =
        settingsDiskSource
            .getGeneratedResultActionCount()
            .orZero() >= COPY_ACTION_REQUIREMENT

    private fun isMinimumCreateActionsMet(): Boolean =
        settingsDiskSource.getCreateSendActionCount().orZero() >= CREATE_ACTION_REQUIREMENT
}
