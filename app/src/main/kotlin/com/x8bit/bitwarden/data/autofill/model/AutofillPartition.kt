package com.x8bit.bitwarden.data.autofill.model

import android.service.autofill.SaveInfo
import android.view.autofill.AutofillId

/**
 * A partition of autofill data.
 */
sealed class AutofillPartition {

    /**
     * [AutofillId]s that are optional for save requests. For example, with cards we require a
     * phone number too trigger the save request, other card data is optional.
     */
    abstract val optionalSaveIds: List<AutofillId>

    /**
     * [AutofillId]s that are required for save requests. If there are no required fields present,
     * then save requests aren't allowed.
     */
    abstract val requiredSaveIds: List<AutofillId>

    /**
     * The autofill save associated with this [AutofillPartition].
     */
    abstract val saveType: Int

    /**
     * The views that correspond to this partition.
     */
    abstract val views: List<AutofillView>

    /**
     * Whether it is possible to perform a save request with this [AutofillPartition].
     */
    val canPerformSaveRequest: Boolean
        get() = requiredSaveIds.isNotEmpty()

    /**
     * The credit card [AutofillPartition] data.
     */
    data class Card(
        override val views: List<AutofillView.Card>,
    ) : AutofillPartition() {
        override val optionalSaveIds: List<AutofillId>
            get() = views
                .filter { it !is AutofillView.Card.Number }
                .map { it.data.autofillId }
        override val requiredSaveIds: List<AutofillId>
            get() = views
                .filterIsInstance<AutofillView.Card.Number>()
                .map { it.data.autofillId }

        override val saveType: Int
            get() = SaveInfo.SAVE_DATA_TYPE_CREDIT_CARD
    }

    /**
     * The login [AutofillPartition] data.
     */
    data class Login(
        override val views: List<AutofillView.Login>,
    ) : AutofillPartition() {
        override val optionalSaveIds: List<AutofillId>
            get() = views
                .filter { it !is AutofillView.Login.Password }
                .map { it.data.autofillId }
        override val requiredSaveIds: List<AutofillId>
            get() = views
                .filterIsInstance<AutofillView.Login.Password>()
                .map { it.data.autofillId }
        override val saveType: Int
            get() = SaveInfo.SAVE_DATA_TYPE_PASSWORD
    }
}
