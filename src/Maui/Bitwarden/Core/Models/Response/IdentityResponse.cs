using System.Net;
using Newtonsoft.Json.Linq;

namespace Bit.Core.Models.Response
{
    public class IdentityResponse
    {
        public IdentityTokenResponse TokenResponse { get; }
        public IdentityTwoFactorResponse TwoFactorResponse { get; }
        public IdentityCaptchaResponse CaptchaResponse { get; }

        public bool TwoFactorNeeded => TwoFactorResponse != null;
        public bool FailedToParse { get; }

        public IdentityResponse(HttpStatusCode httpStatusCode, JObject responseJObject)
        {
            var parsed = false;
            if (responseJObject != null)
            {
                if (IsSuccessStatusCode(httpStatusCode))
                {
                    TokenResponse = responseJObject.ToObject<IdentityTokenResponse>();
                    parsed = true;
                }
                else if (httpStatusCode == HttpStatusCode.BadRequest)
                {
                    if (JObjectHasProperty(responseJObject, "TwoFactorProviders2"))
                    {
                        TwoFactorResponse = responseJObject.ToObject<IdentityTwoFactorResponse>();
                        parsed = true;
                    }
                    else if (JObjectHasProperty(responseJObject, "HCaptcha_SiteKey"))
                    {
                        CaptchaResponse = responseJObject.ToObject<IdentityCaptchaResponse>();
                        parsed = true;
                    }
                }
            }
            FailedToParse = !parsed;
        }

        private bool IsSuccessStatusCode(HttpStatusCode httpStatusCode) =>
            (int)httpStatusCode >= 200 && (int)httpStatusCode < 300;

        private bool JObjectHasProperty(JObject jObject, string propertyName) =>
            jObject.ContainsKey(propertyName) &&
            jObject[propertyName] != null &&
            (jObject[propertyName].HasValues || jObject[propertyName].Value<string>() != null);
    }
}
