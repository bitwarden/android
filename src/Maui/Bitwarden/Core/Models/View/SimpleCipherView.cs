using System.Collections.Generic;
using System.Linq;
using Bit.Core.Enums;
using MessagePack;

namespace Bit.Core.Models.View
{
    [MessagePackObject]
    public class SimpleCipherView
    {
        public SimpleCipherView()
        {
        }

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

        [Key(0)]
        public string Id { get; set; }
        [Key(1)]
        public string Name { get; set; }
        [IgnoreMember]
        public CipherType Type { get; set; } // ignoring on serialization for now, given that all are going to be of type Login
        [Key(2)]
        public SimpleLoginView Login { get; set; }
    }

    [MessagePackObject]
    public class SimpleLoginView
    {
        [Key(0)]
        public string Username { get; set; }
        [Key(1)]
        public string Totp { get; set; }
        [Key(2)]
        public List<SimpleLoginUriView> Uris { get; set; }
    }

    [MessagePackObject]
    public class SimpleLoginUriView
    {
        public SimpleLoginUriView()
        {
        }

        public SimpleLoginUriView(string uri)
        {
            Uri = uri;
        }

        [Key(0)]
        public string Uri { get; set; }
    }
}

