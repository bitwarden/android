using System.Collections.Generic;
using System.Linq;
using System.Text.RegularExpressions;
using Bit.Core.Enums;
using Bit.Core.Models.Domain;
using Bit.Core.Utilities;

namespace Bit.Core.Models.View
{
    public class LoginUriView : View, ILaunchableView
    {
        private HashSet<string> _canLaunchWhitelist = new HashSet<string>
        {
            "https://",
            "http://",
            "ssh://",
            "ftp://",
            "sftp://",
            "irc://",
            "vnc://",
            "chrome://",
            "iosapp://",
            "androidapp://",
        };

        private string _uri;
        private string _domain;
        private string _host;
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

        public string Domain
        {
            get
            {
                if (_domain == null && Uri != null)
                {
                    _domain = CoreHelpers.GetDomain(Uri);
                    if (_domain == string.Empty)
                    {
                        _domain = null;
                    }
                }
                return _domain;
            }
        }

        public string Host
        {
            get
            {
                if (Match == UriMatchType.RegularExpression)
                {
                    return null;
                }
                if (_host == null && Uri != null)
                {
                    _host = CoreHelpers.GetHost(Uri);
                    if (_host == string.Empty)
                    {
                        _host = null;
                    }
                }
                return _host;
            }
        }

        public string HostOrUri => Host ?? Uri;

        public bool IsWebsite => Uri != null && (Uri.StartsWith("http://") || Uri.StartsWith("https://") ||
            (Uri.Contains("://") && Regex.IsMatch(Uri, CoreHelpers.TldEndingRegex)));

        public bool CanLaunch
        {
            get
            {
                if (_canLaunch != null)
                {
                    return _canLaunch.Value;
                }
                if (Uri != null && Match != UriMatchType.RegularExpression)
                {
                    var uri = LaunchUri;
                    _canLaunch = _canLaunchWhitelist.Any(prefix => uri.StartsWith(prefix));
                    return _canLaunch.Value;
                }
                _canLaunch = false;
                return _canLaunch.Value;
            }
        }

        public string LaunchUri => !Uri.Contains("://") && Regex.IsMatch(Uri, CoreHelpers.TldEndingRegex) ?
            string.Concat("http://", Uri) : Uri;
    }
}
