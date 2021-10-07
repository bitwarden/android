﻿using Android.Graphics;
using Bit.App.Utilities;
using Xamarin.Forms.Platform.Android;

namespace Bit.Droid.Utilities
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
        public static Color SwitchTrackOnColor
        {
            get => ThemeManager.GetResourceColor("SwitchTrackOnColor").ToAndroid();
        }
        public static Color SwitchTrackOffColor
        {
            get => ThemeManager.GetResourceColor("SwitchTrackOffColor").ToAndroid();
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
                theme = "dark";
            }

            if (theme == "dark" || theme == "black" || theme == "nord")
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
