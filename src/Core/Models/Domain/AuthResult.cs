using System;
using System.Collections.Generic;
using Bit.Core.Enums;

namespace Bit.Core.Models.Domain
{
    public class AuthResult
    {
        public bool TwoFactor { get; set; }
        public bool CaptchaNeeded => !string.IsNullOrWhiteSpace(CaptchaSiteKey);
        public string CaptchaSiteKey { get; set; }
        // TODO: PM-3287 - Remove after 3 releases of backwards compatibility - Target release 2023.12
        [Obsolete("Use AccountDecryptionOptions to determine if the user does not have a MP")]
        public bool ResetMasterPassword { get; set; }
        public bool ForcePasswordReset { get; set; }
        public bool RequiresEncryptionKeyMigration { get; set; }
        public Dictionary<TwoFactorProviderType, Dictionary<string, object>> TwoFactorProviders { get; set; }
    }
}
