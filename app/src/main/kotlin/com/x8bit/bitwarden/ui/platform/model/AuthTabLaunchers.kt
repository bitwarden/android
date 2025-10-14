package com.x8bit.bitwarden.ui.platform.model

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Immutable

/**
 * Contains all the callbacks for the Auth Tabs.
 */
@Immutable
class AuthTabLaunchers(
    val duo: ActivityResultLauncher<Intent>,
    val sso: ActivityResultLauncher<Intent>,
    val webAuthn: ActivityResultLauncher<Intent>,
)
