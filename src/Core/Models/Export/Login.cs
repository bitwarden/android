using System.Collections.Generic;
using System.Linq;
using Bit.Core.Models.View;

namespace Bit.Core.Models.Export
{
    public class Login
    {
        public Login() { }

        public Login(LoginView obj)
        {
            Uris = obj.Uris?.Select(u => new LoginUri(u)).ToList();

            Username = obj.Username;
            Password = obj.Password;
            Totp = obj.Totp;
        }

        public Login(Domain.Login obj)
        {
            Uris = obj.Uris?.Select(u => new LoginUri(u)).ToList();

            Username = obj.Username?.EncryptedString;
            Password = obj.Password?.EncryptedString;
            Totp = obj.Totp?.EncryptedString;
        }

        public List<LoginUri> Uris { get; set; }
        public string Username { get; set; }
        public string Password { get; set; }
        public string Totp { get; set; }

        public static LoginView ToView(Login req, LoginView view = null)
        {
            if (view == null)
            {
                view = new LoginView();
            }

            view.Uris = req.Uris?.Select(u => LoginUri.ToView(u)).ToList();

            view.Username = req.Username;
            view.Password = req.Password;
            view.Totp = req.Totp;
            return view;
        }
    }
}
