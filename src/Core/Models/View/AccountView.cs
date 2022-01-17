using Bit.Core.Enums;
using Bit.Core.Models.Domain;

namespace Bit.Core.Models.View
{
    public class AccountView : View
    {
        public AccountView() { }

        public AccountView(Account a = null)
        {
            if (a == null)
            {
                // null will render as "Add Account" row
                return;
            }
            IsAccount = true;

            UserId = a.Profile.UserId;
            Email = a.Profile.Email;
            Hostname = a.Settings.EnvironmentUrls.Base;
            AuthStatus = a.Profile.AuthStatus;
        }

        public bool IsAccount { get; set; }
        public string UserId { get; set; }
        public string Email { get; set; }
        public string Hostname { get; set; }
        public AuthenticationStatus? AuthStatus { get; set; }
    }
}
