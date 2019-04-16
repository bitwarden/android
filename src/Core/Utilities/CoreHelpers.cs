using System;
using System.Text.RegularExpressions;

namespace Bit.Core.Utilities
{
    public static class CoreHelpers
    {
        public static readonly string IpRegex =
            "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
            "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
            "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
            "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";

        public static readonly string TldEndingRegex =
            ".*\\.(com|net|org|edu|uk|gov|ca|de|jp|fr|au|ru|ch|io|es|us|co|xyz|info|ly|mil)$";

        public static readonly DateTime Epoc = new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc);

        public static bool InDebugMode()
        {
#if DEBUG
            return true;
#else
            return false;
#endif
        }

        public static string GetHostname(string uriString)
        {
            return GetUri(uriString)?.Host;
        }

        public static string GetHost(string uriString)
        {
            var uri = GetUri(uriString);
            if(uri != null)
            {
                if(uri.IsDefaultPort)
                {
                    return uri.Host;
                }
                else
                {
                    return string.Format("{0}:{1}", uri.Host, uri.Port);
                }
            }
            return null;
        }

        public static string GetDomain(string uriString)
        {
            var uri = GetUri(uriString);
            if(uri == null)
            {
                return null;
            }

            if(uri.Host == "localhost" || Regex.IsMatch(uriString, IpRegex))
            {
                return uri.Host;
            }
            try
            {
                if(DomainName.TryParseBaseDomain(uri.Host, out var baseDomain))
                {
                    return baseDomain ?? uri.Host;
                }
            }
            catch { }
            return null;
        }

        private static Uri GetUri(string uriString)
        {
            if(string.IsNullOrWhiteSpace(uriString))
            {
                return null;
            }
            var httpUrl = uriString.StartsWith("https://") || uriString.StartsWith("http://");
            if(!httpUrl && !uriString.Contains("://") && Regex.IsMatch(uriString, TldEndingRegex))
            {
                uriString = "http://" + uriString;
            }
            if(Uri.TryCreate(uriString, UriKind.Absolute, out var uri))
            {
                return uri;
            }
            return null;
        }
    }
}
