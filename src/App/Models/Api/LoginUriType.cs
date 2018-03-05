using Bit.App.Enums;

namespace Bit.App.Models.Api
{
    public class LoginUriType
    {
        public LoginUriType() { }

        public LoginUriType(LoginUri u)
        {
            Uri = u.Uri?.EncryptedString;
            Match = u.Match;
        }

        public string Uri { get; set; }
        public UriMatchType? Match { get; set; }
    }
}
