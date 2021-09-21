using Bit.Core.Enums;
using Newtonsoft.Json;
using System.Collections.Generic;

namespace Bit.Core.Models.Response
{
    public class IdentityCaptchaResponse
    {
        [JsonProperty("HCaptcha_SiteKey")]
        public string SiteKey { get; set; }
    }

}
