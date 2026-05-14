package com.x8bit.bitwarden.ui.vault.feature.item

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.base.util.toListItemCardStyle
import com.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.bitwarden.ui.platform.components.field.BitwardenTextField
import com.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.ui.vault.feature.item.component.itemHeader
import com.x8bit.bitwarden.ui.vault.feature.item.component.vaultItemAttachments
import com.x8bit.bitwarden.ui.vault.feature.item.component.vaultItemCustomFields
import com.x8bit.bitwarden.ui.vault.feature.item.component.vaultItemHistory
import com.x8bit.bitwarden.ui.vault.feature.item.component.vaultItemNotes
import com.x8bit.bitwarden.ui.vault.feature.item.handlers.VaultCommonItemTypeHandlers
import com.x8bit.bitwarden.ui.vault.feature.item.handlers.VaultPassportItemTypeHandlers
import kotlinx.collections.immutable.persistentListOf

/**
 * The top level content UI state for the [VaultItemScreen] when viewing a passport
 * cipher. Each populated field renders as a separate read-only row. The passport
 * number and national identification number render with a reveal toggle and an
 * inline copy affordance.
 */
@Suppress("LongMethod")
@Composable
fun VaultItemPassportContent(
    commonState: VaultItemState.ViewState.Content.Common,
    passportState: VaultItemState.ViewState.Content.ItemType.Passport,
    vaultCommonItemTypeHandlers: VaultCommonItemTypeHandlers,
    vaultPassportItemTypeHandlers: VaultPassportItemTypeHandlers,
    modifier: Modifier = Modifier,
) {
    var isExpanded by rememberSaveable { mutableStateOf(value = false) }
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        item {
            Spacer(Modifier.height(height = 12.dp))
        }
        itemHeader(
            value = commonState.name,
            isFavorite = commonState.favorite,
            isArchived = commonState.archived,
            iconData = commonState.iconData,
            relatedLocations = commonState.relatedLocations,
            iconTestTag = "PassportItemNameIcon",
            textFieldTestTag = "PassportItemNameEntry",
            isExpanded = isExpanded,
            onExpandClick = { isExpanded = !isExpanded },
            applyIconBackground = commonState.iconData is IconData.Local,
        )
        item(key = "passportDetailsHeader") {
            Spacer(modifier = Modifier.height(height = 16.dp))
            BitwardenListHeaderText(
                label = stringResource(id = BitwardenString.passport_details),
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .padding(horizontal = 16.dp)
                    .animateItem(),
            )
            Spacer(modifier = Modifier.height(height = 8.dp))
        }

        passportState.givenName?.let { givenName ->
            item(key = "givenName") {
                PassportCopyField(
                    label = stringResource(id = BitwardenString.first_name),
                    value = givenName,
                    copyContentDescription = stringResource(id = BitwardenString.copy_first_name),
                    textFieldTestTag = "PassportItemGivenNameEntry",
                    copyActionTestTag = "PassportCopyGivenNameButton",
                    onCopyClick = vaultPassportItemTypeHandlers.onCopyGivenNameClick,
                    cardStyle = passportState
                        .propertyList
                        .toListItemCardStyle(
                            index = passportState.propertyList.indexOf(element = givenName),
                            dividerPadding = 0.dp,
                        ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        passportState.surname?.let { surname ->
            item(key = "surname") {
                PassportCopyField(
                    label = stringResource(id = BitwardenString.last_name),
                    value = surname,
                    copyContentDescription = stringResource(id = BitwardenString.copy_last_name),
                    textFieldTestTag = "PassportItemSurnameEntry",
                    copyActionTestTag = "PassportCopySurnameButton",
                    onCopyClick = vaultPassportItemTypeHandlers.onCopySurnameClick,
                    cardStyle = passportState
                        .propertyList
                        .toListItemCardStyle(
                            index = passportState.propertyList.indexOf(element = surname),
                            dividerPadding = 0.dp,
                        ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        passportState.dateOfBirth?.let { dateOfBirth ->
            item(key = "dateOfBirth") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.date_of_birth),
                    value = dateOfBirth,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = false,
                    textFieldTestTag = "PassportItemDateOfBirthEntry",
                    cardStyle = passportState
                        .propertyList
                        .toListItemCardStyle(
                            index = passportState.propertyList.indexOf(element = dateOfBirth),
                            dividerPadding = 0.dp,
                        ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        passportState.sex?.let { sex ->
            item(key = "sex") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.sex),
                    value = sex,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = false,
                    textFieldTestTag = "PassportItemSexEntry",
                    cardStyle = passportState
                        .propertyList
                        .toListItemCardStyle(
                            index = passportState.propertyList.indexOf(element = sex),
                            dividerPadding = 0.dp,
                        ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        passportState.birthPlace?.let { birthPlace ->
            item(key = "birthPlace") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.birth_place),
                    value = birthPlace,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = false,
                    textFieldTestTag = "PassportItemBirthPlaceEntry",
                    cardStyle = passportState
                        .propertyList
                        .toListItemCardStyle(
                            index = passportState.propertyList.indexOf(element = birthPlace),
                            dividerPadding = 0.dp,
                        ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        passportState.nationality?.let { nationality ->
            item(key = "nationality") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.nationality),
                    value = nationality,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = false,
                    textFieldTestTag = "PassportItemNationalityEntry",
                    cardStyle = passportState
                        .propertyList
                        .toListItemCardStyle(
                            index = passportState.propertyList.indexOf(element = nationality),
                            dividerPadding = 0.dp,
                        ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        passportState.passportNumber?.let { passportNumber ->
            item(key = "passportNumber") {
                BitwardenPasswordField(
                    label = stringResource(id = BitwardenString.passport_number),
                    value = passportNumber,
                    onValueChange = {},
                    readOnly = true,
                    supportingContent = null,
                    actions = {
                        BitwardenStandardIconButton(
                            vectorIconRes = BitwardenDrawable.ic_copy,
                            contentDescription = stringResource(
                                id = BitwardenString.copy_passport_number,
                            ),
                            onClick = vaultPassportItemTypeHandlers.onCopyPassportNumberClick,
                            modifier = Modifier.testTag(
                                tag = "PassportCopyPassportNumberButton",
                            ),
                        )
                    },
                    passwordFieldTestTag = "PassportItemPassportNumberEntry",
                    showPasswordTestTag = "PassportViewPassportNumberButton",
                    cardStyle = passportState
                        .propertyList
                        .toListItemCardStyle(
                            index = passportState
                                .propertyList
                                .indexOf(element = passportNumber),
                            dividerPadding = 0.dp,
                        ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        passportState.passportType?.let { passportType ->
            item(key = "passportType") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.passport_type),
                    value = passportType,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = false,
                    textFieldTestTag = "PassportItemPassportTypeEntry",
                    cardStyle = passportState
                        .propertyList
                        .toListItemCardStyle(
                            index = passportState.propertyList.indexOf(element = passportType),
                            dividerPadding = 0.dp,
                        ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        passportState.nationalIdentificationNumber?.let { nationalIdentificationNumber ->
            item(key = "nationalIdentificationNumber") {
                BitwardenPasswordField(
                    label = stringResource(
                        id = BitwardenString.national_identification_number,
                    ),
                    value = nationalIdentificationNumber,
                    onValueChange = {},
                    readOnly = true,
                    supportingContent = null,
                    actions = {
                        BitwardenStandardIconButton(
                            vectorIconRes = BitwardenDrawable.ic_copy,
                            contentDescription = stringResource(
                                id = BitwardenString.copy_national_identification_number,
                            ),
                            onClick = vaultPassportItemTypeHandlers
                                .onCopyNationalIdentificationNumberClick,
                            modifier = Modifier.testTag(
                                tag = "PassportCopyNationalIdentificationNumberButton",
                            ),
                        )
                    },
                    passwordFieldTestTag = "PassportItemNationalIdentificationNumberEntry",
                    showPasswordTestTag = "PassportViewNationalIdentificationNumberButton",
                    cardStyle = passportState
                        .propertyList
                        .toListItemCardStyle(
                            index = passportState
                                .propertyList
                                .indexOf(element = nationalIdentificationNumber),
                            dividerPadding = 0.dp,
                        ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        passportState.issuingCountry?.let { issuingCountry ->
            item(key = "issuingCountry") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.issuing_country),
                    value = issuingCountry,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = false,
                    textFieldTestTag = "PassportItemIssuingCountryEntry",
                    cardStyle = passportState
                        .propertyList
                        .toListItemCardStyle(
                            index = passportState.propertyList.indexOf(element = issuingCountry),
                            dividerPadding = 0.dp,
                        ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        passportState.issuingAuthority?.let { issuingAuthority ->
            item(key = "issuingAuthority") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.issuing_authority),
                    value = issuingAuthority,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = false,
                    textFieldTestTag = "PassportItemIssuingAuthorityEntry",
                    cardStyle = passportState
                        .propertyList
                        .toListItemCardStyle(
                            index = passportState
                                .propertyList
                                .indexOf(element = issuingAuthority),
                            dividerPadding = 0.dp,
                        ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        passportState.issueDate?.let { issueDate ->
            item(key = "issueDate") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.issue_date),
                    value = issueDate,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = false,
                    textFieldTestTag = "PassportItemIssueDateEntry",
                    cardStyle = passportState
                        .propertyList
                        .toListItemCardStyle(
                            index = passportState.propertyList.indexOf(element = issueDate),
                            dividerPadding = 0.dp,
                        ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        passportState.expirationDate?.let { expirationDate ->
            item(key = "expirationDate") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.expiration_date),
                    value = expirationDate,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = false,
                    textFieldTestTag = "PassportItemExpirationDateEntry",
                    cardStyle = passportState
                        .propertyList
                        .toListItemCardStyle(
                            index = passportState.propertyList.indexOf(element = expirationDate),
                            dividerPadding = 0.dp,
                        ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        vaultItemNotes(
            notes = commonState.notes,
            vaultCommonItemTypeHandlers = vaultCommonItemTypeHandlers,
        )

        vaultItemCustomFields(
            customFields = commonState.customFields,
            vaultCommonItemTypeHandlers = vaultCommonItemTypeHandlers,
        )

        vaultItemAttachments(
            attachments = commonState.attachments,
            vaultCommonItemTypeHandlers = vaultCommonItemTypeHandlers,
        )

        vaultItemHistory(
            commonState = commonState,
            vaultCommonItemTypeHandlers = vaultCommonItemTypeHandlers,
            loginPasswordRevisionDate = null,
        )

        item {
            Spacer(modifier = Modifier.height(88.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun PassportCopyField(
    label: String,
    value: String,
    copyContentDescription: String,
    textFieldTestTag: String,
    copyActionTestTag: String,
    onCopyClick: () -> Unit,
    cardStyle: CardStyle,
    modifier: Modifier = Modifier,
) {
    BitwardenTextField(
        label = label,
        value = value,
        onValueChange = {},
        readOnly = true,
        singleLine = false,
        actions = {
            BitwardenStandardIconButton(
                vectorIconRes = BitwardenDrawable.ic_copy,
                contentDescription = copyContentDescription,
                onClick = onCopyClick,
                modifier = Modifier.testTag(tag = copyActionTestTag),
            )
        },
        textFieldTestTag = textFieldTestTag,
        cardStyle = cardStyle,
        modifier = modifier,
    )
}

//region Previews
@Composable
@Preview(showBackground = true, heightDp = 1200)
private fun VaultItemPassportContent_Preview() {
    BitwardenTheme {
        BitwardenScaffold() {
            VaultItemPassportContent(
                commonState = PREVIEW_COMMON,
                passportState = PREVIEW_PASSPORT,
                vaultCommonItemTypeHandlers = PREVIEW_COMMON_HANDLERS,
                vaultPassportItemTypeHandlers = PREVIEW_PASSPORT_HANDLERS,
            )
        }
    }
}

private val PREVIEW_COMMON: VaultItemState.ViewState.Content.Common =
    VaultItemState.ViewState.Content.Common(
        name = "Passport",
        created = "May 13, 2026, 12:00 PM".asText(),
        lastUpdated = "May 13, 2026, 12:00 PM".asText(),
        notes = "Recovery code: 12323234324",
        customFields = persistentListOf(),
        requiresCloneConfirmation = false,
        currentCipher = null,
        attachments = persistentListOf(),
        canDelete = true,
        canRestore = false,
        canAssignToCollections = true,
        canEdit = true,
        favorite = false,
        archived = false,
        passwordHistoryCount = null,
        iconData = IconData.Local(iconRes = BitwardenDrawable.ic_passport),
        relatedLocations = persistentListOf(),
        hasOrganizations = false,
    )

private val PREVIEW_PASSPORT: VaultItemState.ViewState.Content.ItemType.Passport =
    VaultItemState.ViewState.Content.ItemType.Passport(
        givenName = "Mitchell Allen",
        surname = "Johnson",
        dateOfBirth = "August 10, 1990",
        sex = "Male",
        birthPlace = "Madison, Wisconsin",
        nationality = "United States of America",
        passportNumber = "P12345678",
        passportType = "P",
        nationalIdentificationNumber = "N-987-654-321",
        issuingCountry = "United States of America",
        issuingAuthority = "Department of State",
        issueDate = "August 10, 2021",
        expirationDate = "August 10, 2031",
    )

private val PREVIEW_COMMON_HANDLERS: VaultCommonItemTypeHandlers =
    VaultCommonItemTypeHandlers(
        onRefreshClick = {},
        onCopyCustomHiddenField = {},
        onCopyCustomTextField = {},
        onShowHiddenFieldClick = { _, _ -> },
        onAttachmentDownloadClick = {},
        onAttachmentPreviewClick = {},
        onCopyNotesClick = {},
        onPasswordHistoryClick = {},
        onUpgradeToPremiumClick = {},
    )

private val PREVIEW_PASSPORT_HANDLERS: VaultPassportItemTypeHandlers =
    VaultPassportItemTypeHandlers(
        onCopyGivenNameClick = {},
        onCopySurnameClick = {},
        onCopyPassportNumberClick = {},
        onCopyNationalIdentificationNumberClick = {},
    )
//endregion Previews
