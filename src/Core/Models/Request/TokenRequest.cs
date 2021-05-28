using Bit.Core.Enums;
using System;
using System.Collections.Generic;
using System.Net.Http.Headers;
using System.Text;

namespace Bit.Core.Models.Request
{
    public class TokenRequest
    {
        public string Email { get; set; }
        public string MasterPasswordHash { get; set; }
        public string Code { get; set; }
        public string CodeVerifier { get; set; }
        public string RedirectUri { get; set; }
        public string Token { get; set; }
        public TwoFactorProviderType? Provider { get; set; }
        public bool? Remember { get; set; }
        public DeviceRequest Device { get; set; }

        public TokenRequest(string[] credentials, string[] codes, TwoFactorProviderType? provider, string token,
            bool? remember, DeviceRequest device = null)
        {
            if (credentials != null && credentials.Length > 1)
            {
                Email = credentials[0];
                MasterPasswordHash = credentials[1];
            }
            else if (codes != null && codes.Length > 2)
            {
                Code = codes[0];
                CodeVerifier = codes[1];
                RedirectUri = codes[2];
            }
            Token = token;
            Provider = provider;
            Remember = remember;
            Device = device;
        }

        public Dictionary<string, string> ToIdentityToken(string clientId)
        {
            var obj = new Dictionary<string, string>
            {
                ["scope"] = "api offline_access",
                ["client_id"] = clientId
            };

            if (MasterPasswordHash != null && Email != null)
            {
                obj.Add("grant_type", "password");
                obj.Add("username", Email);
                obj.Add("password", MasterPasswordHash);
            }
            else if (Code != null && CodeVerifier != null && RedirectUri != null)
            {
                obj.Add("grant_type", "authorization_code");
                obj.Add("code", Code);
                obj.Add("code_verifier", CodeVerifier);
                obj.Add("redirect_uri", RedirectUri);
            }
            else
            {
                throw new Exception("must provide credentials or codes");
            }

            if (Device != null)
            {
                obj.Add("deviceType", ((int)Device.Type).ToString());
                obj.Add("deviceIdentifier", Device.Identifier);
                obj.Add("deviceName", Device.Name);
                obj.Add("devicePushToken", Device.PushToken);
            }
            if (!string.IsNullOrWhiteSpace(Token) && Provider != null && Remember.HasValue)
            {
                obj.Add("twoFactorToken", Token);
                obj.Add("twoFactorProvider", ((int)Provider.Value).ToString());
                obj.Add("twoFactorRemember", Remember.GetValueOrDefault() ? "1" : "0");
            }
            return obj;
        }

        public void AlterIdentityTokenHeaders(HttpRequestHeaders headers)
        {
            if (MasterPasswordHash != null && Email != null)
            {
                headers.Add("Auth-Email", Email);
            }
        }
    }
}
