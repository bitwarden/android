using System.Collections.Generic;
using Bit.Core.Enums;
using Newtonsoft.Json;

namespace Bit.Core.Models.Response
{
    public class IdentityTwoFactorResponse
    {
        public List<TwoFactorProviderType> TwoFactorProviders { get; set; }
        public Dictionary<TwoFactorProviderType, Dictionary<string, object>> TwoFactorProviders2 { get; set; }
        [JsonProperty("CaptchaBypassToken")]
        public string CaptchaToken { get; set; }
    }
}
