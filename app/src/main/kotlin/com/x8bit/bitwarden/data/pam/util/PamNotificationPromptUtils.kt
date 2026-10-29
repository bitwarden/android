package com.x8bit.bitwarden.data.pam.util

import android.os.Build
import com.bitwarden.core.data.manager.BuildInfoManager
import com.bitwarden.core.util.isBuildVersionAtLeast
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository

/**
 * Whether the user should still be asked to allow the PAM alert notifications. The question is
 * asked once per install, no matter which PAM screen asked it, and only where it can matter:
 * F-Droid has no push and before Android 13 there is no runtime permission.
 */
fun shouldShowPamNotificationPrompt(
    buildInfoManager: BuildInfoManager,
    settingsRepository: SettingsRepository,
): Boolean = !buildInfoManager.isFdroid &&
    isBuildVersionAtLeast(Build.VERSION_CODES.TIRAMISU) &&
    !settingsRepository.hasShownPamNotificationPrompt
