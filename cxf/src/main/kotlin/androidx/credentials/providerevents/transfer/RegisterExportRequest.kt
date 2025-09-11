package androidx.credentials.providerevents.transfer

/**
 * Placeholder class representing a request to register as a credential export source.
 */
data class RegisterExportRequest(
    val entries: List<ExportEntry>,
)
