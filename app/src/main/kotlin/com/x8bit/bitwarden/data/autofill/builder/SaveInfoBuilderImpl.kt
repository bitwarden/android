package com.x8bit.bitwarden.data.autofill.builder

import android.service.autofill.FillRequest
import android.service.autofill.SaveInfo
import com.x8bit.bitwarden.data.autofill.model.AutofillPartition
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import timber.log.Timber

/**
 * The primary implementation of [SaveInfoBuilder].This is used for converting autofill data into
 * a save info.
 */
class SaveInfoBuilderImpl(
    val settingsRepository: SettingsRepository,
) : SaveInfoBuilder {

    override fun build(
        autofillPartition: AutofillPartition,
        fillRequest: FillRequest,
        packageName: String?,
    ): SaveInfo? {
        Timber.d("Autofill request constructing SaveInfo -- ${fillRequest.id}")
        // Make sure that the save prompt is possible.
        val partitionType = autofillPartition::class.java.simpleName
        if (settingsRepository.isAutofillSavePromptDisabled) {
            Timber.d(
                "Autofill save skipped: SAVE_INFO_DISABLED partition=$partitionType " +
                    "-- ${fillRequest.id}",
            )
            return null
        }
        if (!autofillPartition.canPerformSaveRequest) {
            Timber.d(
                "Autofill save skipped: SAVE_INFO_NO_PASSWORD_FIELD partition=$partitionType " +
                    "-- ${fillRequest.id}",
            )
            return null
        }

        // Docs state that password fields cannot be reliably saved
        // in Compat mode since they show as masked values.
        val isInCompatMode = (fillRequest.flags or
            FillRequest.FLAG_COMPATIBILITY_MODE_REQUEST) == fillRequest.flags
        Timber.d("Autofill request isInCompatMode=$isInCompatMode -- ${fillRequest.id}")

        // If login and compat mode, the password might be obfuscated,
        // in which case we should skip the save request.
        return if (autofillPartition is AutofillPartition.Login && isInCompatMode) {
            Timber.d("Autofill save skipped: SAVE_INFO_COMPAT_LOGIN -- ${fillRequest.id}")
            null
        } else {
            SaveInfo
                .Builder(
                    autofillPartition.saveType,
                    autofillPartition.requiredSaveIds.toTypedArray(),
                )
                .apply {
                    // setOptionalIds will throw an IllegalArgumentException if the array is empty
                    autofillPartition
                        .optionalSaveIds
                        .takeUnless { it.isEmpty() }
                        ?.let { setOptionalIds(it.toTypedArray()) }
                    if (isInCompatMode) setFlags(SaveInfo.FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE)
                }
                .build()
        }
    }
}
