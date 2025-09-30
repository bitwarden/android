package com.bitwarden.data.manager

/**
 * Manager for loading native libraries.
 */
interface NativeLibraryManager {

    /**
     * Loads a native library with the given [libraryName].
     */
    fun loadLibrary(libraryName: String): Result<Unit>
}
