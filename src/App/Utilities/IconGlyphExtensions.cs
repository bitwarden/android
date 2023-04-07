using Bit.Core;
using Bit.Core.Enums;
using Bit.Core.Models.View;

namespace Bit.App.Utilities
{
    public static class IconGlyphExtensions
    {
        public static string GetIcon(this CipherView cipher)
        {
            string icon = null;
            switch (cipher.Type)
            {
                case CipherType.Login:
                    icon = GetLoginIconGlyph(cipher);
                    break;
                case CipherType.SecureNote:
                    icon = BitwardenIcons.StickyNote;
                    break;
                case CipherType.Card:
                    icon = BitwardenIcons.CreditCard;
                    break;
                case CipherType.Identity:
                    icon = BitwardenIcons.IdCard;
                    break;
                default:
                    break;
            }
            return icon;
        }

        static string GetLoginIconGlyph(CipherView cipher)
        {
            var icon = BitwardenIcons.Globe;
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
