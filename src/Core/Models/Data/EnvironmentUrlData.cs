using System.Text.RegularExpressions;
using Bit.Core.Enums;
using Bit.Core.Utilities;

namespace Bit.Core.Models.Data
{
    public class EnvironmentUrlData
    {
        public static EnvironmentUrlData DefaultUS = new EnvironmentUrlData
        {
            Base = "https://vault.bitwarden.com",
            Api = "https://api.bitwarden.com",
            Identity = "https://identity.bitwarden.com",
            Icons = "https://icons.bitwarden.net",
            WebVault = "https://vault.bitwarden.com",
            Notifications = "https://notifications.bitwarden.com",
            Events = "https://events.bitwarden.com",
            Domain = "bitwarden.com"
        };

        public static EnvironmentUrlData DefaultEU = new EnvironmentUrlData
        {
            Base = "https://vault.bitwarden.eu",
            Api = "https://api.bitwarden.eu",
            Identity = "https://identity.bitwarden.eu",
            Icons = "https://icons.bitwarden.eu",
            WebVault = "https://vault.bitwarden.eu",
            Notifications = "https://notifications.bitwarden.eu",
            Events = "https://events.bitwarden.eu",
            Domain = "bitwarden.eu"
        };

        public string Base { get; set; }
        public string Api { get; set; }
        public string Identity { get; set; }
        public string Icons { get; set; }
        public string Notifications { get; set; }
        public string WebVault { get; set; }
        public string Events { get; set; }
        public string Domain { get; set; }

        public bool IsEmpty => string.IsNullOrEmpty(Base)
                && string.IsNullOrEmpty(Api)
                && string.IsNullOrEmpty(Identity)
                && string.IsNullOrEmpty(Icons)
                && string.IsNullOrEmpty(Notifications)
                && string.IsNullOrEmpty(WebVault)
                && string.IsNullOrEmpty(Events);

        public Region Region
        {
            get
            {
                if (Base == Region.US.BaseUrl())
                {
                    return Region.US;
                }
                if (Base == Region.EU.BaseUrl())
                {
                    return Region.EU;
                }
                return Region.SelfHosted;
            }
        }

        public EnvironmentUrlData FormatUrls()
        {
            return new EnvironmentUrlData
            {
                Base = FormatUrl(Base),
                Api = FormatUrl(Api),
                Identity = FormatUrl(Identity),
                Icons = FormatUrl(Icons),
                Notifications = FormatUrl(Notifications),
                WebVault = FormatUrl(WebVault),
                Events = FormatUrl(Events)
            };
        }

        private string FormatUrl(string url)
        {
            if (string.IsNullOrWhiteSpace(url))
            {
                return null;
            }
            url = Regex.Replace(url, "\\/+$", string.Empty);
            if (!url.StartsWith("http://") && !url.StartsWith("https://"))
            {
                url = string.Concat("https://", url);
            }
            return url.Trim();
        }

        public string ParseEndpoint()
        {
            var url = WebVault ?? Base ?? Api ?? Identity;
            if (!string.IsNullOrWhiteSpace(url))
            {
                if (url.Contains(Region.US.Domain()) || url.Contains(Region.EU.Domain()))
                {
                    return CoreHelpers.GetDomain(url);
                }
                return CoreHelpers.GetHostname(url);
            }
            return string.Empty;
        }
    }
}
