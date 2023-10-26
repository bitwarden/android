using System;
using System.Collections.Generic;
using System.Linq;
using Bit.Core.Models.Api;

namespace Bit.Core.Models.Data
{
    public class LoginData : Data
    {
        public LoginData() { }

        public LoginData(LoginApi data)
        {
            Username = data.Username;
            Password = data.Password;
            PasswordRevisionDate = data.PasswordRevisionDate;
            Totp = data.Totp;
            Uris = data.Uris?.Select(u => new LoginUriData(u)).ToList();
            Fido2Credentials = data.Fido2Credentials?.Select(f => new Fido2CredentialData(f)).ToList();
        }

        public List<LoginUriData> Uris { get; set; }
        public string Username { get; set; }
        public string Password { get; set; }
        public DateTime? PasswordRevisionDate { get; set; }
        public string Totp { get; set; }
        public List<Fido2CredentialData> Fido2Credentials { get; set; }
    }
}
