using System.Threading.Tasks;
using Acr.UserDialogs;
using Bit.App.Controls;
using Bit.App.Resources;
using Xamarin.Forms;
using XLabs.Ioc;
using Bit.App.Abstractions;

namespace Bit.App.Pages
{
    public class BaseLockPage : ExtendedContentPage
    {
        private readonly IDeviceActionService _deviceActionService;

        public BaseLockPage()
            : base(false, false)
        {
            AuthService = Resolver.Resolve<IAuthService>();
            _deviceActionService = Resolver.Resolve<IDeviceActionService>();
        }

        protected IAuthService AuthService { get; set; }

        protected override bool OnBackButtonPressed()
        {
            _deviceActionService.Background();
            return true;
        }

        protected async Task LogoutAsync()
        {
            var confirmed = await DisplayAlert(null, AppResources.LogoutConfirmation, AppResources.Yes, AppResources.Cancel);
            if(!confirmed)
            {
                return;
            }
            AuthService.LogOut();
        }
    }
}
