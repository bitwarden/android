using System.Text.RegularExpressions;

namespace Bit.Core.Utilities.Fido2
{
    public class Fido2DomainUtils
    {
        // TODO: This is a basic implementation of the domain validation logic, and is probably not correct.
        // It doesn't support IP-adresses, and it doesn't follow the algorithm in the spec:
        // https://html.spec.whatwg.org/multipage/browsers.html#is-a-registrable-domain-suffix-of-or-is-equal-to
        public static bool IsValidRpId(string rpId, string origin)
        {
            if (rpId == null || origin == null)
            {
                return false;
            }

            // TODO: DomainName doesn't like it when we give it a URL with a protocol or port
            // So we remove the protocol and port here, while still supporting ipv6 shortform
            // https is enforced in the client, so we don't need to worry about that here
            var originWithoutProtocolOrPort = Regex.Replace(origin, @"(https?://)?([^:/]+)(:\d+)?(/.*)?", "$2$4");

            if (rpId == originWithoutProtocolOrPort)
            {
                return true;
            }

            if (!DomainName.TryParse(rpId, out var parsedRpId) || !DomainName.TryParse(originWithoutProtocolOrPort, out var parsedOrgin))
            {
                return false;
            }

            return parsedOrgin.Tld == parsedRpId.Tld && 
                parsedOrgin.Domain == parsedRpId.Domain && 
                (parsedOrgin.SubDomain == parsedRpId.SubDomain || parsedOrgin.SubDomain.EndsWith(parsedRpId.SubDomain));
        }
    }
}
