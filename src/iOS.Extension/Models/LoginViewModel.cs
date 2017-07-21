using Bit.App.Models;
using System;

namespace Bit.iOS.Extension.Models
{
    public class LoginViewModel
    {
        public LoginViewModel(Login login)
        {
            Id = login.Id;
            Name = login.Name?.Decrypt(login.OrganizationId);
            Username = login.Username?.Decrypt(login.OrganizationId);
            Password = login.Password?.Decrypt(login.OrganizationId);
            Uri = login.Uri?.Decrypt(login.OrganizationId);
            Totp = new Lazy<string>(() => login.Totp?.Decrypt(login.OrganizationId));
        }

        public string Id { get; set; }
        public string Name { get; set; }
        public string Username { get; set; }
        public string Password { get; set; }
        public string Uri { get; set; }
        public Lazy<string> Totp { get; set; }
    }
}
