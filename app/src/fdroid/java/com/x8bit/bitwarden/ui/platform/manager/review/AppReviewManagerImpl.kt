package com.x8bit.bitwarden.ui.platform.manager.review

import android.app.Activity

/**
 * No-op implementation of [AppReviewManager] for FDroid builds.
 */
class AppReviewManagerImpl(
    private val activity: Activity,
) : AppReviewManager {
    override fun promptForReview() = Unit
}
