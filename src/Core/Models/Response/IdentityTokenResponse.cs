using Newtonsoft.Json;

namespace Bit.Core.Models.Response
{
    public class IdentityTokenResponse
    {
        [JsonProperty("access_token")]
        public string AccessToken { get; set; }
        [JsonProperty("expires_in")]
        public string ExpiresIn { get; set; }
        [JsonProperty("refresh_token")]
        public string RefreshToken { get; set; }
        [JsonProperty("token_type")]
        public string TokenType { get; set; }
        public string PrivateKey { get; set; }
        public string Key { get; set; }
        public string TwoFactorToken { get; set; }
    }
}
