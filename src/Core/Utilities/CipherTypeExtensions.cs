using Bit.Core.Enums;

namespace Bit.Core.Utilities
{
    public static class CipherTypeExtensions
    {
        public static bool IsEqualToOrCanSignIn(this CipherType type, CipherType type2)
        {
            return type == type2
                   ||
                   (type == CipherType.Login && type2 == CipherType.Fido2Key);
        }
    }
}
