namespace Bit.Core.Enums
{
    public enum PolicyType : byte
    {
        TwoFactorAuthentication = 0, // Requires users to have 2fa enabled
        MasterPassword = 1, // Sets minimum requirements for master password complexity
        PasswordGenerator = 2, // Sets minimum requirements/default type for generated passwords/passphrases
        OnlyOrg = 3, // Allows users to only be apart of one organization
        RequireSso = 4, // Requires users to authenticate with SSO
        PersonalOwnership = 5, // Disables personal vault ownership for adding/cloning items
        DisableSend = 6, // Disables the ability to create and edit Sends
        SendOptions = 7, // Sets restrictions or defaults for Bitwarden Sends
    }
}
