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
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.base.util.toListItemCardStyle
import com.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.bitwarden.ui.platform.components.field.BitwardenTextField
import com.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.ui.vault.feature.item.component.itemHeader
import com.x8bit.bitwarden.ui.vault.feature.item.component.vaultItemAttachments
import com.x8bit.bitwarden.ui.vault.feature.item.component.vaultItemCustomFields
import com.x8bit.bitwarden.ui.vault.feature.item.component.vaultItemHistory
import com.x8bit.bitwarden.ui.vault.feature.item.component.vaultItemNotes
import com.x8bit.bitwarden.ui.vault.feature.item.handlers.VaultCommonItemTypeHandlers
import com.x8bit.bitwarden.ui.vault.feature.item.handlers.VaultPassportItemTypeHandlers

/**
 * The top level content UI state for the [VaultItemScreen] when viewing a passport
 * cipher. Each populated field renders as a separate read-only row, matching the
 * Figma View layout. The passport number is rendered with a reveal toggle and an
 * inline copy affordance; the national identification number is rendered with a
 * reveal toggle only. Reveal state is hoisted into [rememberSaveable] so it survives
 * configuration changes and process death, while a visibility action is still
 * dispatched to the ViewModel for telemetry parity with other sensitive-field flows.
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
    var isPassportNumberVisible by rememberSaveable { mutableStateOf(value = false) }
    var isNationalIdentificationNumberVisible by rememberSaveable { mutableStateOf(value = false) }
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

        passportState.firstName?.let { firstName ->
            item(key = "firstName") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.first_name),
                    value = firstName,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = false,
                    textFieldTestTag = "PassportItemFirstNameEntry",
                    cardStyle = passportState
                        .propertyList
                        .toListItemCardStyle(
                            index = passportState.propertyList.indexOf(element = firstName),
                            dividerPadding = 0.dp,
                        ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        passportState.lastName?.let { lastName ->
            item(key = "lastName") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.last_name),
                    value = lastName,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = false,
                    textFieldTestTag = "PassportItemLastNameEntry",
                    cardStyle = passportState
                        .propertyList
                        .toListItemCardStyle(
                            index = passportState.propertyList.indexOf(element = lastName),
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
                    showPassword = isPassportNumberVisible,
                    showPasswordChange = { shouldShow ->
                        isPassportNumberVisible = shouldShow
                        vaultPassportItemTypeHandlers
                            .onPassportNumberVisibilityClick(shouldShow)
                    },
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
                    showPassword = isNationalIdentificationNumberVisible,
                    showPasswordChange = { shouldShow ->
                        isNationalIdentificationNumberVisible = shouldShow
                        vaultPassportItemTypeHandlers
                            .onNationalIdentificationNumberVisibilityClick(shouldShow)
                    },
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
