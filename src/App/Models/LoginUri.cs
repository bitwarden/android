using Bit.App.Enums;
using Bit.App.Models.Data;

namespace Bit.App.Models
{
    public class LoginUri
    {
        public LoginUri() { }

        public LoginUri(LoginUriDataModel data)
        {
            Uri = data.Uri != null ? new CipherString(data.Uri) : null;
            Match = data.Match;
        }

        public CipherString Uri { get; set; }
        public UriMatchType? Match { get; set; }
    }
}
