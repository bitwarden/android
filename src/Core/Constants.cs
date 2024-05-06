using System;

namespace Bit.Core
{
    public static class Constants
    {
        public const int MaxAccounts = 5;
        public const int VaultTimeoutDefault = 15;
        public const string AndroidAppProtocol = "androidapp://";
        public const string iOSAppProtocol = "iosapp://";
        public const string DefaultUsernameGenerated = "-";
        public const string AppIdKey = "appId";
        public const string StateVersionKey = "stateVersion";
        public const string StateKey = "state";
        public const string PreAuthEnvironmentUrlsKey = "preAuthEnvironmentUrls";
        public const string LastFileCacheClearKey = "lastFileCacheClear";
        public const string AutofillTileAddedKey = "autofillTileAdded";
        public const string PushRegisteredTokenKey = "pushRegisteredToken";
        public const string PushInitialPromptShownKey = "pushInitialPromptShown";
        public const string PushInstallationRegistrationErrorKey = "pushInstallationRegistrationError";
        public const string LastBuildKey = "lastBuild";
        public const string AddSitePromptShownKey = "addSitePromptShown";
        public const string ClearCiphersCacheKey = "clearCiphersCache";
        public const string BiometricIntegritySourceKey = "biometricIntegritySource";
        public const string iOSAutoFillClearCiphersCacheKey = "iOSAutoFillClearCiphersCache";
        public const string iOSAutoFillBiometricIntegritySourceKey = "iOSAutoFillBiometricIntegritySource";
        public const string iOSExtensionClearCiphersCacheKey = "iOSExtensionClearCiphersCache";
        public const string iOSExtensionBiometricIntegritySourceKey = "iOSExtensionBiometricIntegritySource";
        public const string iOSShareExtensionClearCiphersCacheKey = "iOSShareExtensionClearCiphersCache";
        public const string iOSShareExtensionBiometricIntegritySourceKey = "iOSShareExtensionBiometricIntegritySource";
        public const string iOSExtensionActiveUserIdKey = "iOSExtensionActiveUserId";
        public const string EventCollectionKey = "eventCollection";
        public const string RememberedEmailKey = "rememberedEmail";
        public const string RememberedOrgIdentifierKey = "rememberedOrgIdentifier";
        public const string PasswordlessLoginNotificationKey = "passwordlessLoginNotificationKey";
        public const string ThemeKey = "theme";
        public const string AutoDarkThemeKey = "autoDarkTheme";
        public const string DisableFaviconKey = "disableFavicon";
        public const string PasswordlessNotificationId = "26072022";
        public const string AndroidNotificationChannelId = "general_notification_channel";
        public const string iOSNotificationCategoryId = "dismissableCategory";
        public const string iOSNotificationClearActionId = "Clear";
        public const string NotificationData = "notificationData";
        public const string NotificationDataType = "Type";
        public const string PasswordlessLoginRequestKey = "passwordlessLoginRequest";
        public const string PreLoginEmailKey = "preLoginEmailKey";
        public const string ConfigsKey = "configsKey";
        public const string DisplayEuEnvironmentFlag = "display-eu-environment";
        public const string UnassignedItemsBannerFlag = "unassigned-items-banner";
        public const string RegionEnvironment = "regionEnvironment";
        public const string DuoCallback = "bitwarden://duo-callback";
        public const string NavigateToMessageCommand = "navigateTo";
        public const string CredentialNavigateToAutofillCipherMessageCommand = "credentialNavigateToAutofillCipher";

        /// <summary>
        /// This key is used to store the value of "ShouldConnectToWatch" of the last user that had logged in
        /// which is used to handle Apple Watch state logic
        /// </summary>
        public const string LastUserShouldConnectToWatchKey = "lastUserShouldConnectToWatch";
        public const string OtpAuthScheme = "otpauth";
        public const string AppLocaleKey = "appLocale";
        public const string ClearSensitiveFields = "clearSensitiveFields";
        public const string ForceUpdatePassword = "forceUpdatePassword";
        public const string ForceSetPassword = "forceSetPassword";
        public const string ShouldTrustDevice = "shouldTrustDevice";
        public const int SelectFileRequestCode = 42;
        public const int SelectFilePermissionRequestCode = 43;
        public const int SaveFileRequestCode = 44;
        public const int TotpDefaultTimer = 30;
        public const int PasswordlessNotificationTimeoutInMinutes = 15;
        public const int Pbkdf2Iterations = 600000;
        public const int Argon2Iterations = 3;
        public const int Argon2MemoryInMB = 64;
        public const int Argon2Parallelism = 4;
        public const int MasterPasswordMinimumChars = 12;
        public const int CipherKeyRandomBytesLength = 64;
        public const string CipherKeyEncryptionMinServerVersion = "2024.2.0";
        public const string DefaultFido2CredentialType = "public-key";
        public const string DefaultFido2CredentialAlgorithm = "ECDSA";
        public const string DefaultFido2CredentialCurve = "P-256";
        public const int LatestStateVersion = 7;

        public static readonly string[] AndroidAllClearCipherCacheKeys =
        {
            ClearCiphersCacheKey
        };

