using Bit.Core.Enums;
using Bit.Core.Models.Data;
using Bit.Core.Models.Domain;
using Bit.Core.Utilities;

namespace Bit.Core.Models.View
{
    public class AccountView : View
    {
        public AccountView() { }

        public AccountView(Account a = null, bool isActive = false)
        {
            if (a == null)
            {
                // null will render as "Add Account" row
                return;
            }
            IsAccount = true;
            IsActive = isActive;
            UserId = a.Profile?.UserId;
            Email = a.Profile?.Email;
            Name = a.Profile?.Name;
            AvatarColor = a.Profile?.AvatarColor;
            Hostname = ParseEndpoint(a.Settings?.EnvironmentUrls);
        }

        private string ParseEndpoint(EnvironmentUrlData urls)
        {
            var url = urls?.WebVault ?? urls?.Base;
            if (!string.IsNullOrWhiteSpace(url))
            {
                if (url.Contains("bitwarden.com") || url.Contains("bitwarden.eu"))
                {
                    return CoreHelpers.GetDomain(url);
                }
                return CoreHelpers.GetHostname(url);
            }
            return string.Empty;
        }

        public bool IsAccount { get; set; }
        public AuthenticationStatus? AuthStatus { get; set; }
        public bool IsActive { get; set; }
        public string UserId { get; set; }
        public string Email { get; set; }
        public string Name { get; set; }
        public string Hostname { get; set; }
        public string AvatarColor { get; set; }
    }
}
