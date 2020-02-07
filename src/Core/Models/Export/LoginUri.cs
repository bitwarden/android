using Bit.Core.Enums;
using Bit.Core.Models.View;

namespace Bit.Core.Models.Export
{
    public class LoginUri
    {
        public LoginUri()
        {
            Match = null;
            Uri = "https://google.com";
        }

        public static LoginUriView ToView(LoginUri req, LoginUriView view = null)
        {
            if(view == null)
            {
                view = new LoginUriView();
            }

            view.Match = req.Match;
            view.Uri = req.Uri;
            return view;
        }

        public UriMatchType? Match { get; set; }
        public string Uri { get; set; }

        public LoginUri(LoginUriView obj)
        {
            if(obj == null)
            {
                return;
            }

            Match = obj.Match;
            Uri = obj.Uri;
        }
    }
}
