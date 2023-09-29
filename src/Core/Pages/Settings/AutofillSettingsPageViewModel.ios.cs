using System.Windows.Input;
using Microsoft.Maui.Controls;
using Microsoft.Maui;

namespace Bit.App.Pages
{
    public partial class AutofillSettingsPageViewModel
    {
        public bool SupportsiOSAutofill => // TODO Xamarin.Forms.Device.RuntimePlatform is no longer supported. Use Microsoft.Maui.Devices.DeviceInfo.Platform instead. For more details see https://learn.microsoft.com/en-us/dotnet/maui/migration/forms-projects#device-changes
Device.RuntimePlatform == Device.iOS && _deviceActionService.SupportsAutofillServices();

        public ICommand GoToPasswordAutofillCommand { get; private set; }
        public ICommand GoToAppExtensionCommand { get; private set; }

        private void InitIOSCommands()
        {
            GoToPasswordAutofillCommand = CreateDefaultAsyncCommnad(() => Page.Navigation.PushModalAsync(new NavigationPage(new AutofillPage())));
            GoToAppExtensionCommand = CreateDefaultAsyncCommnad(() => Page.Navigation.PushModalAsync(new NavigationPage(new ExtensionPage())));
        }
    }
}
