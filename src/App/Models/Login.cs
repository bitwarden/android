using Bit.App.Models.Data;

namespace Bit.App.Models
{
    public class Login
    {
        public Login() { }

        public Login(CipherData data)
        {
            Uri = data.Uri != null ? new CipherString(data.Uri) : null;
            Username = data.Username != null ? new CipherString(data.Username) : null;
            Password = data.Password != null ? new CipherString(data.Password) : null;
            Totp = data.Totp != null ? new CipherString(data.Totp) : null;
        }

        public CipherString Uri { get; set; }
        public CipherString Username { get; set; }
        public CipherString Password { get; set; }
        public CipherString Totp { get; set; }
    }
}
