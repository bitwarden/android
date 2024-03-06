using System.Text.RegularExpressions;

namespace Bit.Core.Utilities.Fido2
{
    public class Fido2DomainUtils
    {
        // Loosely based on:
        // https://html.spec.whatwg.org/multipage/browsers.html#is-a-registrable-domain-suffix-of-or-is-equal-to
        public static bool IsValidRpId(string rpId, string origin)
        {
            if (rpId == null || rpId == "" || origin == null)
            {
                return false;
            }

            // We only care about the domain part of the origin, not the protocol or port so we remove them here,
            // while still keeping ipv6 intact.
            // https is enforced in the client, so we don't need to worry about that here
            var originWithoutProtocolPortOrPath = Regex.Replace(origin, @"(https?://)?([^:/]+)(:\d+)?(/.*)?$", "$2");
            if (Uri.CheckHostName(rpId) != UriHostNameType.Dns || Uri.CheckHostName(originWithoutProtocolPortOrPath) != UriHostNameType.Dns)
            {
                return false;
            }

            if (rpId == originWithoutProtocolPortOrPath)
            {
                return true;
            }

            if (!DomainName.TryParse(rpId, out var parsedRpId) || !DomainName.TryParse(originWithoutProtocolPortOrPath, out var parsedOrgin))
            {
                return false;
            }

            return parsedOrgin.Tld == parsedRpId.Tld &&
                parsedOrgin.Domain == parsedRpId.Domain &&
                (parsedOrgin.SubDomain == parsedRpId.SubDomain || parsedOrgin.SubDomain.EndsWith(parsedRpId.SubDomain));
        }
    }
}
