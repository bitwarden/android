namespace Bit.Core
{
    public static class Constants
    {
        public const string AndroidAppProtocol = "androidapp://";
        public const string iOSAppProtocol = "iosapp://";
        public static string SyncOnRefreshKey = "syncOnRefresh";
        public static string VaultTimeoutKey = "lockOption";
        public static string VaultTimeoutActionKey = "vaultTimeoutAction";
        public static string LastActiveTimeKey = "lastActiveTime";
        public static string BiometricUnlockKey = "fingerprintUnlock";
        public static string ProtectedPin = "protectedPin";
        public static string PinProtectedKey = "pinProtectedKey";
        public static string DefaultUriMatch = "defaultUriMatch";
        public static string DisableAutoTotpCopyKey = "disableAutoTotpCopy";
        public static string EnvironmentUrlsKey = "environmentUrls";
        public static string LastFileCacheClearKey = "lastFileCacheClear";
        public static string AutofillDisableSavePromptKey = "autofillDisableSavePrompt";
        public static string AutofillBlacklistedUrisKey = "autofillBlacklistedUris";
        public static string AutofillTileAdded = "autofillTileAdded";
        public static string DisableFaviconKey = "disableFavicon";
        public static string PushRegisteredTokenKey = "pushRegisteredToken";
        public static string PushCurrentTokenKey = "pushCurrentToken";
        public static string PushLastRegistrationDateKey = "pushLastRegistrationDate";
        public static string PushInitialPromptShownKey = "pushInitialPromptShown";
        public static string ThemeKey = "theme";
        public static string ClearClipboardKey = "clearClipboard";
        public static string LastBuildKey = "lastBuild";
        public static string OldUserIdKey = "userId";
        public static string AddSitePromptShownKey = "addSitePromptShown";
        public static string ClearCiphersCacheKey = "clearCiphersCache";
        public static string BiometricIntegrityKey = "biometricIntegrityState";
        public static string iOSAutoFillClearCiphersCacheKey = "iOSAutoFillClearCiphersCache";
        public static string iOSAutoFillBiometricIntegrityKey = "iOSAutoFillBiometricIntegrityState";
        public static string iOSExtensionClearCiphersCacheKey = "iOSExtensionClearCiphersCache";
        public static string iOSExtensionBiometricIntegrityKey = "iOSExtensionBiometricIntegrityState";
        public static string MigratedFromV1 = "migratedFromV1";
        public static string MigratedFromV1AutofillPromptShown = "migratedV1AutofillPromptShown";
        public static string TriedV1Resync = "triedV1Resync";
        public static string EventCollectionKey = "eventCollection";
        public static string PreviousPageKey = "previousPage";
        public static string InlineAutofillEnabledKey = "inlineAutofillEnabled";
        public static string InvalidUnlockAttempts = "invalidUnlockAttempts";
        public static string PasswordRepromptAutofillKey = "passwordRepromptAutofillKey";
        public static string PasswordVerifiedAutofillKey = "passwordVerifiedAutofillKey";
        public const int SelectFileRequestCode = 42;
        public const int SelectFilePermissionRequestCode = 43;
        public const int SaveFileRequestCode = 44;
        
        public static readonly string[] AndroidAllClearCipherCacheKeys =
        {
            ClearCiphersCacheKey
        };
        
        public static readonly string[] iOSAllClearCipherCacheKeys =
        {
            ClearCiphersCacheKey,
            iOSAutoFillClearCiphersCacheKey,
            iOSExtensionClearCiphersCacheKey
        };
    }
}
