using Bit.App.Enums;
using System.Collections.Generic;

namespace Bit.App.Models
{
    public class LoginResult
    {
        public bool Success { get; set; }
        public string ErrorMessage { get; set; }
    }

    public class FullLoginResult : LoginResult
    {
        public bool TwoFactorRequired => TwoFactorProviders != null && TwoFactorProviders.Count > 0;
        public Dictionary<TwoFactorProviderType, Dictionary<string, object>> TwoFactorProviders { get; set; }
        public SymmetricCryptoKey Key { get; set; }
        public string MasterPasswordHash { get; set; }
    }
}
