using System;
using System.Threading.Tasks;
using Bit.App.Models;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class LoginSsoPage : BaseContentPage
    {
        private readonly IVaultTimeoutService _vaultTimeoutService;
        private readonly LoginSsoPageViewModel _vm;
        private readonly AppOptions _appOptions;

        private AppOptions _appOptionsCopy;

        public LoginSsoPage(AppOptions appOptions = null)
        {
            _vaultTimeoutService = ServiceContainer.Resolve<IVaultTimeoutService>("vaultTimeoutService");
            _appOptions = appOptions;
            InitializeComponent();
            _vm = BindingContext as LoginSsoPageViewModel;
            _vm.Page = this;
            _vm.StartTwoFactorAction = () => Device.BeginInvokeOnMainThread(async () => await StartTwoFactorAsync());
            _vm.StartSetPasswordAction = () =>
                Device.BeginInvokeOnMainThread(async () => await StartSetPasswordAsync());
            _vm.SsoAuthSuccessAction = () => Device.BeginInvokeOnMainThread(async () => await SsoAuthSuccessAsync());
            _vm.UpdateTempPasswordAction =
                () => Device.BeginInvokeOnMainThread(async () => await UpdateTempPasswordAsync());
            _vm.StartDeviceApprovalOptionsAction =
                () => Device.BeginInvokeOnMainThread(async () => await StartDeviceApprovalOptionsAsync());
            _vm.CloseAction = async () =>
            {
                await Navigation.PopModalAsync();
            };
            if (Device.RuntimePlatform == Device.Android)
            {
                ToolbarItems.RemoveAt(0);
            }
        }

        protected override async void OnAppearing()
        {
            base.OnAppearing();
            await _vm.InitAsync();
            if (string.IsNullOrWhiteSpace(_vm.OrgIdentifier))
            {
                RequestFocus(_orgIdentifier);
            }
        }

        private void CopyAppOptions()
        {
            if (_appOptions != null)
            {
                // create an object copy of _appOptions to persist values when app is exited during web auth flow
                _appOptionsCopy = new AppOptions();
                _appOptionsCopy.SetAllFrom(_appOptions);
            }
        }

        private void RestoreAppOptionsFromCopy()
        {
            if (_appOptions != null)
            {
                // restore values to original readonly _appOptions object from copy
                _appOptions.SetAllFrom(_appOptionsCopy);
                _appOptionsCopy = null;
            }
        }

        private void LogIn_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                CopyAppOptions();
                _vm.LogInCommand.Execute(null);
            }
        }

        private void Close_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                _vm.CloseAction();
            }
        }

        private async Task StartTwoFactorAsync()
        {
            RestoreAppOptionsFromCopy();
            var page = new TwoFactorPage(true, _appOptions, _vm.OrgIdentifier);
            await Navigation.PushModalAsync(new NavigationPage(page));
        }

        private async Task StartSetPasswordAsync()
        {
            RestoreAppOptionsFromCopy();
            var page = new SetPasswordPage(_appOptions, _vm.OrgIdentifier);
            await Navigation.PushModalAsync(new NavigationPage(page));
        }

        private async Task UpdateTempPasswordAsync()
        {
            var page = new UpdateTempPasswordPage();
            await Navigation.PushModalAsync(new NavigationPage(page));
        }

        private async Task StartDeviceApprovalOptionsAsync()
        {
            var page = new LoginApproveDevicePage();
            await Navigation.PushModalAsync(new NavigationPage(page));
        }

        private async Task SsoAuthSuccessAsync()
        {
            RestoreAppOptionsFromCopy();
            await AppHelpers.ClearPreviousPage();

            if (await _vaultTimeoutService.IsLockedAsync())
            {
                Application.Current.MainPage = new NavigationPage(new LockPage(_appOptions));
            }
            else
            {
                Application.Current.MainPage = new TabsPage(_appOptions, null);
            }
        }
    }
}
