package com.x8bit.bitwarden.ui.platform.manager.snackbar

import android.os.Parcelable
import com.x8bit.bitwarden.ui.platform.components.snackbar.BitwardenSnackbarData
import kotlinx.parcelize.Parcelize
import java.util.UUID

/**
 * Models a relay key to be mapped to an instance of [BitwardenSnackbarData] being sent
 * between producers and consumers of the data.
 */
@Parcelize
data class SnackbarRelay(
    val uuid: UUID,
) : Parcelable {
    @Suppress("UndocumentedPublicClass")
    companion object {
        /**
         * Creates a new instance of [SnackbarRelay] using a random UUID.
         */
        fun create(): SnackbarRelay = SnackbarRelay(UUID.randomUUID())
    }
}
