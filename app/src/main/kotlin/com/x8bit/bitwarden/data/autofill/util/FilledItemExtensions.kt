package com.x8bit.bitwarden.data.autofill.util

import android.os.Build
import android.service.autofill.Dataset
import android.service.autofill.Field
import android.service.autofill.Presentations
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import com.x8bit.bitwarden.data.autofill.model.FilledItem

/**
 * Set up an overlay presentation for this [FilledItem] in the [datasetBuilder] for Android devices
 * running on API Tiramisu or greater.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
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
fun FilledItem.applyToDatasetPreTiramisu(
    datasetBuilder: Dataset.Builder,
    remoteViews: RemoteViews,
) {
    @Suppress("DEPRECATION")
    datasetBuilder.setValue(
        autofillId,
        value,
        remoteViews,
    )
}
