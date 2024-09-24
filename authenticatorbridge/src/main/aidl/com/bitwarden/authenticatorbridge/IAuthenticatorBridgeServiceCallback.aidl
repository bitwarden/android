package com.bitwarden.authenticatorbridge;

import com.bitwarden.authenticatorbridge.model.EncryptedSharedAccountData;

interface IAuthenticatorBridgeServiceCallback {

    // This function will be called when there is updated shared account data.
    void onAccountsSync(in EncryptedSharedAccountData data);

}
