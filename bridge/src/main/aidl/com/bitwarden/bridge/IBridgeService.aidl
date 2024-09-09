package com.bitwarden.bridge;

import com.bitwarden.bridge.model.EncryptedAddTotpLoginItemData;
import com.bitwarden.bridge.model.SymmetricEncryptionKeyData;
import com.bitwarden.bridge.model.SymmetricEncryptionKeyFingerprintData;
import com.bitwarden.bridge.IBridgeServiceCallback;

interface IBridgeService {
    // ==============
    // Configuration
    // ==============

    // Returns the version number string of the Bridge SDK. This is useful so that callers
    // can compare the version of their Bridge SDK with this value and ensure that the two are
    // compatible.
    //
    // For more info about versioning the Bridge SDK, see the Bridge SDK README.
    String getVersionNumber();

    // Returns true when the given symmetric fingerprint data matches that contained by the SDK.
    boolean checkSymmetricEncryptionKeyFingerprint(in SymmetricEncryptionKeyFingerprintData data);

    // Returns a symmetric key that will be used for encypting all IPC traffic.
    //
    // Consumers should only call this function once to limit the number of times this key is
    // sent via IPC. Additionally, once the ksy is shared, checkSymmetricEncryptionKeyFingerprint
    // should be used to safely confirm that the key is valid.
    @nullable SymmetricEncryptionKeyData getSymmetricEncryptionKeyData();

    // ==============
    // Registration
    // ==============

    // Register the given callback to recieve updates after syncAccounts is called.
    void registerBridgeServiceCallback(IBridgeServiceCallback callback);

    // Unregister the given callback from reciebing updates.
    void unregisterBridgeServiceCallback(IBridgeServiceCallback callback);

    // ==============
    // Data Syncing
    // ==============

    // Sync available accounts. Callers should register a callback via
    // registerBridgeServiceCallback before calling this function.
    void syncAccounts();

    // ==============
    // Add TOTP Item
    // ==============

    // Returns an intent that can be launched to navigate the user to the add Totp item flow
    // in the main password manager app.
    Intent createAddTotpLoginItemIntent();

    // Give the given TOTP item data to the main Bitwarden app before launching the add TOTP
    // item flow. This should be called before launching the intent returned from
    // createAddTotpLoginItemIntent().
    void setPendingAddTotpLoginItemData(in EncryptedAddTotpLoginItemData data);

}
