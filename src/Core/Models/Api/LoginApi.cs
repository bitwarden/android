using System;
using System.Collections.Generic;

namespace Bit.Core.Models.Api
{
    public class LoginApi
    {
        public List<LoginUriApi> Uris { get; set; }
        public string Username { get; set; }
        public string Password { get; set; }
        public DateTime? PasswordRevisionDate { get; set; }
        public string Totp { get; set; }
        public List<Fido2CredentialApi> Fido2Credentials { get; set; }
    }
}
