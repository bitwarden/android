using Bit.Core.Attributes;

namespace Bit.Core.Enums
{
    public enum ForwardedEmailServiceType
    {
        None = -1,
        [LocalizableEnum("AddyIo")]
        AnonAddy = 0,
        [LocalizableEnum("FirefoxRelay")]
        FirefoxRelay = 1,
        [LocalizableEnum("SimpleLogin")]
        SimpleLogin = 2,
        [LocalizableEnum("DuckDuckGo")]
        DuckDuckGo = 3,
        [LocalizableEnum("Fastmail")]
        Fastmail = 4,
        [LocalizableEnum("ForwardEmail")]
        ForwardEmail = 5,
    }
}
