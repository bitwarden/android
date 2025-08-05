package com.x8bit.bitwarden.ui.vault.feature.addedit

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import com.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.data.platform.repository.model.UriMatchType
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenSelectionDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.row.BitwardenBasicDialogRow
import com.x8bit.bitwarden.ui.platform.components.dropdown.BitwardenMultiSelectDialogContent
import com.x8bit.bitwarden.ui.platform.components.dropdown.MultiSelectOption
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.composition.LocalIntentManager
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.util.displayLabel
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.util.isAdvancedMatching
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.UriItem
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.UriMatchDisplayType
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.displayLabel
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.isAdvancedMatching
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.toDisplayMatchType
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.toUriMatchType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * The URI item displayed to the user.
 */
@Suppress("LongMethod")
@Composable
fun VaultAddEditUriItem(
    uriItem: UriItem,
    onUriItemRemoved: (UriItem) -> Unit,
    onUriValueChange: (UriItem) -> Unit,
    cardStyle: CardStyle,
    defaultUriMatchType: UriMatchType,
    modifier: Modifier = Modifier,
    resources: Resources = LocalContext.current.resources,
    intentManager: IntentManager = LocalIntentManager.current,
) {
    var shouldShowOptionsDialog by rememberSaveable { mutableStateOf(false) }
    var shouldShowMatchDialog by rememberSaveable { mutableStateOf(false) }
    var shouldShowAdvancedMatchDialog by rememberSaveable { mutableStateOf(false) }
    var optionPendingConfirmation by rememberSaveable {
        mutableStateOf<UriMatchDisplayType?>(
            null,
        )
    }
    var shouldShowLearnMoreMatchDetectionDialog by rememberSaveable { mutableStateOf(false) }

    BitwardenTextField(
        label = stringResource(id = BitwardenString.website_uri),
        value = uriItem.uri.orEmpty(),
        onValueChange = { onUriValueChange(uriItem.copy(uri = it)) },
        actions = {
            BitwardenStandardIconButton(
                vectorIconRes = BitwardenDrawable.ic_cog,
                contentDescription = stringResource(id = BitwardenString.options),
                onClick = { shouldShowOptionsDialog = true },
                modifier = Modifier.testTag(tag = "LoginUriOptionsButton"),
            )
        },
        textFieldTestTag = "LoginUriEntry",
        cardStyle = cardStyle,
        modifier = modifier,
    )

    if (shouldShowOptionsDialog) {
        BitwardenSelectionDialog(
            title = stringResource(id = BitwardenString.options),
            onDismissRequest = { shouldShowOptionsDialog = false },
        ) {
            BitwardenBasicDialogRow(
                text = stringResource(id = BitwardenString.match_detection),
                onClick = {
                    shouldShowOptionsDialog = false
                    shouldShowMatchDialog = true
                },
            )
            BitwardenBasicDialogRow(
                text = stringResource(id = BitwardenString.remove),
                onClick = {
                    shouldShowOptionsDialog = false
                    onUriItemRemoved(uriItem)
                },
            )
        }
    }

    if (shouldShowMatchDialog) {
        BitwardenSelectionDialog(
            title = stringResource(id = BitwardenString.uri_match_detection),
            onDismissRequest = { shouldShowMatchDialog = false },
        ) {
            BitwardenMultiSelectDialogContent(
                options = UriMatchDisplayType
                    .entries
                    .filter { !it.isAdvancedMatching() }
                    .map {
                        it
                            .displayLabel(
                                defaultUriOption = defaultUriMatchType
                                    .displayLabel
                                    .invoke(),
                            )
                            .invoke()
                    }
                    .toImmutableList(),
                selectedOption = uriItem
                    .match
                    .toDisplayMatchType()
                    .displayLabel(
                        defaultUriOption = defaultUriMatchType
                            .displayLabel
                            .invoke(),
                    )
                    .invoke(),
                sectionOptions = buildSubSectionsList(),
                onOptionSelected = { selectedOption ->
                    shouldShowMatchDialog = false

                    val newSelectedType =
                        UriMatchDisplayType
                            .entries
                            .first {
                                it.text.invoke(resources) == selectedOption
                            }

                    if (newSelectedType.isAdvancedMatching()) {
                        optionPendingConfirmation = newSelectedType
                        shouldShowAdvancedMatchDialog = true
                    } else {
                        onUriValueChange(
                            uriItem.copy(match = newSelectedType.toUriMatchType()),
                        )
                        optionPendingConfirmation = null
                    }
                },
            )
        }
    }

    val currentOptionToConfirm = optionPendingConfirmation

    if (shouldShowAdvancedMatchDialog && currentOptionToConfirm != null) {
        BuildAdvancedMatchDetectionWarning(
            pendingOption = currentOptionToConfirm,
            defaultUriMatchType = defaultUriMatchType,
            onDialogConfirm = {
                onUriValueChange(
                    uriItem.copy(match = currentOptionToConfirm.toUriMatchType()),
                )
                shouldShowAdvancedMatchDialog = false
                optionPendingConfirmation = null
                shouldShowLearnMoreMatchDetectionDialog = true
            },
            onDialogDismiss = {
                shouldShowAdvancedMatchDialog = false
                optionPendingConfirmation = null
            },
        )
    }

    if (shouldShowLearnMoreMatchDetectionDialog) {
        BuildLearnMoreAboutMatchDetectionDialog(
            uriMatchDisplayType = uriItem.match.toDisplayMatchType(),
            onDialogConfirm = {
                intentManager.launchUri("https://bitwarden.com/help/uri-match-detection/".toUri())
                shouldShowLearnMoreMatchDetectionDialog = false
            },
            onDialogDismiss = {
                shouldShowLearnMoreMatchDetectionDialog = false
            },
        )
    }
}

