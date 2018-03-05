using System.Collections;
using System.Collections.Generic;
using System.Linq;

namespace Bit.App.Models.Api
{
    public class LoginType
    {
        public LoginType() { }

        public LoginType(Cipher cipher)
        {
            Uris = cipher.Login.Uris.Select(u => new LoginUriType(u));
            Username = cipher.Login.Username?.EncryptedString;
            Password = cipher.Login.Password?.EncryptedString;
            Totp = cipher.Login.Totp?.EncryptedString;
        }

        public IEnumerable<LoginUriType> Uris { get; set; }
        public string Username { get; set; }
        public string Password { get; set; }
        public string Totp { get; set; }
    }
}
