using UIKit;
using Xamarin.Forms.Platform.iOS;

namespace Bit.iOS.Core.Utilities
{
    public static class ThemeHelpers
    {
        public static void SetAppearance(string theme)
        {
            var lightTheme = false;
            var mutedColor = Xamarin.Forms.Color.FromHex("#777777").ToUIColor();
            if(theme == "dark")
            {
                mutedColor = Xamarin.Forms.Color.FromHex("#a3a3a3").ToUIColor();
            }
            else if(theme == "black")
            {
                mutedColor = Xamarin.Forms.Color.FromHex("#a3a3a3").ToUIColor();
            }
            else if(theme == "nord")
            {
                mutedColor = Xamarin.Forms.Color.FromHex("#d8dee9").ToUIColor();
            }
            else
            {
                lightTheme = true;
            }

            UIStepper.Appearance.TintColor = mutedColor;
            if(!lightTheme)
            {
                UISwitch.Appearance.TintColor = mutedColor;
            }
        }
    }
}
