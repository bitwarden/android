using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.App.Models;
using Bit.App.Utilities;
using Bit.Core.Enums;
using Bit.Core.Utilities;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class LoginApproveDevicePage : BaseContentPage
    {

        private readonly LoginApproveDeviceViewModel _vm;
        private readonly AppOptions _appOptions;

        public LoginApproveDevicePage(string email, AppOptions appOptions = null)
        {
            InitializeComponent();
            _vm = BindingContext as LoginApproveDeviceViewModel;
            _vm.LogInWithMasterPassword = () => StartLogInWithMasterPassword().FireAndForget();
            _vm.LogInWithDeviceAction = () => StartLoginWithDeviceAsync().FireAndForget();
            _vm.RequestAdminApprovalAction = () => RequestAdminApprovalAsync().FireAndForget();
            _vm.CloseAction = () => { Navigation.PopModalAsync(); };
            _vm.Page = this;
            _vm.Email = email;
            _appOptions = appOptions;
        }

        protected override void OnAppearing()
        {
            _vm.InitAsync();
        }

        private void Cancel_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                _vm.CloseAction();
            }
        }

        private async Task StartLogInWithMasterPassword()
        {
            var page = new TwoFactorPage(false, _appOptions);
            await Navigation.PushModalAsync(new NavigationPage(page));
        }

        private async Task StartLoginWithDeviceAsync()
        {
            var page = new LoginPasswordlessRequestPage(_vm.Email, AuthRequestType.AuthenticateAndUnlock, _appOptions);
            await Navigation.PushModalAsync(new NavigationPage(page));
        }

        private async Task RequestAdminApprovalAsync()
        {
            var page = new LoginPasswordlessRequestPage(_vm.Email, AuthRequestType.AdminApproval, _appOptions);
            await Navigation.PushModalAsync(new NavigationPage(page));
        }
    }
}

