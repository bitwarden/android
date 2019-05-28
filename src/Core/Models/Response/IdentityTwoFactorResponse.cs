using Bit.Core.Enums;
using System.Collections.Generic;

namespace Bit.Core.Models.Response
{
    public class IdentityTwoFactorResponse
    {
        public List<TwoFactorProviderType> TwoFactorProviders { get; set; }
        public Dictionary<TwoFactorProviderType, Dictionary<string, object>> TwoFactorProviders2 { get; set; }
    }
}
