using System;
using System.Collections.Generic;
using System.Text;

namespace Bit.App.Models.Steam
{
    class SteamSession
    {
        public string SessionID { get; set; }
        public string SteamLogin { get; set; }
        public string SteamLoginSecure { get; set; }
        public string WebCookie { get; set; }
        public string OAuthToken { get; set; }
        public ulong SteamID { get; set; }
    }
}
