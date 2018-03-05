using Bit.App.Enums;
using Bit.App.Models.Api;

namespace Bit.App.Models.Data
{
    public class LoginUriDataModel
    {
        public LoginUriDataModel() { }

        public LoginUriDataModel(LoginUriType u)
        {
            Uri = u.Uri;
            Match = u.Match;
        }

        public string Uri { get; set; }
        public UriMatchType? Match { get; set; }
    }
}
