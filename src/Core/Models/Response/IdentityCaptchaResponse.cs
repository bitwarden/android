using System.Collections.Generic;
using Bit.Core.Enums;
using Newtonsoft.Json;

namespace Bit.Core.Models.Response
{
    public class IdentityCaptchaResponse
    {
        [JsonProperty("HCaptcha_SiteKey")]
        public string SiteKey { get; set; }
    }

}
