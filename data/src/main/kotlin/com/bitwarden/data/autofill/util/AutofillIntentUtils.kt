@file:OmitFromCoverage

package com.bitwarden.data.autofill.util

import android.app.Activity
import android.content.Intent
import com.bitwarden.annotation.OmitFromCoverage

/**
 * A unique key used to set or get Autofill saved item data.
 */
const val AUTOFILL_SAVE_ITEM_DATA_KEY = "autofill-save-item-data"

/**
 * A unique key used to set or get Autofill selection data.
 */
const val AUTOFILL_SELECTION_DATA_KEY = "autofill-selection-data"

/**
 * A unique key used to set or get Autofill callback data.
 */
const val AUTOFILL_CALLBACK_DATA_KEY = "autofill-callback-data"

/**
 * A unique key used to set and get the `Bundle` containing autofill data.
 */
const val AUTOFILL_BUNDLE_KEY = "autofill-bundle-key"

/**
 * Checks if the given [Intent] contains an `AutofillSaveItem` related to an ongoing save item
 * process.
 */
private val Intent.hasAutofillSaveItem: Boolean
    get() = this.hasExtra(AUTOFILL_SAVE_ITEM_DATA_KEY)

/**
 * Checks if the given [Intent] contains data about an ongoing manual autofill selection process.
 */
private val Intent.hasAutofillSelectionData: Boolean
    get() = getBundleExtra(AUTOFILL_BUNDLE_KEY)
        ?.containsKey(AUTOFILL_SELECTION_DATA_KEY) == true

/**
 * Checks if the given [Intent] contains Autofill callback data.
 */
private val Intent.hasAutofillCallbackIntent: Boolean
    get() = getBundleExtra(AUTOFILL_BUNDLE_KEY)?.containsKey(AUTOFILL_CALLBACK_DATA_KEY) == true

/**
 * Checks if the given [Activity] was created for Autofill. This is useful to avoid locking the
 * vault if one of the Autofill services starts the only instance of the `Activity`.
 */
val Activity.createdForAutofill: Boolean
    get() = intent.hasAutofillSelectionData ||
        intent.hasAutofillSaveItem ||
        intent.hasAutofillCallbackIntent
