using System.Collections.Generic;
using System.Linq;
using Bit.Core.Enums;

namespace Bit.Core.Models.View
{
    public class SimpleCipherView
    {
        public SimpleCipherView(CipherView c)
        {
            Id = c.Id;
            Name = c.Name;
            Type = c.Type;
            if (c.Login != null)
            {
                Login = new SimpleLoginView
                {
                    Username = c.Login.Username,
                    Totp = c.Login.Totp,
                    Uris = c.Login.Uris?.Select(u => new SimpleLoginUriView(u.Uri)).ToList()
                };
            }
        }

        public string Id { get; set; }
        public string Name { get; set; }
        public CipherType Type { get; set; }
        public SimpleLoginView Login { get; set; }
    }

    public class SimpleLoginView
    {
        public string Username { get; set; }
        public string Totp { get; set; }
        public List<SimpleLoginUriView> Uris { get; set; }
    }

    public class SimpleLoginUriView
    {
        public SimpleLoginUriView(string uri)
        {
            Uri = uri;
        }

        public string Uri { get; set; }
    }
}

