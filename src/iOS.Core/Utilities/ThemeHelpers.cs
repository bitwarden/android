using Bit.App.Utilities;
using Microsoft.Maui.Platform;
using UIKit;

namespace Bit.iOS.Core.Utilities
{
    public static class ThemeHelpers
    {
        public static bool LightTheme { get; private set; } = true;

        public static UIColor SplashBackgroundColor
        {
            get => ThemeManager.GetResourceColor("SplashBackgroundColor").ToPlatform();
        }
        public static UIColor BackgroundColor
        {
            get => ThemeManager.GetResourceColor("BackgroundColor").ToPlatform();
        }
        public static UIColor MutedColor
        {
            get => ThemeManager.GetResourceColor("MutedColor").ToPlatform();
        }
        public static UIColor SuccessColor
        {
            get => ThemeManager.GetResourceColor("SuccessColor").ToPlatform();
        }
        public static UIColor DangerColor
        {
            get => ThemeManager.GetResourceColor("DangerColor").ToPlatform();
        }
        public static UIColor PrimaryColor
        {
            get => ThemeManager.GetResourceColor("PrimaryColor").ToPlatform();
        }
        public static UIColor TextColor
        {
            get => ThemeManager.GetResourceColor("TextColor").ToPlatform();
        }
        public static UIColor SeparatorColor
        {
            get => ThemeManager.GetResourceColor("SeparatorColor").ToPlatform();
        }
        public static UIColor ListHeaderBackgroundColor
        {
            get => ThemeManager.GetResourceColor("ListHeaderBackgroundColor").ToPlatform();
        }
        public static UIColor NavBarBackgroundColor
        {
            get => ThemeManager.GetResourceColor("NavigationBarBackgroundColor").ToPlatform();
        }
        public static UIColor NavBarTextColor
        {
            get => ThemeManager.GetResourceColor("NavigationBarTextColor").ToPlatform();
        }
        public static UIColor TabBarBackgroundColor
        {
            get => ThemeManager.GetResourceColor("TabBarBackgroundColor").ToPlatform();
        }
        public static UIColor TabBarItemColor
        {
            get => ThemeManager.GetResourceColor("TabBarItemColor").ToPlatform();
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
            return Microsoft.Maui.Font.SystemFontOfSize(14, FontWeight.Bold).ToUIFont();
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
