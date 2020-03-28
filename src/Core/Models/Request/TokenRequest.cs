using Bit.Core.Enums;
using System;
using System.Collections.Generic;
using System.Text;

namespace Bit.Core.Models.Request
{
    public class TokenRequest
    {
        public string Email { get; set; }
        public string MasterPasswordHash { get; set; }
        public string Token { get; set; }
        public TwoFactorProviderType? Provider { get; set; }
        public bool Remember { get; set; }
        public DeviceRequest Device { get; set; }

        public Dictionary<string, string> ToIdentityToken(string clientId)
        {
            var obj = new Dictionary<string, string>
            {
                ["grant_type"] = "password",
                ["username"] = Email,
                ["password"] = MasterPasswordHash,
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
            if (!string.IsNullOrWhiteSpace(Token) && Provider != null)
            {
                obj.Add("twoFactorToken", Token);
                obj.Add("twoFactorProvider", ((int)Provider.Value).ToString());
                obj.Add("twoFactorRemember", Remember ? "1" : "0");
            }
            return obj;
        }
    }
}
