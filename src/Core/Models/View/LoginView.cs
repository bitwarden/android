using Bit.Core.Enums;
using Bit.Core.Models.Domain;

namespace Bit.Core.Models.View
{
    public class LoginView : ItemView
    {
        public LoginView() { }

        public LoginView(Login l)
        {
            PasswordRevisionDate = l.PasswordRevisionDate;
        }

        public string Username { get; set; }
        public string Password { get; set; }
        public DateTime? PasswordRevisionDate { get; set; }
        public string Totp { get; set; }
        public List<LoginUriView> Uris { get; set; }
        public List<Fido2CredentialView> Fido2Credentials { get; set; }

        public string Uri => HasUris ? Uris[0].Uri : null;
        public string MaskedPassword => Password != null ? "••••••••" : null;
        public override string SubTitle => Username;
        public bool CanLaunch => HasUris && Uris.Any(u => u.CanLaunch);
        public string LaunchUri => HasUris ? Uris.FirstOrDefault(u => u.CanLaunch)?.LaunchUri : null;
        public bool HasUris => (Uris?.Count ?? 0) > 0;
        public bool HasFido2Credentials => Fido2Credentials?.Any() == true;
        public Fido2CredentialView MainFido2Credential => HasFido2Credentials ? Fido2Credentials[0] : null;

        public override List<KeyValuePair<string, LinkedIdType>> LinkedFieldOptions
        {
            get => new List<KeyValuePair<string, LinkedIdType>>()
            {
                new KeyValuePair<string, LinkedIdType>("Username", LinkedIdType.Login_Username),
                new KeyValuePair<string, LinkedIdType>("Password", LinkedIdType.Login_Password),
            };
        }
    }
}
