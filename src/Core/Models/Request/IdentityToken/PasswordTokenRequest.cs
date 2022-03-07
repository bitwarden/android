using Bit.Core.Utilities;
using System.Collections.Generic;
using System.Net.Http.Headers;
using System.Text;

namespace Bit.Core.Models.Request.IdentityToken
{
    public class PasswordTokenRequest : TokenRequest
    {
        public string Email { get; set; }
        public string MasterPasswordHash { get; set; }
        public string CaptchaToken { get; set; }

        public PasswordTokenRequest(string email, string masterPasswordHash, string captchaToken, TokenRequestTwoFactor twoFactor, DeviceRequest device = null) : base(twoFactor, device)
        {
            Email = email;
            MasterPasswordHash = masterPasswordHash;
            CaptchaToken = captchaToken;
        }

        public override Dictionary<string, string> ToIdentityToken(string clientId)
        {
            var obj = base.ToIdentityToken(clientId);

            obj.Add("grant_type", "password");
            obj.Add("username", Email);
            obj.Add("password", MasterPasswordHash);

            if (CaptchaToken != null)
            {
                obj.Add("captchaResponse", CaptchaToken);
            }

            return obj;
        }

        public override void AlterIdentityTokenHeaders(HttpRequestHeaders headers)
        {
            if (MasterPasswordHash != null && Email != null)
            {
                headers.Add("Auth-Email", CoreHelpers.Base64UrlEncode(Encoding.UTF8.GetBytes(Email)));
            }
        }
    }
}
