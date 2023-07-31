using Bit.App.Utilities;
using Microsoft.Maui.Graphics;
using Microsoft.Maui.Controls;
using Microsoft.Maui;

namespace Bit.App.Controls
{
    public class ExtendedSearchBar : SearchBar
    {
        public ExtendedSearchBar()
        {
            // TODO Xamarin.Forms.Device.RuntimePlatform is no longer supported. Use Microsoft.Maui.Devices.DeviceInfo.Platform instead. For more details see https://learn.microsoft.com/en-us/dotnet/maui/migration/forms-projects#device-changes
            if (Device.RuntimePlatform == Device.iOS)
            {
                if (ThemeManager.UsingLightTheme)
                {
                    TextColor = Colors.Black;
                }
            }
        }
    }
}
