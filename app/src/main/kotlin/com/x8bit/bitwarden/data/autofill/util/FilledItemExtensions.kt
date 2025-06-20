package com.x8bit.bitwarden.data.autofill.util

import android.annotation.SuppressLint
import android.service.autofill.Dataset
import android.service.autofill.Field
import android.service.autofill.Presentations
import android.widget.RemoteViews
import com.x8bit.bitwarden.data.autofill.model.FilledItem

/**
 * Set up an overlay presentation for this [FilledItem] in the [datasetBuilder] for Android devices
 * running on API Tiramisu or greater.
 */
@SuppressLint("NewApi")
fun FilledItem.applyToDatasetPostTiramisu(
    datasetBuilder: Dataset.Builder,
    presentations: Presentations,
) {
    datasetBuilder.setField(
        autofillId,
        Field.Builder()
            .setValue(value)
            .setPresentations(presentations)
            .build(),
    )
}

/**
 * Set up an overlay presentation for this [FilledItem] in the [datasetBuilder] for Android devices
 * running on APIs that predate Tiramisu.
 */
@Suppress("Deprecation")
fun FilledItem.applyToDatasetPreTiramisu(
    datasetBuilder: Dataset.Builder,
    remoteViews: RemoteViews,
) {
    datasetBuilder.setValue(
        autofillId,
        value,
        remoteViews,
    )
}
