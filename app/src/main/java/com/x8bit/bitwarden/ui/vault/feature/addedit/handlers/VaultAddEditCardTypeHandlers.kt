package com.x8bit.bitwarden.ui.vault.feature.addedit.handlers

import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditAction
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditViewModel
import com.x8bit.bitwarden.ui.vault.model.VaultCardBrand
import com.x8bit.bitwarden.ui.vault.model.VaultCardExpirationMonth

/**
 * A collection of handler functions specifically tailored for managing actions
 * within the context of adding card items to a vault.
 *
 * @property onCardHolderNameTextChange Handles the action when the card holder name text is changed.
 * @property onNumberTextChange Handles the action when the number text is changed.
 * @property onBrandSelected Handles the action when a brand is selected.
 * @property onExpirationMonthSelected Handles the action when an expiration month is selected.
 * @property onExpirationYearTextChange Handles the action when the expiration year text is changed.
 * @property onSecurityCodeTextChange Handles the action when the expiration year text is changed.
 * @property onSecurityCodeVisibilityChange Handles the action when the security code visibility
 * changes.
 * @property onNumberVisibilityChange Handles the action when the number visibility changes.
 */
@Suppress("MaxLineLength")
data class VaultAddEditCardTypeHandlers(
    val onCardHolderNameTextChange: (String) -> Unit,
    val onNumberTextChange: (String) -> Unit,
    val onBrandSelected: (VaultCardBrand) -> Unit,
    val onExpirationMonthSelected: (VaultCardExpirationMonth) -> Unit,
    val onExpirationYearTextChange: (String) -> Unit,
    val onSecurityCodeTextChange: (String) -> Unit,
    val onSecurityCodeVisibilityChange: (Boolean) -> Unit,
    val onNumberVisibilityChange: (Boolean) -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {

        /**
         * Creates an instance of [VaultAddEditCardTypeHandlers] by binding actions
         * to the provided [VaultAddEditViewModel].
         */
        fun create(viewModel: VaultAddEditViewModel): VaultAddEditCardTypeHandlers =
            VaultAddEditCardTypeHandlers(
                onCardHolderNameTextChange = { newCardHolderName ->
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.CardType.CardHolderNameTextChange(
                            cardHolderName = newCardHolderName,
                        ),
                    )
                },
                onNumberTextChange = { newNumber ->
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.CardType.NumberTextChange(
                            number = newNumber,
                        ),
                    )
                },
                onBrandSelected = { newBrand ->
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.CardType.BrandSelect(
                            brand = newBrand,
                        ),
                    )
                },
                onExpirationMonthSelected = { newExpirationMonth ->
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.CardType.ExpirationMonthSelect(
                            expirationMonth = newExpirationMonth,
                        ),
                    )
                },
                onExpirationYearTextChange = { newExpirationYear ->
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.CardType.ExpirationYearTextChange(
                            expirationYear = newExpirationYear,
                        ),
                    )
                },
                onSecurityCodeTextChange = { newSecurityCode ->
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.CardType.SecurityCodeTextChange(
                            securityCode = newSecurityCode,
                        ),
                    )
                },
                onSecurityCodeVisibilityChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.CardType.SecurityCodeVisibilityChange(
                            isVisible = it,
                        ),
                    )
                },
                onNumberVisibilityChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.CardType.NumberVisibilityChange(isVisible = it),
                    )
                },
            )
    }
}
