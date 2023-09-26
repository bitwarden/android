using System.Runtime.CompilerServices;

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
    }
}
