using Bit.Core.Attributes;

namespace Bit.Core.Enums
{
    public enum ForwardedEmailServiceType
    {
        [LocalizableEnum("AnonAddy")]
        AnonAddy = 0,
        [LocalizableEnum("FirefoxRelay")]
        FirefoxRelay = 1,
        [LocalizableEnum("SimpleLogin")]
        SimpleLogin = 2,
    }
}
