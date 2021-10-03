using Android.Graphics;
using Bit.App.Utilities;
using Xamarin.Forms.Platform.Android;

namespace Bit.Droid.Utilities
{
    public class ThemeHelpers
    {
        public static Color NavBarBackgroundColor
        {
            get => ThemeManager.GetResourceColor("NavigationBarBackgroundColor").ToAndroid();
        }
        public static Color FabColor
        {
            get => ThemeManager.GetResourceColor("FabColor").ToAndroid();
        }
    }
}
