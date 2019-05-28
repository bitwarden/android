using Bit.Core.Enums;
using System.Collections.Generic;

namespace Bit.Core.Models.Domain
{
    public class AuthResult
    {
        public bool TwoFactor { get; set; }
        public Dictionary<TwoFactorProviderType, Dictionary<string, object>> TwoFactorProviders { get; set; }
    }
}
