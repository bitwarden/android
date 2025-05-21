package com.x8bit.bitwarden.ui.platform.manager.review

import android.app.Activity

/**
 * No-op implementation of [AppReviewManager] for F-Droid builds.
 */
class AppReviewManagerImpl(
    activity: Activity,
) : AppReviewManager {
    override fun promptForReview() = Unit
}
