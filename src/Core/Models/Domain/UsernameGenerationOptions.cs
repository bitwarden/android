using Bit.Core.Enums;

namespace Bit.Core.Models.Domain
{
    public class UsernameGenerationOptions
    {
        public UsernameGenerationOptions() { }

        public UsernameGenerationOptions(bool defaultOptions)
        {
            if (defaultOptions)
            {
                RandomWordUsernameCapitalize = false;
                RandomWordUsernameIncludeNumber = false;
                PlusAddressedEmail = string.Empty;
                CatchAllEmailDomain = string.Empty;
                FirefoxRelayApiAccessToken = string.Empty;
                SimpleLoginApiKey = string.Empty;
                AnonAddyApiAccessToken = string.Empty;
                AnonAddyDomainName = string.Empty;
                EmailWebsite = string.Empty;
            }
        }

        public UsernameType Type { get; set; }
        public ForwardedEmailServiceType ServiceType { get; set; }
        public UsernameEmailType PlusAddressedEmailType { get; set; }
        public UsernameEmailType CatchAllEmailType { get; set; }
        public bool? RandomWordUsernameCapitalize { get; set; }
        public bool? RandomWordUsernameIncludeNumber { get; set; }
        public string PlusAddressedEmail { get; set; }
        public string CatchAllEmailDomain { get; set; }
        public string FirefoxRelayApiAccessToken { get; set; }
        public string SimpleLoginApiKey { get; set; }
        public string AnonAddyApiAccessToken { get; set; }
        public string AnonAddyDomainName { get; set; }
        public string EmailWebsite { get; set; }

        public void Merge(UsernameGenerationOptions defaults)
        {
            RandomWordUsernameCapitalize = RandomWordUsernameCapitalize ?? defaults.RandomWordUsernameCapitalize;
            RandomWordUsernameIncludeNumber = RandomWordUsernameIncludeNumber ?? defaults.RandomWordUsernameIncludeNumber;
            PlusAddressedEmail = PlusAddressedEmail ?? defaults.PlusAddressedEmail;
            CatchAllEmailDomain = CatchAllEmailDomain ?? defaults.CatchAllEmailDomain;
            FirefoxRelayApiAccessToken = FirefoxRelayApiAccessToken ?? defaults.FirefoxRelayApiAccessToken;
            SimpleLoginApiKey = SimpleLoginApiKey ?? defaults.SimpleLoginApiKey;
            AnonAddyApiAccessToken = AnonAddyApiAccessToken ?? defaults.AnonAddyApiAccessToken;
            AnonAddyDomainName = AnonAddyDomainName ?? defaults.AnonAddyDomainName;
            EmailWebsite = EmailWebsite ?? defaults.EmailWebsite;
        }
    }
}