using Bit.App.Models;
using System;
using System.Collections.Generic;
using System.Linq;

namespace Bit.iOS.Extension.Models
{
    public class LoginViewModel
    {
        public LoginViewModel(Cipher cipher)
        {
            Id = cipher.Id;
            Name = cipher.Name?.Decrypt(cipher.OrganizationId);
            Username = cipher.Login?.Username?.Decrypt(cipher.OrganizationId);
            Password = cipher.Login?.Password?.Decrypt(cipher.OrganizationId);
            Uri = cipher.Login?.Uri?.Decrypt(cipher.OrganizationId);
            Totp = new Lazy<string>(() => cipher.Login?.Totp?.Decrypt(cipher.OrganizationId));
            Fields = new Lazy<List<Tuple<string, string>>>(() =>
            {
                if(!cipher.Fields?.Any() ?? true)
                {
                    return null;
                }

                var fields = new List<Tuple<string, string>>();
                foreach(var field in cipher.Fields)
                {
                    fields.Add(new Tuple<string, string>(
                        field.Name?.Decrypt(cipher.OrganizationId), 
                        field.Value?.Decrypt(cipher.OrganizationId)));
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
