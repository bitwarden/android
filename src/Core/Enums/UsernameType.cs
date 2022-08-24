using Bit.Core.Attributes;

namespace Bit.Core.Enums
{
    public enum UsernameType
    {
        [LocalizableEnum("PlusAddressedEmail")]
        PlusAddressedEmail = 0,
        [LocalizableEnum("CatchAllEmail")]
        CatchAllEmail = 1,
        [LocalizableEnum("ForwardedEmailAlias")]
        ForwardedEmailAlias = 2,
        [LocalizableEnum("RandomWord")]
        RandomWord = 3,
    }
}
