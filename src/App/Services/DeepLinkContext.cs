using System;
using Bit.App.Abstractions;
using Bit.Core;

namespace Bit.App.Services
{
    public class DeepLinkContext : IDeepLinkContext
    {
        private Uri _uri;

        public Uri CurrentUri => _uri;

        public bool HandleUri(Uri uri)
        {
            if (uri.Scheme == Constants.OtpAuthScheme)
            {
                _uri = uri;
                return true;
            }

            return false;
        }

        public void Clear()
        {
            _uri = null;
        }
    }
}
