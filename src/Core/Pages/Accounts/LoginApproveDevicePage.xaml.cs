using Bit.App.Models;
using Bit.App.Utilities;
using Bit.Core.Enums;
using Bit.Core.Utilities;

namespace Bit.App.Pages
{
    public partial class LoginApproveDevicePage : BaseContentPage
    {
        private readonly LoginApproveDeviceViewModel _vm;
        private readonly AppOptions _appOptions;

        public LoginApproveDevicePage(AppOptions appOptions = null)
        {
            InitializeComponent();
            _vm = BindingContext as LoginApproveDeviceViewModel;
            _vm.LogInWithMasterPasswordAction = () => StartLogInWithMasterPasswordAsync().FireAndForget();
            _vm.LogInWithDeviceAction = () => StartLoginWithDeviceAsync().FireAndForget();
            _vm.RequestAdminApprovalAction = () => RequestAdminApprovalAsync().FireAndForget();
            _vm.ContinueToVaultAction = () => ContinueToVaultAsync().FireAndForget();
            _vm.Page = this;
            _appOptions = appOptions;
        }

        protected override bool ShouldCheckToPreventOnNavigatedToCalledTwice => true;

        protected override async Task InitOnNavigatedToAsync()
        {
            await _vm.InitAsync();
        }

        private async Task ContinueToVaultAsync()
        {
            if (AppHelpers.SetAlternateMainPage(_appOptions))
            {
                return;
            }
            
            if (_appOptions != null)
            {
                _appOptions.HasJustLoggedInOrUnlocked = true;
            }
            var previousPage = await AppHelpers.ClearPreviousPage();
            App.MainPage = new TabsPage(_appOptions, previousPage);
        }

        private async Task StartLogInWithMasterPasswordAsync()
        {
            var page = new LockPage(_appOptions, checkPendingAuthRequests: false);
            await Navigation.PushModalAsync(new NavigationPage(page));
        }

        private async Task StartLoginWithDeviceAsync()
        {
            var page = new LoginPasswordlessRequestPage(_vm.Email, AuthRequestType.AuthenticateAndUnlock, _appOptions, true);
            await Navigation.PushModalAsync(new NavigationPage(page));
        }

        private async Task RequestAdminApprovalAsync()
        {
            var page = new LoginPasswordlessRequestPage(_vm.Email, AuthRequestType.AdminApproval, _appOptions, true);
            await Navigation.PushModalAsync(new NavigationPage(page));
        }
    }
}
