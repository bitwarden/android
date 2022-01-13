using System;
using Bit.App.Models;
using Bit.App.Services;
using Bit.App.Styles;
using Bit.Core;
using Xamarin.Forms;
#if !FDROID
using Microsoft.AppCenter.Crashes;
#endif

namespace Bit.App.Utilities
{
    public static class ThemeManager
    {
        public static bool UsingLightTheme = true;
        public static Func<ResourceDictionary> Resources = () => null;

        public static void SetThemeStyle(string name, ResourceDictionary resources)
        {
            try
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
                    if (OsDarkModeEnabled())
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
            catch (InvalidOperationException ioex) when (ioex.Message != null && ioex.Message.StartsWith("Collection was modified"))
            {
                // https://github.com/bitwarden/mobile/issues/1689 There are certain scenarios where this might cause "collection was modified; enumeration operation may not execute"
                // the way I found to prevent this for now was to catch the exception here and move on.
                // Because on the screens that I found it to happen, the screen is being closed while trying to apply the resources
                // so we shouldn't be introducing any issues.
                // TODO: Maybe something like this https://github.com/matteobortolazzo/HtmlLabelPlugin/pull/113 can be implemented to avoid this
                // on html labels.
            }
            catch (Exception ex)
            {
#if !FDROID
                Crashes.TrackError(ex);
#endif
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

        public static bool OsDarkModeEnabled()
        {
            if (Application.Current == null)
            {
                // called from iOS extension
                var app = new App(new AppOptions { IosExtension = true });
                return app.RequestedTheme == OSAppTheme.Dark;
            }
            return Application.Current.RequestedTheme == OSAppTheme.Dark;
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
