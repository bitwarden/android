using Bit.App.Models;
using System;
using System.Collections.Generic;
using System.Linq;

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
            Fields = new Lazy<List<Tuple<string, string>>>(() =>
            {
                if(login.Fields?.Any() ?? true)
                {
                    return null;
                }

                var fields = new List<Tuple<string, string>>();
                foreach(var field in login.Fields)
                {
                    fields.Add(new Tuple<string, string>(
                        field.Name?.Decrypt(login.OrganizationId), 
                        field.Value?.Decrypt(login.OrganizationId)));
                }
                return fields;
            });
        }

        public string Id { get; set; }
        public string Name { get; set; }
        public string Username { get; set; }
        public string Password { get; set; }
        public string Uri { get; set; }
        public Lazy<string> Totp { get; set; }
        public Lazy<List<Tuple<string, string>>> Fields { get; set; }
    }
}
