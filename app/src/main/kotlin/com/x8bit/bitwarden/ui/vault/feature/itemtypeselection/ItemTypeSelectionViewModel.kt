package com.x8bit.bitwarden.ui.vault.feature.itemtypeselection

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.bitwarden.core.data.manager.model.FlagKey
import com.bitwarden.network.model.PolicyTypeJson
import com.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * ViewModel for the item type selection screen.
 */
@HiltViewModel
class ItemTypeSelectionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    featureFlagManager: FeatureFlagManager,
    policyManager: PolicyManager,
) : BaseViewModel<ItemTypeSelectionState, ItemTypeSelectionEvent, ItemTypeSelectionAction>(
    initialState = savedStateHandle[KEY_STATE] ?: ItemTypeSelectionState(
        itemTypes = buildItemTypeList(
            isNewItemTypesEnabled = featureFlagManager
                .getFeatureFlag(FlagKey.NewItemTypes),
            hasRestrictItemTypesPolicy = policyManager
                .getActivePolicies(PolicyTypeJson.RESTRICT_ITEM_TYPES)
                .isNotEmpty(),
        ),
    ),
) {
    override fun handleAction(action: ItemTypeSelectionAction) {
        when (action) {
            is ItemTypeSelectionAction.BackClick -> handleBackClick()
            is ItemTypeSelectionAction.ItemTypeClick -> {
                handleItemTypeClick(action)
            }
        }
    }

    private fun handleBackClick() {
        sendEvent(ItemTypeSelectionEvent.NavigateBack)
    }

    private fun handleItemTypeClick(
        action: ItemTypeSelectionAction.ItemTypeClick,
    ) {
        sendEvent(
            ItemTypeSelectionEvent.NavigateToAddItem(action.cipherType),
        )
    }
}

@Suppress("LongMethod")
private fun buildItemTypeList(
    isNewItemTypesEnabled: Boolean,
    hasRestrictItemTypesPolicy: Boolean,
): ImmutableList<ItemTypeSelectionState.ItemTypeOption> {
    val options = mutableListOf(
        ItemTypeSelectionState.ItemTypeOption(
            cipherType = VaultItemCipherType.LOGIN,
            icon = IconData.Local(BitwardenDrawable.ic_globe),
            title = BitwardenString.type_login.asText(),
        ),
    )

    // Card is excluded when restrict item types policy is active
    if (!hasRestrictItemTypesPolicy) {
        options.add(
            ItemTypeSelectionState.ItemTypeOption(
                cipherType = VaultItemCipherType.CARD,
                icon = IconData.Local(
                    BitwardenDrawable.ic_payment_card,
                ),
                title = BitwardenString.type_card.asText(),
            ),
        )
    }

    options.addAll(
        listOf(
            ItemTypeSelectionState.ItemTypeOption(
                cipherType = VaultItemCipherType.IDENTITY,
                icon = IconData.Local(BitwardenDrawable.ic_id_card),
                title = BitwardenString.type_identity.asText(),
            ),
            ItemTypeSelectionState.ItemTypeOption(
                cipherType = VaultItemCipherType.SECURE_NOTE,
                icon = IconData.Local(BitwardenDrawable.ic_note),
                title = BitwardenString.type_secure_note.asText(),
            ),
        ),
    )

    // SSH Key is intentionally excluded — SSH keys cannot be created
    // from the app, only imported.

    if (isNewItemTypesEnabled) {
        options.addAll(
            listOf(
                ItemTypeSelectionState.ItemTypeOption(
                    cipherType = VaultItemCipherType.BANK_ACCOUNT,
                    icon = IconData.Local(
                        BitwardenDrawable.ic_bank_account,
                    ),
                    title = BitwardenString.type_bank_account.asText(),
                ),
                ItemTypeSelectionState.ItemTypeOption(
                    cipherType = VaultItemCipherType.DRIVERS_LICENSE,
                    icon = IconData.Local(
                        BitwardenDrawable.ic_drivers_license,
                    ),
                    title = BitwardenString.type_drivers_license
                        .asText(),
                ),
                ItemTypeSelectionState.ItemTypeOption(
                    cipherType = VaultItemCipherType.PASSPORT,
                    icon = IconData.Local(BitwardenDrawable.ic_passport),
                    title = BitwardenString.type_passport.asText(),
                ),
            ),
        )
    }

    return options.toImmutableList()
}

/**
 * State for the item type selection screen.
 */
@Parcelize
data class ItemTypeSelectionState(
    val itemTypes: ImmutableList<ItemTypeOption>,
) : Parcelable {

    /**
     * Represents a selectable item type option.
     *
     * @property cipherType The cipher type this option represents.
     * @property icon The icon to display for this option.
     * @property title The display title for this option.
     */
    @Parcelize
    data class ItemTypeOption(
        val cipherType: VaultItemCipherType,
        val icon: IconData.Local,
        val title: Text,
    ) : Parcelable
}

/**
 * Events for the item type selection screen.
 */
sealed class ItemTypeSelectionEvent {
    /**
     * Navigate back.
     */
    data object NavigateBack : ItemTypeSelectionEvent()

    /**
     * Navigate to add item screen with the selected type.
     */
    data class NavigateToAddItem(
        val cipherType: VaultItemCipherType,
    ) : ItemTypeSelectionEvent()
}

/**
 * Actions for the item type selection screen.
 */
sealed class ItemTypeSelectionAction {
    /**
     * User clicked the back button.
     */
    data object BackClick : ItemTypeSelectionAction()

    /**
     * User clicked an item type.
     */
    data class ItemTypeClick(
        val cipherType: VaultItemCipherType,
    ) : ItemTypeSelectionAction()
}
