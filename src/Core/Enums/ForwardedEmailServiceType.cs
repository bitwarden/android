using Bit.Core.Attributes;

namespace Bit.Core.Enums
{
    public enum ForwardedEmailServiceType
    {
        None = 0,
        [LocalizableEnum("AnonAddy")]
        AnonAddy = 1,
        [LocalizableEnum("DuckDuckGo")]
        DuckDuckGo = 2,
        [LocalizableEnum("Fastmail")]
        Fastmail = 3,
        [LocalizableEnum("FirefoxRelay")]
        FirefoxRelay = 4,
        [LocalizableEnum("SimpleLogin")]
        SimpleLogin = 5
    }
}
