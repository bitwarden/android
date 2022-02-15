using Bit.Core.Enums;

namespace Bit.Core.Models.Request.IdentityToken
{
    public class TokenRequestTwoFactor
    {
        public TwoFactorProviderType? Provider { get; set; }
        public string Token { get; set; }
        public bool? Remember { get; set; }

        public TokenRequestTwoFactor()
        {
        }
    }
}
