using System;
using Bit.App.Models;
using Bit.App.Styles;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Xamarin.Forms;
using System.Linq;
using System.Threading.Tasks;
#if !FDROID
using Microsoft.AppCenter.Crashes;
#endif

namespace Bit.App.Utilities
{
    public static class ThemeManager
    {
        public static bool UsingLightTheme = true;
        public static Func<ResourceDictionary> Resources = () => null;

        public static bool IsThemeDirty = false;

        public static void SetThemeStyle(string name, ResourceDictionary resources)
        {
            try
            {
                Resources = () => resources;

                var newTheme = NeedsThemeUpdate(name, resources);
                if (newTheme is null)
                {
                    return;
                }

                var currentTheme = resources.MergedDictionaries.FirstOrDefault(md => md is IThemeResourceDictionary);
                if (currentTheme != null)
                {
                    resources.MergedDictionaries.Remove(currentTheme);
                    resources.MergedDictionaries.Add(newTheme);
                    UsingLightTheme = newTheme is Light;
                    IsThemeDirty = true;
                    return;
                }

                // Reset styles
                resources.Clear();
                resources.MergedDictionaries.Clear();

                // Variables
                resources.MergedDictionaries.Add(new Variables());

                // Theme
                resources.MergedDictionaries.Add(newTheme);
                UsingLightTheme = newTheme is Light;

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

        static ResourceDictionary CheckAndGetThemeForMergedDictionaries(Type themeType, ResourceDictionary resources)
        {
            return resources.MergedDictionaries.Any(rd => rd.GetType() == themeType)
                ? null
                : Activator.CreateInstance(themeType) as ResourceDictionary;
        }

        static ResourceDictionary NeedsThemeUpdate(string themeName, ResourceDictionary resources)
        {
            switch (themeName)
            {
                case "dark":
                    return CheckAndGetThemeForMergedDictionaries(typeof(Dark), resources);
                case "black":
                    return CheckAndGetThemeForMergedDictionaries(typeof(Black), resources);
                case "nord":
                    return CheckAndGetThemeForMergedDictionaries(typeof(Nord), resources);
                case "light":
                    return CheckAndGetThemeForMergedDictionaries(typeof(Light), resources);
                default:
                    if (OsDarkModeEnabled())
                    {
                        return CheckAndGetThemeForMergedDictionaries(typeof(Dark), resources);
                    }
                    return CheckAndGetThemeForMergedDictionaries(typeof(Light), resources);
            }
        }

        public static void SetTheme(ResourceDictionary resources)
        {
            SetThemeStyle(GetTheme(), resources);
        }

        public static string GetTheme()
        {
            var stateService = ServiceContainer.Resolve<IStateService>("stateService");
            return stateService.GetThemeAsync().GetAwaiter().GetResult();
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

        public static async Task UpdateThemeOnPagesAsync()
        {
            try
            {
                if (IsThemeDirty)
                {
                    IsThemeDirty = false;

                    await Application.Current.MainPage.TraverseNavigationRecursivelyAsync(async p =>
                    {
                        if (p is IThemeDirtablePage themeDirtablePage)
                        {
                            themeDirtablePage.IsThemeDirty = true;
                            if (p.IsVisible)
                            {
                                await themeDirtablePage.UpdateOnThemeChanged();
                            }
                        }
                    });
                }
            }
            catch (Exception ex)
            {
#if !FDROID
                Crashes.TrackError(ex);
#endif
            }
        }
    }
}
