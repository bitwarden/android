package androidx.credentials.providerevents

import android.annotation.SuppressLint
import android.content.Context
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.providerevents.transfer.ImportCredentialsRequest
import androidx.credentials.providerevents.transfer.ImportCredentialsResponse
import androidx.credentials.providerevents.transfer.ProviderImportCredentialsResponse
import androidx.credentials.providerevents.transfer.RegisterExportRequest
import com.bitwarden.annotation.OmitFromCoverage

/**
 * Placeholder interface representing a provider events manager.
 */
interface ProviderEventsManager {

    /**
     * Register as a credential export source.
     */
    fun registerExport(request: RegisterExportRequest): Boolean

    /**
     * Begin the process of importing credentials.
     */
    fun importCredentials(
        context: Context,
        request: ImportCredentialsRequest,
    ): ProviderImportCredentialsResponse

    @OmitFromCoverage
    @Suppress("UndocumentedPublicClass")
    companion object {
        /**
         * Create a new instance of [ProviderEventsManager].
         */
        fun create(context: Context): ProviderEventsManager = StubProviderEventsManager()
    }
}

/**
 * Stub implementation of [ProviderEventsManager].
 */
@OmitFromCoverage
internal class StubProviderEventsManager : ProviderEventsManager {
    override fun registerExport(request: RegisterExportRequest): Boolean {
        return true
    }

    override fun importCredentials(
        context: Context,
        request: ImportCredentialsRequest,
    ): ProviderImportCredentialsResponse {
        @SuppressLint("VisibleForTests")
        return ProviderImportCredentialsResponse(
            response = ImportCredentialsResponse(
                responseJson = "",
            ),
            callingAppInfo = CallingAppInfo(
                packageName = "",
                signatures = emptyList(),
                origin = null,
            ),
        )
    }
}
