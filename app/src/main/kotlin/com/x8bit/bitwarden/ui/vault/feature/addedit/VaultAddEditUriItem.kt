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
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.core.net.toUri
import com.bitwarden.ui.platform.base.util.annotatedStringResource
import com.bitwarden.ui.platform.base.util.spanStyleOf
import com.bitwarden.ui.platform.base.util.toAnnotatedString
import com.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenSelectionDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.row.BitwardenBasicDialogRow
import com.x8bit.bitwarden.ui.platform.components.dropdown.BitwardenMultiSelectDialogContent
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.composition.LocalIntentManager
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.UriItem
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.UriMatchDisplayType
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.isAdvancedMatching
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.toDisplayMatchType
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.toUriMatchType
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

    BitwardenTextField(
        label = stringResource(id = R.string.website_uri),
        value = uriItem.uri.orEmpty(),
        onValueChange = { onUriValueChange(uriItem.copy(uri = it)) },
        actions = {
            BitwardenStandardIconButton(
                vectorIconRes = BitwardenDrawable.ic_cog,
                contentDescription = stringResource(id = R.string.options),
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
            title = stringResource(id = R.string.options),
            onDismissRequest = { shouldShowOptionsDialog = false },
        ) {
            BitwardenBasicDialogRow(
                text = stringResource(id = R.string.match_detection),
                onClick = {
                    shouldShowOptionsDialog = false
                    shouldShowMatchDialog = true
                },
            )
            BitwardenBasicDialogRow(
                text = stringResource(id = R.string.remove),
                onClick = {
                    shouldShowOptionsDialog = false
                    onUriItemRemoved(uriItem)
                },
            )
        }
    }

    if (shouldShowMatchDialog) {
        BitwardenSelectionDialog(
            title = stringResource(id = R.string.uri_match_detection),
            onDismissRequest = { shouldShowMatchDialog = false },
        ) {
            BitwardenMultiSelectDialogContent(
                options = UriMatchDisplayType.entries.filter { !it.isAdvancedMatching() }
                    .map { it.text.invoke() }.toImmutableList(),
                selectedOption = uriItem.match.toDisplayMatchType().text.invoke(),
                sectionTitle = stringResource(id = R.string.advanced_options),
                sectionOptions = UriMatchDisplayType.entries.filter { it.isAdvancedMatching() }
                    .map { it.text.invoke() }.toImmutableList(),
                sectionTestTag = "AdvancedOptionsSection",
                onOptionSelected = { selectedOption ->
                    shouldShowMatchDialog = false

                    val newSelectedType =
                        UriMatchDisplayType
                            .entries
                            .first { it.text.invoke(resources) == selectedOption }

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
            onDialogConfirm = {
                onUriValueChange(
                    uriItem.copy(match = currentOptionToConfirm.toUriMatchType()),
                )
                shouldShowAdvancedMatchDialog = false
                optionPendingConfirmation = null
            },
            onDialogDismiss = {
                shouldShowAdvancedMatchDialog = false
                optionPendingConfirmation = null
            },
            onMoreAboutMatchDetectionClick = {
                intentManager.launchUri(
                    uri = "https://bitwarden.com/help/uri-match-detection/".toUri(),
                )
            },
        )
    }
}

@Composable
private fun BuildAdvancedMatchDetectionWarning(
    pendingOption: UriMatchDisplayType,
    onDialogConfirm: () -> Unit,
    onDialogDismiss: () -> Unit,
    onMoreAboutMatchDetectionClick: () -> Unit,
) {
    val moreAboutMatchDetectionStr = stringResource(R.string.more_about_match_detection)

    BitwardenTwoButtonDialog(
        titleAnnotatedString = stringResource(id = R.string.warning).toAnnotatedString(),
        messageAnnotatedString = annotatedStringResource(
            id = R.string.advanced_options_warning,
            args = arrayOf(pendingOption.text.invoke()),
            style = spanStyleOf(
                color = BitwardenTheme.colorScheme.text.primary,
                textStyle = BitwardenTheme.typography.bodyMedium,
            ),
        ).plus(
            AnnotatedString("\n")
                .plus(
                    annotatedStringResource(
                        id = R.string.more_about_match_detection,
                        onAnnotationClick = { annotationValue ->
                            when (annotationValue) {
                                "moreAboutMatchDetection" -> onMoreAboutMatchDetectionClick()
                            }
                        },
                    ),
                ),
        ),
        confirmButtonText = stringResource(id = R.string.continue_text),
        dismissButtonText = stringResource(id = R.string.cancel),
        onConfirmClick = onDialogConfirm,
        onDismissClick = onDialogDismiss,
        onDismissRequest = onDialogDismiss,
        messageModifier = Modifier
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(
                        label = moreAboutMatchDetectionStr,
                        action = {
                            onMoreAboutMatchDetectionClick()
                            true
                        },
                    ),
                )
            },
    )
}
