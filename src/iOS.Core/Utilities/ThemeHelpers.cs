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
            var textColor = Xamarin.Forms.Color.FromHex("#000000").ToUIColor();
            if(theme == "dark")
            {
                textColor = Xamarin.Forms.Color.FromHex("#ffffff").ToUIColor();
                mutedColor = Xamarin.Forms.Color.FromHex("#a3a3a3").ToUIColor();
            }
            else if(theme == "black")
            {
                textColor = Xamarin.Forms.Color.FromHex("#ffffff").ToUIColor();
                mutedColor = Xamarin.Forms.Color.FromHex("#a3a3a3").ToUIColor();
            }
            else if(theme == "nord")
            {
                textColor = Xamarin.Forms.Color.FromHex("#e5e9f0").ToUIColor();
                mutedColor = Xamarin.Forms.Color.FromHex("#d8dee9").ToUIColor();
            }
            else
            {
                lightTheme = true;
            }

            UITextField.Appearance.TintColor = textColor;
            UITextView.Appearance.TintColor = textColor;
            UIStepper.Appearance.TintColor = mutedColor;
            if(!lightTheme)
            {
                UISwitch.Appearance.TintColor = mutedColor;
            }
        }
    }
}
