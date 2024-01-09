package com.x8bit.bitwarden.data.autofill.model

/**
 * A partition of autofill data.
 */
sealed class AutofillPartition {
    /**
     * The views that correspond to this partition.
     */
    abstract val views: List<AutofillView>

    /**
     * The credit card [AutofillPartition] data.
     */
    data class Card(
        override val views: List<AutofillView.Card>,
    ) : AutofillPartition()

    /**
     * The identity [AutofillPartition] data.
     */
    data class Identity(
        override val views: List<AutofillView.Identity>,
    ) : AutofillPartition()

    /**
     * The login [AutofillPartition] data.
     */
    data class Login(
        override val views: List<AutofillView.Login>,
    ) : AutofillPartition()
}
