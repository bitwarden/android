using Bit.Core;
using Bit.Core.Enums;
using Bit.Core.Models.View;

namespace Bit.App.Utilities
{
    public static class IconGlyphExtensions
    {
        public static string GetIcon(this CipherView cipher, bool usePasskeyIconAsPlaceholderFallback = false)
        {
            switch (cipher.Type)
            {
                case CipherType.Login:
                    return GetLoginIconGlyph(cipher, usePasskeyIconAsPlaceholderFallback);
                case CipherType.SecureNote:
                    return BitwardenIcons.StickyNote;
                case CipherType.Card:
                    return BitwardenIcons.CreditCard;
                case CipherType.Identity:
                    return BitwardenIcons.IdCard;
            }
            return null;
        }

        static string GetLoginIconGlyph(CipherView cipher, bool usePasskeyIconAsPlaceholderFallback = false)
        {
            var icon = cipher.HasFido2Credential && usePasskeyIconAsPlaceholderFallback ? BitwardenIcons.Passkey : BitwardenIcons.Globe;
            if (cipher.Login.Uri != null)
            {
                var hostnameUri = cipher.Login.Uri;
                if (hostnameUri.StartsWith(Constants.AndroidAppProtocol))
                {
                    icon = BitwardenIcons.Android;
                }
                else if (hostnameUri.StartsWith(Constants.iOSAppProtocol))
                {
                    icon = BitwardenIcons.Apple;
                }
            }
            return icon;
        }

        public static string GetBooleanIconGlyph(bool value, BooleanGlyphType type)
        {
            switch (type)
            {
                case BooleanGlyphType.Checkbox:
                    return value ? BitwardenIcons.CheckSquare : BitwardenIcons.Square;
                case BooleanGlyphType.Eye:
                    return value ? BitwardenIcons.EyeSlash : BitwardenIcons.Eye;
                default:
                    return "";
            }
        }
    }

    public enum BooleanGlyphType
    {
        Checkbox,
        Eye
    }
}
