using Bit.Core.Enums;

namespace Bit.Core.Models.Api
{
    public class LoginUriApi
    {
        public string Uri { get; set; }
        public UriMatchType? Match { get; set; }
    }
}
