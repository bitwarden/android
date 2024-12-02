package com.x8bit.bitwarden.ui.platform.feature.search.util

import android.net.Uri
import com.bitwarden.send.SendView
import com.bitwarden.vault.CipherRepromptType
import com.bitwarden.vault.CipherType
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.CollectionView
import com.bitwarden.vault.FolderView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSendView
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.feature.search.SearchState
import com.x8bit.bitwarden.ui.platform.feature.search.SearchTypeData
import com.x8bit.bitwarden.ui.platform.feature.search.model.AutofillSelectionOption
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class SearchTypeDataExtensionsTest {

    private val clock: Clock = Clock.fixed(
        Instant.parse("2023-10-27T12:00:00Z"),
        ZoneOffset.UTC,
    )

    @Test
    fun tearDown() {
        unmockkStatic(Uri::parse)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `updateWithAdditionalDataIfNecessary should update the collection name when searchTypeData is a vault collection`() {
        val collectionId = "collectionId"
        val collectionName = "collectionName"
        val searchTypeData = SearchTypeData.Vault.Collection(
            collectionId = collectionId,
            collectionName = "",
        )
        val collectionView = mockk<CollectionView> {
            every { id } returns collectionId
            every { name } returns collectionName
        }
        assertEquals(
            SearchTypeData.Vault.Collection(
                collectionId = collectionId,
                collectionName = collectionName,
            ),
            searchTypeData.updateWithAdditionalDataIfNecessary(
                folderList = emptyList(),
                collectionList = listOf(collectionView),
            ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `updateWithAdditionalDataIfNecessary should update the folder name when searchTypeData is a vault folder`() {
        val folderId = "folderId"
        val folderName = "folderName"
        val searchTypeData = SearchTypeData.Vault.Folder(
            folderId = folderId,
            folderName = "",
        )
        val folderView = mockk<FolderView> {
            every { id } returns folderId
            every { name } returns folderName
        }
        assertEquals(
            SearchTypeData.Vault.Folder(
                folderId = folderId,
                folderName = folderName,
            ),
            searchTypeData.updateWithAdditionalDataIfNecessary(
                folderList = listOf(folderView),
                collectionList = emptyList(),
            ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `updateWithAdditionalDataIfNecessary should return the searchTypeData unchanged for Sends All`() {
        val searchTypeData = SearchTypeData.Sends.All
        assertEquals(
            searchTypeData,
            searchTypeData.updateWithAdditionalDataIfNecessary(
                folderList = listOf(),
                collectionList = emptyList(),
            ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `updateWithAdditionalDataIfNecessary should return the searchTypeData unchanged for Sends Files`() {
        val searchTypeData = SearchTypeData.Sends.Files
        assertEquals(
            searchTypeData,
            searchTypeData.updateWithAdditionalDataIfNecessary(
                folderList = listOf(),
                collectionList = emptyList(),
            ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `updateWithAdditionalDataIfNecessary should return the searchTypeData unchanged for Sends Texts`() {
        val searchTypeData = SearchTypeData.Sends.Texts
        assertEquals(
            searchTypeData,
            searchTypeData.updateWithAdditionalDataIfNecessary(
                folderList = listOf(),
                collectionList = emptyList(),
            ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `updateWithAdditionalDataIfNecessary should return the searchTypeData unchanged for Vault All`() {
        val searchTypeData = SearchTypeData.Vault.All
        assertEquals(
            searchTypeData,
            searchTypeData.updateWithAdditionalDataIfNecessary(
                folderList = listOf(),
                collectionList = emptyList(),
            ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `updateWithAdditionalDataIfNecessary should return the searchTypeData unchanged for Vault Cards`() {
        val searchTypeData = SearchTypeData.Vault.Cards
        assertEquals(
            searchTypeData,
            searchTypeData.updateWithAdditionalDataIfNecessary(
                folderList = listOf(),
                collectionList = emptyList(),
            ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `updateWithAdditionalDataIfNecessary should return the searchTypeData unchanged for Vault Identities`() {
        val searchTypeData = SearchTypeData.Vault.Identities
        assertEquals(
            searchTypeData,
            searchTypeData.updateWithAdditionalDataIfNecessary(
                folderList = listOf(),
                collectionList = emptyList(),
            ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `updateWithAdditionalDataIfNecessary should return the searchTypeData unchanged for Vault Logins`() {
        val searchTypeData = SearchTypeData.Vault.Logins
        assertEquals(
            searchTypeData,
            searchTypeData.updateWithAdditionalDataIfNecessary(
                folderList = listOf(),
                collectionList = emptyList(),
            ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `updateWithAdditionalDataIfNecessary should return the searchTypeData unchanged for Vault NoFolder`() {
        val searchTypeData = SearchTypeData.Vault.NoFolder
        assertEquals(
            searchTypeData,
            searchTypeData.updateWithAdditionalDataIfNecessary(
                folderList = listOf(),
                collectionList = emptyList(),
            ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `updateWithAdditionalDataIfNecessary should return the searchTypeData unchanged for Vault SecureNotes`() {
        val searchTypeData = SearchTypeData.Vault.SecureNotes
        assertEquals(
            searchTypeData,
            searchTypeData.updateWithAdditionalDataIfNecessary(
                folderList = listOf(),
                collectionList = emptyList(),
            ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `updateWithAdditionalDataIfNecessary should return the searchTypeData unchanged for Vault SshKeys`() {
        val searchTypeData = SearchTypeData.Vault.SshKeys
        assertEquals(
            searchTypeData,
            searchTypeData.updateWithAdditionalDataIfNecessary(
                folderList = listOf(),
                collectionList = emptyList(),
            ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `updateWithAdditionalDataIfNecessary should return the searchTypeData unchanged for Vault VerificationCodes`() {
        val searchTypeData = SearchTypeData.Vault.VerificationCodes
        assertEquals(
            searchTypeData,
            searchTypeData.updateWithAdditionalDataIfNecessary(
                folderList = listOf(),
                collectionList = emptyList(),
            ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `updateWithAdditionalDataIfNecessary should return the searchTypeData unchanged for Vault Trash`() {
        val searchTypeData = SearchTypeData.Vault.Trash
        assertEquals(
            searchTypeData,
            searchTypeData.updateWithAdditionalDataIfNecessary(
                folderList = listOf(),
                collectionList = emptyList(),
            ),
        )
    }

    @Test
    fun `CipherViews filterAndOrganize should return empty list when search term is blank`() {
        val ciphers = listOf(createMockCipherView(number = 1))
        val result = ciphers.filterAndOrganize(
            searchTypeData = SearchTypeData.Vault.Logins,
            searchTerm = "",
        )
        assertEquals(emptyList<CipherView>(), result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `CipherViews filterAndOrganize should return filtered list when search term is not blank`() {
        val ciphers = listOf(
            createMockCipherView(number = 1),
            createMockCipherView(number = 2),
        )
        val result = ciphers.filterAndOrganize(
            searchTypeData = SearchTypeData.Vault.Logins,
            searchTerm = "1",
        )
        assertEquals(listOf(createMockCipherView(number = 1)), result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `CipherViews filterAndOrganize should return list organized by priority when search term is not blank`() {
        val match1 = createMockCipherView(number = 1).copy(name = "match1")
        val match2 = createMockCipherView(number = 2).copy(name = "match2")
        val ciphers = listOf(
            createMockCipherView(number = 0),
            match2,
            match1,
        )
        val result = ciphers.filterAndOrganize(
            searchTypeData = SearchTypeData.Vault.Logins,
            searchTerm = "match",
        )
        assertEquals(listOf(match1, match2), result)
    }

    @Test
    fun `CipherViews filterAndOrganize should return list without deleted items`() {
        val match1 = createMockCipherView(number = 1, isDeleted = true).copy(name = "match1")
        val match2 = createMockCipherView(number = 2).copy(name = "match2")
        val match3 = createMockCipherView(number = 3, isDeleted = true).copy(name = "match3")
        val ciphers = listOf(match1, match2, match3)
        val result = ciphers.filterAndOrganize(
            searchTypeData = SearchTypeData.Vault.Logins,
            searchTerm = "match",
        )
        assertEquals(listOf(match2), result)
    }

    @Test
    fun `CipherViews filterAndOrganize should return list with only deleted items`() {
        val match1 = createMockCipherView(number = 1, isDeleted = true).copy(name = "match1")
        val match2 = createMockCipherView(number = 2).copy(name = "match2")
        val match3 = createMockCipherView(number = 3, isDeleted = true).copy(name = "match3")
        val ciphers = listOf(match1, match2, match3)
        val result = ciphers.filterAndOrganize(
            searchTypeData = SearchTypeData.Vault.Trash,
            searchTerm = "match",
        )
        assertEquals(listOf(match1, match3), result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `CipherViews toViewState should return empty state with no message when search term is blank`() {
        val ciphers = listOf(
            createMockCipherView(number = 0),
            createMockCipherView(number = 1),
            createMockCipherView(number = 2),
        )

        val result = ciphers.toViewState(
            searchTerm = "",
            baseIconUrl = "www.test.com",
            isIconLoadingDisabled = false,
            isAutofill = false,
            hasMasterPassword = true,
            isPremiumUser = true,
            isTotp = true,
            organizationPremiumStatusMap = emptyMap(),
        )

        assertEquals(SearchState.ViewState.Empty(message = null), result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `CipherViews toViewState should return content state when search term is not blank and ciphers is not empty`() {
        mockkStatic(Uri::parse)
        every { Uri.parse(any()) } returns mockk {
            every { host } returns "www.mockuri.com"
        }
        val sends = listOf(
            createMockCipherView(number = 0),
            createMockCipherView(number = 1),
            createMockCipherView(number = 2),
        )

        val result = sends.toViewState(
            searchTerm = "mock",
            baseIconUrl = "https://vault.bitwarden.com/icons",
            isIconLoadingDisabled = false,
            isAutofill = false,
            hasMasterPassword = true,
            isPremiumUser = true,
            isTotp = false,
            organizationPremiumStatusMap = emptyMap(),
        )

        assertEquals(
            SearchState.ViewState.Content(
                displayItems = listOf(
                    createMockDisplayItemForCipher(number = 0),
                    createMockDisplayItemForCipher(number = 1),
                    createMockDisplayItemForCipher(number = 2),
                ),
            ),
            result,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `CipherViews toViewState should return content state for autofill when search term is not blank and ciphers is not empty`() {
        mockkStatic(Uri::parse)
        every { Uri.parse(any()) } returns mockk {
            every { host } returns "www.mockuri.com"
        }
        val sends = listOf(
            createMockCipherView(
                number = 0,
                cipherType = CipherType.CARD,
            )
                .copy(
                    reprompt = CipherRepromptType.PASSWORD,
                ),
            createMockCipherView(number = 1),
            createMockCipherView(number = 2),
        )

        val result = sends.toViewState(
            searchTerm = "mock",
            baseIconUrl = "https://vault.bitwarden.com/icons",
            isIconLoadingDisabled = false,
            isAutofill = true,
            hasMasterPassword = true,
            isPremiumUser = true,
            isTotp = false,
            organizationPremiumStatusMap = emptyMap(),
        )

        assertEquals(
            SearchState.ViewState.Content(
                displayItems = listOf(
                    createMockDisplayItemForCipher(
                        number = 0,
                        cipherType = CipherType.CARD,
                    )
                        .copy(
                            autofillSelectionOptions = listOf(
                                AutofillSelectionOption.AUTOFILL,
                                AutofillSelectionOption.VIEW,
                            ),
                            shouldDisplayMasterPasswordReprompt = true,
                        ),
                    createMockDisplayItemForCipher(number = 1)
                        .copy(
                            autofillSelectionOptions = listOf(
                                AutofillSelectionOption.AUTOFILL,
                                AutofillSelectionOption.AUTOFILL_AND_SAVE,
                                AutofillSelectionOption.VIEW,
                            ),
                            shouldDisplayMasterPasswordReprompt = false,
                        ),
                    createMockDisplayItemForCipher(number = 2)
                        .copy(
                            autofillSelectionOptions = listOf(
                                AutofillSelectionOption.AUTOFILL,
                                AutofillSelectionOption.AUTOFILL_AND_SAVE,
                                AutofillSelectionOption.VIEW,
                            ),
                            shouldDisplayMasterPasswordReprompt = false,
                        ),
                ),
            ),
            result,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `CipherViews toViewState should return empty state with message when search term is not blank and ciphers is empty`() {
        val result = emptyList<CipherView>().toViewState(
            searchTerm = "a",
            baseIconUrl = "www.test.com",
            isIconLoadingDisabled = false,
            isAutofill = false,
            hasMasterPassword = true,
            isPremiumUser = true,
            isTotp = true,
            organizationPremiumStatusMap = emptyMap(),
        )

        assertEquals(
            SearchState.ViewState.Empty(
                message = R.string.there_are_no_items_that_match_the_search.asText(),
            ),
            result,
        )
    }

    @Test
    fun `SendViews filterAndOrganize should return empty list when search term is blank`() {
        val sends = listOf(createMockSendView(number = 1))
        val result = sends.filterAndOrganize(
            searchTypeData = SearchTypeData.Sends.Files,
            searchTerm = "",
        )
        assertEquals(emptyList<SendView>(), result)
    }

    @Test
    fun `SendViews filterAndOrganize should return filtered list when search term is not blank`() {
        val sends = listOf(
            createMockSendView(number = 1),
            createMockSendView(number = 2),
        )
        val result = sends.filterAndOrganize(
            searchTypeData = SearchTypeData.Sends.Files,
            searchTerm = "2",
        )
        assertEquals(
            listOf(createMockSendView(number = 2)),
            result,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `SendViews filterAndOrganize should return list organized by priority when search term is not blank`() {
        val match1 = createMockSendView(number = 1).copy(name = "match1")
        val match2 = createMockSendView(number = 2).copy(name = "match2")
        val ciphers = listOf(
            createMockSendView(number = 0),
            match2,
            match1,
        )
        val result = ciphers.filterAndOrganize(
            searchTypeData = SearchTypeData.Sends.Files,
            searchTerm = "match",
        )
        assertEquals(listOf(match1, match2), result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `SendViews toViewState should return empty state with no message when search term is blank`() {
        val sends = listOf(
            createMockSendView(number = 0),
            createMockSendView(number = 1),
            createMockSendView(number = 2),
        )

        val result = sends.toViewState(
            searchTerm = "",
            baseWebSendUrl = "www,test.com",
            clock = clock,
        )

        assertEquals(SearchState.ViewState.Empty(message = null), result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `SendViews toViewState should return content state when search term is not blank and sends is not empty`() {
        val sends = listOf(
            createMockSendView(number = 0),
            createMockSendView(number = 1),
            createMockSendView(number = 2),
        )

        val result = sends.toViewState(
            searchTerm = "mock",
            baseWebSendUrl = "https://vault.bitwarden.com/#/send/",
            clock = clock,
        )

        assertEquals(
            SearchState.ViewState.Content(
                displayItems = listOf(
                    createMockDisplayItemForSend(number = 0),
                    createMockDisplayItemForSend(number = 1),
                    createMockDisplayItemForSend(number = 2),
                ),
            ),
            result,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `SendViews toViewState should return empty state with message when search term is not blank and sends is empty`() {
        val result = emptyList<SendView>().toViewState(
            searchTerm = "a",
            baseWebSendUrl = "www,test.com",
            clock = clock,
        )

        assertEquals(
            SearchState.ViewState.Empty(
                message = R.string.there_are_no_items_that_match_the_search.asText(),
            ),
            result,
        )
    }
}
