using Bit.Core.Enums;
using System.Collections.Generic;

namespace Bit.Core.Models.Domain
{
    public class AuthResult
    {
        public bool TwoFactor { get; set; }
        public bool CaptchaNeeded => !string.IsNullOrWhiteSpace(CaptchaSiteKey);
        public string CaptchaSiteKey { get; set; }
        public bool ResetMasterPassword { get; set; }
        public bool ForcePasswordReset { get; set; }
        public Dictionary<TwoFactorProviderType, Dictionary<string, object>> TwoFactorProviders { get; set; }
    }
}
