using Bit.App.Services;
using Bit.App.Styles;
using Bit.Core;
using Xamarin.Forms;

namespace Bit.App.Utilities
{
    public static class ThemeManager
    {
        public static void SetThemeStyle(string name)
        {
            // Reset styles
            Application.Current.Resources.Clear();
            Application.Current.Resources.MergedDictionaries.Clear();

            // Variables
            Application.Current.Resources.MergedDictionaries.Add(new Variables());

            // Themed variables
            if(name == "dark")
            {
                Application.Current.Resources.MergedDictionaries.Add(new Dark());
            }
            else if(name == "black")
            {
                Application.Current.Resources.MergedDictionaries.Add(new Black());
            }
            else if(name == "nord")
            {
                Application.Current.Resources.MergedDictionaries.Add(new Nord());
            }
            else
            {
                Application.Current.Resources.MergedDictionaries.Add(new Light());
            }

            // Base styles
            Application.Current.Resources.MergedDictionaries.Add(new Base());

            // Platform styles
            if(Device.RuntimePlatform == Device.Android)
            {
                Application.Current.Resources.MergedDictionaries.Add(new Android());
            }
            else if(Device.RuntimePlatform == Device.iOS)
            {
                Application.Current.Resources.MergedDictionaries.Add(new iOS());
            }
        }

        public static void SetTheme(bool android)
        {
            SetThemeStyle(GetTheme(android));
        }

        public static string GetTheme(bool android)
        {
            return Xamarin.Essentials.Preferences.Get(
                string.Format(PreferencesStorageService.KeyFormat, Constants.ThemeKey), default(string),
                !android ? "group.com.8bit.bitwarden" : default(string));
        }
    }
}