@Composable
private fun BuildAdvancedMatchDetectionWarning(
    pendingOption: UriMatchDisplayType,
    defaultUriMatchType: UriMatchType,
    onDialogConfirm: () -> Unit,
    onDialogDismiss: () -> Unit,
) {

    val descriptionStringResId = when (pendingOption) {
        UriMatchDisplayType.STARTS_WITH ->
            BitwardenString.selected_matching_option_is_an_advanced_option

        UriMatchDisplayType.REGULAR_EXPRESSION ->
            BitwardenString.selected_matching_option_is_an_advanced_option_if_used_incorrectly

        else -> {
            return
        }
    }

    val nameOfSelectedMatchDisplayType = pendingOption
        .displayLabel(
            defaultUriOption = defaultUriMatchType
                .displayLabel(),
        ).invoke()

    BitwardenTwoButtonDialog(
        title = stringResource(
            id = BitwardenString.are_you_sure_you_want_to_use,
            formatArgs = arrayOf(nameOfSelectedMatchDisplayType),
        ),
        message = stringResource(
            id = descriptionStringResId,
            formatArgs = arrayOf(nameOfSelectedMatchDisplayType),
        ),
        confirmButtonText = stringResource(id = BitwardenString.yes),
        dismissButtonText = stringResource(id = BitwardenString.close),
        onConfirmClick = onDialogConfirm,
        onDismissClick = onDialogDismiss,
        onDismissRequest = onDialogDismiss,
    )
}

@Composable
private fun BuildLearnMoreAboutMatchDetectionDialog(
    uriMatchDisplayType: UriMatchDisplayType,
    onDialogConfirm: () -> Unit,
    onDialogDismiss: () -> Unit,
) {
    BitwardenTwoButtonDialog(
        title = stringResource(id = BitwardenString.keep_your_credential_secure),
        message = stringResource(
            id = BitwardenString.learn_more_about_how_to_keep_credentirals_secure,
            formatArgs = arrayOf(uriMatchDisplayType.text()),
        ),
        confirmButtonText = stringResource(id = BitwardenString.learn_more),
        dismissButtonText = stringResource(id = BitwardenString.cancel),
        onConfirmClick = onDialogConfirm,
        onDismissClick = onDialogDismiss,
        onDismissRequest = onDialogDismiss,
    )
}

@Composable
private fun buildSubSectionsList(): ImmutableList<MultiSelectOption>? {
    return buildList {
        val advancedOptions = UriMatchType.entries.filter { it.isAdvancedMatching() }
        if (advancedOptions.isNotEmpty()) {
            add(
                MultiSelectOption.Header(
                    title = stringResource(id = BitwardenString.advanced_options),
                    testTag = "AdvancedOptionsSection",
                ),
            )
            advancedOptions.forEach { uriMatchType ->
                add(MultiSelectOption.Row(title = uriMatchType.displayLabel()))
            }
        }
    }.toImmutableList()
}
