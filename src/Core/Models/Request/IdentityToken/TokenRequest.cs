using System.Collections.Generic;
using System.Net.Http.Headers;

namespace Bit.Core.Models.Request.IdentityToken
{
    public abstract class TokenRequest
    {
        protected DeviceRequest Device { get; set; }
        protected TokenRequestTwoFactor TwoFactor { get; set; }

        public TokenRequest(TokenRequestTwoFactor twoFactor, DeviceRequest device = null)
        {
            TwoFactor = twoFactor;
            Device = device;
        }

        public virtual void AlterIdentityTokenHeaders(HttpRequestHeaders headers)
        {
            // Implemented in subclass if required
        }

        public void SetTwoFactor(TokenRequestTwoFactor twoFactor)
        {
            TwoFactor = twoFactor;
        }

        public virtual Dictionary<string, string> ToIdentityToken(string clientId)
        {
            var obj = new Dictionary<string, string>
            {
                ["scope"] = "api offline_access",
                ["client_id"] = clientId
            };

            if (Device != null)
            {
                obj.Add("deviceType", ((int)Device.Type).ToString());
                obj.Add("deviceIdentifier", Device.Identifier);
                obj.Add("deviceName", Device.Name);
                obj.Add("devicePushToken", Device.PushToken);
            }
            if (!string.IsNullOrWhiteSpace(TwoFactor.Token) && TwoFactor.Provider != null && TwoFactor.Remember.HasValue)
            {
                obj.Add("twoFactorToken", TwoFactor.Token);
                obj.Add("twoFactorProvider", ((int)TwoFactor.Provider.Value).ToString());
                obj.Add("twoFactorRemember", TwoFactor.Remember.GetValueOrDefault() ? "1" : "0");
            }

            return obj;
        }
    }
}
