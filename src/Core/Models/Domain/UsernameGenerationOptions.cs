using Bit.Core.Enums;

namespace Bit.Core.Models.Domain
{
    public class UsernameGenerationOptions
    {
        public UsernameGenerationOptions()
        {
            ServiceType = ForwardedEmailServiceType.None;
        }

        public UsernameType Type { get; set; }
        public ForwardedEmailServiceType ServiceType { get; set; }
        public UsernameEmailType PlusAddressedEmailType { get; set; }
        public UsernameEmailType CatchAllEmailType { get; set; }
        public bool CapitalizeRandomWordUsername { get; set; }
        public bool IncludeNumberRandomWordUsername { get; set; }
        public string PlusAddressedEmail { get; set; }
        public string CatchAllEmailDomain { get; set; }
        public string FirefoxRelayApiAccessToken { get; set; }
        public string SimpleLoginApiKey { get; set; }
        public string DuckDuckGoApiKey { get; set; }
        public string FastMailApiKey { get; set; }
        public string AnonAddyApiAccessToken { get; set; }
        public string AnonAddyDomainName { get; set; }
        public string EmailWebsite { get; set; }
    }
}
