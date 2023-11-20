using UIKit;

namespace Bit.iOS.Core.Utilities
{
    public static class FontElementExtensions
    {
        public static UIFont ToUIFont(this Microsoft.Maui.Font font)
        {
            var fontSize = font.Size;
            var fontAttributes = font.GetFontAttributes();
            var fontFamily = font.Family;
            var fontWeight = fontAttributes == FontAttributes.Bold ? UIFontWeight.Bold : UIFontWeight.Regular;

            return fontFamily is null
                ? UIFont.SystemFontOfSize((nfloat)fontSize, fontWeight)
                : UIFont.FromName(fontFamily, (nfloat)fontSize);
        }
    }
}
