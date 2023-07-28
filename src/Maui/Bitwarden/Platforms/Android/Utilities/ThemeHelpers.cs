using Android.Graphics;
using Bit.App.Utilities;
using Microsoft.Maui.Controls.Compatibility.Platform.Android;
using Color = Android.Graphics.Color;

namespace Bit.App.Droid.Utilities
{
    public class ThemeHelpers
    {
        public static bool LightTheme = true;
        
        public static Color PrimaryColor
        {
            get => ThemeManager.GetResourceColor("PrimaryColor").ToAndroid();
        }
        public static Color MutedColor
        {
            get => ThemeManager.GetResourceColor("MutedColor").ToAndroid();
        }
        public static Color BackgroundColor
        {
            get => ThemeManager.GetResourceColor("BackgroundColor").ToAndroid();
        }
        public static Color NavBarBackgroundColor
        {
            get => ThemeManager.GetResourceColor("NavigationBarBackgroundColor").ToAndroid();
        }
        public static Color FabColor
        {
            get => ThemeManager.GetResourceColor("FabColor").ToAndroid();
        }
        public static Color SwitchOnColor
        {
            get => ThemeManager.GetResourceColor("SwitchOnColor").ToAndroid();
        }
        public static Color SwitchThumbColor
        {
            get => ThemeManager.GetResourceColor("SwitchThumbColor").ToAndroid();
        }
        public static Color TextColor
        {
            get => ThemeManager.GetResourceColor("TextColor").ToAndroid();
        }
        
        public static void SetAppearance(string theme, bool osDarkModeEnabled)
        {
            SetThemeVariables(theme, osDarkModeEnabled);
        }
        
        public static int GetDialogTheme()
        {
            if (LightTheme)
            {
                return Android.Resource.Style.ThemeMaterialLightDialog;
            }
            return Android.Resource.Style.ThemeMaterialDialog;
        }
        
        private static void SetThemeVariables(string theme, bool osDarkModeEnabled)
        {
            if (string.IsNullOrWhiteSpace(theme) && osDarkModeEnabled)
            {
                theme = ThemeManager.Dark;
            }

            if (theme == ThemeManager.Dark || theme == ThemeManager.Black || theme == ThemeManager.Nord || theme == ThemeManager.SolarizedDark)
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
