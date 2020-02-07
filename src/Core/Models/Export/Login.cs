using System.Collections.Generic;
using System.Linq;
using Bit.Core.Models.View;

namespace Bit.Core.Models.Export
{
    public class Login
    {
        public Login()
        {
            Uris = new List<LoginUri>();
            Username = "jdoe";
            Password = "myp@ssword123";
            Totp = "JBSWY3DPEHPK3PXP";
        }

        public static LoginView ToView(Login req, LoginView view = null)
        {
            if(view == null)
            {
                view = new LoginView();
            }

            if(req.Uris?.Any() ?? false)
            {
                view.Uris = req.Uris.Select(u => LoginUri.ToView(u)).ToList();
            }

            view.Username = req.Username;
            view.Password = req.Password;
            view.Totp = req.Totp;
            return view;
        }

        public List<LoginUri> Uris { get; set; }
        public string Username { get; set; }
        public string Password { get; set; }
        public string Totp { get; set; }

        public Login(LoginView obj)
        {
            if(obj == null)
            {
                return;
            }

            if(obj.Uris?.Any() ?? false)
            {
                Uris = obj.Uris.Select(u => new LoginUri(u)).ToList();
            }

            Username = obj.Username;
            Password = obj.Password;
            Totp = obj.Totp;
        }
    }
}
