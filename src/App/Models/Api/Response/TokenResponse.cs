using Bit.App.Enums;
using Newtonsoft.Json;
using System.Collections.Generic;

namespace Bit.App.Models.Api
{
    public class TokenResponse
    {
        [JsonProperty("access_token")]
        public string AccessToken { get; set; }
        [JsonProperty("expires_in")]
        public long ExpiresIn { get; set; }
        [JsonProperty("refresh_token")]
        public string RefreshToken { get; set; }
        [JsonProperty("token_type")]
        public string TokenType { get; set; }
        public Dictionary<TwoFactorProviderType, Dictionary<string, object>> TwoFactorProviders2 { get; set; }
        public string PrivateKey { get; set; }
        public string TwoFactorToken { get; set; }
        public string Key { get; set; }
    }
}
