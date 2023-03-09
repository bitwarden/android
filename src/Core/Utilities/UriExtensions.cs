using System;

namespace Bit.Core.Utilities
{
    public static class UriExtensions
    {
        public static string UnescapeDataString(string uriString)
        {
            string unescapedUri;
            while ((unescapedUri = System.Uri.UnescapeDataString(uriString)) != uriString)
            {
                uriString = unescapedUri;
            }

            return unescapedUri;
        }
    }
}
