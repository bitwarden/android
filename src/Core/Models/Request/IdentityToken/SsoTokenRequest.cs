using System.Collections.Generic;

namespace Bit.Core.Models.Request.IdentityToken
{
    public class SsoTokenRequest : TokenRequest
    {
        public string Code { get; set; }
        public string CodeVerifier { get; set; }
        public string RedirectUri { get; set; }

        public SsoTokenRequest(string code, string codeVerifier, string redirectUri, TokenRequestTwoFactor twoFactor, DeviceRequest device = null): base(twoFactor, device)
        {
            Code = code;
            CodeVerifier = codeVerifier;
            RedirectUri = RedirectUri;
        }

        public override Dictionary<string, string> ToIdentityToken(string clientId)
        {
            var obj = base.ToIdentityToken(clientId);
            
            obj.Add("grant_type", "authorization_code");
            obj.Add("code", Code);
            obj.Add("code_verifier", CodeVerifier);
            obj.Add("redirect_uri", RedirectUri);

            return obj;
        }
    }
}
