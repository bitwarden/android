using System;
using Foundation;
using Bit.App;

namespace Bit.iOS.Extension.Models
{
    public class Context
    {
        private string _uriString;

        public NSExtensionContext ExtContext { get; set; }
        public string ProviderType { get; set; }
        public Uri Uri
        {
            get
            {
                Uri uri;
                if(string.IsNullOrWhiteSpace(UrlString) || !Uri.TryCreate(UrlString, UriKind.Absolute, out uri))
                {
                    return null;
                }

                return uri;
            }
        }
        public string UrlString
        {
            get
            {
                return _uriString;
            }
            set
            {
                _uriString = value;
                if(_uriString != null && !_uriString.StartsWith(Constants.iOSAppProtocol) && _uriString.Contains("."))
                {
                    if(!_uriString.Contains("://") && !_uriString.Contains(" "))
                    {
                        _uriString = string.Concat("http://", _uriString);
                    }
                }

                if(!_uriString.StartsWith("http") && !_uriString.StartsWith(Constants.iOSAppProtocol))
                {
                    _uriString = string.Concat(Constants.iOSAppProtocol, _uriString);
                }
            }
        }
        public string LoginTitle { get; set; }
        public string Username { get; set; }
        public string Password { get; set; }
        public string OldPassword { get; set; }
        public string Notes { get; set; }
        public PasswordGenerationOptions PasswordOptions { get; set; }
        public PageDetails Details { get; set; }
    }
}
