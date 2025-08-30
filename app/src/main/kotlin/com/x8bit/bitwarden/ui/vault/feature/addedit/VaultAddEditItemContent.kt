package com.x8bit.bitwarden.ui.vault.feature.addedit

import android.Manifest
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.bitwarden.ui.platform.components.button.BitwardenTextSelectionButton
import com.bitwarden.ui.platform.components.card.BitwardenActionCard
import com.bitwarden.ui.platform.components.card.BitwardenInfoCalloutCard
import com.bitwarden.ui.platform.components.coachmark.scope.CoachMarkScope
import com.bitwarden.ui.platform.components.field.BitwardenTextField
import com.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.data.platform.repository.model.UriMatchType
import com.x8bit.bitwarden.ui.platform.manager.permissions.PermissionsManager
import com.x8bit.bitwarden.ui.vault.components.collectionItemsSelector
import com.x8bit.bitwarden.ui.vault.feature.addedit.handlers.VaultAddEditCardTypeHandlers
import com.x8bit.bitwarden.ui.vault.feature.addedit.handlers.VaultAddEditCommonHandlers
import com.x8bit.bitwarden.ui.vault.feature.addedit.handlers.VaultAddEditIdentityTypeHandlers
import com.x8bit.bitwarden.ui.vault.feature.addedit.handlers.VaultAddEditLoginTypeHandlers
import com.x8bit.bitwarden.ui.vault.feature.addedit.handlers.VaultAddEditSshKeyTypeHandlers

/**
 * The top level content UI state for the [VaultAddEditScreen].
 */
