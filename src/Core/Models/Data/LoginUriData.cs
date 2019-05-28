using Bit.Core.Enums;
using Bit.Core.Models.Api;

namespace Bit.Core.Models.Data
{
    public class LoginUriData : Data
    {
        public LoginUriData() { }

        public LoginUriData(LoginUriApi data)
        {
            Uri = data.Uri;
            Match = data.Match;
        }

        public string Uri { get; set; }
        public UriMatchType? Match { get; set; }
    }
}
