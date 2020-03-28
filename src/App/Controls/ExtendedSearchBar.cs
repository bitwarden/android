using Bit.App.Abstractions;
using Bit.Core.Utilities;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class ExtendedSearchBar : SearchBar
    {
        public ExtendedSearchBar()
        {
            if (Device.RuntimePlatform == Device.iOS)
            {
                var deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService", true);
                if (!deviceActionService?.UsingDarkTheme() ?? false)
                {
                    TextColor = Color.Black;
                }
            }
        }
    }
}
