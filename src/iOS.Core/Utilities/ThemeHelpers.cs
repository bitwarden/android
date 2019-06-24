using System;
using UIKit;
using Xamarin.Forms.Platform.iOS;

namespace Bit.iOS.Core.Utilities
{
    public static class ThemeHelpers
    {
        public static void SetAppearance(string theme)
        {
            var lightTheme = false;
            var tabBarItemColor = Xamarin.Forms.Color.FromHex("#757575").ToUIColor();
            var primaryColor = Xamarin.Forms.Color.FromHex("#3c8dbc").ToUIColor();
            var mutedColor = Xamarin.Forms.Color.FromHex("#777777").ToUIColor();
            if(theme == "dark")
            {
                tabBarItemColor = Xamarin.Forms.Color.FromHex("#C0C0C0").ToUIColor();
                primaryColor = Xamarin.Forms.Color.FromHex("#52bdfb").ToUIColor();
                mutedColor = Xamarin.Forms.Color.FromHex("#a3a3a3").ToUIColor();
            }
            else if(theme == "black")
            {
                tabBarItemColor = Xamarin.Forms.Color.FromHex("#C0C0C0").ToUIColor();
                primaryColor = Xamarin.Forms.Color.FromHex("#52bdfb").ToUIColor();
                mutedColor = Xamarin.Forms.Color.FromHex("#a3a3a3").ToUIColor();
            }
            else if(theme == "nord")
            {
                tabBarItemColor = Xamarin.Forms.Color.FromHex("#e5e9f0").ToUIColor();
                primaryColor = Xamarin.Forms.Color.FromHex("#81a1c1").ToUIColor();
                mutedColor = Xamarin.Forms.Color.FromHex("#d8dee9").ToUIColor();
            }
            else
            {
                lightTheme = true;
            }

            UITabBar.Appearance.TintColor = tabBarItemColor;
            UITabBar.Appearance.SelectedImageTintColor = primaryColor;

            UIStepper.Appearance.TintColor = mutedColor;
            if(!lightTheme)
            {
                UISwitch.Appearance.TintColor = mutedColor;
            }
        }
    }
}