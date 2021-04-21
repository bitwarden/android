using Bit.Core.Models.Data;
using Bit.Core.Models.View;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace Bit.Core.Models.Domain
{
    public class Login : Domain
    {
        public Login() { }

        public Login(LoginData obj, bool alreadyEncrypted = false)
        {
            PasswordRevisionDate = obj.PasswordRevisionDate;
            Uris = obj.Uris?.Select(u => new LoginUri(u, alreadyEncrypted)).ToList();
            BuildDomainModel(this, obj, new HashSet<string>
            {
                "Username",
                "Password",
                "Totp"
            }, alreadyEncrypted);
        }

        public List<LoginUri> Uris { get; set; }
        public EncString Username { get; set; }
        public EncString Password { get; set; }
        public DateTime? PasswordRevisionDate { get; set; }
        public EncString Totp { get; set; }

        public async Task<LoginView> DecryptAsync(string orgId)
        {
            var view = await DecryptObjAsync(new LoginView(this), this, new HashSet<string>
            {
                "Username",
                "Password",
                "Totp"
            }, orgId);
            if (Uris != null)
            {
                view.Uris = new List<LoginUriView>();
                foreach (var uri in Uris)
                {
                    view.Uris.Add(await uri.DecryptAsync(orgId));
                }
            }
            return view;
        }

        public LoginData ToLoginData()
        {
            var l = new LoginData();
            l.PasswordRevisionDate = PasswordRevisionDate;
            BuildDataModel(this, l, new HashSet<string>
            {
                "Username",
                "Password",
                "Totp"
            });
            if (Uris?.Any() ?? false)
            {
                l.Uris = Uris.Select(u => u.ToLoginUriData()).ToList();
            }
            return l;
        }
    }
}
