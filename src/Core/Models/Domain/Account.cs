using Bit.Core.Enums;
using Bit.Core.Models.Data;

namespace Bit.Core.Models.Domain
{
    public class Account : Domain
    {
        public AccountProfile Profile;
        public AccountTokens Tokens;
        public AccountSettings Settings;
        public AccountVolatileData VolatileData;

        public Account() { }

        public Account(AccountProfile profile, AccountTokens tokens)
        {
            Profile = profile;
            Tokens = tokens;
            Settings = new AccountSettings();
            VolatileData = new AccountVolatileData();
        }

        public Account(Account account)
        {
            // Copy constructor excludes VolatileData (for storage)
            Profile = new AccountProfile(account.Profile);
            Tokens = new AccountTokens(account.Tokens);
            Settings = new AccountSettings(account.Settings);
        }

        public class AccountProfile
        {
            public AccountProfile() { }

            public AccountProfile(AccountProfile copy)
            {
                if (copy == null)
                {
                    return;
                }

                UserId = copy.UserId;
                Email = copy.Email;
                Name = copy.Name;
                Stamp = copy.Stamp;
                OrgIdentifier = copy.OrgIdentifier;
                KdfType = copy.KdfType;
                KdfIterations = copy.KdfIterations;
                KdfMemory = copy.KdfMemory;
                KdfParallelism = copy.KdfParallelism;
                EmailVerified = copy.EmailVerified;
                HasPremiumPersonally = copy.HasPremiumPersonally;
                AvatarColor = copy.AvatarColor;
            }

            public string UserId;
            public string Email;
            public string Name;
            public string Stamp;
            public string OrgIdentifier;
            public string AvatarColor;
            public KdfType? KdfType;
            public int? KdfIterations;
            public int? KdfMemory;
            public int? KdfParallelism;
            public bool? EmailVerified;
            public bool? HasPremiumPersonally;
        }

        public class AccountTokens
        {
            public AccountTokens() { }

            public AccountTokens(AccountTokens copy)
            {
                if (copy == null)
                {
                    return;
                }

                AccessToken = copy.AccessToken;
                RefreshToken = copy.RefreshToken;
            }

            public string AccessToken;
            public string RefreshToken;
        }

        public class AccountSettings
        {
            public AccountSettings() { }

            public AccountSettings(AccountSettings copy)
            {
                if (copy == null)
                {
                    return;
                }

                EnvironmentUrls = copy.EnvironmentUrls;
                VaultTimeout = copy.VaultTimeout;
                VaultTimeoutAction = copy.VaultTimeoutAction;
                ScreenCaptureAllowed = copy.ScreenCaptureAllowed;
            }

            public EnvironmentUrlData EnvironmentUrls;
            public int? VaultTimeout;
            public VaultTimeoutAction? VaultTimeoutAction;
            public bool ScreenCaptureAllowed;
        }

        public class AccountVolatileData
        {
            public SymmetricCryptoKey Key;
            public EncString PinProtectedKey;
            public bool? BiometricLocked;
        }
    }
}
