using Bit.App.Models;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Services;
using Bit.Core.Utilities;

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
            _vm.FromIosExtension = _appOptions?.IosExtension ?? false;
            _vm.StartTwoFactorAction = () => MainThread.BeginInvokeOnMainThread(async () => await StartTwoFactorAsync());
            _vm.StartSetPasswordAction = () =>
                MainThread.BeginInvokeOnMainThread(async () => await StartSetPasswordAsync());
            _vm.SsoAuthSuccessAction = () => MainThread.BeginInvokeOnMainThread(async () => await SsoAuthSuccessAsync());
            _vm.UpdateTempPasswordAction =
                () => MainThread.BeginInvokeOnMainThread(async () => await UpdateTempPasswordAsync());
            _vm.StartDeviceApprovalOptionsAction =
                () => MainThread.BeginInvokeOnMainThread(async () => await StartDeviceApprovalOptionsAsync());
            _vm.CloseAction = async () =>
            {
                await Navigation.PopModalAsync();
            };

            if (DeviceInfo.Platform == DevicePlatform.Android)
            {
                ToolbarItems.RemoveAt(0);
            }
        }

        protected override bool ShouldCheckToPreventOnNavigatedToCalledTwice => true;

        protected override async Task InitOnNavigatedToAsync()
        {
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
            try
            {
                RestoreAppOptionsFromCopy();
                var page = new TwoFactorPage(true, _appOptions, _vm.OrgIdentifier);
                await Navigation.PushModalAsync(new NavigationPage(page));
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
            }
        }

        private async Task StartSetPasswordAsync()
        {
            try
            {
                RestoreAppOptionsFromCopy();
                var page = new SetPasswordPage(_appOptions, _vm.OrgIdentifier);
                await Navigation.PushModalAsync(new NavigationPage(page));
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
            }
        }

        private async Task UpdateTempPasswordAsync()
        {
            var page = new UpdateTempPasswordPage();
            await Navigation.PushModalAsync(new NavigationPage(page));
        }

        private async Task StartDeviceApprovalOptionsAsync()
        {
            var page = new LoginApproveDevicePage(_appOptions);
            await Navigation.PushModalAsync(new NavigationPage(page));
        }

        private async Task SsoAuthSuccessAsync()
        {
            try
            {
                RestoreAppOptionsFromCopy();
                await AppHelpers.ClearPreviousPage();

                if (await _vaultTimeoutService.IsLockedAsync())
                {
                    App.MainPage = new NavigationPage(new LockPage(_appOptions));
                }
                else
                {
                    App.MainPage = new TabsPage(_appOptions, null);
                }
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
            }
        }
    }
}
