using Bit.App.Styles;
using System;
using System.Reflection;
using Xamarin.Forms;
using Xamarin.Forms.StyleSheets;

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
    }
}
