package com.x8bit.bitwarden.data.autofill.util

import android.annotation.SuppressLint
import android.os.Build
import android.service.autofill.Dataset
import android.service.autofill.Presentations
import android.widget.RemoteViews
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo
import com.x8bit.bitwarden.data.autofill.model.FilledPartition
import com.x8bit.bitwarden.ui.autofill.buildAutofillRemoteViews

/**
 * Build a [Dataset] to represent the [FilledPartition]. This dataset includes an overlay UI
 * presentation for each filled item.
 */
fun FilledPartition.buildDataset(
    autofillAppInfo: AutofillAppInfo,
): Dataset {
    val remoteViewsPlaceholder = buildAutofillRemoteViews(
        packageName = autofillAppInfo.packageName,
        title = autofillCipher.name,
    )
    val datasetBuilder = Dataset.Builder()

    if (autofillAppInfo.sdkInt >= Build.VERSION_CODES.TIRAMISU) {
        applyToDatasetPostTiramisu(
            datasetBuilder = datasetBuilder,
            remoteViews = remoteViewsPlaceholder,
        )
    } else {
        buildDatasetPreTiramisu(
            datasetBuilder = datasetBuilder,
            remoteViews = remoteViewsPlaceholder,
        )
    }

    return datasetBuilder.build()
}

/**
 * Apply this [FilledPartition] to the [datasetBuilder] on devices running OS version Tiramisu or
 * greater.
 */
@SuppressLint("NewApi")
private fun FilledPartition.applyToDatasetPostTiramisu(
    datasetBuilder: Dataset.Builder,
    remoteViews: RemoteViews,
) {
    val presentation = Presentations.Builder()
        .setMenuPresentation(remoteViews)
        .build()

    filledItems.forEach { filledItem ->
        filledItem.applyToDatasetPostTiramisu(
            datasetBuilder = datasetBuilder,
            presentations = presentation,
        )
    }
}

/**
 * Apply this [FilledPartition] to the [datasetBuilder] on devices running OS versions that predate
 * Tiramisu.
 */
private fun FilledPartition.buildDatasetPreTiramisu(
    datasetBuilder: Dataset.Builder,
    remoteViews: RemoteViews,
) {
    filledItems.forEach { filledItem ->
        filledItem.applyToDatasetPreTiramisu(
            datasetBuilder = datasetBuilder,
            remoteViews = remoteViews,
        )
    }
}
