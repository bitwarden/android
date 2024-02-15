using System;
using System.Linq;
using System.Threading.Tasks;
using Bit.App.Models;
using Bit.App.Styles;
using Bit.Core.Abstractions;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Microsoft.Maui.ApplicationModel;
using Microsoft.Maui.Controls;
using Microsoft.Maui;
#if IOS
using Foundation;
using UIKit;
#endif

namespace Bit.App.Utilities
{
    public static class ThemeManager
    {
        public const string UPDATED_THEME_MESSAGE_KEY = "updatedTheme";

        public static bool UsingLightTheme = true;
        public static Func<ResourceDictionary> Resources = () => null;

        public static bool IsThemeDirty = false;

        public const string Light = "light";
        public const string Dark = "dark";
        public const string Black = "black";
        public const string Nord = "nord";
        public const string SolarizedDark = "solarizeddark";

        public static void SetThemeStyle(string name, string autoDarkName, ResourceDictionary resources)
        {
            try
            {
                Resources = () => resources;

                var newTheme = NeedsThemeUpdate(name, autoDarkName, resources);
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
                resources.MergedDictionaries.Add(new ControlTemplates());

                // Platform styles
                if (DeviceInfo.Platform == DevicePlatform.Android)
                {
                    resources.MergedDictionaries.Add(new Styles.Android());
                }
                else if (DeviceInfo.Platform == DevicePlatform.iOS)
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
                LoggerHelper.LogEvenIfCantBeResolved(ex);
            }
        }

        static ResourceDictionary CheckAndGetThemeForMergedDictionaries(Type themeType, ResourceDictionary resources)
        {
            return resources.MergedDictionaries.Any(rd => rd.GetType() == themeType)
                ? null
                : Activator.CreateInstance(themeType) as ResourceDictionary;
        }

        static ResourceDictionary NeedsThemeUpdate(string themeName, string autoDarkThemeName, ResourceDictionary resources)
        {
            switch (themeName)
            {
                case Dark:
                    return CheckAndGetThemeForMergedDictionaries(typeof(Dark), resources);
                case Black:
                    return CheckAndGetThemeForMergedDictionaries(typeof(Black), resources);
                case Nord:
                    return CheckAndGetThemeForMergedDictionaries(typeof(Nord), resources);
                case Light:
                    return CheckAndGetThemeForMergedDictionaries(typeof(Light), resources);
                case SolarizedDark:
                    return CheckAndGetThemeForMergedDictionaries(typeof(SolarizedDark), resources);
                default:
                    if (OsDarkModeEnabled())
                    {
                        switch (autoDarkThemeName)
                        {
                            case Black:
                                return CheckAndGetThemeForMergedDictionaries(typeof(Black), resources);
                            case Nord:
                                return CheckAndGetThemeForMergedDictionaries(typeof(Nord), resources);
                            case SolarizedDark:
                                return CheckAndGetThemeForMergedDictionaries(typeof(SolarizedDark), resources);
                            default:
                                return CheckAndGetThemeForMergedDictionaries(typeof(Dark), resources);
                        }
                    }
                    return CheckAndGetThemeForMergedDictionaries(typeof(Light), resources);
            }
        }

        public static void SetTheme(ResourceDictionary resources)
        {
            SetThemeStyle(GetTheme(), GetAutoDarkTheme(), resources);
        }

        public static string GetTheme()
        {
            var stateService = ServiceContainer.Resolve<IStateService>("stateService");
            return stateService.GetThemeAsync().GetAwaiter().GetResult();
        }

        public static string GetAutoDarkTheme()
        {
            var stateService = ServiceContainer.Resolve<IStateService>("stateService");
            return stateService.GetAutoDarkThemeAsync().GetAwaiter().GetResult();
        }

        //HACK: OsDarkModeEnabled() is divided into Android and iOS implementations due to a MAUI bug.
        // Currently on iOS when resuming the app after showing a System "Share/Sheet" (or other similar UI)
        // MAUI reports the incorrect Theme. To avoid this we are fetching the current OS Theme directly on iOS from the iOS API.
        // MAUI Issue: https://github.com/dotnet/maui/issues/19614
        public static bool OsDarkModeEnabled()
        {
#if UT
            return false;
#else

#if ANDROID
            return Application.Current.RequestedTheme == AppTheme.Dark;
#else
            var requestedTheme = AppTheme.Unspecified;
            if (!OperatingSystem.IsIOSVersionAtLeast(13, 0))
                return false;

            var traits = InvokeOnMainThread(() => WindowStateManager.Default.GetCurrentUIViewController()?.TraitCollection) ?? UITraitCollection.CurrentTraitCollection;
            var uiStyle = traits.UserInterfaceStyle;

            requestedTheme = uiStyle switch
            {
                UIUserInterfaceStyle.Light => AppTheme.Light,
                UIUserInterfaceStyle.Dark => AppTheme.Dark,
                _ => AppTheme.Unspecified
            };
            return requestedTheme == AppTheme.Dark;
#endif

#endif
        }

#if IOS
        private static T InvokeOnMainThread<T>(Func<T> factory)
        {
            T value = default;
            NSRunLoop.Main.InvokeOnMainThread(() => value = factory());
            return value;
        }
#endif

        public static void ApplyResourcesTo(VisualElement element)
        {
            foreach (var resourceDict in Resources().MergedDictionaries)
            {
                element.Resources.Add(resourceDict);
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
                LoggerHelper.LogEvenIfCantBeResolved(ex);
            }
        }
    }
}
