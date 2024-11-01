package com.bitwarden.authenticatorbridge;

import com.bitwarden.authenticatorbridge.model.EncryptedAddTotpLoginItemData;
import com.bitwarden.authenticatorbridge.model.SymmetricEncryptionKeyData;
import com.bitwarden.authenticatorbridge.model.SymmetricEncryptionKeyFingerprintData;
import com.bitwarden.authenticatorbridge.IAuthenticatorBridgeServiceCallback;

interface IAuthenticatorBridgeService {
    // ==============
    // Configuration
    // ==============

    // Returns the version number string of the Authenticator Bridge SDK. This is useful so that
    // callers can compare the version of their Authenticator Bridge SDK with this value and
    // ensure that the two are compatible.
    //
    // For more info about versioning the Authenticator Bridge SDK, see the README.
    String getVersionNumber();

    // Returns true when the given symmetric fingerprint data matches that contained by the SDK.
    boolean checkSymmetricEncryptionKeyFingerprint(in SymmetricEncryptionKeyFingerprintData symmetricKeyFingerprint);

    // Returns a symmetric key that will be used for encypting all IPC traffic.
    //
    // Consumers should only call this function once to limit the number of times this key is
    // sent via IPC. Additionally, once the key is shared, checkSymmetricEncryptionKeyFingerprint
    // should be used to safely confirm that the key is valid.
    @nullable SymmetricEncryptionKeyData getSymmetricEncryptionKeyData();

    // ==============
    // Registration
    // ==============

    // Register the given callback to receive updates after syncAccounts is called.
    void registerBridgeServiceCallback(IAuthenticatorBridgeServiceCallback callback);

    // Unregister the given callback from receiving updates.
    void unregisterBridgeServiceCallback(IAuthenticatorBridgeServiceCallback callback);

    // ==============
    // Data Syncing
    // ==============

    // Sync available accounts. Callers should register a callback via
    // registerBridgeServiceCallback before calling this function.
    void syncAccounts();

    // ==============
    // Add TOTP Item
    // ==============

    // Start the add TOTP item flow in the main Bitwarden app with the given data.
    // Returns true if the flow was successfully launched and false otherwise.
    boolean startAddTotpLoginItemFlow(in EncryptedAddTotpLoginItemData data);

}
