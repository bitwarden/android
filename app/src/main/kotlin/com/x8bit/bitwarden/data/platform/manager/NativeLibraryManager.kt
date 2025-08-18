package com.x8bit.bitwarden.data.platform.manager

/**
 * Manager for loading native libraries.
 */
interface NativeLibraryManager {

    /**
     * Loads a native library with the given [libraryName].
     */
    fun loadLibrary(libraryName: String): Result<Unit>
}
