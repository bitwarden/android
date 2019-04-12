using Bit.Core.Models.Domain;
using System;
using System.Collections.Generic;

namespace Bit.Core.Models.View
{
    public class LoginView : View
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
        public string Uri => HashUris ? Uris[0].Uri : null;
        public string MaskedPassword => Password != null ? "••••••••" : null;
        public string SubTitle => Username;
        // TODO: uri launch props
        public bool HashUris => (Uris?.Count ?? 0) > 0;
    }
}
