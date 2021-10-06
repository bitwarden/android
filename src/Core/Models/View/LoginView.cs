using Bit.Core.Models.Domain;
using System;
using System.Collections.Generic;
using System.Linq;

namespace Bit.Core.Models.View
{
    public class LoginView : View
    {
        public static List<KeyValuePair<string, int>> LinkedFieldOptions = new List<KeyValuePair<string, int>>()
        {
            new KeyValuePair<string, int>("Username", 0),
            new KeyValuePair<string, int>("Password", 1),
        };

        public LoginView() { }

        public LoginView(Login l)
        {
            PasswordRevisionDate = l.PasswordRevisionDate;
        }

        public string Username { get; set; }
        public string Password { get; set; }
        public DateTime? PasswordRevisionDate { get; set; }
        public string Totp { get; set; }
        public List<LoginUriView> Uris { get; set; }
        public string Uri => HasUris ? Uris[0].Uri : null;
        public string MaskedPassword => Password != null ? "••••••••" : null;
        public string SubTitle => Username;
        public bool CanLaunch => HasUris && Uris.Any(u => u.CanLaunch);
        public string LaunchUri => HasUris ? Uris.FirstOrDefault(u => u.CanLaunch)?.LaunchUri : null;
        public bool HasUris => (Uris?.Count ?? 0) > 0;
    }
}
