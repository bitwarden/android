package androidx.credentials.providerevents.transfer

import android.graphics.Bitmap

/**
 * Placeholder class representing an entry in the export request.
 */
data class ExportEntry(
    val id: String,
    val accountDisplayName: CharSequence?,
    val userDisplayName: CharSequence,
    val icon: Bitmap,
    val supportedCredentialTypes: Set<String>,
)
