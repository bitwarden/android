namespace Bit.Core
{
    public static class ExternalLinksConstants
    {
        public const string HELP_CENTER = "https://bitwarden.com/help/";
        public const string HELP_ABOUT_ORGANIZATIONS = "https://bitwarden.com/help/about-organizations/";
        public const string HELP_FINGERPRINT_PHRASE = "https://bitwarden.com/help/fingerprint-phrase/";

        public const string CONTACT_SUPPORT = "https://bitwarden.com/contact/";

        /// <summary>
        /// Link to go to settings website. Requires to pass website URL as parameter.
        /// </summary>
        public const string WEB_VAULT_SETTINGS_FORMAT = "{0}/#/settings";

        /// <summary>
        /// Link to go to individual vault import page. Requires to pass vault URL as parameter.
        /// </summary>
        public const string WEB_VAULT_TOOLS_IMPORT_FORMAT = "{0}/#/tools/import";

        /// <summary>
        /// General website, not in the full format of a URL given that this is used as parameter of string resources to be shown to the user.
        /// </summary>
        public const string BITWARDEN_WEBSITE = "bitwarden.com";
    }
}
