package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import timber.log.Timber

/**
 * Primary implementation of [NativeLibraryManager].
 */
@OmitFromCoverage
class NativeLibraryManagerImpl : NativeLibraryManager {
    override fun loadLibrary(libraryName: String): Result<Unit> {
        return try {
            System.loadLibrary(libraryName)
            Result.success(Unit)
        } catch (e: UnsatisfiedLinkError) {
            Timber.e(e, "Failed to load native library $libraryName.")
            Result.failure(e)
        }
    }
}