@Composable
@Suppress("LongMethod", "CyclomaticComplexMethod")
fun CoachMarkScope<AddEditItemCoachMark>.VaultAddEditContent(
    state: VaultAddEditState.ViewState.Content,
    isAddItemMode: Boolean,
    defaultUriMatchType: UriMatchType,
    commonTypeHandlers: VaultAddEditCommonHandlers,
    loginItemTypeHandlers: VaultAddEditLoginTypeHandlers,
    identityItemTypeHandlers: VaultAddEditIdentityTypeHandlers,
    cardItemTypeHandlers: VaultAddEditCardTypeHandlers,
    sshKeyItemTypeHandlers: VaultAddEditSshKeyTypeHandlers,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState,
    permissionsManager: PermissionsManager,
    onNextCoachMark: () -> Unit,
    onPreviousCoachMark: () -> Unit,
    onCoachMarkTourComplete: () -> Unit,
    onCoachMarkDismissed: () -> Unit,
    shouldShowLearnAboutLoginsCard: Boolean,
) {
    val launcher = permissionsManager.getLauncher(
        onResult = { isGranted ->
            when (state.type) {
                is VaultAddEditState.ViewState.Content.ItemType.SecureNotes -> Unit
                is VaultAddEditState.ViewState.Content.ItemType.Card -> Unit
                is VaultAddEditState.ViewState.Content.ItemType.Identity -> Unit
                is VaultAddEditState.ViewState.Content.ItemType.SshKey -> Unit
                is VaultAddEditState.ViewState.Content.ItemType.Login -> {
                    loginItemTypeHandlers.onSetupTotpClick(isGranted)
                }
            }
        },
    )

    val isAdditionalOptionsExpanded = rememberSaveable { mutableStateOf(value = false) }
    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    LazyColumn(modifier = modifier, state = lazyListState) {
        item {
            Spacer(modifier = Modifier.height(height = 12.dp))
        }
        if (state.isIndividualVaultDisabled && isAddItemMode) {
            item {
                BitwardenInfoCalloutCard(
                    text = stringResource(BitwardenString.personal_ownership_policy_in_effect),
                    modifier = Modifier
                        .standardHorizontalMargin()
                        .testTag("PersonalOwnershipPolicyLabel")
                        .fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(height = 16.dp))
            }
        }

        if (shouldShowLearnAboutLoginsCard) {
            item {
                BitwardenActionCard(
                    cardTitle = stringResource(BitwardenString.learn_about_new_logins),
                    cardSubtitle = stringResource(
                        BitwardenString.we_ll_walk_you_through_the_key_features_to_add_a_new_login,
                    ),
                    actionText = stringResource(BitwardenString.get_started),
                    onActionClick = loginItemTypeHandlers.onStartLoginCoachMarkTour,
                    onDismissClick = loginItemTypeHandlers.onDismissLearnAboutLoginsCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin(),
                )
                Spacer(modifier = Modifier.height(height = 16.dp))
            }
        }
        if (isAddItemMode) {
            item {
                BitwardenListHeaderText(
                    label = stringResource(id = BitwardenString.item_details),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .padding(horizontal = 16.dp),
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(height = 8.dp))
            BitwardenTextField(
                label = stringResource(id = BitwardenString.item_name_required),
                value = state.common.name,
                onValueChange = commonTypeHandlers.onNameTextChange,
                actions = {
                    BitwardenStandardIconButton(
                        vectorIconRes = if (state.common.favorite) {
                            BitwardenDrawable.ic_favorite_full
                        } else {
                            BitwardenDrawable.ic_favorite_empty
                        },
                        contentDescription = if (state.common.favorite) {
                            stringResource(id = BitwardenString.favorite)
                        } else {
                            stringResource(id = BitwardenString.unfavorite)
                        },
                        onClick = { commonTypeHandlers.onToggleFavorite(!state.common.favorite) },
                        modifier = Modifier.testTag(tag = "ItemFavoriteToggle"),
                    )
                },
                textFieldTestTag = "ItemNameEntry",
                cardStyle = CardStyle.Full,
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )
        }

        item {
            Spacer(modifier = Modifier.height(height = 8.dp))
            BitwardenTextSelectionButton(
                label = stringResource(id = BitwardenString.folder),
                selectedOption = state.common.selectedFolder?.name,
                onClick = commonTypeHandlers.onSelectOrAddFolderForItem,
                cardStyle = if (isAddItemMode && state.common.hasOrganizations) {
                    CardStyle.Top(dividerPadding = 0.dp)
                } else {
                    CardStyle.Full
                },
                modifier = Modifier
                    .testTag(tag = "FolderPicker")
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )
        }

        if (isAddItemMode && state.common.hasOrganizations) {
            val collections = state.common.selectedOwner?.collections.orEmpty()
            item {
                BitwardenTextSelectionButton(
                    label = stringResource(id = BitwardenString.owner),
                    selectedOption = state.common.selectedOwner?.name,
                    onClick = commonTypeHandlers.onPresentOwnerOptions,
                    cardStyle = if (collections.isNotEmpty()) {
                        CardStyle.Middle()
                    } else {
                        CardStyle.Bottom
                    },
                    modifier = Modifier
                        .testTag(tag = "ItemOwnershipPicker")
                        .fillMaxWidth()
                        .standardHorizontalMargin(),
                )
            }

            if (collections.isNotEmpty()) {
                collectionItemsSelector(
                    collectionList = collections,
                    onCollectionSelect = commonTypeHandlers.onCollectionSelect,
                )
            }
        }

        when (state.type) {
            is VaultAddEditState.ViewState.Content.ItemType.Login -> {
                vaultAddEditLoginItems(
                    loginState = state.type,
                    loginItemTypeHandlers = loginItemTypeHandlers,
                    windowAdaptiveInfo = windowAdaptiveInfo,
                    onTotpSetupClick = {
                        if (permissionsManager.checkPermission(Manifest.permission.CAMERA)) {
                            loginItemTypeHandlers.onSetupTotpClick(true)
                        } else {
                            launcher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    coachMarkScope = this@VaultAddEditContent,
                    onPreviousCoachMark = onPreviousCoachMark,
                    onNextCoachMark = onNextCoachMark,
                    onCoachMarkTourComplete = onCoachMarkTourComplete,
                    onCoachMarkDismissed = onCoachMarkDismissed,
                    defaultUriMatchType = defaultUriMatchType,
                )
            }

            is VaultAddEditState.ViewState.Content.ItemType.Card -> {
                vaultAddEditCardItems(
                    cardState = state.type,
                    cardHandlers = cardItemTypeHandlers,
                )
            }

            is VaultAddEditState.ViewState.Content.ItemType.Identity -> {
                vaultAddEditIdentityItems(
                    identityState = state.type,
                    identityItemTypeHandlers = identityItemTypeHandlers,
                )
            }

            is VaultAddEditState.ViewState.Content.ItemType.SecureNotes -> {
                vaultAddEditSecureNotesItems(
                    commonState = state.common,
                    commonTypeHandlers = commonTypeHandlers,
                )
            }

            is VaultAddEditState.ViewState.Content.ItemType.SshKey -> {
                vaultAddEditSshKeyItems(
                    sshKeyState = state.type,
                    sshKeyTypeHandlers = sshKeyItemTypeHandlers,
                )
            }
        }

        vaultAddEditAdditionalOptions(
            itemType = state.type,
            commonState = state.common,
            commonTypeHandlers = commonTypeHandlers,
            isAdditionalOptionsExpanded = isAdditionalOptionsExpanded.value,
            onAdditionalOptionsClick = {
                isAdditionalOptionsExpanded.value = !isAdditionalOptionsExpanded.value
            },
        )

        item {
            Spacer(modifier = Modifier.height(height = 16.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

/**
 * Enumerated values representing the coach mark items to be shown.
 */
enum class AddEditItemCoachMark {
    GENERATE_PASSWORD,
    TOTP,
    URI,
}
