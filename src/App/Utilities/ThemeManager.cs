using System.Reflection;
using Xamarin.Forms;
using Xamarin.Forms.StyleSheets;

namespace Bit.App.Utilities
{
    public static class ThemeManager
    {
        public static void SetTheme(string name)
        {
            var themeFormat = "Bit.App.Css.{0}.css";
            var assembly = IntrospectionExtensions.GetTypeInfo(typeof(App)).Assembly;
            // Other supported theme names can be added here.
            if(name == "dark")
            {
                Application.Current.Resources.Add(StyleSheet.FromAssemblyResource(assembly,
                    string.Format(themeFormat, name)));
            }
            Application.Current.Resources.Add(StyleSheet.FromAssemblyResource(assembly,
                string.Format(themeFormat, Device.RuntimePlatform.ToLowerInvariant())));
            Application.Current.Resources.Add(StyleSheet.FromAssemblyResource(assembly,
                string.Format(themeFormat, "styles")));
        }
    }
}
