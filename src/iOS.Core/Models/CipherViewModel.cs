using Bit.Core.Enums;
using Bit.Core.Models.View;
using System;
using System.Collections.Generic;
using System.Linq;

namespace Bit.iOS.Core.Models
{
    public class CipherViewModel
    {
        public CipherViewModel(CipherView cipher)
        {
            CipherView = cipher;
            Id = cipher.Id;
            Name = cipher.Name;
            Username = cipher.Login?.Username;
            Password = cipher.Login?.Password;
            Totp = cipher.Login?.Totp;
            Uris = cipher.Login?.Uris?.Select(u => new LoginUriModel(u)).ToList();
            Fields = cipher.Fields?.Select(f => new Tuple<string, string>(f.Name, f.Value)).ToList();
            Reprompt = cipher.Reprompt;
        }

        public string Id { get; set; }
        public string Name { get; set; }
        public string Username { get; set; }
        public string Password { get; set; }
        public List<LoginUriModel> Uris { get; set; }
        public string Totp { get; set; }
        public List<Tuple<string, string>> Fields { get; set; }
        public CipherView CipherView { get; set; }
        public CipherRepromptType Reprompt { get; set; }

        public class LoginUriModel
        {
            public LoginUriModel(LoginUriView data)
            {
                Uri = data?.Uri;
                Match = data?.Match;
            }

            public string Uri { get; set; }
            public UriMatchType? Match { get; set; }
        }
    }
}
