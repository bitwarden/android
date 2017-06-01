namespace Bit.App
{
    public static class Constants
    {
        public const string AndroidAppProtocol = "androidapp://";

        public const string SettingFingerprintUnlockOn = "setting:fingerprintUnlockOn";
        public const string SettingPinUnlockOn = "setting:pinUnlockOn";
        public const string SettingLockSeconds = "setting:lockSeconds";
        public const string SettingGaOptOut = "setting:googleAnalyticsOptOut";
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
        public const string Locked = "other:locked";
        public const string LastLoginEmail = "other:lastLoginEmail";
        public const string LastSync = "other:lastSync";
    }
}
