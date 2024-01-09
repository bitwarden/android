package com.x8bit.bitwarden.data.autofill.util

import android.annotation.SuppressLint
import android.os.Build
import android.service.autofill.Dataset
import android.service.autofill.Field
import android.service.autofill.Presentations
import android.view.autofill.AutofillId
import android.widget.RemoteViews
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo
import com.x8bit.bitwarden.data.autofill.model.FilledItem

/**
 * Apply this [FilledItem] to the dataset being built by [datasetBuilder] in the form of an
 * overlay presentation.
 */
fun FilledItem.applyOverlayToDataset(
    appInfo: AutofillAppInfo,
    datasetBuilder: Dataset.Builder,
    remoteViews: RemoteViews,
) {
    if (appInfo.sdkInt >= Build.VERSION_CODES.TIRAMISU) {
        setOverlay(
            autoFillId = autofillId,
            datasetBuilder = datasetBuilder,
            remoteViews = remoteViews,
        )
    } else {
        setOverlayPreTiramisu(
            autoFillId = autofillId,
            datasetBuilder = datasetBuilder,
            remoteViews = remoteViews,
        )
    }
}

/**
 * Set up an overlay presentation in the [datasetBuilder] for Android devices running on API
 * Tiramisu or greater.
 */
@SuppressLint("NewApi")
private fun setOverlay(
    autoFillId: AutofillId,
    datasetBuilder: Dataset.Builder,
    remoteViews: RemoteViews,
) {
    val presentation = Presentations.Builder()
        .setDialogPresentation(remoteViews)
        .build()

    datasetBuilder.setField(
        autoFillId,
        Field.Builder()
            .setPresentations(presentation)
            .build(),
    )
}

/**
 * Set up an overlay presentation in the [datasetBuilder] for Android devices running on APIs that
 * predate Tiramisu.
 */
@Suppress("Deprecation")
private fun setOverlayPreTiramisu(
    autoFillId: AutofillId,
    datasetBuilder: Dataset.Builder,
    remoteViews: RemoteViews,
) {
    datasetBuilder.setValue(
        autoFillId,
        null,
        remoteViews,
    )
}
