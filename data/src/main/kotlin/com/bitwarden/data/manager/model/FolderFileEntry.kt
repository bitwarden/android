package com.bitwarden.data.manager.model

/**
 * Represents a single file entry within a folder, with its [relativePath] from the folder root
 * and its absolute [diskPath] on the local file system.
 */
data class FolderDiskEntry(
    val relativePath: String,
    val diskPath: String,
)
