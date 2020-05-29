using System;
using Bit.App.Abstractions;
using Bit.App.Services;
using Bit.App.Styles;
using Bit.Core;
using Bit.Core.Utilities;
using Xamarin.Forms;

namespace Bit.App.Utilities
{
    public static class ThemeManager
    {
        public static bool UsingLightTheme = true;
        public static Func<ResourceDictionary> Resources = () => null;

        public static void SetThemeStyle(string name, ResourceDictionary resources)
        {
            Resources = () => resources;

            // Reset styles
            resources.Clear();
            resources.MergedDictionaries.Clear();

            // Variables
            resources.MergedDictionaries.Add(new Variables());

            // Themed variables
            if (name == "dark")
            {
                resources.MergedDictionaries.Add(new Dark());
                UsingLightTheme = false;
            }
            else if (name == "black")
            {
                resources.MergedDictionaries.Add(new Black());
                UsingLightTheme = false;
            }
            else if (name == "nord")
            {
                resources.MergedDictionaries.Add(new Nord());
                UsingLightTheme = false;
            }
            else if (name == "light")
            {
                resources.MergedDictionaries.Add(new Light());
                UsingLightTheme = true;
            }
            else
            {
                var deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService", true);
                if (deviceActionService?.UsingDarkTheme() ?? false)
                {
                    resources.MergedDictionaries.Add(new Dark());
                    UsingLightTheme = false;
                }
                else
                {
                    resources.MergedDictionaries.Add(new Light());
                    UsingLightTheme = true;
                }
            }

            // Base styles
            resources.MergedDictionaries.Add(new Base());

            // Platform styles
            if (Device.RuntimePlatform == Device.Android)
            {
                resources.MergedDictionaries.Add(new Android());
            }
            else if (Device.RuntimePlatform == Device.iOS)
            {
                resources.MergedDictionaries.Add(new iOS());
            }
        }

        public static void SetTheme(bool android, ResourceDictionary resources)
        {
            SetThemeStyle(GetTheme(android), resources);
        }

        public static string GetTheme(bool android)
        {
            return Xamarin.Essentials.Preferences.Get(
                string.Format(PreferencesStorageService.KeyFormat, Constants.ThemeKey), default(string),
                !android ? "group.com.8bit.bitwarden" : default(string));
        }

        public static void ApplyResourcesToPage(ContentPage page)
        {
            foreach (var resourceDict in Resources().MergedDictionaries)
            {
                page.Resources.Add(resourceDict);
            }
        }

        public static Color GetResourceColor(string color)
        {
            return (Color)Resources()[color];
        }
    }
}
