using System;
using System.Collections.Generic;
using System.Text;

namespace Bit.Core.Models.Domain
{
    public class Login : Domain
    {
        public List<LoginUri> Uris { get; set; }
        public CipherString Username { get; set; }
        public CipherString Password { get; set; }
        public DateTime? PasswordRevisionDate { get; set; }
        public CipherString Totp { get; set; }
    }
}
