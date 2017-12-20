namespace Bit.App
{
    public static class Constants
    {
        public const string AndroidAppProtocol = "androidapp://";
        public const string iOSAppProtocol = "iosapp://";

        public const string SettingFingerprintUnlockOn = "setting:fingerprintUnlockOn";
        public const string SettingPinUnlockOn = "setting:pinUnlockOn";
        public const string SettingLockSeconds = "setting:lockSeconds";
        public const string SettingGaOptOut = "setting:googleAnalyticsOptOut";
        public const string SettingDisableWebsiteIcons = "setting:disableWebsiteIcons";
        public const string SettingDisableTotpCopy = "setting:disableAutoCopyTotp";
        public const string AutofillPersistNotification = "setting:persistNotification";
        public const string AutofillPasswordField = "setting:autofillPasswordField";

        public const string PasswordGeneratorLength = "pwGenerator:length";
        public const string PasswordGeneratorUppercase = "pwGenerator:uppercase";
        public const string PasswordGeneratorLowercase = "pwGenerator:lowercase";
        public const string PasswordGeneratorNumbers = "pwGenerator:numbers";
        public const string PasswordGeneratorMinNumbers = "pwGenerator:minNumbers";
        public const string PasswordGeneratorSpecial = "pwGenerator:special";
        public const string PasswordGeneratorMinSpecial = "pwGenerator:minSpecial";
        public const string PasswordGeneratorAmbiguous = "pwGenerator:ambiguous";

        public const string PushInitialPromptShown = "push:initialPromptShown";
        public const string PushLastRegistrationDate = "push:lastRegistrationDate";

        public const string ExtensionStarted = "extension:started";
        public const string ExtensionActivated = "extension:activated";

        public const string SecurityStamp = "other:securityStamp";
        public const string LastActivityDate = "other:lastActivityDate";
        public const string LastCacheClearDate = "other:cacheClearDate";
        public const string Locked = "other:locked";
        public const string LastLoginEmail = "other:lastLoginEmail";
        public const string LastSync = "other:lastSync";
        public const string LastBuildKey = "LastBuild";
        public const string BaseUrl = "other:baseUrl";
        public const string WebVaultUrl = "other:webVaultUrl";
        public const string ApiUrl = "other:apiUrl";
        public const string IdentityUrl = "other:identityUrl";
        public const string IconsUrl = "other:iconsUrl";

        public const int SelectFileRequestCode = 42;
        public const int SelectFilePermissionRequestCode = 43;
    }
}
