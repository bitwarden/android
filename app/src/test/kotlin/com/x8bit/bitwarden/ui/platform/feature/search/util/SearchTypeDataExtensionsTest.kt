package com.x8bit.bitwarden.ui.platform.feature.search.util

import android.net.Uri
import com.bitwarden.collections.CollectionView
import com.bitwarden.core.data.util.toFormattedDateTimeStyle
import com.bitwarden.send.SendView
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.bitwarden.vault.CipherListView
import com.bitwarden.vault.CipherListViewType
import com.bitwarden.vault.CipherRepromptType
import com.bitwarden.vault.CipherType
import com.bitwarden.vault.CopyableCipherFields
import com.bitwarden.vault.FolderView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCardListView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherListView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockLoginListView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSendView
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
import java.time.format.FormatStyle
import java.time.temporal.TemporalAccessor

private const val DEFAULT_FORMATTED_DATE_TIME = "Oct 27, 2023, 12:00 PM"

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
        val ciphers = listOf(createMockCipherListView(number = 1))
        val result = ciphers.filterAndOrganize(
            searchTypeData = SearchTypeData.Vault.Logins,
            searchTerm = "",
        )
        assertEquals(emptyList<CipherListView>(), result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `CipherViews filterAndOrganize should return filtered list when search term is not blank`() {
        val ciphers = listOf(
            createMockCipherListView(number = 1),
            createMockCipherListView(number = 2),
        )
        val result = ciphers.filterAndOrganize(
            searchTypeData = SearchTypeData.Vault.Logins,
            searchTerm = "1",
        )
        assertEquals(listOf(createMockCipherListView(number = 1)), result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `CipherViews filterAndOrganize should return list organized by priority when search term is not blank`() {
        val match1 = createMockCipherListView(number = 1).copy(name = "match1")
        val match2 = createMockCipherListView(number = 2).copy(name = "match2")
        val ciphers = listOf(
            createMockCipherListView(number = 0),
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
        val match1 = createMockCipherListView(number = 1, isDeleted = true).copy(name = "match1")
        val match2 = createMockCipherListView(number = 2).copy(name = "match2")
        val match3 = createMockCipherListView(number = 3, isDeleted = true).copy(name = "match3")
        val ciphers = listOf(match1, match2, match3)
        val result = ciphers.filterAndOrganize(
            searchTypeData = SearchTypeData.Vault.Logins,
            searchTerm = "match",
        )
        assertEquals(listOf(match2), result)
    }

    @Test
    fun `CipherViews filterAndOrganize should return list with only deleted items`() {
        val match1 = createMockCipherListView(number = 1, isDeleted = true).copy(name = "match1")
        val match2 = createMockCipherListView(number = 2).copy(name = "match2")
        val match3 = createMockCipherListView(number = 3, isDeleted = true).copy(name = "match3")
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
            createMockCipherListView(number = 0),
            createMockCipherListView(number = 1),
            createMockCipherListView(number = 2),
        )

        val result = ciphers.toViewState(
            searchTerm = "",
            baseIconUrl = "www.test.com",
            isIconLoadingDisabled = false,
            isAutofill = false,
            hasMasterPassword = true,
            isPremiumUser = true,
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
            createMockCipherListView(number = 0),
            createMockCipherListView(number = 1),
            createMockCipherListView(number = 2),
        )

        val result = sends.toViewState(
            searchTerm = "mock",
            baseIconUrl = "https://vault.bitwarden.com/icons",
            isIconLoadingDisabled = false,
            isAutofill = false,
            hasMasterPassword = true,
            isPremiumUser = true,
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
            createMockCipherListView(
                number = 0,
                type = CipherListViewType.Card(createMockCardListView(number = 0)),
                reprompt = CipherRepromptType.PASSWORD,
                copyableFields = listOf(
                    CopyableCipherFields.CARD_NUMBER,
                    CopyableCipherFields.CARD_SECURITY_CODE,
                ),
            ),
            createMockCipherListView(number = 1),
            createMockCipherListView(number = 2),
        )

        val result = sends.toViewState(
            searchTerm = "mock",
            baseIconUrl = "https://vault.bitwarden.com/icons",
            isIconLoadingDisabled = false,
            isAutofill = true,
            hasMasterPassword = true,
            isPremiumUser = true,
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
        val result = emptyList<CipherListView>().toViewState(
            searchTerm = "a",
            baseIconUrl = "www.test.com",
            isIconLoadingDisabled = false,
            isAutofill = false,
            hasMasterPassword = true,
            isPremiumUser = true,
        )

        assertEquals(
            SearchState.ViewState.Empty(
                message = BitwardenString.there_are_no_items_that_match_the_search.asText(),
            ),
            result,
        )
    }

    @Test
    fun `CipherViews toViewState should usePasskeyDefaultIcon based on cipher fido2 credentials`() {
        mockkStatic(Uri::parse)
        every { Uri.parse(any()) } returns mockk {
            every { host } returns "www.mockuri.com"
        }
        val result = listOf(
            createMockCipherListView(
                number = 1,
                type = CipherListViewType.Login(
                    createMockLoginListView(
                        number = 1,
                        hasFido2 = true,
                    ),
                ),
            ),
            createMockCipherListView(number = 2),
        ).toViewState(
            searchTerm = "mock",
            baseIconUrl = "https://vault.bitwarden.com/icons",
            isIconLoadingDisabled = false,
            hasMasterPassword = true,
            isAutofill = false,
            isPremiumUser = true,
        )

        assertEquals(
            SearchState.ViewState.Content(
                displayItems = listOf(
                    createMockDisplayItemForCipher(
                        number = 1,
                        cipherType = CipherType.LOGIN,
                        fallbackIconRes = BitwardenDrawable.ic_bw_passkey,
                    ),
                    createMockDisplayItemForCipher(number = 2),
                ),
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
        mockkStatic(TemporalAccessor::toFormattedDateTimeStyle) {
            every {
                any<TemporalAccessor>().toFormattedDateTimeStyle(
                    dateStyle = FormatStyle.MEDIUM,
                    timeStyle = FormatStyle.SHORT,
                    clock = clock,
                )
            } returns DEFAULT_FORMATTED_DATE_TIME
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
                message = BitwardenString.there_are_no_items_that_match_the_search.asText(),
            ),
            result,
        )
    }
}
