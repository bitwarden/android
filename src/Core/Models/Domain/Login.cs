using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Bit.Core.Models.Data;
using Bit.Core.Models.View;

namespace Bit.Core.Models.Domain
{
    public class Login : Domain
    {
        public Login() { }

        public Login(LoginData obj, bool alreadyEncrypted = false)
        {
            PasswordRevisionDate = obj.PasswordRevisionDate;
            Uris = obj.Uris?.Select(u => new LoginUri(u, alreadyEncrypted)).ToList();
            Fido2Keys = obj.Fido2Keys?.Select(f => new Fido2Key(f, alreadyEncrypted)).ToList();
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
        public List<Fido2Key> Fido2Keys { get; set; }

        public async Task<LoginView> DecryptAsync(string orgId, SymmetricCryptoKey key = null)
        {
            var view = await DecryptObjAsync(new LoginView(this), this, new HashSet<string>
            {
                "Username",
                "Password",
                "Totp"
            }, orgId, key);
            if (Uris != null)
            {
                view.Uris = new List<LoginUriView>();
                foreach (var uri in Uris)
                {
                    view.Uris.Add(await uri.DecryptAsync(orgId, key));
                }
            }
            if (Fido2Keys != null)
            {
                view.Fido2Keys = new List<Fido2KeyView>();
                foreach (var fido2Key in Fido2Keys)
                {
                    view.Fido2Keys.Add(await fido2Key.DecryptAsync(orgId, key));
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
            if (Fido2Keys != null)
            {
                l.Fido2Keys = Fido2Keys.Select(f => f.ToFido2KeyData()).ToList();
            }
            return l;
        }
    }
}
