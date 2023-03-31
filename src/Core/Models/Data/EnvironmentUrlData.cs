namespace Bit.Core.Models.Data
{
    public class EnvironmentUrlData
    {
        public static EnvironmentUrlData DefaultUS = new EnvironmentUrlData { Base = "https://vault.bitwarden.com" };
        public static EnvironmentUrlData DefaultEU = new EnvironmentUrlData { Base = "https://vault.bitwarden.eu" };

        public string Base { get; set; }
        public string Api { get; set; }
        public string Identity { get; set; }
        public string Icons { get; set; }
        public string Notifications { get; set; }
        public string WebVault { get; set; }
        public string Events { get; set; }

        public bool IsEmpty() => string.IsNullOrEmpty(this.Base)
                && string.IsNullOrEmpty(this.Api)
                && string.IsNullOrEmpty(this.Identity)
                && string.IsNullOrEmpty(this.Icons)
                && string.IsNullOrEmpty(this.Notifications)
                && string.IsNullOrEmpty(this.WebVault)
                && string.IsNullOrEmpty(this.Events);
    }
}
