package com.x8bit.bitwarden.ui.vault.feature.itemtypeselection

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * ViewModel for the [ItemTypeSelectionScreen] which lets the user pick a cipher type
 * to create a new vault item.
 */
@HiltViewModel
class ItemTypeSelectionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<ItemTypeSelectionState, ItemTypeSelectionEvent, ItemTypeSelectionAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: ItemTypeSelectionState(itemTypes = DEFAULT_ITEM_TYPES),
) {
    override fun handleAction(action: ItemTypeSelectionAction) {
        when (action) {
            ItemTypeSelectionAction.BackClick -> handleBackClick()
            is ItemTypeSelectionAction.ItemTypeClick -> handleItemTypeClick(action)
        }
    }

    private fun handleBackClick() {
        sendEvent(ItemTypeSelectionEvent.NavigateBack)
    }

    private fun handleItemTypeClick(action: ItemTypeSelectionAction.ItemTypeClick) {
        sendEvent(ItemTypeSelectionEvent.NavigateToAddItem(cipherType = action.cipherType))
    }
}

/**
 * The list of item types presented on the selection screen. The screen is only reachable when
 * the new item types feature flag is enabled, so all eight types are always shown.
 */
private val DEFAULT_ITEM_TYPES: ImmutableList<ItemTypeSelectionState.ItemTypeOption> =
    persistentListOf(
        ItemTypeSelectionState.ItemTypeOption(
            cipherType = VaultItemCipherType.LOGIN,
            icon = IconData.Local(iconRes = BitwardenDrawable.ic_globe),
            title = BitwardenString.type_login.asText(),
        ),
        ItemTypeSelectionState.ItemTypeOption(
            cipherType = VaultItemCipherType.CARD,
            icon = IconData.Local(iconRes = BitwardenDrawable.ic_payment_card),
            title = BitwardenString.type_card.asText(),
        ),
        ItemTypeSelectionState.ItemTypeOption(
            cipherType = VaultItemCipherType.IDENTITY,
            icon = IconData.Local(iconRes = BitwardenDrawable.ic_id_card),
            title = BitwardenString.type_identity.asText(),
        ),
        ItemTypeSelectionState.ItemTypeOption(
            cipherType = VaultItemCipherType.SECURE_NOTE,
            icon = IconData.Local(iconRes = BitwardenDrawable.ic_note),
            title = BitwardenString.type_secure_note.asText(),
        ),
        ItemTypeSelectionState.ItemTypeOption(
            cipherType = VaultItemCipherType.SSH_KEY,
            icon = IconData.Local(iconRes = BitwardenDrawable.ic_ssh_key),
            title = BitwardenString.type_ssh_key.asText(),
        ),
        ItemTypeSelectionState.ItemTypeOption(
            cipherType = VaultItemCipherType.BANK_ACCOUNT,
            icon = IconData.Local(iconRes = BitwardenDrawable.ic_bank_account),
            title = BitwardenString.type_bank_account.asText(),
        ),
        ItemTypeSelectionState.ItemTypeOption(
            cipherType = VaultItemCipherType.DRIVERS_LICENSE,
            icon = IconData.Local(iconRes = BitwardenDrawable.ic_drivers_license),
            title = BitwardenString.type_drivers_license.asText(),
        ),
        ItemTypeSelectionState.ItemTypeOption(
            cipherType = VaultItemCipherType.PASSPORT,
            icon = IconData.Local(iconRes = BitwardenDrawable.ic_passport),
            title = BitwardenString.type_passport.asText(),
        ),
    )

/**
 * Represents the state for the [ItemTypeSelectionScreen].
 *
 * @property itemTypes The list of selectable cipher type options.
 */
@Parcelize
data class ItemTypeSelectionState(
    val itemTypes: ImmutableList<ItemTypeOption>,
) : Parcelable {

    /**
     * A selectable cipher type option.
     *
     * @property cipherType The [VaultItemCipherType] this option represents.
     * @property icon The icon to display alongside the option.
     * @property title The display title for the option.
     */
    @Parcelize
    data class ItemTypeOption(
        val cipherType: VaultItemCipherType,
        val icon: IconData.Local,
        val title: Text,
    ) : Parcelable
}

/**
 * Models events emitted by the [ItemTypeSelectionViewModel].
 */
sealed class ItemTypeSelectionEvent {
    /**
     * Navigate back to the previous screen.
     */
    data object NavigateBack : ItemTypeSelectionEvent()

    /**
     * Navigate to the add-item screen for the given [cipherType].
     */
    data class NavigateToAddItem(
        val cipherType: VaultItemCipherType,
    ) : ItemTypeSelectionEvent()
}

/**
 * Models actions handled by the [ItemTypeSelectionViewModel].
 */
sealed class ItemTypeSelectionAction {
    /**
     * The user clicked the back button.
     */
    data object BackClick : ItemTypeSelectionAction()

    /**
     * The user clicked an item type option.
     */
    data class ItemTypeClick(
        val cipherType: VaultItemCipherType,
    ) : ItemTypeSelectionAction()
}
