package com.bitwarden.bridge;

import com.bitwarden.bridge.model.EncryptedSharedAccountData;

interface IBridgeServiceCallback {

    // This function will be called when there is updated shared account data.
    void onAccountsSync(in EncryptedSharedAccountData data);

}
