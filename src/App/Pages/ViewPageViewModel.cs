using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Exceptions;
using Bit.Core.Utilities;
using System.Threading.Tasks;
using System.Windows.Input;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class ViewPageViewModel : BaseViewModel
    {
        private readonly IDeviceActionService _deviceActionService;
        private readonly IAuthService _authService;
        private readonly ISyncService _syncService;

        public ViewPageViewModel()
        {
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _authService = ServiceContainer.Resolve<IAuthService>("authService");
            _syncService = ServiceContainer.Resolve<ISyncService>("syncService");

            PageTitle = AppResources.Bitwarden;
            ShowPasswordCommand = new Command(() =>
                Page.DisplayAlert("Button 1 Command", "Button 1 message", "Cancel"));
        }

        public ICommand ShowPasswordCommand { get; }
        public string Email { get; set; }
        public string MasterPassword { get; set; }
    }
}
