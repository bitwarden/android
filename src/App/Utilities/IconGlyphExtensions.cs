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
                    icon = "\uf24a"; // fa-sticky-note-o
                    break;
                case CipherType.Card:
                    icon = "\uf09d"; // fa-credit-card
                    break;
                case CipherType.Identity:
                    icon = "\uf2c3"; // fa-id-card-o
                    break;
                default:
                    break;
            }
            return icon;
        }

        static string GetLoginIconGlyph(CipherView cipher)
        {
            var icon = "\uf0ac"; // fa-globe
            if (cipher.Login.Uri != null)
            {
                var hostnameUri = cipher.Login.Uri;
                if (hostnameUri.StartsWith(Constants.AndroidAppProtocol))
                {
                    icon = "\uf17b"; // fa-android
                }
                else if (hostnameUri.StartsWith(Constants.iOSAppProtocol))
                {
                    icon = "\uf179"; // fa-apple
                }
            }
            return icon;
        }

        public static string GetBooleanIconGlyph(bool value, BooleanGlyphType type)
        {
            switch (type)
            {
                case BooleanGlyphType.Checkbox:
                    return value ? "\uf046" : "\uf096"; // fa-check-square-o : fa-square-o
                case BooleanGlyphType.Eye:
                    return value ? "\uf06e" : "\uf070"; // fa-eye : fa-eye-slash
                default:
                    return "";
            }
        }

        public static string GetLinkedGlyph()
        {
            return "\uf0c1"; // fa-link
        }
    }

    public enum BooleanGlyphType
    {
        Checkbox,
        Eye
    }
}
