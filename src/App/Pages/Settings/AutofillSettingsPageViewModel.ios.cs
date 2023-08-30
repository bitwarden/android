using System.Windows.Input;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class AutofillSettingsPageViewModel
    {
        public bool SupportsiOSAutofill => Device.RuntimePlatform == Device.iOS && _deviceActionService.SupportsAutofillServices();

        public ICommand GoToPasswordAutofillCommand { get; private set; }
        public ICommand GoToAppExtensionCommand { get; private set; }

        private void InitIOSCommands()
        {
            GoToPasswordAutofillCommand = CreateDefaultAsyncCommnad(() => Page.Navigation.PushModalAsync(new NavigationPage(new AutofillPage())));
            GoToAppExtensionCommand = CreateDefaultAsyncCommnad(() => Page.Navigation.PushModalAsync(new NavigationPage(new ExtensionPage())));
        }
    }
}
