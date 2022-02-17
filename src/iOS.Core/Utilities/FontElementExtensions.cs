using UIKit;
using Xamarin.Forms;
using Xamarin.Forms.Internals;
using Xamarin.Forms.Platform.iOS;

namespace Bit.iOS.Core.Utilities
{
    public static class FontElementExtensions
    {
        public static UIFont ToUIFont(this IFontElement fontElement)
        {
            var fontSize = fontElement.FontSize;
            var fontAttributes = fontElement.FontAttributes;
            var fontFamily = fontElement.FontFamily;

            return fontFamily is null
                ? Font.SystemFontOfSize(fontSize, fontAttributes).ToUIFont()
                : Font.OfSize(fontFamily, fontSize).WithAttributes(fontAttributes).ToUIFont();
        }
    }
}
