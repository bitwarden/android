using UIKit;
using Xamarin.Forms.Platform.iOS;

namespace Bit.iOS.Core.Utilities
{
    public static class ThemeHelpers
    {
        public static bool LightTheme = true;
        public static UIColor SplashBackgroundColor = Xamarin.Forms.Color.FromHex("#efeff4").ToUIColor();
        public static UIColor BackgroundColor = Xamarin.Forms.Color.FromHex("#ffffff").ToUIColor();
        public static UIColor MutedColor = Xamarin.Forms.Color.FromHex("#777777").ToUIColor();

        public static void SetAppearance(string theme)
        {
            SetThemeVariables(theme);
            UIStepper.Appearance.TintColor = MutedColor;
            if(!LightTheme)
            {
                UISwitch.Appearance.TintColor = MutedColor;
            }
        }

        public static void SetExtensionAppearance(string theme)
        {
            SetAppearance(theme);
            UIView.Appearance.BackgroundColor = BackgroundColor;
        }

        private static void SetThemeVariables(string theme)
        {
            LightTheme = false;
            if(theme == "dark")
            {
                MutedColor = Xamarin.Forms.Color.FromHex("#a3a3a3").ToUIColor();
                BackgroundColor = Xamarin.Forms.Color.FromHex("#303030").ToUIColor();
                SplashBackgroundColor = Xamarin.Forms.Color.FromHex("#222222").ToUIColor();
            }
            else if(theme == "black")
            {
                MutedColor = Xamarin.Forms.Color.FromHex("#a3a3a3").ToUIColor();
                BackgroundColor = Xamarin.Forms.Color.FromHex("#000000").ToUIColor();
                SplashBackgroundColor = BackgroundColor;
            }
            else if(theme == "nord")
            {
                MutedColor = Xamarin.Forms.Color.FromHex("#d8dee9").ToUIColor();
                BackgroundColor = Xamarin.Forms.Color.FromHex("#3b4252").ToUIColor();
                SplashBackgroundColor = Xamarin.Forms.Color.FromHex("#2e3440").ToUIColor();
            }
            else
            {
                LightTheme = true;
            }
        }
    }
}
