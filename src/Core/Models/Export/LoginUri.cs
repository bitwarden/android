using Bit.Core.Enums;
using Bit.Core.Models.View;

namespace Bit.Core.Models.Export
{
    public class LoginUri
    {
        public LoginUri() { }

        public LoginUri(LoginUriView obj)
        {
            Match = obj.Match;
            Uri = obj.Uri;
        }

        public LoginUri(Domain.LoginUri obj)
        {
            Match = obj.Match;
            Uri = obj.Uri?.EncryptedString;
        }

        public UriMatchType? Match { get; set; }
        public string Uri { get; set; }

        public static LoginUriView ToView(LoginUri req, LoginUriView view = null)
        {
            if (view == null)
            {
                view = new LoginUriView();
            }

            view.Match = req.Match;
            view.Uri = req.Uri;
            return view;
        }
    }
}
