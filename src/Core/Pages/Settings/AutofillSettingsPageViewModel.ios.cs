using System.Windows.Input;

namespace Bit.App.Pages
{
    public partial class AutofillSettingsPageViewModel
    {
        public bool SupportsiOSAutofill => DeviceInfo.Platform == DevicePlatform.iOS && _deviceActionService.SupportsAutofillServices();

        public ICommand GoToPasswordAutofillCommand { get; private set; }
        public ICommand GoToAppExtensionCommand { get; private set; }

        private void InitIOSCommands()
        {
            GoToPasswordAutofillCommand = CreateDefaultAsyncRelayCommand(() => Page.Navigation.PushModalAsync(new NavigationPage(new AutofillPage())), allowsMultipleExecutions: false);
            GoToAppExtensionCommand = CreateDefaultAsyncRelayCommand(() => Page.Navigation.PushModalAsync(new NavigationPage(new ExtensionPage())), allowsMultipleExecutions: false);
        }
    }
}
