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

        public override bool Equals(object obj)
        {
            if (obj is null)
            {
                return false;
            }

            if (obj is EnvironmentUrlData env)
            {
                return env.Base == this.Base
                    && env.Api == this.Api
                    && env.Identity == this.Identity
                    && env.Icons == this.Icons
                    && env.Notifications == this.Notifications
                    && env.WebVault == this.WebVault
                    && env.Events == this.Events;
            }

            return base.Equals(obj);
        }
    }
}
