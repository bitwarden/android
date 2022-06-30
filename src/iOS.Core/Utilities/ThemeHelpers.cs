using Bit.App.Utilities;
using UIKit;
using Xamarin.Forms.Platform.iOS;

namespace Bit.iOS.Core.Utilities
{
    public static class ThemeHelpers
    {
        public static bool LightTheme = true;

        public static UIColor SplashBackgroundColor
        {
            get => ThemeManager.GetResourceColor("SplashBackgroundColor").ToUIColor();
        }
        public static UIColor BackgroundColor
        {
            get => ThemeManager.GetResourceColor("BackgroundColor").ToUIColor();
        }
        public static UIColor MutedColor
        {
            get => ThemeManager.GetResourceColor("MutedColor").ToUIColor();
        }
        public static UIColor SuccessColor
        {
            get => ThemeManager.GetResourceColor("SuccessColor").ToUIColor();
        }
        public static UIColor DangerColor
        {
            get => ThemeManager.GetResourceColor("DangerColor").ToUIColor();
        }
        public static UIColor PrimaryColor
        {
            get => ThemeManager.GetResourceColor("PrimaryColor").ToUIColor();
        }
        public static UIColor TextColor
        {
            get => ThemeManager.GetResourceColor("TextColor").ToUIColor();
        }
        public static UIColor SeparatorColor
        {
            get => ThemeManager.GetResourceColor("SeparatorColor").ToUIColor();
        }
        public static UIColor ListHeaderBackgroundColor
        {
            get => ThemeManager.GetResourceColor("ListHeaderBackgroundColor").ToUIColor();
        }
        public static UIColor NavBarBackgroundColor
        {
            get => ThemeManager.GetResourceColor("NavigationBarBackgroundColor").ToUIColor();
        }
        public static UIColor NavBarTextColor
        {
            get => ThemeManager.GetResourceColor("NavigationBarTextColor").ToUIColor();
        }
        public static UIColor TabBarBackgroundColor
        {
            get => ThemeManager.GetResourceColor("TabBarBackgroundColor").ToUIColor();
        }
        public static UIColor TabBarItemColor
        {
            get => ThemeManager.GetResourceColor("TabBarItemColor").ToUIColor();
        }

        public static void SetAppearance(string theme, bool osDarkModeEnabled)
        {
            SetThemeVariables(theme, osDarkModeEnabled);
            UINavigationBar.Appearance.ShadowImage = new UIImage();
            UINavigationBar.Appearance.SetBackgroundImage(new UIImage(), UIBarMetrics.Default);
            UIStepper.Appearance.TintColor = MutedColor;
            if (!LightTheme)
            {
                UISwitch.Appearance.TintColor = MutedColor;
            }
        }

        public static UIFont GetDangerFont()
        {
            return Xamarin.Forms.Font.SystemFontOfSize(Xamarin.Forms.NamedSize.Small, 
                Xamarin.Forms.FontAttributes.Bold).ToUIFont();
        }

        private static void SetThemeVariables(string theme, bool osDarkModeEnabled)
        {
            if (string.IsNullOrWhiteSpace(theme) && osDarkModeEnabled)
            {
                theme = ThemeManager.Dark;
            }

            if (theme == ThemeManager.Dark || theme == ThemeManager.Black || theme == ThemeManager.Nord)
            {
                LightTheme = false;
            }
            else
            {
                LightTheme = true;
            }
        }
    }
}
