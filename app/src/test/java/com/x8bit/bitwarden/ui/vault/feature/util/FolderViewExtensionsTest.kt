package com.x8bit.bitwarden.ui.vault.feature.util

import com.bitwarden.vault.FolderView
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class FolderViewExtensionsTest {

    private val clock: Clock = Clock.fixed(
        Instant.parse("2023-10-27T12:00:00Z"),
        ZoneOffset.UTC,
    )

    @Test
    fun `getFolders should get the folders for a folderId with the correct names`() {
        val folderList: List<FolderView> = listOf(
            FolderView("1", "test", clock.instant()),
            FolderView("2", "test/test", clock.instant()),
            FolderView("2", "test/Folder", clock.instant()),
            FolderView("3", "test//", clock.instant()),
            FolderView("4", "test/test/test/", clock.instant()),
            FolderView("5", "Folder", clock.instant()),
        )

        val expected = listOf(
            FolderView("2", "test", clock.instant()),
            FolderView("2", "Folder", clock.instant()),
            FolderView("3", "/", clock.instant()),
        )

        assertEquals(
            expected,
            folderList.getFolders(1.toString()),
        )
    }

    @Test
    fun `getFilteredFolders should properly filter out sub folders in a list`() {
        val folderList: List<FolderView> = listOf(
            FolderView("1", "test", clock.instant()),
            FolderView("2", "test/test", clock.instant()),
            FolderView("3", "test/", clock.instant()),
            FolderView("4", "test/test/test/", clock.instant()),
            FolderView("5", "Folder", clock.instant()),
        )

        val expected = listOf(
            FolderView("1", "test", clock.instant()),
            FolderView("3", "test/", clock.instant()),
            FolderView("5", "Folder", clock.instant()),
        )

        assertEquals(
            expected,
            folderList.getFilteredFolders(),
        )
    }

    @Test
    fun `toFolderDisplayName should return the correct name`() {
        val folderName = "Folder/test/2"

        val folderList: List<FolderView> = listOf(
            FolderView("2", "Folder/test", clock.instant()),
            FolderView("3", "test/", clock.instant()),
            FolderView("4", folderName, clock.instant()),
            FolderView("5", "Folder", clock.instant()),
        )

        assertEquals(
            "2",
            folderName.toFolderDisplayName(folderList),
        )
    }
}
