using Microsoft.Maui.Controls;
using Microsoft.Maui;

namespace Bit.App.Controls
{
    public class MiButton : Button
    {
        public MiButton()
        {
            Padding = 0;
            // TODO Xamarin.Forms.Device.RuntimePlatform is no longer supported. Use Microsoft.Maui.Devices.DeviceInfo.Platform instead. For more details see https://learn.microsoft.com/en-us/dotnet/maui/migration/forms-projects#device-changes
            switch (Device.RuntimePlatform)
            {
                case Device.iOS:
                    FontFamily = "Material Icons";
                    break;
                case Device.Android:
                    FontFamily = "MaterialIcons_Regular.ttf#Material Icons";
                    break;
            }
        }
    }
}
