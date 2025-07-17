package com.x8bit.bitwarden.data.autofill.util

import com.bitwarden.vault.CardListView
import com.bitwarden.vault.CipherListView
import com.bitwarden.vault.CipherListViewType
import com.bitwarden.vault.LoginListView

/**
 * Returns the [LoginListView] if the cipher is of type [CipherListViewType.Login], otherwise null.
 */
val CipherListView.login: LoginListView?
    get() = (this.type as? CipherListViewType.Login)?.v1

/**
 * Returns the [CardListView] if the cipher is of type [CipherListViewType.Card], otherwise null.
 */
val CipherListView.card: CardListView?
    get() = (this.type as? CipherListViewType.Card)?.v1