        public static readonly string[] iOSAllClearCipherCacheKeys =
        {
            ClearCiphersCacheKey,
            iOSAutoFillClearCiphersCacheKey,
            iOSExtensionClearCiphersCacheKey,
            iOSShareExtensionClearCiphersCacheKey
        };

        public static string VaultTimeoutKey(string userId) => $"vaultTimeout_{userId}";
        public static string VaultTimeoutActionKey(string userId) => $"vaultTimeoutAction_{userId}";
        public static string MasterKeyEncryptedUserKeyKey(string userId) => $"masterKeyEncryptedUserKey_{userId}";
        public static string UserKeyAutoUnlockKey(string userId) => $"userKeyAutoUnlock_{userId}";
        public static string UserKeyBiometricUnlockKey(string userId) => $"userKeyBiometricUnlock_{userId}";
        public static string CiphersKey(string userId) => $"ciphers_{userId}";
        public static string FoldersKey(string userId) => $"folders_{userId}";
        public static string CollectionsKey(string userId) => $"collections_{userId}";
        public static string OrganizationsKey(string userId) => $"organizations_{userId}";
        public static string CiphersLocalDataKey(string userId) => $"ciphersLocalData_{userId}";
        public static string SendsKey(string userId) => $"sends_{userId}";
        public static string PoliciesKey(string userId) => $"policies_{userId}";
        public static string EncOrgKeysKey(string userId) => $"encOrgKeys_{userId}";
        public static string EncPrivateKeyKey(string userId) => $"encPrivateKey_{userId}";
        public static string DeviceKeyKey(string userId) => $"deviceKey_{userId}";
        public static string KeyHashKey(string userId) => $"keyHash_{userId}";
        public static string PinKeyEncryptedUserKeyKey(string userId) => $"pinKeyEncryptedUserKey_{userId}";
        public static string PassGenOptionsKey(string userId) => $"passwordGenerationOptions_{userId}";
        public static string PassGenHistoryKey(string userId) => $"generatedPasswordHistory_{userId}";
        public static string TwoFactorTokenKey(string email) => $"twoFactorToken_{email}";
        public static string LastActiveTimeKey(string userId) => $"lastActiveTime_{userId}";
        public static string InvalidUnlockAttemptsKey(string userId) => $"invalidUnlockAttempts_{userId}";
        public static string InlineAutofillEnabledKey(string userId) => $"inlineAutofillEnabled_{userId}";
        public static string AutofillDisableSavePromptKey(string userId) => $"autofillDisableSavePrompt_{userId}";
        public static string AutofillBlacklistedUrisKey(string userId) => $"autofillBlacklistedUris_{userId}";
        public static string ClearClipboardKey(string userId) => $"clearClipboard_{userId}";
        public static string SyncOnRefreshKey(string userId) => $"syncOnRefresh_{userId}";
        public static string DefaultUriMatchKey(string userId) => $"defaultUriMatch_{userId}";
        public static string DisableAutoTotpCopyKey(string userId) => $"disableAutoTotpCopy_{userId}";
        public static string PreviousPageKey(string userId) => $"previousPage_{userId}";
        public static string PasswordRepromptAutofillKey(string userId) => $"passwordRepromptAutofillKey_{userId}";
        public static string PasswordVerifiedAutofillKey(string userId) => $"passwordVerifiedAutofillKey_{userId}";
        public static string SettingsKey(string userId) => $"settings_{userId}";
        public static string UsesKeyConnectorKey(string userId) => $"usesKeyConnector_{userId}";
        public static string ProtectedPinKey(string userId) => $"protectedPin_{userId}";
        public static string LastSyncKey(string userId) => $"lastSync_{userId}";
        public static string BiometricUnlockKey(string userId) => $"biometricUnlock_{userId}";
        public static string AccountBiometricIntegrityValidKey(string userId, string systemBioIntegrityState) =>
            $"accountBiometricIntegrityValid_{userId}_{systemBioIntegrityState}";
        public static string ApprovePasswordlessLoginsKey(string userId) => $"approvePasswordlessLogins_{userId}";
        public static string UsernameGenOptionsKey(string userId) => $"usernameGenerationOptions_{userId}";
        public static string PushLastRegistrationDateKey(string userId) => $"pushLastRegistrationDate_{userId}";
        public static string PushCurrentTokenKey(string userId) => $"pushCurrentToken_{userId}";
        public static string ShouldConnectToWatchKey(string userId) => $"shouldConnectToWatch_{userId}";
        public static string ScreenCaptureAllowedKey(string userId) => $"screenCaptureAllowed_{userId}";
        public static string PendingAdminAuthRequest(string userId) => $"pendingAdminAuthRequest_{userId}";
        public static string ShouldCheckOrganizationUnassignedItemsKey(string userId) => $"shouldCheckOrganizationUnassignedItems_{userId}";
        [Obsolete]
        public static string KeyKey(string userId) => $"key_{userId}";
        [Obsolete]
        public static string EncKeyKey(string userId) => $"encKey_{userId}";
        [Obsolete]
        public static string PinProtectedKey(string userId) => $"pinProtectedKey_{userId}";
    }
}
