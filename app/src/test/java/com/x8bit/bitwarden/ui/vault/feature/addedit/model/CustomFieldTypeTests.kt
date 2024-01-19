package com.x8bit.bitwarden.ui.vault.feature.addedit.model

import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditState
import com.x8bit.bitwarden.ui.vault.model.VaultLinkedFieldType
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class CustomFieldTypeTests {

    @BeforeEach
    fun setup() {
        mockkStatic(UUID::randomUUID)
        every { UUID.randomUUID().toString() } returns TEST_ID
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(UUID::randomUUID)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toCustomField should return a custom boolean type when we pass in required boolean type`() {
        val name = "test"
        val type = CustomFieldType.BOOLEAN

        val expected = VaultAddEditState.Custom.BooleanField(TEST_ID, "test", false)
        val actual = type.toCustomField(
            name = name,
            itemType = VaultAddEditState.ViewState.Content.ItemType.SecureNotes,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `toCustomField should return a custom linked type when we pass in Login type`() {
        val name = "test"
        val type = CustomFieldType.LINKED

        val expected =
            VaultAddEditState.Custom.LinkedField(TEST_ID, "test", VaultLinkedFieldType.USERNAME)
        val actual = type.toCustomField(
            name = name,
            itemType = VaultAddEditState.ViewState.Content.ItemType.Login(),
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `toCustomField should return a custom linked type when we pass in Identity type`() {
        val name = "test"
        val type = CustomFieldType.LINKED

        val expected =
            VaultAddEditState.Custom.LinkedField(TEST_ID, "test", VaultLinkedFieldType.TITLE)
        val actual = type.toCustomField(
            name = name,
            itemType = VaultAddEditState.ViewState.Content.ItemType.Identity(),
        )

        assertEquals(expected, actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toCustomField should return a custom linked type when we pass in Card type`() {
        val name = "test"
        val type = CustomFieldType.LINKED

        val expected =
            VaultAddEditState.Custom.LinkedField(
                TEST_ID,
                "test",
                VaultLinkedFieldType.CARDHOLDER_NAME,
            )
        val actual = type.toCustomField(
            name = name,
            itemType = VaultAddEditState.ViewState.Content.ItemType.Card(),
        )

        assertEquals(expected, actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toCustomField should return a null custom linked type when we pass in Secure Note type`() {
        val name = "test"
        val type = CustomFieldType.LINKED

        val expected =
            VaultAddEditState.Custom.LinkedField(TEST_ID, "test", null)
        val actual = type.toCustomField(
            name = name,
            itemType = VaultAddEditState.ViewState.Content.ItemType.SecureNotes,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `toCustomField should return a custom texttype when we pass in required text type`() {
        val name = "test"
        val type = CustomFieldType.TEXT

        val expected = VaultAddEditState.Custom.TextField(TEST_ID, "test", "")
        val actual = type.toCustomField(
            name = name,
            itemType = VaultAddEditState.ViewState.Content.ItemType.SecureNotes,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `toCustomField should return a custom hidden type when we pass in required hidden type`() {
        val name = "test"
        val type = CustomFieldType.HIDDEN

        val expected = VaultAddEditState.Custom.HiddenField(TEST_ID, "test", "")
        val actual = type.toCustomField(
            name = name,
            itemType = VaultAddEditState.ViewState.Content.ItemType.SecureNotes,
        )

        assertEquals(expected, actual)
    }
}

private const val TEST_ID = "testID"
