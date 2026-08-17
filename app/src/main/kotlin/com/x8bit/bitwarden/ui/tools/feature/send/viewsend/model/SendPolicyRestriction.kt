package com.x8bit.bitwarden.ui.tools.feature.send.viewsend.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * The reason a Send is restricted by its organization's SendControls policy, which determines the
 * warning shown on the view send screen and whether a compliant copy can be made.
 */
sealed class SendPolicyRestriction : Parcelable {
    /**
     * Whether a compliant copy of the Send can be made to replace it.
     */
    abstract val isCopyable: Boolean

    /**
     * The Send is a file send, so it cannot be brought into compliance: its attachment is not
     * available to upload again.
     */
    @Parcelize
    data object FileNotCompliant : SendPolicyRestriction() {
        override val isCopyable: Boolean get() = false
    }

    /**
     * The Send's type is no longer allowed by the policy, so a copy would be blocked by the same
     * restriction.
     */
    @Parcelize
    data object TypeNotAllowed : SendPolicyRestriction() {
        override val isCopyable: Boolean get() = false
    }

    /**
     * The Send can be replaced by a copy that satisfies the policy.
     */
    @Parcelize
    data object CopyRequired : SendPolicyRestriction() {
        override val isCopyable: Boolean get() = true
    }
}
