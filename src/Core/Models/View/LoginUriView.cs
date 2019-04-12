using Bit.Core.Enums;
using Bit.Core.Models.Domain;

namespace Bit.Core.Models.View
{
    public class LoginUriView : View
    {
        private string _uri;
        private string _domain;
        private string _hostname;
        private bool? _canLaunch;

        public LoginUriView() { }

        public LoginUriView(LoginUri u)
        {
            Match = u.Match;
        }

        public UriMatchType? Match { get; set; }
        public string Uri
        {
            get => _uri;
            set
            {
                _uri = value;
                _domain = null;
                _canLaunch = null;
            }
        }

        // TODO
    }
}
