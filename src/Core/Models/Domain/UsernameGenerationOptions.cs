using Bit.Core.Enums;
using Bit.Core.Services.EmailForwarders;

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
        public string AddyIoApiAccessToken { get; set; }
        public string AddyIoDomainName { get; set; }
        public string EmailWebsite { get; set; }

        public ForwarderOptions GetForwarderOptions()
        {
            if (Type != UsernameType.ForwardedEmailAlias)
            {
                return null;
            }

            switch (ServiceType)
            {
                case ForwardedEmailServiceType.AddyIo:
                    return new AddyIoForwarderOptions
                    {
                        ApiKey = AddyIoApiAccessToken,
                        DomainName = AddyIoDomainName
                    };
                case ForwardedEmailServiceType.DuckDuckGo:
                    return new ForwarderOptions { ApiKey = DuckDuckGoApiKey };
                case ForwardedEmailServiceType.Fastmail:
                    return new ForwarderOptions { ApiKey = FastMailApiKey };
                case ForwardedEmailServiceType.FirefoxRelay:
                    return new ForwarderOptions { ApiKey = FirefoxRelayApiAccessToken };
                case ForwardedEmailServiceType.SimpleLogin:
                    return new ForwarderOptions { ApiKey = SimpleLoginApiKey };
                default:
                    return null;
            }
        }
    }
}
