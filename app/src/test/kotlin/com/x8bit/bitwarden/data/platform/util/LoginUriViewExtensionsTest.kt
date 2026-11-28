package com.x8bit.bitwarden.data.platform.util

import com.bitwarden.vault.LoginUriView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockUriView
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LoginUriViewExtensionsTest {

    @Test
    fun `isDesktopAppUri should return true when the uri starts with the desktop app prefix`() {
        assertTrue(
            createMockUriView(number = 1, uri = "desktopapp://com.example.app").isDesktopAppUri,
        )
    }

    @Test
    fun `isDesktopAppUri should return true when the uri is only the desktop app prefix`() {
        assertTrue(createMockUriView(number = 1, uri = "desktopapp://").isDesktopAppUri)
    }

    @Test
    fun `isDesktopAppUri should return false when the uri is null`() {
        val loginUriView = LoginUriView(uri = null, match = null, uriChecksum = null)
        assertFalse(loginUriView.isDesktopAppUri)
    }

    @Test
    fun `isDesktopAppUri should return false when the uri is empty`() {
        assertFalse(createMockUriView(number = 1, uri = "").isDesktopAppUri)
    }

    @Test
    fun `isDesktopAppUri should return false when the uri is a web uri`() {
        assertFalse(createMockUriView(number = 1, uri = "https://www.example.com").isDesktopAppUri)
    }

    @Test
    fun `isDesktopAppUri should return false when the uri is an android app uri`() {
        assertFalse(
            createMockUriView(number = 1, uri = "androidapp://com.example.app").isDesktopAppUri,
        )
    }

    @Test
    fun `isDesktopAppUri should return false when the prefix is not at the start of the uri`() {
        assertFalse(
            createMockUriView(number = 1, uri = "https://desktopapp://com.example").isDesktopAppUri,
        )
    }
}
