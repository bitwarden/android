namespace Bit.iOS.Core
{
    public static class Constants
    {
        public const string AppExtensionVersionNumberKey = "version_number";
        public const string AppExtensionUrlStringKey = "url_string";
        public const string AppExtensionUsernameKey = "username";
        public const string AppExtensionPasswordKey = "password";
        public const string AppExtensionTotpKey = "totp";
        public const string AppExtensionTitleKey = "login_title";
        public const string AppExtensionNotesKey = "notes";
        public const string AppExtensionSectionTitleKey = "section_title";
        public const string AppExtensionFieldsKey = "fields";
        public const string AppExtensionReturnedFieldsKey = "returned_fields";
        public const string AppExtensionOldPasswordKey = "old_password";
        public const string AppExtensionPasswordGeneratorOptionsKey = "password_generator_options";
        public const string AppExtensionGeneratedPasswordMinLengthKey = "password_min_length";
        public const string AppExtensionGeneratedPasswordMaxLengthKey = "password_max_length";
        public const string AppExtensionGeneratedPasswordRequireDigitsKey = "password_require_digits";
        public const string AppExtensionGeneratedPasswordRequireSymbolsKey = "password_require_symbols";
        public const string AppExtensionGeneratedPasswordForbiddenCharactersKey = "password_forbidden_characters";
        public const string AppExtensionWebViewPageFillScript = "fillScript";
        public const string AppExtensionWebViewPageDetails = "pageDetails";

        public const string UTTypeAppExtensionFindLoginAction = "org.appextension.find-login-action";
        public const string UTTypeAppExtensionSaveLoginAction = "org.appextension.save-login-action";
        public const string UTTypeAppExtensionChangePasswordAction = "org.appextension.change-password-action";
        public const string UTTypeAppExtensionFillWebViewAction = "org.appextension.fill-webview-action";
        public const string UTTypeAppExtensionFillBrowserAction = "org.appextension.fill-browser-action";
        public const string UTTypeAppExtensionSetup = "com.8bit.bitwarden.extension-setup";
        public const string UTTypeAppExtensionUrl = "public.url";

        public const string AutofillNeedsIdentityReplacementKey = "autofillNeedsIdentityReplacement";
    }
}
