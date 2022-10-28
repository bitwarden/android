using System;
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
            OrganizationUseTotp = c.OrganizationUseTotp;
            if (c.Login != null)
            {
                Login = new SimpleLoginView
                {
                    Username = c.Login.Username,
                    Totp = c.Login.Totp
                };
            }
        }

        public string Id { get; set; }
        public string Name { get; set; }
        public bool OrganizationUseTotp { get; set; }
        public CipherType Type { get; set; }
        public SimpleLoginView Login { get; set; }
    }

    public class SimpleLoginView
    {
        public string Username { get; set; }
        public string Totp { get; set; }
    }
}

